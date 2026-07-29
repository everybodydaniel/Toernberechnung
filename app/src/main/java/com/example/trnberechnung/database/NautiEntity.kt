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
    tableName = "nauti_conversations",
    primaryKeys = ["ownerId", "id"],
    indices = [
        Index(value = ["ownerId"], name = "index_nauti_conversations_ownerId"),
        Index(
            value = ["ownerId", "isPinned", "updatedAt"],
            name = "index_nauti_conversations_ownerId_isPinned_updatedAt",
        ),
    ],
)
data class NautiConversationEntity(
    val ownerId: String,
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val lastMessageAt: Long? = null,
    val isPinned: Boolean = false,
    val draft: String = "",
)

@Entity(
    tableName = "nauti_messages",
    primaryKeys = ["ownerId", "id"],
    foreignKeys = [
        ForeignKey(
            entity = NautiConversationEntity::class,
            parentColumns = ["ownerId", "id"],
            childColumns = ["ownerId", "conversationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(
            value = ["ownerId", "conversationId", "createdAt"],
            name = "index_nauti_messages_ownerId_conversationId_createdAt",
        ),
    ],
)
data class NautiMessageEntity(
    val ownerId: String,
    val id: String,
    val conversationId: String,
    val role: String,
    val content: String,
    val createdAt: Long,
    val actionType: String? = null,
    val actionPayloadJson: String? = null,
    val requiresConfirmation: Boolean = false,
    val actionState: String? = null,
    val isError: Boolean = false,
)

@Dao
abstract class NautiDao {
    @Query(
        """
        SELECT * FROM nauti_conversations
        WHERE ownerId = :ownerId
        ORDER BY isPinned DESC, COALESCE(lastMessageAt, updatedAt) DESC, id ASC
        """,
    )
    abstract fun observeConversations(ownerId: String): Flow<List<NautiConversationEntity>>

    @Query(
        """
        SELECT * FROM nauti_conversations
        WHERE ownerId = :ownerId
          AND title LIKE '%' || :query || '%' COLLATE NOCASE
        ORDER BY isPinned DESC, COALESCE(lastMessageAt, updatedAt) DESC, id ASC
        """,
    )
    abstract fun searchConversations(
        ownerId: String,
        query: String,
    ): Flow<List<NautiConversationEntity>>

    @Query(
        "SELECT * FROM nauti_conversations " +
            "WHERE ownerId = :ownerId AND id = :conversationId LIMIT 1",
    )
    abstract suspend fun getConversation(
        ownerId: String,
        conversationId: String,
    ): NautiConversationEntity?

    @Query(
        """
        SELECT * FROM (
            SELECT * FROM nauti_messages
            WHERE ownerId = :ownerId AND conversationId = :conversationId
            ORDER BY createdAt DESC, id DESC
            LIMIT :limit
        )
        ORDER BY createdAt ASC, id ASC
        """,
    )
    abstract fun observeMessages(
        ownerId: String,
        conversationId: String,
        limit: Int = 100,
    ): Flow<List<NautiMessageEntity>>

    @Upsert
    abstract suspend fun upsertConversation(conversation: NautiConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertMessage(message: NautiMessageEntity)

    @Query(
        """
        UPDATE nauti_conversations
        SET title = :title, updatedAt = :updatedAt
        WHERE ownerId = :ownerId AND id = :conversationId
        """,
    )
    abstract suspend fun rename(
        ownerId: String,
        conversationId: String,
        title: String,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE nauti_conversations
        SET isPinned = :pinned, updatedAt = :updatedAt
        WHERE ownerId = :ownerId AND id = :conversationId
        """,
    )
    abstract suspend fun setPinned(
        ownerId: String,
        conversationId: String,
        pinned: Boolean,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE nauti_conversations
        SET draft = :draft, updatedAt = :updatedAt
        WHERE ownerId = :ownerId AND id = :conversationId
        """,
    )
    abstract suspend fun updateDraft(
        ownerId: String,
        conversationId: String,
        draft: String,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE nauti_messages
        SET actionState = :state
        WHERE ownerId = :ownerId
          AND conversationId = :conversationId
          AND id = :messageId
        """,
    )
    abstract suspend fun updateActionState(
        ownerId: String,
        conversationId: String,
        messageId: String,
        state: String,
    )

    @Query(
        """
        UPDATE nauti_conversations
        SET lastMessageAt = :messageAt, updatedAt = :messageAt
        WHERE ownerId = :ownerId AND id = :conversationId
        """,
    )
    protected abstract suspend fun touchConversation(
        ownerId: String,
        conversationId: String,
        messageAt: Long,
    )

    @Query(
        """
        DELETE FROM nauti_messages
        WHERE ownerId = :ownerId
          AND conversationId = :conversationId
          AND id NOT IN (
              SELECT id FROM nauti_messages
              WHERE ownerId = :ownerId AND conversationId = :conversationId
              ORDER BY createdAt DESC, id DESC
              LIMIT :keep
          )
        """,
    )
    protected abstract suspend fun trimMessages(
        ownerId: String,
        conversationId: String,
        keep: Int,
    )

    @Query(
        "DELETE FROM nauti_conversations " +
            "WHERE ownerId = :ownerId AND id = :conversationId",
    )
    abstract suspend fun deleteConversation(
        ownerId: String,
        conversationId: String,
    )

    @Transaction
    open suspend fun insertAndTrim(
        message: NautiMessageEntity,
        keep: Int = 100,
    ) {
        require(keep > 0)
        insertMessage(message)
        touchConversation(message.ownerId, message.conversationId, message.createdAt)
        trimMessages(message.ownerId, message.conversationId, keep)
    }
}
