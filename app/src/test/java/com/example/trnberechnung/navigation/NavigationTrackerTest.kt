package com.example.trnberechnung.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationTrackerTest {
    private val route =
        NavigationRoute(
            id = "emden-juist",
            title = "Emden → Juist",
            waypoints =
                listOf(
                    waypoint("start", "Emden", 0.0, 0.0, isUser = true),
                    waypoint("fairway", "Fahrwasser 1", 0.0, 0.01, isUser = false),
                    waypoint("stop", "Borkum", 0.0, 0.02, isUser = true),
                    waypoint("finish", "Juist", 0.0, 0.03, isUser = true),
                ),
        )

    @Test
    fun `tracker reports next user harbour and along-route distances`() {
        val now = 1_800_000_000_000L
        val snapshot =
            NavigationTracker.calculate(
                route = route,
                userWaypointIds = setOf("start", "stop", "finish"),
                plannedSpeedKnots = 6.0,
                location = fix(latitude = 0.0, longitude = 0.005, speedMps = 0.1),
                nowEpochMillis = now,
            )

        assertEquals("stop", snapshot.activeWaypointId)
        assertEquals("Borkum", snapshot.activeWaypointName)
        assertEquals(1, snapshot.activeWaypointIndex)
        assertEquals(0.90, snapshot.distanceToWaypointNm, 0.02)
        assertEquals(1.50, snapshot.distanceToFinalNm, 0.02)
        assertEquals(0.0, snapshot.crossTrackErrorMeters, 0.1)
        assertEquals(90.0, snapshot.bearingToWaypointDegrees, 0.1)
        assertFalse(snapshot.isOffCourse)
        assertFalse(snapshot.hasArrived)
        assertNotNull(snapshot.dynamicEtaEpochMillis)

        val expectedEta =
            now + (snapshot.distanceToFinalNm / 6.0 * 3_600_000.0).toLong()
        assertEquals(expectedEta, snapshot.dynamicEtaEpochMillis)
    }

    @Test
    fun `live SOG overrides planned speed and xte threshold is strict`() {
        val now = 1_800_000_000_000L
        val live =
            NavigationTracker.calculate(
                route = route,
                userWaypointIds = setOf("start", "stop", "finish"),
                plannedSpeedKnots = 6.0,
                location = fix(latitude = 0.002, longitude = 0.005, speedMps = 5.14444),
                nowEpochMillis = now,
            )
        val fallback =
            NavigationTracker.calculate(
                route = route,
                userWaypointIds = setOf("start", "stop", "finish"),
                plannedSpeedKnots = 6.0,
                location = fix(latitude = 0.002, longitude = 0.005, speedMps = 0.0),
                nowEpochMillis = now,
            )

        assertTrue(live.crossTrackErrorMeters > 150.0)
        assertTrue(live.isOffCourse)
        assertTrue(live.dynamicEtaEpochMillis!! < fallback.dynamicEtaEpochMillis!!)
    }

    @Test
    fun `arrival latches within fifty metres of final waypoint`() {
        val snapshot =
            NavigationTracker.calculate(
                route = route,
                userWaypointIds = setOf("start", "stop", "finish"),
                plannedSpeedKnots = 6.0,
                location = fix(latitude = 0.0, longitude = 0.0298, speedMps = 2.0),
            )

        assertEquals("finish", snapshot.activeWaypointId)
        assertTrue(snapshot.distanceToFinalNm < 0.03)
        assertTrue(snapshot.hasArrived)
    }

    private fun waypoint(
        id: String,
        name: String,
        latitude: Double,
        longitude: Double,
        isUser: Boolean,
    ) = NavigationWaypoint(
        id = id,
        name = name,
        coordinate = GeoPoint(latitude, longitude),
        isUserWaypoint = isUser,
    )

    private fun fix(
        latitude: Double,
        longitude: Double,
        speedMps: Double,
    ) = LocationFix(
        coordinate = GeoPoint(latitude, longitude),
        horizontalAccuracyMeters = 5.0,
        timestampEpochMillis = 1_800_000_000_000L,
        speedMetersPerSecond = speedMps,
    )
}
