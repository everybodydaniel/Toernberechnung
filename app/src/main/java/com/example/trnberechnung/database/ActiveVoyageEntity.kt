package com.example.trnberechnung.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "active_voyages",
    primaryKeys = ["ownerId", "id"],
    indices = [
        Index(value = ["ownerId", "status"], name = "index_active_voyages_ownerId_status"),
    ],
)
data class ActiveVoyageEntity(
    val ownerId: String,
    val id: String,
    val routeId: String,
    val routeDescription: String,
    val startHarbourId: String,
    val destinationHarbourId: String,
    val intermediateHarbourIdsJson: String = "[]",
    val routeCoordinatesJson: String,
    val waypointCoordinatesJson: String,
    val plannedDepartureAt: Long,
    val plannedSpeedKnots: Double,
    val routeStatus: String,
    val status: String = STATUS_ACTIVE,
    val startedAt: Long,
    val updatedAt: Long,
    val endedAt: Long? = null,
    val nextWaypointIndex: Int = 0,
    val distanceMeters: Double = 0.0,
    val maxSogKnots: Double = 0.0,
    val sogSampleSum: Double = 0.0,
    val sogSampleCount: Int = 0,
) {
    companion object {
        const val STATUS_ACTIVE = "ACTIVE"
        const val STATUS_MINIMIZED = "MINIMIZED"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_ABANDONED = "ABANDONED"
    }
}

