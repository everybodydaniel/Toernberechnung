package com.example.trnberechnung.ui.map

import com.example.trnberechnung.mapplanning.BoatSettings
import com.example.trnberechnung.mapplanning.GeoPoint
import com.example.trnberechnung.mapplanning.HarbourId
import com.example.trnberechnung.mapplanning.IntermediateStop
import com.example.trnberechnung.mapplanning.RoutePlanningUiState
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.time.ZonedDateTime
import org.junit.Test

class MapRouteAdaptersTest {
    @Test
    fun `GPS route uses fairway nodes but reports only selected harbours as user waypoints`() {
        val state =
            RoutePlanningUiState(
                startHarbourId = HarbourId.EMDEN_HARBOR,
                destinationHarbourId = HarbourId.NORDERNEY_HARBOR,
                intermediateStops =
                    listOf(IntermediateStop(harbourId = HarbourId.JUIST_HARBOR)),
                departure =
                    ZonedDateTime.parse("2026-07-29T13:35:00+02:00[Europe/Berlin]"),
                boatSettings = BoatSettings(),
                routeGeometry =
                    listOf(
                        GeoPoint(53.3421, 7.1852),
                        GeoPoint(53.7024, 7.1637),
                    ),
            )

        val route = requireNotNull(state.toNavigationRouteOrNull())

        route.waypoints.filter { it.isUserWaypoint }.map { it.id }.shouldContainExactly(
            HarbourId.EMDEN_HARBOR.rawValue,
            HarbourId.JUIST_HARBOR.rawValue,
            HarbourId.NORDERNEY_HARBOR.rawValue,
        )
        (route.waypoints.size > 3) shouldBe true
    }
}
