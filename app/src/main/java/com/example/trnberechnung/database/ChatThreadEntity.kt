package com.example.trnberechnung.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.trnberechnung.model.ChatThreadType

@Entity(tableName = "chat_threads")
data class ChatThreadEntity(
    @PrimaryKey val id: String,
    val type: ChatThreadType,
    val participant1Id: String,
    val participant1Name: String,
    val participant2Id: String,
    val participant2Name: String,
    val lastMessage: String,
    val lastMessageTimestamp: Long,
    val unreadCount: Int
)
