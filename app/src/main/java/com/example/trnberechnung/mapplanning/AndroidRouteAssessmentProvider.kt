package com.example.trnberechnung.mapplanning

import com.example.trnberechnung.dto.WeatherDto
import com.example.trnberechnung.model.TideEvent
import com.example.trnberechnung.model.TideStationData
import com.example.trnberechnung.routing.v2.SeaMask
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlinx.coroutines.flow.StateFlow

fun interface TideStationSnapshotProvider {
    fun currentStations(): List<TideStationData>
}

fun interface ChartDepthProvider {
    fun depthMetersAt(point: GeoPoint): Double?
}

object SeaMaskChartDepthProvider : ChartDepthProvider {
    override fun depthMetersAt(point: GeoPoint): Double? =
        if (SeaMask.ready) {
            SeaMask.depthAtLatLng(point.latitude, point.longitude)
        } else {
            null
        }
}

/**
 * Bridges the live BSH/DWD snapshot already owned by `TideViewModel` into the
 * deterministic map-planning domain. Construct it with
 * `AndroidRouteAssessmentProvider(tideViewModel.allStations)`.
 */
class AndroidRouteAssessmentProvider(
    private val stationSnapshotProvider: TideStationSnapshotProvider,
    private val sampleSpacingMeters: Double = 100.0,
    private val chartDepthProvider: ChartDepthProvider = SeaMaskChartDepthProvider,
    private val fairwayRouteResolver: FairwayRouteResolver? = null,
) : RouteAssessmentProvider {
    constructor(
        stations: StateFlow<List<TideStationData>>,
        sampleSpacingMeters: Double = 100.0,
        chartDepthProvider: ChartDepthProvider = SeaMaskChartDepthProvider,
        fairwayRouteResolver: FairwayRouteResolver? = null,
    ) : this(
        stationSnapshotProvider = TideStationSnapshotProvider { stations.value },
        sampleSpacingMeters = sampleSpacingMeters,
        chartDepthProvider = chartDepthProvider,
        fairwayRouteResolver = fairwayRouteResolver,
    )

    init {
        require(sampleSpacingMeters > 0) { "Der Punktabstand muss positiv sein." }
    }

    override suspend fun assess(input: RouteAssessmentInput): RouteSafetyAssessment {
        val stations = preparedStations()
        if (stations.isEmpty()) {
            return incompleteAssessment(
                input,
                "Es liegen noch keine BSH- und Wetterdaten vor.",
            )
        }

        val samples =
            when (val fairway = fairwayRouteResolver?.resolve(input.request)) {
                null -> sampleRoute(input.routeGeometry)
                is FairwayRouteResult.Incomplete ->
                    return incompleteAssessment(input, fairway.reason)
                is FairwayRouteResult.Success -> fairwaySamples(fairway.waypoints)
            }
        if (samples.isEmpty()) {
            return incompleteAssessment(
                input,
                "Die Routengeometrie enthält keine auswertbaren Wegpunkte.",
            )
        }

        val clearances = mutableListOf<ClearanceSample>()
        val weather = mutableListOf<MarineWeatherAssessment?>()
        var allSamplesValid = true

        for (sample in samples) {
            kotlinx.coroutines.yield()
            val arrival =
                input.request.departure.plus(
                    Duration.ofSeconds(
                        (sample.cumulativeDistanceNm / input.request.boatSettings.speedKnots * 3_600)
                            .toLong(),
                    ),
                )
            val station =
                stations.minByOrNull {
                    RouteMetricsCalculator.haversineNm(sample.point, it.coordinate)
                }
            val tideHeight = station?.tideHeightAt(arrival)

            // Sicherheit geht vor: Wir nehmen die geringere Tiefe aus Katalog und SeaMask.
            val maskDepth = chartDepthProvider.depthMetersAt(sample.point)
            var catalogDepth = sample.chartDepthMeters

            if (catalogDepth == null) {
                catalogDepth = HarbourCatalog.all.find {
                    RouteMetricsCalculator.haversineNm(it.coordinate, sample.point) < 0.1
                }?.chartDepthMeters
            }

            var chartDepth = when {
                catalogDepth != null && maskDepth != null -> kotlin.math.min(catalogDepth, maskDepth)
                catalogDepth != null -> catalogDepth
                else -> maskDepth
            }

            // Watt-Depth-Sanitizer: In bekannten Flachwassergebieten (Ostfriesland)
            // sind Tiefen über 0,5m ohne expliziten Katalog-Eintrag im Watt verdächtig.
            if (sample.point.latitude in 53.65..53.85 &&
                sample.point.longitude in 6.7..8.5 &&
                !sample.hasCatalogDepth) {
                if (chartDepth == null || chartDepth > 0.5) {
                    chartDepth = -1.5 // Extrem konservativ für unbekannte Wattflächen
                }
            }

            val clearance = tideHeight?.let { height ->
                chartDepth?.let { depth ->
                    depth + height - input.request.boatSettings.draftMeters
                }
            }

            val sampleValid = station != null && tideHeight != null && chartDepth != null
            if (!sampleValid) allSamplesValid = false

            clearances +=
                ClearanceSample(
                    waypointName = sample.name ?: station?.source?.gaugeLabel ?: station?.source?.area ?: "Route",
                    clearanceMeters = clearance,
                    isValid = sampleValid,
                    waterLevelQuality =
                        if (station != null && tideHeight != null) {
                            WaterLevelQuality.LOCAL_OFFICIAL
                        } else {
                            WaterLevelQuality.UNAVAILABLE
                        },
                    anchoredHighWater = station?.nearestHighWater(arrival),
                )
            weather += station?.weatherAt(arrival)
        }

        val maxWind = weather.filterNotNull().maxByOrNull { it.windKnots }?.windKnots
        val maxGust = weather.filterNotNull().maxByOrNull { it.gustKnots }?.gustKnots

        return RouteSafetyAssessment(
            expectedWaypointCount = samples.size,
            clearanceSamples = clearances,
            allLegsValid = allSamplesValid,
            weatherStatus = WeatherSafetyEvaluator.evaluateAll(weather),
            maxWindKnots = maxWind,
            maxGustKnots = maxGust,
            messages =
                buildList {
                    if (clearances.any { it.clearanceMeters == null }) {
                        add("Für mindestens einen Routenpunkt fehlen Tiefen- oder Gezeitendaten.")
                    }
                    if (weather.any { it == null }) {
                        add("Für mindestens einen Routenpunkt fehlen Wetterdaten im ±90-Minuten-Fenster.")
                    }
                },
        )
    }

    @Volatile
    private var cachedSource: List<TideStationData>? = null

    @Volatile
    private var cachedPrepared: List<PreparedStation> = emptyList()

    private fun preparedStations(): List<PreparedStation> {
        val source = stationSnapshotProvider.currentStations()
        if (source === cachedSource) return cachedPrepared
        return synchronized(this) {
            if (source !== cachedSource) {
                cachedSource = source
                cachedPrepared = source.map(::prepareStation)
            }
            cachedPrepared
        }
    }

    private fun prepareStation(station: TideStationData): PreparedStation =
        PreparedStation(
            source = station,
            coordinate = GeoPoint(station.latitude, station.longitude),
            tideEvents =
                station.events.mapNotNull { event ->
                    val instant = parseInstant(event.timestamp) ?: return@mapNotNull null
                    val value = event.value ?: return@mapNotNull null
                    PreparedTideEvent(instant, event.type.uppercase(), value)
                }.sortedBy(PreparedTideEvent::instant),
            weather =
                station.weatherForecast.mapNotNull { forecast ->
                    val instant = parseInstant(forecast.timestamp) ?: return@mapNotNull null
                    PreparedWeather(instant, forecast)
                }.sortedBy(PreparedWeather::instant),
        )

    private fun sampleRoute(route: List<GeoPoint>): List<RouteSample> {
        if (route.size < 2) return emptyList()
        val result = mutableListOf(RouteSample(route.first(), 0.0, name = "Start"))
        var cumulativeDistanceNm = 0.0
        for ((start, end) in route.zipWithNext()) {
            val legDistanceNm = RouteMetricsCalculator.haversineNm(start, end)
            val steps =
                ceil(legDistanceNm * METERS_PER_NAUTICAL_MILE / sampleSpacingMeters)
                    .toInt()
                    .coerceAtLeast(1)
            for (step in 1..steps) {
                val fraction = step.toDouble() / steps
                val point =
                    GeoPoint(
                        latitude = start.latitude + (end.latitude - start.latitude) * fraction,
                        longitude = start.longitude + (end.longitude - start.longitude) * fraction,
                    )
                result +=
                    RouteSample(
                        point = point,
                        cumulativeDistanceNm =
                            cumulativeDistanceNm + legDistanceNm * fraction,
                        name = if (step == steps) "Wegpunkt" else null
                    )
            }
            cumulativeDistanceNm += legDistanceNm
        }
        return result
    }

    private fun fairwaySamples(waypoints: List<RouteSafetyWaypoint>): List<RouteSample> {
        if (waypoints.size < 2) return emptyList()
        val result = mutableListOf<RouteSample>()
        var cumulativeDistanceNm = 0.0

        // Startpunkt hinzufügen
        result +=
            RouteSample(
                point = waypoints.first().coordinate,
                cumulativeDistanceNm = 0.0,
                chartDepthMeters = waypoints.first().chartDepthMeters,
                hasCatalogDepth = true,
                name = waypoints.first().name,
            )

        for (i in 0 until waypoints.size - 1) {
            val start = waypoints[i]
            val end = waypoints[i + 1]
            val legDistanceNm = RouteMetricsCalculator.haversineNm(start.coordinate, end.coordinate)

            val steps =
                ceil(legDistanceNm * METERS_PER_NAUTICAL_MILE / sampleSpacingMeters)
                    .toInt()
                    .coerceAtLeast(1)

            for (step in 1..steps) {
                val fraction = step.toDouble() / steps
                val point =
                    GeoPoint(
                        latitude = start.coordinate.latitude + (end.coordinate.latitude - start.coordinate.latitude) * fraction,
                        longitude = start.coordinate.longitude + (end.coordinate.longitude - start.coordinate.longitude) * fraction,
                    )

                val startDepth = start.chartDepthMeters
                val endDepth = end.chartDepthMeters
                val interpolatedDepth =
                    if (startDepth != null && endDepth != null) {
                        startDepth + (endDepth - startDepth) * fraction
                    } else {
                        startDepth ?: endDepth
                    }

                result +=
                    RouteSample(
                        point = point,
                        cumulativeDistanceNm = cumulativeDistanceNm + legDistanceNm * fraction,
                        chartDepthMeters = interpolatedDepth,
                        hasCatalogDepth = true,
                        name = if (step == steps) end.name else null
                    )
            }
            cumulativeDistanceNm += legDistanceNm
        }
        return result
    }

    private fun incompleteAssessment(
        input: RouteAssessmentInput,
        message: String,
    ): RouteSafetyAssessment =
        RouteSafetyAssessment(
            expectedWaypointCount = input.request.harbourChain.size,
            clearanceSamples = emptyList(),
            allLegsValid = false,
            weatherStatus = WeatherStatus.UNVOLLSTAENDIG,
            messages = listOf(message),
        )

    private data class RouteSample(
        val point: GeoPoint,
        val cumulativeDistanceNm: Double,
        val chartDepthMeters: Double? = null,
        val hasCatalogDepth: Boolean = false,
        val name: String? = null,
    )

    private data class PreparedTideEvent(
        val instant: Instant,
        val type: String,
        val heightMeters: Double,
    )

    private data class PreparedWeather(
        val instant: Instant,
        val forecast: WeatherDto,
    )

    private data class PreparedStation(
        val source: TideStationData,
        val coordinate: GeoPoint,
        val tideEvents: List<PreparedTideEvent>,
        val weather: List<PreparedWeather>,
    ) {
        fun tideHeightAt(arrival: ZonedDateTime): Double? {
            val target = arrival.toInstant()
            val exact = tideEvents.firstOrNull { it.instant == target }
            if (exact != null) return exact.heightMeters
            val previous = tideEvents.lastOrNull { it.instant < target } ?: return null
            val next = tideEvents.firstOrNull { it.instant > target } ?: return null

            // Sicherheitsprüfung: Verhindere Interpolation über große Datenlücken (> 7h)
            // oder zwischen gleichen Event-Typen (z.B. HW zu HW), da dies auf fehlende
            // Datenpunkte (wie das Niedrigwasser) hindeutet.
            val gap = Duration.between(previous.instant, next.instant)
            if (gap.toHours() > 7 || previous.type == next.type) {
                return null
            }

            // ZUSATZ: Strenge Prüfung für Watt-Stationen. Wenn wir HW -> HW interpolieren
            // ohne ein NW-Event in der Liste zu haben, ist die Kurve zu unsicher.
            val hasLowWater = tideEvents.any { it.type == "NW" }
            if (!hasLowWater && gap.toHours() > 4) return null

            return interpolateByTwelfths(
                start = previous,
                end = next,
                target = target,
            )
        }

        fun minTideHeightBetween(
            start: ZonedDateTime,
            end: ZonedDateTime,
        ): Double? {
            val startInstant = start.toInstant()
            val endInstant = end.toInstant()
            if (tideEvents.isEmpty()) return null

            // Die Randhöhen müssen valide sein (keine Lücke an den Rändern)
            val hStart = tideHeightAt(start) ?: return null
            val hEnd = tideHeightAt(end) ?: return null

            val eventsInRange = tideEvents.filter {
                it.instant.isAfter(startInstant) && it.instant.isBefore(endInstant)
            }

            // Prüfe auf Lücken zwischen aufeinanderfolgenden Ereignissen im Zeitfenster
            val allPoints = mutableListOf<PreparedTideEvent>()
            allPoints.add(PreparedTideEvent(startInstant, "BOUNDARY", hStart))
            allPoints.addAll(eventsInRange)
            allPoints.add(PreparedTideEvent(endInstant, "BOUNDARY", hEnd))

            for ((p1, p2) in allPoints.zipWithNext()) {
                val gap = Duration.between(p1.instant, p2.instant)
                if (gap.toHours() > 7 || (p1.type != "BOUNDARY" && p2.type != "BOUNDARY" && p1.type == p2.type)) {
                    return null
                }
            }

            val eventHeights = eventsInRange.map { it.heightMeters }
            return (listOf(hStart, hEnd) + eventHeights).minOrNull()
        }

        fun nearestHighWater(arrival: ZonedDateTime): ZonedDateTime? =
            tideEvents.filter { it.type == "HW" || it.type == "HIGH" }
                .minByOrNull {
                    kotlin.math.abs(
                        Duration.between(arrival.toInstant(), it.instant).toMinutes(),
                    )
                }
                ?.instant
                ?.atZone(MAP_PLANNING_ZONE_ID)

        fun weatherAt(arrival: ZonedDateTime): MarineWeatherAssessment? {
            val nearest =
                weather.minByOrNull {
                    kotlin.math.abs(
                        Duration.between(arrival.toInstant(), it.instant).toMinutes(),
                    )
                } ?: return null
            if (kotlin.math.abs(
                    Duration.between(arrival.toInstant(), nearest.instant).toMinutes(),
                ) > 90
            ) {
                return null
            }

            val windKilometersPerHour = nearest.forecast.windSpeed ?: return null
            val visibilityMeters = nearest.forecast.visibility ?: return null
            return MarineWeatherAssessment(
                windKnots = windKilometersPerHour / KILOMETERS_PER_HOUR_PER_KNOT,
                gustKnots =
                    (nearest.forecast.windGustSpeed ?: windKilometersPerHour) /
                        KILOMETERS_PER_HOUR_PER_KNOT,
                visibilityKilometers = visibilityMeters / 1_000.0,
                precipitationChancePercent =
                    nearest.forecast.precipitationProbability?.toDouble() ?: 0.0,
                precipitationMillimeters = nearest.forecast.precipitation ?: 0.0,
            )
        }
    }

    companion object {
        private const val METERS_PER_NAUTICAL_MILE = 1_852.0
        private const val KILOMETERS_PER_HOUR_PER_KNOT = 1.852

        private fun parseInstant(rawValue: String?): Instant? {
            val raw = rawValue?.trim()?.takeIf(String::isNotEmpty) ?: return null
            val isoCandidate = raw.replace(' ', 'T')
            return runCatching { OffsetDateTime.parse(isoCandidate).toInstant() }.getOrNull()
                ?: runCatching { Instant.parse(raw) }.getOrNull()
                ?: runCatching {
                    LocalDateTime.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        .atZone(MAP_PLANNING_ZONE_ID)
                        .toInstant()
                }.getOrNull()
                ?: runCatching {
                    LocalDateTime.parse(raw, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        .atZone(MAP_PLANNING_ZONE_ID)
                        .toInstant()
                }.getOrNull()
        }

        private fun interpolateByTwelfths(
            start: PreparedTideEvent,
            end: PreparedTideEvent,
            target: Instant,
        ): Double {
            val totalMillis = Duration.between(start.instant, end.instant).toMillis().toDouble()
            if (totalMillis <= 0) return end.heightMeters
            val elapsedMillis =
                Duration.between(start.instant, target).toMillis().toDouble()
                    .coerceIn(0.0, totalMillis)
            val phase = elapsedMillis / totalMillis
            val segmentLength = 1.0 / 6.0
            val twelfths = doubleArrayOf(1.0, 2.0, 3.0, 3.0, 2.0, 1.0)
            val segment = (phase / segmentLength).toInt().coerceIn(0, 5)
            val fractionInSegment = (phase - segment * segmentLength) / segmentLength
            var accumulatedTwelfths = 0.0
            for (index in 0 until segment) {
                accumulatedTwelfths += twelfths[index]
            }
            accumulatedTwelfths += twelfths[segment] * fractionInSegment
            return start.heightMeters +
                (end.heightMeters - start.heightMeters) * (accumulatedTwelfths / 12.0)
        }
    }
}
