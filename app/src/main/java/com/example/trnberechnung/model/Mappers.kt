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
        messages = messages,
        isChatAvailable = !blocked,
        isBlockedByMe = blockedByMe
    )
}

fun ChatThread.toEntity(ownSkipperId: String, ownName: String): ChatThreadEntity {
    return ChatThreadEntity(
        ownerId = ownSkipperId,
        id = id,
        type = type,
        participant1Id = ownSkipperId,
        participant1Name = ownName,
        participant2Id = participantSkipperId,
        participant2Name = participantName,
        lastMessage = lastMessage,
        lastMessageTimestamp = lastMessageTimestamp,
        unreadCount = unreadCount,
        blocked = !isChatAvailable,
        blockedByMe = isBlockedByMe
    )
}

fun ChatMessageEntity.toModel(ownSkipperId: String): ChatMessage {
    return ChatMessage(
        id = id,
        serverId = serverId,
        clientMessageId = clientMessageId,
        threadId = threadId,
        senderId = senderId,
        senderName = senderName,
        content = content,
        type = type,
        voiceDurationSeconds = voiceDurationSeconds,
        timestamp = timestamp,
        isOwnMessage = (senderId == ownSkipperId),
        mediaUrl = mediaUrl,
        localMediaUri = localMediaUri,
        mediaMimeType = mediaMimeType,
        deliveryState =
            runCatching { ChatDeliveryState.valueOf(deliveryState) }
                .getOrDefault(ChatDeliveryState.SENT),
        failureReason = failureReason
    )
}

fun ChatMessage.toEntity(ownerId: String): ChatMessageEntity {
    return ChatMessageEntity(
        ownerId = ownerId,
        id = id,
        serverId = serverId,
        clientMessageId = clientMessageId,
        threadId = threadId,
        senderId = senderId,
        senderName = senderName,
        content = content,
        type = type,
        voiceDurationSeconds = voiceDurationSeconds,
        timestamp = timestamp,
        mediaUrl = mediaUrl,
        localMediaUri = localMediaUri,
        mediaMimeType = mediaMimeType,
        deliveryState = deliveryState.name,
        failureReason = failureReason
    )
}

fun PlannerEventEntity.toModel(): PlannerEvent {
    return PlannerEvent(
        id = id,
        date = date,
        title = title,
        description = description,
        startTime = startTime,
        endTime = endTime,
        location = location,
        category = category
    )
}

fun PlannerEvent.toEntity(): PlannerEventEntity {
    return PlannerEventEntity(
        id = id,
        date = date,
        title = title,
        description = description,
        startTime = startTime,
        endTime = endTime,
        location = location,
        category = category
    )
}
