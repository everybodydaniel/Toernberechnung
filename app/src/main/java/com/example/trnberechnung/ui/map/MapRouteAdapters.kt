package com.example.trnberechnung.ui.map

import com.example.trnberechnung.mapplanning.CatalogFairwayRouteResolver
import com.example.trnberechnung.mapplanning.FairwayRouteResult
import com.example.trnberechnung.mapplanning.HarbourCatalog
import com.example.trnberechnung.mapplanning.RoutePlanningRequest
import com.example.trnberechnung.mapplanning.RoutePlanningUiState
import com.example.trnberechnung.mapplanning.RouteStatus
import com.example.trnberechnung.navigation.GeoPoint
import com.example.trnberechnung.navigation.NavigationRoute
import com.example.trnberechnung.navigation.NavigationWaypoint
import java.util.UUID
import org.maplibre.android.geometry.LatLng

/**
 * Only the harbours that belong to the current plan become markers.
 *
 * The catalog used to be drawn in full, which put a blue dot and a name label on every harbour in
 * the region and buried the nautical chart underneath them. Start, stops and destination carry
 * meaning; the rest is noise, and harbours are selected in the planner sheet anyway.
 */
fun RoutePlanningUiState.mapHarbourMarkers(): List<MapHarbourMarker> {
    val stopOrder = intermediateStops.mapIndexed { index, stop -> stop.harbourId to index + 1 }.toMap()
    return HarbourCatalog.all.mapNotNull { harbour ->
        val role =
            when (harbour.id) {
                startHarbourId -> MapHarbourRole.START
                destinationHarbourId -> MapHarbourRole.DESTINATION
                in stopOrder -> MapHarbourRole.STOP
                else -> return@mapNotNull null
            }
        MapHarbourMarker(
            id = harbour.id.rawValue,
            title = harbour.name,
            subtitle = harbour.subtitle,
            coordinate = LatLng(harbour.coordinate.latitude, harbour.coordinate.longitude),
            role = role,
            order = stopOrder[harbour.id],
        )
    }
}

fun RoutePlanningUiState.mapRouteColor() =
    when (routeStatus) {
        RouteStatus.BEFAHRBAR -> androidx.compose.ui.graphics.Color(0xFF0E9F6E)
        RouteStatus.EINGESCHRAENKT -> androidx.compose.ui.graphics.Color(0xFFF97316)
        RouteStatus.NICHT_BEFAHRBAR -> androidx.compose.ui.graphics.Color(0xFFDC2626)
        RouteStatus.UNVOLLSTAENDIG -> androidx.compose.ui.graphics.Color(0xFF64748B)
    }

fun RoutePlanningUiState.toNavigationRouteOrNull(): NavigationRoute? {
    if (!hasCompleteRouteInput || routeGeometry.size < 2) return null
    val start = startHarbourId ?: return null
    val destination = destinationHarbourId ?: return null
    val request =
        RoutePlanningRequest(
            startHarbourId = start,
            destinationHarbourId = destination,
            intermediateStops = intermediateStops,
            departure = departure,
            boatSettings = boatSettings,
        )
    val fairway =
        CatalogFairwayRouteResolver.resolve(request) as? FairwayRouteResult.Success
            ?: return null

    return NavigationRoute(
        id = UUID.randomUUID().toString(),
        title = routeTitle,
        waypoints =
            fairway.waypoints.map { waypoint ->
                NavigationWaypoint(
                    id = waypoint.id,
                    name = waypoint.name,
                    coordinate =
                        GeoPoint(
                            latitude = waypoint.coordinate.latitude,
                            longitude = waypoint.coordinate.longitude,
                        ),
                    isUserWaypoint = waypoint.isUserWaypoint,
                )
            },
    )
}
