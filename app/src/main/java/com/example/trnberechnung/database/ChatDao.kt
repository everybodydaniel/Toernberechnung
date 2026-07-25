package com.example.trnberechnung.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query(
        "SELECT * FROM chat_threads WHERE ownerId = :ownerId " +
            "ORDER BY lastMessageTimestamp DESC, id ASC",
    )
    fun getThreadsForUser(ownerId: String): Flow<List<ChatThreadEntity>>

    @Query("SELECT * FROM chat_threads WHERE ownerId = :ownerId AND id = :threadId LIMIT 1")
    suspend fun getThread(
        ownerId: String,
        threadId: String,
    ): ChatThreadEntity?

    @Query("SELECT * FROM chat_threads WHERE ownerId = :ownerId")
    suspend fun getThreads(ownerId: String): List<ChatThreadEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThread(thread: ChatThreadEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThreads(threads: List<ChatThreadEntity>)

    @Query(
        "SELECT * FROM chat_messages WHERE ownerId = :ownerId AND threadId = :threadId " +
            "ORDER BY timestamp ASC, id ASC",
    )
    fun getMessagesForThread(
        ownerId: String,
        threadId: String,
    ): Flow<List<ChatMessageEntity>>

    @Query(
        "SELECT * FROM chat_messages WHERE ownerId = :ownerId AND id = :localId LIMIT 1",
    )
    suspend fun getMessage(
        ownerId: String,
        localId: String,
    ): ChatMessageEntity?

    @Query(
        "SELECT * FROM chat_messages WHERE ownerId = :ownerId AND serverId = :serverId LIMIT 1",
    )
    suspend fun getMessageByServerId(
        ownerId: String,
        serverId: String,
    ): ChatMessageEntity?

    @Query(
        "SELECT * FROM chat_messages " +
            "WHERE ownerId = :ownerId AND threadId = :threadId AND clientMessageId = :clientMessageId " +
            "LIMIT 1",
    )
    suspend fun getMessageByClientId(
        ownerId: String,
        threadId: String,
        clientMessageId: String,
    ): ChatMessageEntity?

    @Query(
        "SELECT * FROM chat_messages WHERE ownerId = :ownerId " +
            "AND deliveryState IN ('PENDING', 'UPLOADING') ORDER BY timestamp ASC",
    )
    suspend fun getPendingMessages(ownerId: String): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE ownerId = :ownerId AND id = :localId")
    suspend fun deleteMessage(
        ownerId: String,
        localId: String,
    )

    @Query(
        "UPDATE chat_messages SET deliveryState = :state, failureReason = :reason " +
            "WHERE ownerId = :ownerId AND id = :localId",
    )
    suspend fun updateDeliveryState(
        ownerId: String,
        localId: String,
        state: String,
        reason: String? = null,
    )

    @Query(
        "UPDATE chat_messages SET mediaUrl = :mediaUrl, deliveryState = 'PENDING' " +
            "WHERE ownerId = :ownerId AND id = :localId",
    )
    suspend fun markMediaUploaded(
        ownerId: String,
        localId: String,
        mediaUrl: String,
    )

    @Query(
        "UPDATE chat_threads SET blockedByMe = 0 " +
            "WHERE ownerId = :ownerId AND type = 'DIRECT'",
    )
    suspend fun clearDirectChatBlocks(ownerId: String)

    @Query(
        "UPDATE chat_threads SET blocked = :blocked, blockedByMe = :blocked " +
            "WHERE ownerId = :ownerId AND participant2Id = :otherUid AND type = 'DIRECT'",
    )
    suspend fun setDirectChatBlocked(
        ownerId: String,
        otherUid: String,
        blocked: Boolean,
    )

    @Query(
        "UPDATE chat_threads SET afterCursor = :afterCursor, beforeCursor = :beforeCursor " +
            "WHERE ownerId = :ownerId AND id = :threadId",
    )
    suspend fun updateCursors(
        ownerId: String,
        threadId: String,
        afterCursor: String?,
        beforeCursor: String?,
    )

    @Query(
        "UPDATE chat_threads SET lastMessage = :preview, lastMessageTimestamp = :timestamp " +
            "WHERE ownerId = :ownerId AND id = :threadId",
    )
    suspend fun updateThreadPreview(
        ownerId: String,
        threadId: String,
        preview: String,
        timestamp: Long,
    )

    @Query(
        "UPDATE chat_threads SET unreadCount = 0 WHERE ownerId = :ownerId AND id = :threadId",
    )
    suspend fun clearUnreadCount(
        ownerId: String,
        threadId: String,
    )
}
