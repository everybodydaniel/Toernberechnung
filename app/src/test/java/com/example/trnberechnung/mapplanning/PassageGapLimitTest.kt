package com.example.trnberechnung.mapplanning

import com.example.trnberechnung.model.TideEvent
import com.example.trnberechnung.model.TideStationData
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.time.ZonedDateTime
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class PassageGapLimitTest {
    @get:Rule
    val mainDispatcherRule = MapPlanningMainDispatcherRule()

    @Test
    fun `gap larger than 7 hours results in null height`() = runTest {
        val station = TideStationData(
            area = "GapTest",
            region = "Nordsee",
            latitude = 53.0,
            longitude = 7.0,
            waterLevel = null,
            meanHighWater = null,
            meanLowWater = null,
            gaugeLabel = "GapTest",
            forecastTimestamp = "2026-07-29T10:00:00Z",
            weatherForecast = emptyList(),
            events = listOf(
                TideEvent("2026-07-29 10:00:00+02:00", "NW", 1.0),
                TideEvent("2026-07-29 18:00:00+02:00", "HW", 3.0) // 8h gap
            )
        )
        val provider = AndroidRouteAssessmentProvider(
            stationSnapshotProvider = { listOf(station) },
            chartDepthProvider = { 2.0 }
        )

        val time = ZonedDateTime.parse("2026-07-29T14:00:00+02:00[Europe/Berlin]")
        val assessment = provider.assess(mockInput(time))

        assessment.clearanceSamples.first().isValid shouldBe false
        assessment.clearanceSamples.first().clearanceMeters shouldBe null
    }

    @Test
    fun `HW-to-HW block results in null height`() = runTest {
        val station = TideStationData(
            area = "SequenceTest",
            region = "Nordsee",
            latitude = 53.0,
            longitude = 7.0,
            waterLevel = null,
            meanHighWater = null,
            meanLowWater = null,
            gaugeLabel = "SequenceTest",
            forecastTimestamp = "2026-07-29T10:00:00Z",
            weatherForecast = emptyList(),
            events = listOf(
                TideEvent("2026-07-29 10:00:00+02:00", "HW", 3.5),
                TideEvent("2026-07-29 16:00:00+02:00", "HW", 3.4) // Missing NW between HWs
            )
        )
        val provider = AndroidRouteAssessmentProvider(
            stationSnapshotProvider = { listOf(station) },
            chartDepthProvider = { 2.0 }
        )

        val time = ZonedDateTime.parse("2026-07-29T13:00:00+02:00[Europe/Berlin]")
        val assessment = provider.assess(mockInput(time))

        assessment.clearanceSamples.first().isValid shouldBe false
    }

    private fun mockInput(departure: ZonedDateTime) = RouteAssessmentInput(
        request = RoutePlanningRequest(
            HarbourId.EMDEN_HARBOR,
            HarbourId.JUIST_HARBOR,
            emptyList(),
            departure,
            BoatSettings(draftMeters = 1.0)
        ),
        routeGeometry = listOf(GeoPoint(53.0, 7.0), GeoPoint(53.1, 7.1)),
        routeMetrics = RouteMetrics(
            distanceNm = 10.0,
            travelTime = Duration.ofHours(2),
            arrival = departure.plusHours(2),
            worstUnderKeelClearanceMeters = 1.0,
            dieselLiters = 5.0,
            dieselReserveLiters = 2.0
        )
    )
}
