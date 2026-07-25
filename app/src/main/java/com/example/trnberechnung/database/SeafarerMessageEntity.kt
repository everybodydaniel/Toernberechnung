package com.example.trnberechnung.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Room entity representing a BfS (Bekanntmachungen für Seefahrer) nautical message.
 * These are official maritime safety notifications (NtM - Notices to Mariners).
 */
@Entity(tableName = "seafarer_messages")
data class SeafarerMessageEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val area: String,            // Geographic area, e.g. "Nordsee", "Ostsee"
    val category: String,        // e.g. "NfS", "Warnung", "Information"
    val content: String,         // Full message text
    val publishedAt: Long,       // Epoch millis of publication date
    val expiresAt: Long? = null, // Optional expiry date
    val source: String = "BSH",  // Source authority
    val isRead: Boolean = false,
    val isArchived: Boolean = false,
    val bfsNumber: String = "",  // Official BfS reference number
    val latitude: Double? = null,
    val longitude: Double? = null
)

/**
 * DAO for seafarer messages (BfS-Nachrichten).
 */
@Dao
interface SeafarerMessageDao {

    @Query("SELECT * FROM seafarer_messages WHERE isArchived = 0 ORDER BY publishedAt DESC")
    fun getAllActive(): Flow<List<SeafarerMessageEntity>>

    @Query("SELECT * FROM seafarer_messages WHERE isRead = 0 AND isArchived = 0 ORDER BY publishedAt DESC")
    fun getUnread(): Flow<List<SeafarerMessageEntity>>

    @Query("SELECT * FROM seafarer_messages WHERE isRead = 1 AND isArchived = 0 ORDER BY publishedAt DESC")
    fun getRead(): Flow<List<SeafarerMessageEntity>>

    @Query("SELECT * FROM seafarer_messages WHERE isArchived = 1 ORDER BY publishedAt DESC")
    fun getArchived(): Flow<List<SeafarerMessageEntity>>

    @Query("SELECT COUNT(*) FROM seafarer_messages WHERE isRead = 0 AND isArchived = 0")
    fun getUnreadCount(): Flow<Int>

    @Query("SELECT * FROM seafarer_messages WHERE title LIKE '%' || :query || '%' OR area LIKE '%' || :query || '%' OR bfsNumber LIKE '%' || :query || '%' ORDER BY publishedAt DESC")
    fun search(query: String): Flow<List<SeafarerMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<SeafarerMessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: SeafarerMessageEntity)

    @Update
    suspend fun update(message: SeafarerMessageEntity)

    @Query("UPDATE seafarer_messages SET isRead = 1 WHERE isArchived = 0")
    suspend fun markAllAsRead()

    @Query("UPDATE seafarer_messages SET isRead = 1 WHERE id = :messageId")
    suspend fun markAsRead(messageId: String)

    @Query("UPDATE seafarer_messages SET isArchived = 1 WHERE id = :messageId")
    suspend fun archive(messageId: String)

    @Query("DELETE FROM seafarer_messages")
    suspend fun deleteAll()
}
