package com.example.trnberechnung.navigation

import java.util.UUID
import kotlin.math.max

const val METERS_PER_NAUTICAL_MILE = 1_852.0
const val METERS_PER_SECOND_TO_KNOTS = 1.94384

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
) {
    init {
        require(latitude.isFinite() && latitude in -90.0..90.0) {
            "Latitude must be finite and between -90 and 90 degrees."
        }
        require(longitude.isFinite() && longitude in -180.0..180.0) {
            "Longitude must be finite and between -180 and 180 degrees."
        }
    }
}

data class NavigationWaypoint(
    val id: String,
    val name: String,
    val coordinate: GeoPoint,
    val isUserWaypoint: Boolean,
) {
    init {
        require(id.isNotBlank()) { "Waypoint id must not be blank." }
        require(name.isNotBlank()) { "Waypoint name must not be blank." }
    }
}

data class NavigationRoute(
    val id: String,
    val title: String,
    val waypoints: List<NavigationWaypoint>,
) {
    init {
        require(id.isNotBlank()) { "Route id must not be blank." }
        require(title.isNotBlank()) { "Route title must not be blank." }
        require(waypoints.size >= 2) { "A navigation route needs at least two waypoints." }
        require(waypoints.map { it.id }.distinct().size == waypoints.size) {
            "Waypoint ids must be unique inside a route."
        }
    }
}

data class LocationFix(
    val coordinate: GeoPoint,
    val horizontalAccuracyMeters: Double,
    val timestampEpochMillis: Long,
    val speedMetersPerSecond: Double = 0.0,
    val courseDegrees: Double? = null,
) {
    val speedKnots: Double
        get() = max(0.0, speedMetersPerSecond.takeIf(Double::isFinite) ?: 0.0) *
            METERS_PER_SECOND_TO_KNOTS
}

data class NavigationSnapshot(
    val activeWaypointId: String,
    val activeWaypointName: String,
    val activeWaypointIndex: Int,
    val distanceToWaypointNm: Double,
    val distanceToFinalNm: Double,
    val dynamicEtaEpochMillis: Long?,
    val crossTrackErrorMeters: Double,
    val bearingToWaypointDegrees: Double,
    val isOffCourse: Boolean,
    val hasArrived: Boolean,
)

data class VoyageBreadcrumb(
    val coordinate: GeoPoint,
    val timestampEpochMillis: Long,
    val horizontalAccuracyMeters: Double,
    val speedKnots: Double,
    val courseDegrees: Double? = null,
)

data class ActiveVoyageSession(
    val id: String = UUID.randomUUID().toString(),
    val route: NavigationRoute,
    val userWaypointIds: Set<String>,
    val plannedSpeedKnots: Double,
    val startedAtEpochMillis: Long,
    val totalDistanceNm: Double = 0.0,
    val averageSogKnots: Double = 0.0,
    val maxSogKnots: Double = 0.0,
    val sogSampleCount: Int = 0,
    val sogSumKnots: Double = 0.0,
    val breadcrumbs: List<VoyageBreadcrumb> = emptyList(),
    val latestNavigation: NavigationSnapshot? = null,
) {
    init {
        require(id.isNotBlank()) { "Voyage id must not be blank." }
        require(plannedSpeedKnots.isFinite() && plannedSpeedKnots > 0.0) {
            "Planned speed must be greater than zero."
        }
        require(startedAtEpochMillis >= 0L) { "Voyage start time must be positive." }
        require(totalDistanceNm >= 0.0) { "Voyage distance must not be negative." }
        require(sogSampleCount >= 0) { "SOG sample count must not be negative." }
        require(userWaypointIds.all { waypointId -> route.waypoints.any { it.id == waypointId } }) {
            "Every user waypoint id must belong to the active route."
        }
    }

    fun elapsedSeconds(nowEpochMillis: Long): Long =
        ((nowEpochMillis - startedAtEpochMillis).coerceAtLeast(0L)) / 1_000L
}

data class CompletedVoyage(
    val sessionId: String,
    val routeId: String,
    val routeTitle: String,
    val startName: String,
    val destinationName: String,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long,
    val plannedDistanceNm: Double,
    val actualDistanceNm: Double,
    val averageSogKnots: Double,
    val maxSogKnots: Double,
    val durationSeconds: Long,
    val breadcrumbs: List<VoyageBreadcrumb>,
)

sealed interface ActiveVoyageState {
    data object Inactive : ActiveVoyageState

    data class Active(
        val session: ActiveVoyageSession,
    ) : ActiveVoyageState
}
