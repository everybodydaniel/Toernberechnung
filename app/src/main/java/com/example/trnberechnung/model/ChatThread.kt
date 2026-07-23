package com.example.trnberechnung.model

import java.util.UUID

/**
 * Art des Chat-Threads.
 */
enum class ChatThreadType {
    DIRECT,   // Direktnachricht zwischen zwei Personen
    GROUP     // Gruppenchat
}

/**
 * Mock-Datenmodell für einen Chat-Thread (Konversation).
 * Repräsentiert entweder eine Direktnachricht oder eine Gruppenunterhaltung.
 */
data class ChatThread(
    val id: String = UUID.randomUUID().toString(),
    val type: ChatThreadType = ChatThreadType.DIRECT,
    val participantName: String,       // Anzeigename (bei Direkt: Name des Partners, bei Gruppe: Gruppenname)
    val participantSkipperId: String = "",  // Skipper-ID des Chat-Partners (nur bei DIRECT)
    val lastMessage: String = "",      // Vorschau der letzten Nachricht
    val lastMessageTimestamp: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val messages: List<ChatMessage> = emptyList()
)
