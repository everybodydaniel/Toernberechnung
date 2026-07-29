package com.example.trnberechnung.ui.navigation

import com.example.trnberechnung.navigation.ActiveVoyageSession
import com.example.trnberechnung.navigation.GeoPoint
import com.example.trnberechnung.navigation.LocationFix
import com.example.trnberechnung.navigation.NavigationRoute
import com.example.trnberechnung.navigation.NavigationSnapshot
import com.example.trnberechnung.navigation.NavigationWaypoint
import com.example.trnberechnung.navigation.VoyageBreadcrumb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationDisplayValuesTest {
    @Test
    fun `dashboard formats live nautical values and warning`() {
        val session =
            session(
                snapshot =
                    NavigationSnapshot(
                        activeWaypointId = "finish",
                        activeWaypointName = "Juist, Hafen",
                        activeWaypointIndex = 1,
                        distanceToWaypointNm = 1.234,
                        distanceToFinalNm = 1.234,
                        dynamicEtaEpochMillis = 1_800_000_000_000L,
                        crossTrackErrorMeters = 186.8,
                        bearingToWaypointDegrees = 89.8,
                        isOffCourse = true,
                        hasArrived = false,
                    ),
            )
        val fix =
            LocationFix(
                coordinate = GeoPoint(53.5, 7.2),
                horizontalAccuracyMeters = 8.0,
                timestampEpochMillis = START_TIME,
                speedMetersPerSecond = 3.0,
                courseDegrees = 359.7,
            )

        val values =
            navigationDisplayValues(
                session = session,
                latestFix = fix,
                nowEpochMillis = START_TIME + 3_661_000L,
            )

        assertEquals("Juist, Hafen", values.waypointTitle)
        assertTrue(values.instruction.contains("Peilung 090°"))
        assertEquals("5,8 kn", values.sog)
        assertEquals("000°", values.cog)
        assertEquals("1,23 nm", values.dtw)
        assertEquals("186 m", values.xte)
        assertEquals("01:01:01", values.elapsed)
        assertTrue(values.isOffCourse)
        assertFalse(values.hasArrived)
    }

    @Test
    fun `imprecise live fix falls back to accepted breadcrumb`() {
        val session =
            session(snapshot = null).copy(
                breadcrumbs =
                    listOf(
                        VoyageBreadcrumb(
                            coordinate = GeoPoint(53.5, 7.2),
                            timestampEpochMillis = START_TIME,
                            horizontalAccuracyMeters = 10.0,
                            speedKnots = 4.2,
                            courseDegrees = 72.0,
                        ),
                    ),
            )
        val impreciseFix =
            LocationFix(
                coordinate = GeoPoint(53.6, 7.3),
                horizontalAccuracyMeters = 31.0,
                timestampEpochMillis = START_TIME,
                speedMetersPerSecond = 10.0,
                courseDegrees = 180.0,
            )

        val values = navigationDisplayValues(session, impreciseFix, START_TIME)

        assertEquals("4,2 kn", values.sog)
        assertEquals("072°", values.cog)
        assertEquals("Route folgen", values.waypointTitle)
        assertFalse(values.isOffCourse)
    }

    @Test
    fun `arrival replaces route instruction with destination state`() {
        val session =
            session(
                snapshot =
                    NavigationSnapshot(
                        activeWaypointId = "finish",
                        activeWaypointName = "Ziel",
                        activeWaypointIndex = 1,
                        distanceToWaypointNm = 0.01,
                        distanceToFinalNm = 0.01,
                        dynamicEtaEpochMillis = START_TIME,
                        crossTrackErrorMeters = 3.0,
                        bearingToWaypointDegrees = 90.0,
                        isOffCourse = false,
                        hasArrived = true,
                    ),
            )

        val values = navigationDisplayValues(session, null, START_TIME)

        assertEquals("Ziel erreicht", values.waypointTitle)
        assertTrue(values.instruction.contains("50 m"))
        assertTrue(values.hasArrived)
    }

    private fun session(snapshot: NavigationSnapshot?): ActiveVoyageSession =
        ActiveVoyageSession(
            id = "voyage",
            route =
                NavigationRoute(
                    id = "route",
                    title = "Emden → Juist",
                    waypoints =
                        listOf(
                            NavigationWaypoint(
                                id = "start",
                                name = "Emden",
                                coordinate = GeoPoint(53.3, 7.2),
                                isUserWaypoint = true,
                            ),
                            NavigationWaypoint(
                                id = "finish",
                                name = "Juist",
                                coordinate = GeoPoint(53.68, 7.0),
                                isUserWaypoint = true,
                            ),
                        ),
                ),
            userWaypointIds = setOf("start", "finish"),
            plannedSpeedKnots = 6.0,
            startedAtEpochMillis = START_TIME,
            totalDistanceNm = 2.5,
            latestNavigation = snapshot,
        )

    companion object {
        private const val START_TIME = 1_800_000_000_000L
    }
}
