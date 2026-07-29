package com.example.trnberechnung.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Offline copy of an official ELWIS "Bekanntmachung für Seefahrer".
 *
 * Summary and detail data deliberately share one row. [detailRevision] tells
 * consumers whether [body] and the JSON detail columns belong to the current
 * server revision. [readRevision] is independent of server updates, so a
 * revised notice automatically becomes unread again.
 */
@Entity(
    tableName = "seafarer_messages",
    indices = [
        Index(value = ["updatedAt"], name = "index_seafarer_messages_updatedAt"),
        Index(
            value = ["publicationState", "validUntil"],
            name = "index_seafarer_messages_publicationState_validUntil",
        ),
    ],
)
data class SeafarerMessageEntity(
    @PrimaryKey
    val id: String,
    val bfsNumber: String,
    val isTemporary: Boolean = false,
    val publisher: String,
    val title: String,
    val regionPath: String,
    val location: String? = null,
    val body: String = "",
    val publishedAt: Long? = null,
    val validFrom: Long? = null,
    val validUntil: Long? = null,
    val publicationState: String = PUBLICATION_CURRENT,
    val revision: Int = 1,
    val updatedAt: Long,
    val sourceUrl: String? = null,
    val chartReferencesJson: String = "[]",
    val coordinatesJson: String = "[]",
    val previousNoticesJson: String = "[]",
    val parseStatus: String = PARSE_PARTIAL,
    val readRevision: Int = 0,
    val detailRevision: Int = 0,
    val detailFetchedAt: Long? = null,
    val cachedAt: Long,
    val locallyArchived: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
) {
    // Compatibility accessors for the pre-v11 UI while it is being replaced.
    val area: String
        get() = regionPath
    val category: String
        get() = publicationState
    val content: String
        get() = body
    val expiresAt: Long?
        get() = validUntil
    val source: String
        get() = publisher
    val isRead: Boolean
        get() = readRevision >= revision
    val isArchived: Boolean
        get() =
            locallyArchived ||
                publicationState == PUBLICATION_REVOKED ||
                publicationState == PUBLICATION_EXPIRED ||
                (validUntil?.let { it < System.currentTimeMillis() } == true)

    companion object {
        const val PUBLICATION_CURRENT = "current"
        const val PUBLICATION_UPDATED = "updated"
        const val PUBLICATION_REVOKED = "revoked"
        const val PUBLICATION_EXPIRED = "expired"

        const val PARSE_PARSED = "parsed"
        const val PARSE_PARTIAL = "partial"
        const val PARSE_FAILED = "failed"
    }
}

@Entity(tableName = "maritime_notice_sync")
data class MaritimeNoticeSyncEntity(
    @PrimaryKey
    val id: Int = SINGLETON_ID,
    val fetchedAt: Long,
    val lastIngestedAt: Long? = null,
    val etag: String? = null,
    val isStale: Boolean = false,
    val lastError: String? = null,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}

@Dao
interface SeafarerMessageDao {
    @Query(
        """
        SELECT * FROM seafarer_messages
        WHERE locallyArchived = 0
          AND publicationState NOT IN ('revoked', 'expired')
          AND (validUntil IS NULL OR validUntil >= (CAST(strftime('%s','now') AS INTEGER) * 1000))
        ORDER BY updatedAt DESC, id ASC
        """,
    )
    fun getAllActive(): Flow<List<SeafarerMessageEntity>>

    @Query(
        """
        SELECT * FROM seafarer_messages
        WHERE readRevision < revision
          AND locallyArchived = 0
          AND publicationState NOT IN ('revoked', 'expired')
          AND (validUntil IS NULL OR validUntil >= (CAST(strftime('%s','now') AS INTEGER) * 1000))
        ORDER BY updatedAt DESC, id ASC
        """,
    )
    fun getUnread(): Flow<List<SeafarerMessageEntity>>

    @Query(
        """
        SELECT * FROM seafarer_messages
        WHERE readRevision >= revision
          AND locallyArchived = 0
          AND publicationState NOT IN ('revoked', 'expired')
          AND (validUntil IS NULL OR validUntil >= (CAST(strftime('%s','now') AS INTEGER) * 1000))
        ORDER BY updatedAt DESC, id ASC
        """,
    )
    fun getRead(): Flow<List<SeafarerMessageEntity>>

