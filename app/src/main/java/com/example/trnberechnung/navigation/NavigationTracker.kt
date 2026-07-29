package com.example.trnberechnung.navigation

class NavigationTracker(
    private val offCourseThresholdMeters: Double = OFF_COURSE_THRESHOLD_METERS,
    private val arrivalRadiusMeters: Double = ARRIVAL_RADIUS_METERS,
    private val minimumLiveSogKnots: Double = MINIMUM_LIVE_SOG_KNOTS,
) {
    private var routeBinding: RouteBinding? = null

    @Synchronized
    fun setRoute(
        route: NavigationRoute,
        userWaypointIds: Set<String>,
        plannedSpeedKnots: Double,
    ) {
        require(userWaypointIds.all { id -> route.waypoints.any { it.id == id } }) {
            "Every user waypoint id must belong to the route."
        }
        routeBinding =
            RouteBinding(
                route = route,
                userWaypointIds = userWaypointIds,
                plannedSpeedKnots = plannedSpeedKnots.coerceAtLeast(MINIMUM_PLANNED_SPEED_KNOTS),
            )
    }

    @Synchronized
    fun reset() {
        routeBinding = null
    }

    @Synchronized
    fun update(
        location: LocationFix,
        nowEpochMillis: Long = location.timestampEpochMillis,
    ): NavigationSnapshot? =
        routeBinding?.let { binding ->
            calculate(
                route = binding.route,
                userWaypointIds = binding.userWaypointIds,
                plannedSpeedKnots = binding.plannedSpeedKnots,
                location = location,
                nowEpochMillis = nowEpochMillis,
                offCourseThresholdMeters = offCourseThresholdMeters,
                arrivalRadiusMeters = arrivalRadiusMeters,
                minimumLiveSogKnots = minimumLiveSogKnots,
            )
        }

    private data class RouteBinding(
        val route: NavigationRoute,
        val userWaypointIds: Set<String>,
        val plannedSpeedKnots: Double,
    )

    companion object {
        const val OFF_COURSE_THRESHOLD_METERS = 150.0
        const val ARRIVAL_RADIUS_METERS = 50.0
        const val MINIMUM_LIVE_SOG_KNOTS = 1.0
        const val MINIMUM_PLANNED_SPEED_KNOTS = 0.5

        fun calculate(
            route: NavigationRoute,
            userWaypointIds: Set<String>,
            plannedSpeedKnots: Double,
            location: LocationFix,
            nowEpochMillis: Long = location.timestampEpochMillis,
            offCourseThresholdMeters: Double = OFF_COURSE_THRESHOLD_METERS,
            arrivalRadiusMeters: Double = ARRIVAL_RADIUS_METERS,
            minimumLiveSogKnots: Double = MINIMUM_LIVE_SOG_KNOTS,
        ): NavigationSnapshot {
            val coordinates = route.waypoints.map { it.coordinate }
            val closestSegment =
                (0 until coordinates.lastIndex)
                    .minBy { segmentIndex ->
                        GeoMath.perpendicularDistanceMeters(
                            point = location.coordinate,
                            segmentStart = coordinates[segmentIndex],
                            segmentEnd = coordinates[segmentIndex + 1],
                        )
                    }
            val segmentEndIndex = closestSegment + 1
            val crossTrackError =
                GeoMath.perpendicularDistanceMeters(
                    point = location.coordinate,
                    segmentStart = coordinates[closestSegment],
                    segmentEnd = coordinates[segmentEndIndex],
                )
            val nextUserWaypointIndex =
                (segmentEndIndex..route.waypoints.lastIndex).firstOrNull { index ->
                    route.waypoints[index].id in userWaypointIds
                } ?: route.waypoints.lastIndex
            val distanceToWaypointMeters =
                alongRouteDistanceMeters(
                    origin = location.coordinate,
                    startIndex = segmentEndIndex,
                    endIndex = nextUserWaypointIndex,
                    coordinates = coordinates,
                )
            val distanceToFinalMeters =
                alongRouteDistanceMeters(
                    origin = location.coordinate,
                    startIndex = segmentEndIndex,
                    endIndex = coordinates.lastIndex,
                    coordinates = coordinates,
                )
            val fallbackSpeedKnots =
                plannedSpeedKnots.takeIf { it.isFinite() && it > 0.0 }
                    ?.coerceAtLeast(MINIMUM_PLANNED_SPEED_KNOTS)
                    ?: MINIMUM_PLANNED_SPEED_KNOTS
            val effectiveSpeedKnots =
                location.speedKnots.takeIf { it >= minimumLiveSogKnots }
                    ?: fallbackSpeedKnots
            val etaMillis =
                if (effectiveSpeedKnots > 0.0) {
                    val hours = distanceToFinalMeters / METERS_PER_NAUTICAL_MILE /
                        effectiveSpeedKnots
                    nowEpochMillis + (hours * 3_600_000.0).toLong()
                } else {
                    null
                }
            val activeWaypoint = route.waypoints[nextUserWaypointIndex]

            return NavigationSnapshot(
                activeWaypointId = activeWaypoint.id,
                activeWaypointName = activeWaypoint.name,
                activeWaypointIndex = segmentEndIndex,
                distanceToWaypointNm = distanceToWaypointMeters / METERS_PER_NAUTICAL_MILE,
                distanceToFinalNm = distanceToFinalMeters / METERS_PER_NAUTICAL_MILE,
                dynamicEtaEpochMillis = etaMillis,
                crossTrackErrorMeters = crossTrackError,
                bearingToWaypointDegrees =
                    GeoMath.bearingDegrees(
                        from = location.coordinate,
                        to = coordinates[segmentEndIndex],
                    ),
                isOffCourse = crossTrackError > offCourseThresholdMeters,
                hasArrived =
                    GeoMath.distanceMeters(
                        location.coordinate,
                        coordinates.last(),
                    ) <= arrivalRadiusMeters,
            )
        }

        fun alongRouteDistanceMeters(
            origin: GeoPoint,
            startIndex: Int,
            endIndex: Int,
            coordinates: List<GeoPoint>,
        ): Double {
            if (startIndex !in coordinates.indices) return 0.0
            var total = GeoMath.distanceMeters(origin, coordinates[startIndex])
            if (endIndex <= startIndex) return total
            for (index in startIndex until endIndex.coerceAtMost(coordinates.lastIndex)) {
                total += GeoMath.distanceMeters(coordinates[index], coordinates[index + 1])
            }
            return total
        }
    }
}
