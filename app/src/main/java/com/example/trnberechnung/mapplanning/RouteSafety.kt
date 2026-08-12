package com.example.trnberechnung.mapplanning

import java.time.ZonedDateTime

data class MarineWeatherAssessment(
    val windKnots: Double,
    val gustKnots: Double,
    val visibilityKilometers: Double,
    val precipitationChancePercent: Double,
    val precipitationMillimeters: Double,
)

object WeatherSafetyEvaluator {
    fun evaluate(assessment: MarineWeatherAssessment?): WeatherStatus {
        if (assessment == null) return WeatherStatus.UNVOLLSTAENDIG

        return when {
            assessment.windKnots >= 28 ||
                assessment.gustKnots >= 34 ||
                assessment.visibilityKilometers < 1 ->
                WeatherStatus.NICHT_BEFAHRBAR

            assessment.windKnots >= 20 ||
                assessment.gustKnots >= 27 ||
                assessment.visibilityKilometers < 5 ||
                assessment.precipitationChancePercent >= 60 ||
                assessment.precipitationMillimeters >= 3 ->
                WeatherStatus.EINGESCHRAENKT

            else -> WeatherStatus.BEFAHRBAR
        }
    }

    /**
     * Selects the nearest hourly sample for every arrival time. Every waypoint
     * must have a sample no farther than 90 minutes away.
     */
    fun evaluateRoute(
        waypointArrivals: List<ZonedDateTime>,
        hourlyAssessments: List<Pair<ZonedDateTime, MarineWeatherAssessment>>,
    ): WeatherStatus {
        if (waypointArrivals.isEmpty()) return WeatherStatus.UNVOLLSTAENDIG

        val statuses =
            waypointArrivals.map { arrival ->
                val nearest =
                    hourlyAssessments.minByOrNull { (time) ->
                        kotlin.math.abs(
                            java.time.Duration.between(arrival.toInstant(), time.toInstant())
                                .toMinutes(),
                        )
                    }
                val differenceMinutes =
                    nearest?.let { (time) ->
                        kotlin.math.abs(
                            java.time.Duration.between(arrival.toInstant(), time.toInstant())
                                .toMinutes(),
                        )
                    }
                if (nearest == null || differenceMinutes == null || differenceMinutes > 90) {
                    WeatherStatus.UNVOLLSTAENDIG
                } else {
                    evaluate(nearest.second)
                }
            }

        return statuses.reduce(::combineWeatherStatus)
    }

    fun evaluateAll(assessments: List<MarineWeatherAssessment?>): WeatherStatus {
        if (assessments.isEmpty()) return WeatherStatus.UNVOLLSTAENDIG
        return assessments.map(::evaluate).reduce(::combineWeatherStatus)
    }

    private fun combineWeatherStatus(
        first: WeatherStatus,
        second: WeatherStatus,
    ): WeatherStatus =
        listOf(first, second).maxBy(WeatherStatus::precedence)
}

data class ClearanceSample(
    val waypointName: String,
    val clearanceMeters: Double?,
    val isValid: Boolean = true,
    val waterLevelQuality: WaterLevelQuality = WaterLevelQuality.LOCAL_OFFICIAL,
    val anchoredHighWater: ZonedDateTime? = null,
)