    @Query(
        """
        SELECT * FROM seafarer_messages
        WHERE locallyArchived = 1
           OR publicationState IN ('revoked', 'expired')
           OR (validUntil IS NOT NULL AND validUntil < (CAST(strftime('%s','now') AS INTEGER) * 1000))
        ORDER BY updatedAt DESC, id ASC
        """,
    )
    fun getArchived(): Flow<List<SeafarerMessageEntity>>

    @Query(
        """
        SELECT COUNT(*) FROM seafarer_messages
        WHERE readRevision < revision
          AND locallyArchived = 0
          AND publicationState NOT IN ('revoked', 'expired')
          AND (validUntil IS NULL OR validUntil >= (CAST(strftime('%s','now') AS INTEGER) * 1000))
        """,
    )
    fun getUnreadCount(): Flow<Int>

    @Query(
        """
        SELECT * FROM seafarer_messages
        WHERE title LIKE '%' || :query || '%' COLLATE NOCASE
           OR regionPath LIKE '%' || :query || '%' COLLATE NOCASE
           OR location LIKE '%' || :query || '%' COLLATE NOCASE
           OR publisher LIKE '%' || :query || '%' COLLATE NOCASE
           OR bfsNumber LIKE '%' || :query || '%' COLLATE NOCASE
        ORDER BY updatedAt DESC, id ASC
        """,
    )
    fun search(query: String): Flow<List<SeafarerMessageEntity>>

    @Query("SELECT * FROM seafarer_messages ORDER BY updatedAt DESC, id ASC")
    fun observeAll(): Flow<List<SeafarerMessageEntity>>

    @Query(
        """
        SELECT * FROM seafarer_messages
        WHERE locallyArchived = 0
          AND publicationState NOT IN ('revoked', 'expired')
          AND (validUntil IS NULL OR validUntil >= :now)
        ORDER BY updatedAt DESC, id ASC
        """,
    )
    fun observeCurrent(now: Long): Flow<List<SeafarerMessageEntity>>

    @Query(
        """
        SELECT * FROM seafarer_messages
        WHERE readRevision < revision
          AND locallyArchived = 0
          AND publicationState NOT IN ('revoked', 'expired')
          AND (validUntil IS NULL OR validUntil >= :now)
        ORDER BY updatedAt DESC, id ASC
        """,
    )
    fun observeUnread(now: Long): Flow<List<SeafarerMessageEntity>>

    @Query(
        """
        SELECT * FROM seafarer_messages
        WHERE locallyArchived = 1
           OR publicationState IN ('revoked', 'expired')
           OR (validUntil IS NOT NULL AND validUntil < :now)
        ORDER BY updatedAt DESC, id ASC
        """,
    )
    fun observeArchive(now: Long): Flow<List<SeafarerMessageEntity>>

    @Query("SELECT * FROM seafarer_messages ORDER BY updatedAt DESC, id ASC")
    suspend fun getAllOnce(): List<SeafarerMessageEntity>

    @Query("SELECT * FROM seafarer_messages WHERE id = :noticeId LIMIT 1")
    suspend fun getById(noticeId: String): SeafarerMessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<SeafarerMessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: SeafarerMessageEntity)

    @Query(
        """
        UPDATE seafarer_messages
        SET readRevision = CASE
            WHEN readRevision < revision THEN revision
            ELSE readRevision
        END
        WHERE id = :messageId
        """,
    )
    suspend fun markAsRead(messageId: String)

    @Query(
        """
        UPDATE seafarer_messages
        SET readRevision = CASE
            WHEN readRevision < revision THEN revision
            ELSE readRevision
        END
        WHERE locallyArchived = 0
        """,
    )
    suspend fun markAllAsRead()

    @Query("UPDATE seafarer_messages SET locallyArchived = 1 WHERE id = :messageId")
    suspend fun archive(messageId: String)

    @Query("DELETE FROM seafarer_messages")
    suspend fun deleteAll()
}

@Dao
interface MaritimeNoticeSyncDao {
    @Query("SELECT * FROM maritime_notice_sync WHERE id = 1 LIMIT 1")
    suspend fun get(): MaritimeNoticeSyncEntity?

    @Query("SELECT * FROM maritime_notice_sync WHERE id = 1 LIMIT 1")
    fun observe(): Flow<MaritimeNoticeSyncEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(metadata: MaritimeNoticeSyncEntity)
}
