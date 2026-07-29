package com.example.trnberechnung.repository

import androidx.room.withTransaction
import com.example.trnberechnung.database.ActiveVoyageEntity
import com.example.trnberechnung.database.AppDatabase
import com.example.trnberechnung.database.VoyageBreadcrumbEntity
import com.example.trnberechnung.model.LogbookEntry
import com.example.trnberechnung.navigation.ActiveVoyagePersistence
import com.example.trnberechnung.navigation.ActiveVoyageSession
import com.example.trnberechnung.navigation.CompletedVoyage
import com.example.trnberechnung.navigation.GeoPoint
import com.example.trnberechnung.navigation.METERS_PER_NAUTICAL_MILE
import com.example.trnberechnung.navigation.NavigationRoute
import com.example.trnberechnung.navigation.NavigationWaypoint
import com.example.trnberechnung.navigation.VoyageBreadcrumb
import com.google.gson.Gson
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Room adapter for the navigation module's persistence boundary.
 *
 * A completed voyage and its logbook row are committed atomically. Corrupt
 * route JSON is treated as non-restorable instead of constructing an invalid
 * navigation route.
 */
class RoomActiveVoyagePersistence(
    private val database: AppDatabase,
    private val ownerIdProvider: () -> String,
    private val now: () -> Long = System::currentTimeMillis,
) : ActiveVoyagePersistence {
    private val dao
        get() = database.activeVoyageDao()

    override suspend fun loadActiveVoyage(): ActiveVoyageSession? {
        val owner = ownerId()
        val entity = dao.getActive(owner) ?: return null
        val storedWaypoints =
            runCatching {
                VoyageGsonHolder.gson
                    .fromJson(
                        entity.waypointCoordinatesJson,
                        Array<StoredWaypoint>::class.java,
                    ).orEmpty()
            }.getOrNull() ?: return null
        if (storedWaypoints.size < 2) return null

        return runCatching {
            val waypoints =
                storedWaypoints.map { stored ->
                    NavigationWaypoint(
                        id = stored.id,
                        name = stored.name,
                        coordinate = GeoPoint(stored.latitude, stored.longitude),
                        isUserWaypoint = stored.isUserWaypoint,
                    )
                }
            val breadcrumbs =
                dao.getBreadcrumbs(owner, entity.id).map(VoyageBreadcrumbEntity::toDomain)
            ActiveVoyageSession(
                id = entity.id,
                route =
                    NavigationRoute(
                        id = entity.routeId,
                        title = entity.routeDescription,
                        waypoints = waypoints,
                    ),
                userWaypointIds =
                    waypoints
                        .filter(NavigationWaypoint::isUserWaypoint)
                        .mapTo(linkedSetOf(), NavigationWaypoint::id),
                plannedSpeedKnots = entity.plannedSpeedKnots,
                startedAtEpochMillis = entity.startedAt,
                totalDistanceNm = entity.distanceMeters / METERS_PER_NAUTICAL_MILE,
                averageSogKnots =
                    if (entity.sogSampleCount > 0) {
                        entity.sogSampleSum / entity.sogSampleCount.toDouble()
                    } else {
                        0.0
                    },
                maxSogKnots = entity.maxSogKnots,
                sogSampleCount = entity.sogSampleCount,
                sogSumKnots = entity.sogSampleSum,
                breadcrumbs = breadcrumbs,
                latestNavigation = null,
            )
        }.getOrNull()
    }

    override suspend fun saveActiveVoyage(session: ActiveVoyageSession) {
        val owner = ownerId()
        val timestamp = now()
        database.withTransaction {
            val previous = dao.getVoyage(owner, session.id)
            val storedWaypoints =
                session.route.waypoints.map { waypoint ->
                    StoredWaypoint(
                        id = waypoint.id,
                        name = waypoint.name,
                        latitude = waypoint.coordinate.latitude,
                        longitude = waypoint.coordinate.longitude,
                        isUserWaypoint = waypoint.id in session.userWaypointIds,
                    )
                }
            val userStops =
                session.route.waypoints
                    .drop(1)
                    .dropLast(1)
                    .filter { it.id in session.userWaypointIds }
                    .map(NavigationWaypoint::id)
            val entity =
                ActiveVoyageEntity(
                    ownerId = owner,
                    id = session.id,
                    routeId = session.route.id,
                    routeDescription = session.route.title,
                    startHarbourId =
                        session.route.waypoints
                            .first()
                            .id,
                    destinationHarbourId =
                        session.route.waypoints
                            .last()
                            .id,
                    intermediateHarbourIdsJson = VoyageGsonHolder.gson.toJson(userStops),
                    routeCoordinatesJson =
                        VoyageGsonHolder.gson.toJson(
                            storedWaypoints.map {
                                StoredCoordinate(it.latitude, it.longitude)
                            },
                        ),
                    waypointCoordinatesJson = VoyageGsonHolder.gson.toJson(storedWaypoints),
                    plannedDepartureAt = session.startedAtEpochMillis,
                    plannedSpeedKnots = session.plannedSpeedKnots,
                    routeStatus = previous?.routeStatus ?: ROUTE_STATUS_UNSPECIFIED,
                    status =
                        previous
                            ?.status
                            ?.takeIf {
                                it == ActiveVoyageEntity.STATUS_ACTIVE ||
                                    it == ActiveVoyageEntity.STATUS_MINIMIZED
                            } ?: ActiveVoyageEntity.STATUS_ACTIVE,
                    startedAt = session.startedAtEpochMillis,
                    updatedAt = timestamp,
                    endedAt = null,
                    nextWaypointIndex = session.latestNavigation?.activeWaypointIndex ?: 0,
                    distanceMeters = session.totalDistanceNm * METERS_PER_NAUTICAL_MILE,
                    maxSogKnots = session.maxSogKnots,
                    sogSampleSum = session.sogSumKnots,
                    sogSampleCount = session.sogSampleCount,
                )
            val breadcrumbs =
                session.breadcrumbs.mapIndexed { index, breadcrumb ->
                    breadcrumb.toEntity(owner, session.id, index.toLong())
                }
            dao.saveSession(entity, breadcrumbs)
        }
    }

    override suspend fun finishActiveVoyage(voyage: CompletedVoyage) {
        val owner = ownerId()
        database.withTransaction {
            database.logbookDao().insertLog(voyage.toLogbookEntry())
            dao.deleteVoyage(owner, voyage.sessionId)
        }
    }

    override suspend fun clearActiveVoyage() {
        dao.clearActive(ownerId())
    }

    private fun CompletedVoyage.toLogbookEntry(): LogbookEntry =
        LogbookEntry(
            date =
                Instant
                    .ofEpochMilli(startedAtEpochMillis)
                    .atZone(BERLIN_ZONE)
                    .toLocalDate()
                    .format(DATE_FORMATTER),
            routeDesc = "$startName → $destinationName",
            distance = String.format(Locale.GERMANY, "%.1f nm", actualDistanceNm),
            duration = durationLabel(durationSeconds),
            status = "completed",
            details = "GPS-Navigation · geplant ${formatNm(plannedDistanceNm)}",
            voyageId = sessionId,
            startedAt = startedAtEpochMillis,
            endedAt = endedAtEpochMillis,
            actualDistanceMeters = actualDistanceNm * METERS_PER_NAUTICAL_MILE,
            averageSogKnots = averageSogKnots,
            maxSogKnots = maxSogKnots,
            gpsTrackJson =
                VoyageGsonHolder.gson.toJson(
                    breadcrumbs.map {
                        StoredBreadcrumb(
                            latitude = it.coordinate.latitude,
                            longitude = it.coordinate.longitude,
                            timestamp = it.timestampEpochMillis,
                            accuracyMeters = it.horizontalAccuracyMeters,
                            speedKnots = it.speedKnots,
                            courseDegrees = it.courseDegrees,
                        )
                    },
                ),
        )

    private fun ownerId(): String =
        ownerIdProvider().trim().also {
            require(it.isNotEmpty()) { "Eine stabile Owner-ID ist erforderlich." }
        }

    private fun formatNm(value: Double): String = String.format(Locale.GERMANY, "%.1f nm", value)

    private fun durationLabel(seconds: Long): String {
        val safeSeconds = seconds.coerceAtLeast(0)
        val hours = safeSeconds / 3_600
        val minutes = (safeSeconds % 3_600) / 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    private data class StoredWaypoint(
        val id: String,
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val isUserWaypoint: Boolean,
    )

    private data class StoredCoordinate(
        val latitude: Double,
        val longitude: Double,
    )

    private data class StoredBreadcrumb(
        val latitude: Double,
        val longitude: Double,
        val timestamp: Long,
        val accuracyMeters: Double,
        val speedKnots: Double,
        val courseDegrees: Double?,
    )

    companion object {
        private val BERLIN_ZONE = ZoneId.of("Europe/Berlin")
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        private const val ROUTE_STATUS_UNSPECIFIED = "UNSPECIFIED"
    }
}

private fun VoyageBreadcrumb.toEntity(
    ownerId: String,
    voyageId: String,
    sequence: Long,
) = VoyageBreadcrumbEntity(
    ownerId = ownerId,
    voyageId = voyageId,
    sequence = sequence,
    timestamp = timestampEpochMillis,
    latitude = coordinate.latitude,
    longitude = coordinate.longitude,
    accuracyMeters = horizontalAccuracyMeters.toFloat(),
    speedKnots = speedKnots,
    courseDegrees = courseDegrees,
)

private fun VoyageBreadcrumbEntity.toDomain() =
    VoyageBreadcrumb(
        coordinate = GeoPoint(latitude, longitude),
        timestampEpochMillis = timestamp,
        horizontalAccuracyMeters = accuracyMeters.toDouble(),
        speedKnots = speedKnots,
        courseDegrees = courseDegrees,
    )

private object VoyageGsonHolder {
    val gson = Gson()
}
