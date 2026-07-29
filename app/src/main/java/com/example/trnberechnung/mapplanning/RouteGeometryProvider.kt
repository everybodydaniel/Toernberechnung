package com.example.trnberechnung.mapplanning

import com.example.trnberechnung.routing.v2.NauticalRouterV2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.android.geometry.LatLng

sealed interface RouteGeometryResult {
    data class Success(val points: List<GeoPoint>) : RouteGeometryResult

    data class Incomplete(
        val reason: String,
        val partialPoints: List<GeoPoint> = emptyList(),
    ) : RouteGeometryResult
}

fun interface RouteGeometryProvider {
    suspend fun calculate(harbourChain: List<Harbour>): RouteGeometryResult
}

class NauticalRouterV2GeometryProvider(
    private val routeCalculator: (LatLng, LatLng) -> NauticalRouterV2.RouteResult =
        NauticalRouterV2::calculateBridgedRouteResult,
) : RouteGeometryProvider {
    override suspend fun calculate(harbourChain: List<Harbour>): RouteGeometryResult =
        withContext(Dispatchers.Default) {
            if (harbourChain.size < 2) {
                return@withContext RouteGeometryResult.Incomplete(
                    reason = "Start und Ziel fehlen.",
                )
            }

            val completeRoute = mutableListOf<GeoPoint>()
            for ((from, to) in harbourChain.zipWithNext()) {
                val result =
                    routeCalculator(
                        from.coordinate.toLatLng(),
                        to.coordinate.toLatLng(),
                    )

                when (result) {
                    is NauticalRouterV2.RouteResult.Success -> {
                        val leg = result.points.map(LatLng::toGeoPoint)
                        if (leg.size < 2) {
                            return@withContext RouteGeometryResult.Incomplete(
                                reason = "Der Seeweg ${from.name} → ${to.name} ist unvollständig.",
                                partialPoints = completeRoute,
                            )
                        }
                        if (completeRoute.isEmpty()) {
                            completeRoute += leg
                        } else {
                            completeRoute += leg.drop(1)
                        }
                    }

                    is NauticalRouterV2.RouteResult.Incomplete -> {
                        return@withContext RouteGeometryResult.Incomplete(
                            reason = "${from.name} → ${to.name}: ${result.message}",
                            partialPoints = completeRoute,
                        )
                    }
                }
            }

            RouteGeometryResult.Success(completeRoute)
        }
}

private fun GeoPoint.toLatLng(): LatLng = LatLng(latitude, longitude)

private fun LatLng.toGeoPoint(): GeoPoint = GeoPoint(latitude, longitude)
