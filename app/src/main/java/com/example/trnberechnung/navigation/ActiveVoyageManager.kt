package com.example.trnberechnung.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ActiveVoyageManager(
    private val navigationTracker: NavigationTracker,
    private val persistence: ActiveVoyagePersistence,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {
    private val mutationMutex = Mutex()
    private val mutableState = MutableStateFlow<ActiveVoyageState>(ActiveVoyageState.Inactive)

    val state: StateFlow<ActiveVoyageState> = mutableState.asStateFlow()

    suspend fun restoreActiveVoyage(): ActiveVoyageSession? =
        mutationMutex.withLock {
            (mutableState.value as? ActiveVoyageState.Active)?.session?.let {
                return@withLock it
            }
            val restored = persistence.loadActiveVoyage() ?: return@withLock null
            navigationTracker.setRoute(
                route = restored.route,
                userWaypointIds = restored.userWaypointIds,
                plannedSpeedKnots = restored.plannedSpeedKnots,
            )
            mutableState.value = ActiveVoyageState.Active(restored)
            restored
        }

    suspend fun startVoyage(
        route: NavigationRoute,
        userWaypointIds: Set<String> =
            route.waypoints.filter(NavigationWaypoint::isUserWaypoint).mapTo(mutableSetOf()) {
                it.id
            },
        plannedSpeedKnots: Double,
        startedAtEpochMillis: Long = currentTimeMillis(),
    ): Boolean =
        mutationMutex.withLock {
            if (mutableState.value is ActiveVoyageState.Active) return@withLock false

            val session =
                ActiveVoyageSession(
                    route = route,
                    userWaypointIds = userWaypointIds,
                    plannedSpeedKnots =
                        plannedSpeedKnots.coerceAtLeast(
                            NavigationTracker.MINIMUM_PLANNED_SPEED_KNOTS,
                        ),
                    startedAtEpochMillis = startedAtEpochMillis,
                )
            navigationTracker.setRoute(
                route = route,
                userWaypointIds = userWaypointIds,
                plannedSpeedKnots = session.plannedSpeedKnots,
            )
            try {
                persistence.saveActiveVoyage(session)
            } catch (error: Throwable) {
                navigationTracker.reset()
                throw error
            }
            mutableState.value = ActiveVoyageState.Active(session)
            true
        }

    /**
     * Applies a hardware fix. Fixes worse than [MAX_HORIZONTAL_ACCURACY_METERS]
     * are ignored completely. Valid fixes always update navigation derivatives;
     * distance and SOG metrics only advance when the breadcrumb moved at least
     * [MIN_BREADCRUMB_DISTANCE_METERS].
     */
    suspend fun processLocation(
        fix: LocationFix,
        nowEpochMillis: Long = currentTimeMillis(),
    ): NavigationSnapshot? =
        mutationMutex.withLock {
            val active = mutableState.value as? ActiveVoyageState.Active
                ?: return@withLock null
            if (
                !fix.horizontalAccuracyMeters.isFinite() ||
                fix.horizontalAccuracyMeters < 0.0 ||
                fix.horizontalAccuracyMeters > MAX_HORIZONTAL_ACCURACY_METERS
            ) {
                return@withLock null
            }

            val navigation =
                navigationTracker.update(
                    location = fix,
                    nowEpochMillis = nowEpochMillis,
                ) ?: return@withLock null
            val previousSession = active.session
            val previousBreadcrumb = previousSession.breadcrumbs.lastOrNull()
            val movedMeters =
                previousBreadcrumb?.let {
                    GeoMath.distanceMeters(it.coordinate, fix.coordinate)
                }
            if (movedMeters != null && movedMeters < MIN_BREADCRUMB_DISTANCE_METERS) {
                mutableState.value =
                    ActiveVoyageState.Active(
                        previousSession.copy(latestNavigation = navigation),
                    )
                return@withLock navigation
            }

            val sogKnots = fix.speedKnots
            val hasSpeedSample = sogKnots > 0.0 && sogKnots.isFinite()
            val sampleCount =
                previousSession.sogSampleCount + if (hasSpeedSample) 1 else 0
            val sogSum =
                previousSession.sogSumKnots + if (hasSpeedSample) sogKnots else 0.0
            val updated =
                previousSession.copy(
                    totalDistanceNm =
                        previousSession.totalDistanceNm +
                            (movedMeters ?: 0.0) / METERS_PER_NAUTICAL_MILE,
                    averageSogKnots =
                        if (sampleCount > 0) {
                            sogSum / sampleCount.toDouble()
                        } else {
                            0.0
                        },
                    maxSogKnots =
                        if (hasSpeedSample) {
                            maxOf(previousSession.maxSogKnots, sogKnots)
                        } else {
                            previousSession.maxSogKnots
                        },
                    sogSampleCount = sampleCount,
                    sogSumKnots = sogSum,
                    breadcrumbs =
                        previousSession.breadcrumbs +
                            VoyageBreadcrumb(
                                coordinate = fix.coordinate,
                                timestampEpochMillis = fix.timestampEpochMillis,
                                horizontalAccuracyMeters = fix.horizontalAccuracyMeters,
                                speedKnots = sogKnots,
                                courseDegrees = fix.courseDegrees,
                            ),
                    latestNavigation = navigation,
                )
            persistence.saveActiveVoyage(updated)
            mutableState.value = ActiveVoyageState.Active(updated)
            navigation
        }

    suspend fun finishVoyage(
        endedAtEpochMillis: Long = currentTimeMillis(),
    ): CompletedVoyage? =
        mutationMutex.withLock {
            val active = mutableState.value as? ActiveVoyageState.Active
                ?: return@withLock null
            val session = active.session
            val safeEnd = maxOf(endedAtEpochMillis, session.startedAtEpochMillis)
            val completed =
                CompletedVoyage(
                    sessionId = session.id,
                    routeId = session.route.id,
                    routeTitle = session.route.title,
                    startName = session.route.waypoints.first().name,
                    destinationName = session.route.waypoints.last().name,
                    startedAtEpochMillis = session.startedAtEpochMillis,
                    endedAtEpochMillis = safeEnd,
                    plannedDistanceNm =
                        GeoMath.routeDistanceMeters(
                            session.route.waypoints.map { it.coordinate },
                        ) / METERS_PER_NAUTICAL_MILE,
                    actualDistanceNm = session.totalDistanceNm,
                    averageSogKnots = session.averageSogKnots,
                    maxSogKnots = session.maxSogKnots,
                    durationSeconds = (safeEnd - session.startedAtEpochMillis) / 1_000L,
                    breadcrumbs = session.breadcrumbs,
                )
            persistence.finishActiveVoyage(completed)
            navigationTracker.reset()
            mutableState.value = ActiveVoyageState.Inactive
            completed
        }

    suspend fun cancelVoyage(): Boolean =
        mutationMutex.withLock {
            if (mutableState.value !is ActiveVoyageState.Active) return@withLock false
            persistence.clearActiveVoyage()
            navigationTracker.reset()
            mutableState.value = ActiveVoyageState.Inactive
            true
        }

    companion object {
        const val MAX_HORIZONTAL_ACCURACY_METERS = 30.0
        const val MIN_BREADCRUMB_DISTANCE_METERS = 5.0
    }
}
