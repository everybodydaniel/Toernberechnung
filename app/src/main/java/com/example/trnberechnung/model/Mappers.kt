package com.example.trnberechnung.model

import com.example.trnberechnung.database.ChatMessageEntity
import com.example.trnberechnung.database.ChatThreadEntity
import com.example.trnberechnung.database.PlannerEventEntity

fun ChatThreadEntity.toModel(ownSkipperId: String, messages: List<ChatMessage> = emptyList()): ChatThread {
    val otherName = if (ownSkipperId == participant1Id) participant2Name else participant1Name
    val otherId = if (ownSkipperId == participant1Id) participant2Id else participant1Id
    
    return ChatThread(
        id = id,
        type = type,
        participantName = otherName,
        participantSkipperId = otherId,
        lastMessage = lastMessage,
        lastMessageTimestamp = lastMessageTimestamp,
        unreadCount = unreadCount,
        messages = messages
    )
}

fun ChatThread.toEntity(ownSkipperId: String, ownName: String): ChatThreadEntity {
    return ChatThreadEntity(
        id = id,
        type = type,
        participant1Id = ownSkipperId,
        participant1Name = ownName,
        participant2Id = participantSkipperId,
        participant2Name = participantName,
        lastMessage = lastMessage,
        lastMessageTimestamp = lastMessageTimestamp,
        unreadCount = unreadCount
    )
}

fun ChatMessageEntity.toModel(ownSkipperId: String): ChatMessage {
    return ChatMessage(
        id = id,
        threadId = threadId,
        senderId = senderId,
        senderName = senderName,
        content = content,
        type = type,
        voiceDurationSeconds = voiceDurationSeconds,
        timestamp = timestamp,
        isOwnMessage = (senderId == ownSkipperId)
    )
}

fun ChatMessage.toEntity(): ChatMessageEntity {
    return ChatMessageEntity(
        id = id,
        threadId = threadId,
        senderId = senderId,
        senderName = senderName,
        content = content,
        type = type,
        voiceDurationSeconds = voiceDurationSeconds,
        timestamp = timestamp
    )
}

fun PlannerEventEntity.toModel(): PlannerEvent {
    return PlannerEvent(
        id = id,
        date = date,
        title = title,
        description = description
    )
}

fun PlannerEvent.toEntity(): PlannerEventEntity {
    return PlannerEventEntity(
        id = id,
        date = date,
        title = title,
        description = description
    )
}
