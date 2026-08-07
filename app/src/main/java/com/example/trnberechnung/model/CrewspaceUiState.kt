package com.example.trnberechnung.model

import java.time.LocalDate

/**
 * Die drei Haupt-Tabs des Crewspace-Screens.
 */
enum class CrewspaceTab(val label: String) {
    CHATS("Chats"),
    PLANUNG("Planung"),
    CREW("Crew"),
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
    val chatConnectionState: String = "DISCONNECTED",
    val chatError: String? = null,
    val chatBusy: Boolean = false,

    // ── Lösch-Bestätigung & Bearbeitung ──
    val memberToDelete: CrewMember? = null,
    val eventToDelete: PlannerEvent? = null,
    val editingMember: CrewMember? = null,

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

    // ── Profil / Einstellungen ──
    val ownSkipperId: String = "",
    val ownDisplayName: String = "",
    val ownEmail: String = ""
)