@Entity(
    tableName = "voyage_breadcrumbs",
    primaryKeys = ["ownerId", "voyageId", "sequence"],
    foreignKeys = [
        ForeignKey(
            entity = ActiveVoyageEntity::class,
            parentColumns = ["ownerId", "id"],
            childColumns = ["ownerId", "voyageId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(
            value = ["ownerId", "voyageId", "timestamp"],
            name = "index_voyage_breadcrumbs_ownerId_voyageId_timestamp",
        ),
    ],
)
data class VoyageBreadcrumbEntity(
    val ownerId: String,
    val voyageId: String,
    val sequence: Long,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val speedKnots: Double,
    val courseDegrees: Double? = null,
)

@Dao
abstract class ActiveVoyageDao {
    @Query(
        """
        SELECT * FROM active_voyages
        WHERE ownerId = :ownerId AND status IN ('ACTIVE', 'MINIMIZED')
        ORDER BY updatedAt DESC
        LIMIT 1
        """,
    )
    abstract fun observeActive(ownerId: String): Flow<ActiveVoyageEntity?>

    @Query(
        """
        SELECT * FROM active_voyages
        WHERE ownerId = :ownerId AND status IN ('ACTIVE', 'MINIMIZED')
        ORDER BY updatedAt DESC
        LIMIT 1
        """,
    )
    abstract suspend fun getActive(ownerId: String): ActiveVoyageEntity?

    @Query(
        "SELECT * FROM active_voyages " +
            "WHERE ownerId = :ownerId AND id = :voyageId LIMIT 1",
    )
    abstract suspend fun getVoyage(
        ownerId: String,
        voyageId: String,
    ): ActiveVoyageEntity?

    @Upsert
    protected abstract suspend fun upsertVoyage(voyage: ActiveVoyageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertBreadcrumbs(breadcrumbs: List<VoyageBreadcrumbEntity>,)

    @Query(
        """
        UPDATE active_voyages
        SET status = 'ABANDONED', endedAt = :at, updatedAt = :at
        WHERE ownerId = :ownerId AND status IN ('ACTIVE', 'MINIMIZED')
        """,
    )
    protected abstract suspend fun abandonActive(
        ownerId: String,
        at: Long,
    )

    @Transaction
    open suspend fun startVoyage(voyage: ActiveVoyageEntity) {
        abandonActive(voyage.ownerId, voyage.startedAt)
        upsertVoyage(voyage)
    }

    @Query(
        """
        UPDATE active_voyages
        SET nextWaypointIndex = :nextWaypointIndex,
            distanceMeters = :distanceMeters,
            maxSogKnots = :maxSogKnots,
            sogSampleSum = :sogSampleSum,
            sogSampleCount = :sogSampleCount,
            updatedAt = :updatedAt
        WHERE ownerId = :ownerId AND id = :voyageId
        """,
    )
    abstract suspend fun updateProgress(
        ownerId: String,
        voyageId: String,
        nextWaypointIndex: Int,
        distanceMeters: Double,
        maxSogKnots: Double,
        sogSampleSum: Double,
        sogSampleCount: Int,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE active_voyages
        SET status = :status, updatedAt = :updatedAt
        WHERE ownerId = :ownerId AND id = :voyageId
        """,
    )
    abstract suspend fun updateStatus(
        ownerId: String,
        voyageId: String,
        status: String,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE active_voyages
        SET status = :status, endedAt = :endedAt, updatedAt = :endedAt
        WHERE ownerId = :ownerId AND id = :voyageId
        """,
    )
    abstract suspend fun finish(
        ownerId: String,
        voyageId: String,
        status: String,
        endedAt: Long,
    )

    @Query(
        "DELETE FROM active_voyages " +
            "WHERE ownerId = :ownerId AND id = :voyageId",
    )
    abstract suspend fun deleteVoyage(
        ownerId: String,
        voyageId: String,
    )

    @Query(
        """
        DELETE FROM active_voyages
        WHERE ownerId = :ownerId AND status IN ('ACTIVE', 'MINIMIZED')
        """,
    )
    abstract suspend fun clearActive(ownerId: String)

    @Query(
        """
        SELECT * FROM voyage_breadcrumbs
        WHERE ownerId = :ownerId AND voyageId = :voyageId
        ORDER BY sequence ASC
        """,
    )
    abstract fun observeBreadcrumbs(
        ownerId: String,
        voyageId: String,
    ): Flow<List<VoyageBreadcrumbEntity>>

    @Query(
        """
        SELECT * FROM voyage_breadcrumbs
        WHERE ownerId = :ownerId AND voyageId = :voyageId
        ORDER BY sequence ASC
        """,
    )
    abstract suspend fun getBreadcrumbs(
        ownerId: String,
        voyageId: String,
    ): List<VoyageBreadcrumbEntity>

    @Query(
        """
        SELECT COALESCE(MAX(sequence), -1) + 1
        FROM voyage_breadcrumbs
        WHERE ownerId = :ownerId AND voyageId = :voyageId
        """,
    )
    protected abstract suspend fun nextSequence(
        ownerId: String,
        voyageId: String,
    ): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertBreadcrumb(entity: VoyageBreadcrumbEntity)

    @Query(
        "DELETE FROM voyage_breadcrumbs " +
            "WHERE ownerId = :ownerId AND voyageId = :voyageId",
    )
    protected abstract suspend fun deleteBreadcrumbs(
        ownerId: String,
        voyageId: String,
    )

    @Query(
        "SELECT COUNT(*) FROM voyage_breadcrumbs " +
            "WHERE ownerId = :ownerId AND voyageId = :voyageId",
    )
    protected abstract suspend fun breadcrumbCount(
        ownerId: String,
        voyageId: String,
    ): Int

    @Transaction
    open suspend fun replaceSession(
        voyage: ActiveVoyageEntity,
        breadcrumbs: List<VoyageBreadcrumbEntity>,
    ) {
        upsertVoyage(voyage)
        deleteBreadcrumbs(voyage.ownerId, voyage.id)
        if (breadcrumbs.isNotEmpty()) insertBreadcrumbs(breadcrumbs)
    }

    @Transaction
    open suspend fun saveSession(
        voyage: ActiveVoyageEntity,
        breadcrumbs: List<VoyageBreadcrumbEntity>,
    ) {
        val currentlyActive = getActive(voyage.ownerId)
        if (currentlyActive != null && currentlyActive.id != voyage.id) {
            abandonActive(voyage.ownerId, voyage.startedAt)
        }
        upsertVoyage(voyage)
        val storedCount = breadcrumbCount(voyage.ownerId, voyage.id)
        if (storedCount > breadcrumbs.size) {
            deleteBreadcrumbs(voyage.ownerId, voyage.id)
            if (breadcrumbs.isNotEmpty()) insertBreadcrumbs(breadcrumbs)
        } else if (storedCount < breadcrumbs.size) {
            insertBreadcrumbs(breadcrumbs.drop(storedCount))
        }
    }

    @Transaction
    open suspend fun appendBreadcrumb(
        ownerId: String,
        voyageId: String,
        timestamp: Long,
        latitude: Double,
        longitude: Double,
        accuracyMeters: Float,
        speedKnots: Double,
        courseDegrees: Double?,
    ): VoyageBreadcrumbEntity {
        val entity =
            VoyageBreadcrumbEntity(
                ownerId = ownerId,
                voyageId = voyageId,
                sequence = nextSequence(ownerId, voyageId),
                timestamp = timestamp,
                latitude = latitude,
                longitude = longitude,
                accuracyMeters = accuracyMeters,
                speedKnots = speedKnots,
                courseDegrees = courseDegrees,
            )
        insertBreadcrumb(entity)
        return entity
    }
}
