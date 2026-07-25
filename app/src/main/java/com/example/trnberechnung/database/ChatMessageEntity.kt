package com.example.trnberechnung.database

import androidx.room.Entity
import androidx.room.Index
import com.example.trnberechnung.model.ChatMessageType

@Entity(
    tableName = "chat_messages",
    primaryKeys = ["ownerId", "id"],
    indices = [
        Index(
            value = ["ownerId", "serverId"],
            name = "index_chat_messages_ownerId_serverId",
            unique = true,
        ),
        Index(
            value = ["ownerId", "threadId", "clientMessageId"],
            name = "index_chat_messages_ownerId_threadId_clientMessageId",
            unique = true,
        ),
        Index(
            value = ["ownerId", "threadId", "timestamp"],
            name = "index_chat_messages_ownerId_threadId_timestamp",
        ),
    ],
)
data class ChatMessageEntity(
    val ownerId: String,
    val id: String,
    val serverId: String? = null,
    val clientMessageId: String? = null,
    val threadId: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val type: ChatMessageType,
    val voiceDurationSeconds: Int,
    val timestamp: Long,
    val serverCreatedAt: String? = null,
    val mediaUrl: String? = null,
    val localMediaUri: String? = null,
    val mediaMimeType: String? = null,
    val deliveryState: String = "SENT",
    val failureReason: String? = null,
)
