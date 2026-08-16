package com.example.trnberechnung.model

import java.time.LocalDate

/**
 * Die beiden Haupt-Tabs des Crew-Screens.
 */
enum class CrewspaceTab(val label: String) {
    PLANUNG("Planung"),
    CREW("Crew"),
}

/**
 * Gesamter UI-State für den CrewspaceScreen.
 *
 * Rein lokal: seit dem Wegfall des Crewspace-Servers stammen Termine und Crewliste
 * ausschließlich aus Room, es gibt keinen angemeldeten Skipper mehr.
 */
data class CrewspaceUiState(
    // ── Allgemein ──
    val selectedTab: CrewspaceTab = CrewspaceTab.PLANUNG,
    val searchQuery: String = "",
    val formError: String? = null,

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
)