object UnderKeelSafetyEvaluator {
    fun evaluate(
        samples: List<ClearanceSample>,
        safetyMarginMeters: Double,
        allLegsValid: Boolean = true,
    ): RouteStatus {
        require(safetyMarginMeters >= 0) {
            "Der Sicherheitsabstand darf nicht negativ sein."
        }
        if (samples.isEmpty()) return RouteStatus.UNVOLLSTAENDIG

        val statuses =
            samples.map { sample ->
                when {
                    sample.clearanceMeters != null && sample.clearanceMeters < 0 ->
                        RouteStatus.NICHT_BEFAHRBAR

                    !sample.isValid || sample.clearanceMeters == null ->
                        RouteStatus.UNVOLLSTAENDIG

                    sample.waterLevelQuality == WaterLevelQuality.STALE ||
                        sample.waterLevelQuality == WaterLevelQuality.OUTSIDE_FORECAST_HORIZON ||
                        sample.waterLevelQuality == WaterLevelQuality.UNAVAILABLE ->
                        RouteStatus.UNVOLLSTAENDIG

                    sample.clearanceMeters < safetyMarginMeters ||
                        sample.waterLevelQuality == WaterLevelQuality.MANUAL ||
                        sample.waterLevelQuality == WaterLevelQuality.CONFIRMED_COMPARISON ->
                        RouteStatus.EINGESCHRAENKT

                    else -> RouteStatus.BEFAHRBAR
                }
            }.toMutableList()

        if (!allLegsValid) statuses += RouteStatus.UNVOLLSTAENDIG
        return statuses.maxBy(RouteStatus::precedence)
    }
}

object RouteStatusEvaluator {
    fun combine(
        tidalStatus: RouteStatus,
        weatherStatus: WeatherStatus,
    ): RouteStatus {
        val normalizedWeather =
            when (weatherStatus) {
                WeatherStatus.BEFAHRBAR -> RouteStatus.BEFAHRBAR
                WeatherStatus.EINGESCHRAENKT -> RouteStatus.EINGESCHRAENKT
                WeatherStatus.NICHT_BEFAHRBAR -> RouteStatus.NICHT_BEFAHRBAR
                WeatherStatus.UNVOLLSTAENDIG -> RouteStatus.UNVOLLSTAENDIG
            }
        return listOf(tidalStatus, normalizedWeather).maxBy(RouteStatus::precedence)
    }
}

data class RouteAssessmentInput(
    val request: RoutePlanningRequest,
    val routeGeometry: List<GeoPoint>,
    val routeMetrics: RouteMetrics,
)

data class RouteSafetyAssessment(
    val expectedWaypointCount: Int,
    val clearanceSamples: List<ClearanceSample>,
    val allLegsValid: Boolean,
    val weatherStatus: WeatherStatus,
    val messages: List<String> = emptyList(),
    val maxWindKnots: Double? = null,
    val maxGustKnots: Double? = null,
) {
    val worstClearanceSample: ClearanceSample?
        get() = clearanceSamples.filter { it.clearanceMeters != null }
            .minByOrNull { it.clearanceMeters!! }

    val worstClearanceMeters: Double?
        get() = worstClearanceSample?.clearanceMeters
}

fun interface RouteAssessmentProvider {
    suspend fun assess(input: RouteAssessmentInput): RouteSafetyAssessment
}

object IncompleteRouteAssessmentProvider : RouteAssessmentProvider {
    override suspend fun assess(input: RouteAssessmentInput): RouteSafetyAssessment =
        RouteSafetyAssessment(
            expectedWaypointCount = input.request.harbourChain.size,
            clearanceSamples = emptyList(),
            allLegsValid = false,
            weatherStatus = WeatherStatus.UNVOLLSTAENDIG,
            messages = listOf("Gezeiten-, Wasserstands- und Wetterdaten fehlen."),
        )
}

private val RouteStatus.precedence: Int
    get() =
        when (this) {
            RouteStatus.BEFAHRBAR -> 0
            RouteStatus.EINGESCHRAENKT -> 1
            RouteStatus.UNVOLLSTAENDIG -> 2
            RouteStatus.NICHT_BEFAHRBAR -> 3
        }

private val WeatherStatus.precedence: Int
    get() =
        when (this) {
            WeatherStatus.BEFAHRBAR -> 0
            WeatherStatus.EINGESCHRAENKT -> 1
            WeatherStatus.UNVOLLSTAENDIG -> 2
            WeatherStatus.NICHT_BEFAHRBAR -> 3
        }
