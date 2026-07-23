package com.example.trnberechnung.model

import java.time.LocalDate

/**
 * Die drei Haupt-Tabs des Crewspace-Screens.
 */
enum class CrewspaceTab(val label: String) {
    CHATS("Chats"),
    PLANUNG("Planung"),
    CREW("Crew"),
    AI_ASSISTENT("AI")
}

/**
 * Sub-Tabs im "Neue Unterhaltung"-BottomSheet.
 */
enum class NewConversationTab {
    DIRECT,
    GROUP
}

/**
 * Gesamter UI-State für den CrewspaceScreen.
 */
data class CrewspaceUiState(
    // ── Allgemein ──
    val selectedTab: CrewspaceTab = CrewspaceTab.CHATS,
    val searchQuery: String = "",

    // ── Chats ──
    val chatThreads: List<ChatThread> = emptyList(),
    val activeChatThread: ChatThread? = null,      // Aktuell geöffneter Chat
    val chatInput: String = "",
    val showNewConversationSheet: Boolean = false,
    val newConversationTab: NewConversationTab = NewConversationTab.DIRECT,
    val newConversationSkipperId: String = "",

    // ── Planung ──
    val selectedDate: LocalDate = LocalDate.now(),
    val currentMonth: LocalDate = LocalDate.now().withDayOfMonth(1),
    val plannerEvents: List<PlannerEvent> = emptyList(),

    // ── Crew ──
    val crewMembers: List<CrewMember> = emptyList(),
    val onBoardCount: Int = 0,

    // ── Crew hinzufügen Formular ──
    val addName: String = "",
    val addSkipperId: String = "",
    val addSelectedRole: CrewRole = CrewRole.SKIPPER,
    val addEmergencyContact: String = "",
    val addPhone: String = "",
    val addMedicalNotes: String = "",

    // ── AI Assistent ──
    val aiMessages: List<AiChatMessage> = emptyList(),
    val aiInput: String = "",
    val aiIsLoading: Boolean = false,

    // ── Profil / Einstellungen ──
    val ownSkipperId: String = "ApJsWzmN0PXHF7pVJsx9saqzD1x2",
    val ownDisplayName: String = "Daniel",
    val ownEmail: String = "gamil.vom.com4@gmail.com"
)

/**
 * Einzelne Nachricht im AI-Chat.
 */
data class AiChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
