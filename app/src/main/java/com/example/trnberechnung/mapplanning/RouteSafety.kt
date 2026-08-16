package com.example.trnberechnung.mapplanning

import java.time.Duration
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
            assessment.windKnots >= 28.0 || assessment.gustKnots >= 34.0 ||
                assessment.visibilityKilometers < 1.0 ->
                WeatherStatus.NICHT_BEFAHRBAR

            assessment.windKnots >= 20.0 || assessment.gustKnots >= 27.0 ||
                assessment.visibilityKilometers < 5.0 ||
                assessment.precipitationChancePercent >= 60.0 ||
                assessment.precipitationMillimeters >= 3.0 ->
                WeatherStatus.EINGESCHRAENKT

            else -> WeatherStatus.BEFAHRBAR
        }
    }

    fun evaluateAll(assessments: List<MarineWeatherAssessment?>): WeatherStatus {
        if (assessments.isEmpty()) return WeatherStatus.UNVOLLSTAENDIG
        return assessments.map { evaluate(it) }.maxBy { it.precedence }
    }

    fun evaluateRoute(
        waypointArrivals: List<ZonedDateTime>,
        hourlyAssessments: List<Pair<ZonedDateTime, MarineWeatherAssessment>>,
    ): WeatherStatus {
        if (waypointArrivals.isEmpty()) return WeatherStatus.UNVOLLSTAENDIG

        val assessments =
            waypointArrivals.map { arrival ->
                val nearest =
                    hourlyAssessments.minByOrNull { (time, _) ->
                        Duration.between(time, arrival).abs().toMinutes()
                    }

                if (nearest != null &&
                    Duration.between(nearest.first, arrival).abs().toMinutes() <= 90
                ) {
                    nearest.second
                } else {
                    null
                }
            }

        return evaluateAll(assessments)
    }
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
    fun combine(tidal: RouteStatus, weather: WeatherStatus): RouteStatus {
        val normalizedWeather =
            when (weather) {
                WeatherStatus.BEFAHRBAR -> RouteStatus.BEFAHRBAR
                WeatherStatus.EINGESCHRAENKT -> RouteStatus.EINGESCHRAENKT
                WeatherStatus.NICHT_BEFAHRBAR -> RouteStatus.NICHT_BEFAHRBAR
                WeatherStatus.UNVOLLSTAENDIG -> RouteStatus.UNVOLLSTAENDIG
            }
        return listOf(tidal, normalizedWeather).maxBy(RouteStatus::precedence)
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
            expectedWaypointCount = 0,
            clearanceSamples = emptyList(),
            allLegsValid = false,
            weatherStatus = WeatherStatus.UNVOLLSTAENDIG,
        )
}

val RouteStatus.precedence: Int
    get() =
        when (this) {
            RouteStatus.NICHT_BEFAHRBAR -> 4
            RouteStatus.UNVOLLSTAENDIG -> 3
            RouteStatus.EINGESCHRAENKT -> 2
            RouteStatus.BEFAHRBAR -> 1
        }

val WeatherStatus.precedence: Int
    get() =
        when (this) {
            WeatherStatus.NICHT_BEFAHRBAR -> 4
            WeatherStatus.UNVOLLSTAENDIG -> 3
            WeatherStatus.EINGESCHRAENKT -> 2
            WeatherStatus.BEFAHRBAR -> 1
        }
