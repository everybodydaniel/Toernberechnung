package com.example.trnberechnung.model

import java.util.UUID

/**
 * Typ einer Chat-Nachricht.
 */
enum class ChatMessageType {
    TEXT,
    VOICE,
    IMAGE,
    EVENT
}

/**
 * Mock-Datenmodell für eine einzelne Chat-Nachricht.
 * Wird vorerst nicht in Room gespeichert (reine In-Memory / Mock-Daten).
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val threadId: String,
    val senderId: String,          // Skipper-ID des Absenders
    val senderName: String,
    val content: String,           // Textinhalt oder Beschreibung ("Sprachnachricht")
    val type: ChatMessageType = ChatMessageType.TEXT,
    val voiceDurationSeconds: Int = 0,  // Dauer bei VOICE-Nachrichten
    val timestamp: Long = System.currentTimeMillis(),
    val isOwnMessage: Boolean = false
)
