package com.example.trnberechnung.mapplanning

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.ZonedDateTime
import org.junit.Test

class FairwayRouteResolverTest {
    @Test
    fun `Dijkstra expansion preserves ordered user harbours and injects fairway points`() {
        val request =
            RoutePlanningRequest(
                startHarbourId = HarbourId.EMDEN_HARBOR,
                destinationHarbourId = HarbourId.NORDERNEY_HARBOR,
                intermediateStops =
                    listOf(IntermediateStop(harbourId = HarbourId.JUIST_HARBOR)),
                departure =
                    ZonedDateTime.parse("2026-07-29T13:35:00+02:00[Europe/Berlin]"),
                boatSettings = BoatSettings(),
            )

        val result =
            CatalogFairwayRouteResolver.resolve(request)
                .shouldBeInstanceOf<FairwayRouteResult.Success>()

        result.waypoints
            .filter(RouteSafetyWaypoint::isUserWaypoint)
            .map(RouteSafetyWaypoint::harbourId)
            .shouldContainExactly(
                HarbourId.EMDEN_HARBOR,
                HarbourId.JUIST_HARBOR,
                HarbourId.NORDERNEY_HARBOR,
            )
        (result.waypoints.size > 3) shouldBe true
        (result.waypoints.map(RouteSafetyWaypoint::id).distinct().size) shouldBe
            result.waypoints.size
        result.waypoints
            .filterNot(RouteSafetyWaypoint::isUserWaypoint)
            .all { it.chartDepthMeters != null } shouldBe true
    }
}
