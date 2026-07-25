package com.example.trnberechnung.database

import androidx.room.Entity
import androidx.room.Index
import com.example.trnberechnung.model.ChatThreadType

@Entity(
    tableName = "chat_threads",
    primaryKeys = ["ownerId", "id"],
    indices = [
        Index(value = ["ownerId"]),
        Index(value = ["ownerId", "lastMessageTimestamp"]),
    ],
)
data class ChatThreadEntity(
    val ownerId: String,
    val id: String,
    val type: ChatThreadType,
    val participant1Id: String,
    val participant1Name: String,
    val participant2Id: String,
    val participant2Name: String,
    val lastMessage: String,
    val lastMessageTimestamp: Long,
    val unreadCount: Int,
    val lastMessageAt: String? = null,
    val updatedAt: String? = null,
    val blocked: Boolean = false,
    val blockedByMe: Boolean = false,
    val afterCursor: String? = null,
    val beforeCursor: String? = null,
)
