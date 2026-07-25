package com.example.trnberechnung.model

import java.util.UUID

/**
 * Typ einer Chat-Nachricht.
 */
enum class ChatMessageType {
    TEXT,
    VOICE,
    IMAGE,
    EVENT,
    UNKNOWN
}

enum class ChatDeliveryState {
    PENDING,
    UPLOADING,
    SENT,
    FAILED
}

/**
 * UI-Modell einer kanonischen oder lokal ausstehenden Chat-Nachricht.
 * Die accountbezogene Persistenz übernimmt Room über ChatMessageEntity.
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val serverId: String? = null,
    val clientMessageId: String? = null,
    val threadId: String,
    val senderId: String,          // Skipper-ID des Absenders
    val senderName: String,
    val content: String,           // Textinhalt oder Beschreibung ("Sprachnachricht")
    val type: ChatMessageType = ChatMessageType.TEXT,
    val voiceDurationSeconds: Int = 0,  // Dauer bei VOICE-Nachrichten
    val timestamp: Long = System.currentTimeMillis(),
    val isOwnMessage: Boolean = false,
    val mediaUrl: String? = null,
    val localMediaUri: String? = null,
    val mediaMimeType: String? = null,
    val deliveryState: ChatDeliveryState = ChatDeliveryState.SENT,
    val failureReason: String? = null
)
