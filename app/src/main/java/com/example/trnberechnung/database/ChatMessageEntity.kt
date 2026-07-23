package com.example.trnberechnung.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.trnberechnung.model.ChatMessageType

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val threadId: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val type: ChatMessageType,
    val voiceDurationSeconds: Int,
    val timestamp: Long
)
