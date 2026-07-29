package com.example.trnberechnung.mapplanning

import io.kotest.matchers.shouldBe
import java.time.ZonedDateTime
import org.junit.Test

class RouteSafetyTest {
    private val safeWeather =
        MarineWeatherAssessment(
            windKnots = 19.9,
            gustKnots = 26.9,
            visibilityKilometers = 5.0,
            precipitationChancePercent = 59.9,
            precipitationMillimeters = 2.9,
        )

    @Test
    fun `weather thresholds match iOS boundaries`() {
        WeatherSafetyEvaluator.evaluate(safeWeather) shouldBe WeatherStatus.BEFAHRBAR
        WeatherSafetyEvaluator.evaluate(safeWeather.copy(windKnots = 20.0)) shouldBe
            WeatherStatus.EINGESCHRAENKT
        WeatherSafetyEvaluator.evaluate(safeWeather.copy(gustKnots = 27.0)) shouldBe
            WeatherStatus.EINGESCHRAENKT
        WeatherSafetyEvaluator.evaluate(safeWeather.copy(visibilityKilometers = 4.99)) shouldBe
            WeatherStatus.EINGESCHRAENKT
        WeatherSafetyEvaluator.evaluate(
            safeWeather.copy(precipitationChancePercent = 60.0),
        ) shouldBe WeatherStatus.EINGESCHRAENKT
        WeatherSafetyEvaluator.evaluate(
            safeWeather.copy(precipitationMillimeters = 3.0),
        ) shouldBe WeatherStatus.EINGESCHRAENKT
        WeatherSafetyEvaluator.evaluate(safeWeather.copy(windKnots = 28.0)) shouldBe
            WeatherStatus.NICHT_BEFAHRBAR
        WeatherSafetyEvaluator.evaluate(safeWeather.copy(gustKnots = 34.0)) shouldBe
            WeatherStatus.NICHT_BEFAHRBAR
        WeatherSafetyEvaluator.evaluate(safeWeather.copy(visibilityKilometers = 0.99)) shouldBe
            WeatherStatus.NICHT_BEFAHRBAR
        WeatherSafetyEvaluator.evaluate(null) shouldBe WeatherStatus.UNVOLLSTAENDIG
    }

    @Test
    fun `route weather requires a sample within 90 minutes for every arrival`() {
        val arrival = ZonedDateTime.parse("2026-07-29T13:00:00+02:00[Europe/Berlin]")
        val status =
            WeatherSafetyEvaluator.evaluateRoute(
                waypointArrivals = listOf(arrival, arrival.plusHours(2)),
                hourlyAssessments = listOf(arrival to safeWeather),
            )

        status shouldBe WeatherStatus.UNVOLLSTAENDIG
    }

    @Test
    fun `under keel precedence is no go then incomplete then warning then go`() {
        val missing = ClearanceSample("A", null)
        val negative = ClearanceSample("B", -0.01)
        UnderKeelSafetyEvaluator.evaluate(
            samples = listOf(missing, negative),
            safetyMarginMeters = 0.3,
        ) shouldBe RouteStatus.NICHT_BEFAHRBAR

        UnderKeelSafetyEvaluator.evaluate(
            samples = listOf(missing, ClearanceSample("B", 0.1)),
            safetyMarginMeters = 0.3,
        ) shouldBe RouteStatus.UNVOLLSTAENDIG

        UnderKeelSafetyEvaluator.evaluate(
            samples = listOf(ClearanceSample("A", 0.1)),
            safetyMarginMeters = 0.3,
        ) shouldBe RouteStatus.EINGESCHRAENKT

        UnderKeelSafetyEvaluator.evaluate(
            samples = listOf(ClearanceSample("A", 0.3)),
            safetyMarginMeters = 0.3,
        ) shouldBe RouteStatus.BEFAHRBAR
    }

    @Test
    fun `manual data is warning and stale data stays incomplete`() {
        UnderKeelSafetyEvaluator.evaluate(
            samples =
                listOf(
                    ClearanceSample(
                        "A",
                        1.0,
                        waterLevelQuality = WaterLevelQuality.MANUAL,
                    ),
                ),
            safetyMarginMeters = 0.3,
        ) shouldBe RouteStatus.EINGESCHRAENKT

        UnderKeelSafetyEvaluator.evaluate(
            samples =
                listOf(
                    ClearanceSample(
                        "A",
                        1.0,
                        waterLevelQuality = WaterLevelQuality.STALE,
                    ),
                ),
            safetyMarginMeters = 0.3,
        ) shouldBe RouteStatus.UNVOLLSTAENDIG
    }

    @Test
    fun `combined status uses safety precedence`() {
        RouteStatusEvaluator.combine(
            RouteStatus.EINGESCHRAENKT,
            WeatherStatus.UNVOLLSTAENDIG,
        ) shouldBe RouteStatus.UNVOLLSTAENDIG

        RouteStatusEvaluator.combine(
            RouteStatus.UNVOLLSTAENDIG,
            WeatherStatus.NICHT_BEFAHRBAR,
        ) shouldBe RouteStatus.NICHT_BEFAHRBAR
    }
}
