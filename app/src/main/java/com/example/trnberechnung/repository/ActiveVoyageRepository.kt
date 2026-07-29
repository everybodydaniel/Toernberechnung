package com.example.trnberechnung.repository

import com.example.trnberechnung.database.ActiveVoyageDao
import com.example.trnberechnung.database.ActiveVoyageEntity
import com.example.trnberechnung.database.VoyageBreadcrumbEntity
import kotlinx.coroutines.flow.Flow

data class CompletedVoyage(
    val voyage: ActiveVoyageEntity,
    val breadcrumbs: List<VoyageBreadcrumbEntity>,
) {
    val averageSogKnots: Double
        get() =
            if (voyage.sogSampleCount > 0) {
                voyage.sogSampleSum / voyage.sogSampleCount
            } else {
                0.0
            }
}

class ActiveVoyageRepository(
    private val dao: ActiveVoyageDao,
    private val ownerIdProvider: () -> String,
    private val now: () -> Long = System::currentTimeMillis,
) {
    val activeVoyage: Flow<ActiveVoyageEntity?>
        get() = dao.observeActive(ownerId())

    fun breadcrumbs(voyageId: String): Flow<List<VoyageBreadcrumbEntity>> =
        dao.observeBreadcrumbs(ownerId(), voyageId.requireIdentifier())

    suspend fun start(voyage: ActiveVoyageEntity) {
        val timestamp = now()
        dao.startVoyage(
            voyage.copy(
                ownerId = ownerId(),
                id = voyage.id.requireIdentifier(),
                status = ActiveVoyageEntity.STATUS_ACTIVE,
                startedAt = timestamp,
                updatedAt = timestamp,
                endedAt = null,
            ),
        )
    }

    suspend fun updateProgress(
        voyageId: String,
        nextWaypointIndex: Int,
        distanceMeters: Double,
        maxSogKnots: Double,
        sogSampleSum: Double,
        sogSampleCount: Int,
    ) {
        require(nextWaypointIndex >= 0)
        require(distanceMeters >= 0.0)
        require(maxSogKnots >= 0.0)
        require(sogSampleCount >= 0)
        dao.updateProgress(
            ownerId = ownerId(),
            voyageId = voyageId.requireIdentifier(),
            nextWaypointIndex = nextWaypointIndex,
            distanceMeters = distanceMeters,
            maxSogKnots = maxSogKnots,
            sogSampleSum = sogSampleSum,
            sogSampleCount = sogSampleCount,
            updatedAt = now(),
        )
    }

    suspend fun appendBreadcrumb(
        voyageId: String,
        timestamp: Long,
        latitude: Double,
        longitude: Double,
        accuracyMeters: Float,
        speedKnots: Double,
        courseDegrees: Double?,
    ): VoyageBreadcrumbEntity {
        require(latitude in -90.0..90.0)
        require(longitude in -180.0..180.0)
        require(accuracyMeters >= 0f)
        require(speedKnots >= 0.0)
        return dao.appendBreadcrumb(
            ownerId = ownerId(),
            voyageId = voyageId.requireIdentifier(),
            timestamp = timestamp,
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = accuracyMeters,
            speedKnots = speedKnots,
            courseDegrees = courseDegrees,
        )
    }

    suspend fun minimize(voyageId: String) {
        dao.updateStatus(
            ownerId(),
            voyageId.requireIdentifier(),
            ActiveVoyageEntity.STATUS_MINIMIZED,
            now(),
        )
    }

    suspend fun resume(voyageId: String) {
        dao.updateStatus(
            ownerId(),
            voyageId.requireIdentifier(),
            ActiveVoyageEntity.STATUS_ACTIVE,
            now(),
        )
    }

    suspend fun complete(voyageId: String): CompletedVoyage? = finish(voyageId, ActiveVoyageEntity.STATUS_COMPLETED)

    suspend fun abandon(voyageId: String): CompletedVoyage? = finish(voyageId, ActiveVoyageEntity.STATUS_ABANDONED)

    private suspend fun finish(
        voyageId: String,
        status: String,
    ): CompletedVoyage? {
        val cleanId = voyageId.requireIdentifier()
        val owner = ownerId()
        dao.finish(owner, cleanId, status, now())
        val voyage = dao.getVoyage(owner, cleanId) ?: return null
        return CompletedVoyage(voyage, dao.getBreadcrumbs(owner, cleanId))
    }

    private fun ownerId(): String = ownerIdProvider().trim().requireIdentifier()

    private fun String.requireIdentifier(): String {
        val clean = trim()
        require(clean.isNotEmpty()) { "Eine stabile ID ist erforderlich." }
        return clean
    }
}
