package com.example.trnberechnung.navigation

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveVoyageManagerTest {
    private val route =
        NavigationRoute(
            id = "route-1",
            title = "Testfahrt",
            waypoints =
                listOf(
                    NavigationWaypoint(
                        id = "start",
                        name = "Start",
                        coordinate = GeoPoint(0.0, 0.0),
                        isUserWaypoint = true,
                    ),
                    NavigationWaypoint(
                        id = "finish",
                        name = "Ziel",
                        coordinate = GeoPoint(0.0, 0.01),
                        isUserWaypoint = true,
                    ),
                ),
        )

    @Test
    fun `bad accuracy and jitter do not alter recorded distance or SOG`() =
        runTest {
            val persistence = FakeVoyagePersistence()
            val manager =
                ActiveVoyageManager(
                    navigationTracker = NavigationTracker(),
                    persistence = persistence,
                    currentTimeMillis = { START_TIME },
                )
            assertTrue(
                manager.startVoyage(
                    route = route,
                    plannedSpeedKnots = 6.0,
                    startedAtEpochMillis = START_TIME,
                ),
            )

            manager.processLocation(
                fix(longitude = 0.0, accuracy = 31.0, speedMps = 12.0),
                nowEpochMillis = START_TIME,
            )
            manager.processLocation(
                fix(longitude = 0.0, accuracy = 5.0, speedMps = 2.0),
                nowEpochMillis = START_TIME,
            )
            manager.processLocation(
                fix(longitude = 0.00001, accuracy = 5.0, speedMps = 12.0),
                nowEpochMillis = START_TIME + 1_000L,
            )

            val session =
                (manager.state.value as ActiveVoyageState.Active).session
            assertEquals(1, session.breadcrumbs.size)
            assertEquals(0.0, session.totalDistanceNm, 0.000_001)
            assertEquals(1, session.sogSampleCount)
            assertEquals(2.0 * METERS_PER_SECOND_TO_KNOTS, session.averageSogKnots, 0.000_001)
            assertEquals(2.0 * METERS_PER_SECOND_TO_KNOTS, session.maxSogKnots, 0.000_001)
            assertEquals(1, persistence.active?.breadcrumbs?.size)
        }

    @Test
    fun `accepted breadcrumbs accumulate distance average and maximum SOG`() =
        runTest {
            val persistence = FakeVoyagePersistence()
            val manager =
                ActiveVoyageManager(
                    navigationTracker = NavigationTracker(),
                    persistence = persistence,
                )
            manager.startVoyage(
                route = route,
                plannedSpeedKnots = 6.0,
                startedAtEpochMillis = START_TIME,
            )
            manager.processLocation(fix(longitude = 0.0, speedMps = 2.0))
            manager.processLocation(
                fix(longitude = 0.0001, speedMps = 4.0, courseDegrees = 72.0),
            )

            val session =
                (manager.state.value as ActiveVoyageState.Active).session
            val expectedDistanceMeters =
                GeoMath.distanceMeters(GeoPoint(0.0, 0.0), GeoPoint(0.0, 0.0001))
            assertEquals(
                expectedDistanceMeters / METERS_PER_NAUTICAL_MILE,
                session.totalDistanceNm,
                0.000_001,
            )
            assertEquals(2, session.breadcrumbs.size)
            assertEquals(72.0, session.breadcrumbs.last().courseDegrees!!, 0.0)
            assertEquals(2, session.sogSampleCount)
            assertEquals(3.0 * METERS_PER_SECOND_TO_KNOTS, session.averageSogKnots, 0.000_001)
            assertEquals(4.0 * METERS_PER_SECOND_TO_KNOTS, session.maxSogKnots, 0.000_001)
        }

    @Test
    fun `finish atomically emits metrics and clears active session`() =
        runTest {
            val persistence = FakeVoyagePersistence()
            val manager =
                ActiveVoyageManager(
                    navigationTracker = NavigationTracker(),
                    persistence = persistence,
                )
            manager.startVoyage(
                route = route,
                plannedSpeedKnots = 6.0,
                startedAtEpochMillis = START_TIME,
            )
            manager.processLocation(fix(longitude = 0.0, speedMps = 2.0))
            manager.processLocation(fix(longitude = 0.0001, speedMps = 4.0))

            val completed = manager.finishVoyage(START_TIME + 3_600_000L)

            requireNotNull(completed)
            assertEquals(3_600L, completed.durationSeconds)
            assertEquals("Start", completed.startName)
            assertEquals("Ziel", completed.destinationName)
            assertEquals(2, completed.breadcrumbs.size)
            assertEquals(completed, persistence.completed)
            assertNull(persistence.active)
            assertTrue(manager.state.value is ActiveVoyageState.Inactive)
            assertNull(manager.finishVoyage())
        }

    @Test
    fun `persisted session restores tracker and blocks duplicate start`() =
        runTest {
            val persisted =
                ActiveVoyageSession(
                    route = route,
                    userWaypointIds = setOf("start", "finish"),
                    plannedSpeedKnots = 6.0,
                    startedAtEpochMillis = START_TIME,
                )
            val persistence = FakeVoyagePersistence(active = persisted)
            val manager =
                ActiveVoyageManager(
                    navigationTracker = NavigationTracker(),
                    persistence = persistence,
                )

            assertEquals(persisted, manager.restoreActiveVoyage())
            assertFalse(
                manager.startVoyage(
                    route = route,
                    plannedSpeedKnots = 6.0,
                ),
            )
            assertTrue(manager.processLocation(fix(longitude = 0.001)) != null)
        }

    private fun fix(
        longitude: Double,
        accuracy: Double = 5.0,
        speedMps: Double = 0.0,
        courseDegrees: Double? = null,
    ) = LocationFix(
        coordinate = GeoPoint(0.0, longitude),
        horizontalAccuracyMeters = accuracy,
        timestampEpochMillis = START_TIME,
        speedMetersPerSecond = speedMps,
        courseDegrees = courseDegrees,
    )

    private class FakeVoyagePersistence(
        var active: ActiveVoyageSession? = null,
    ) : ActiveVoyagePersistence {
        var completed: CompletedVoyage? = null

        override suspend fun loadActiveVoyage(): ActiveVoyageSession? = active

        override suspend fun saveActiveVoyage(session: ActiveVoyageSession) {
            active = session
        }

        override suspend fun finishActiveVoyage(voyage: CompletedVoyage) {
            completed = voyage
            active = null
        }

        override suspend fun clearActiveVoyage() {
            active = null
        }
    }

    companion object {
        private const val START_TIME = 1_800_000_000_000L
    }
}
