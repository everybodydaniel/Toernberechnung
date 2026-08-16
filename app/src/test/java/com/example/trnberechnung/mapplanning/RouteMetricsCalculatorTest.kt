package com.example.trnberechnung.mapplanning

import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Test

class RouteMetricsCalculatorTest {
    @Test
    fun `metrics use speed Berlin time and exact diesel factor`() {
        val departure = ZonedDateTime.of(2026, 7, 29, 13, 35, 0, 0, ZoneOffset.UTC)
        val boatSettings = BoatSettings(
            speedKnots = 6.0,
            dieselLitersPerNm = 0.35,
            draftMeters = 1.0
        )

        val metrics =
            RouteMetricsCalculator.fromDistance(
                distanceNm = 25.5,
                departure = departure,
                boatSettings = boatSettings,
                worstClearanceMeters = 1.24,
                worstClearanceName = "Baltrumer Wattfahrwasser",
            )

        metrics.travelTime shouldBe Duration.ofMinutes(255)
        metrics.arrival.zone shouldBe MAP_PLANNING_ZONE_ID
        metrics.arrival.hour shouldBe 19
        metrics.arrival.minute shouldBe 50
        metrics.dieselLiters shouldBe (8.925 plusOrMinus 0.000_001)
        metrics.dieselReserveLiters shouldBe (1.785 plusOrMinus 0.000_001)
        metrics.totalDieselLiters shouldBe (10.71 plusOrMinus 0.000_001)
        metrics.worstUnderKeelClearanceMeters shouldBe 1.24
        metrics.worstClearanceName shouldBe "Baltrumer Wattfahrwasser"
    }

    @Test
    fun `haversine distance is calculated across every geometry leg`() {
        val geometry =
            listOf(
                GeoPoint(53.3421, 7.1852),
                GeoPoint(53.5000, 7.1000),
                GeoPoint(53.6722, 6.9982),
            )
        val boatSettings = BoatSettings(speedKnots = 6.0)

        val metrics =
            RouteMetricsCalculator.calculate(
                routeGeometry = geometry,
                departure =
                    ZonedDateTime.parse(
                        "2026-07-29T13:35:00+02:00[Europe/Berlin]",
                    ),
                boatSettings = boatSettings,
            )

        (metrics?.distanceNm ?: 0.0) shouldBe (20.75 plusOrMinus 0.5)
    }

    @Test
    fun `diesel consumption uses boat-specific rates`() {
        val boatSettings = BoatSettings(
            dieselLitersPerNm = 0.5,
            speedKnots = 10.0
        )
        val metrics = RouteMetricsCalculator.fromDistance(
            distanceNm = 100.0,
            departure = ZonedDateTime.now(),
            boatSettings = boatSettings
        )

        metrics.dieselLiters shouldBe (50.0 plusOrMinus 0.000_001)
        metrics.dieselReserveLiters shouldBe (10.0 plusOrMinus 0.000_001) // 20% of 50
        metrics.totalDieselLiters shouldBe (60.0 plusOrMinus 0.000_001)
    }
}
