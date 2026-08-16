package com.example.trnberechnung.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trnberechnung.model.*
import com.example.trnberechnung.repository.TideRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class CrewspaceViewModel(
    private val repository: TideRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CrewspaceUiState())
    val uiState: StateFlow<CrewspaceUiState> = _uiState.asStateFlow()

    init {
        // Crew-Daten aus der Datenbank beobachten
        viewModelScope.launch {
            repository.allCrew.collect { crewList ->
                _uiState.update { state ->
                    state.copy(
                        crewMembers = crewList,
                        onBoardCount = crewList.count { it.isOnBoard }
                    )
                }
            }
        }

        // Planner Events beobachten
        viewModelScope.launch {
            repository.allPlannerEvents.collect { events ->
                _uiState.update { state ->
                    state.copy(plannerEvents = events.map { it.toModel() })
                }
            }
        }

    }

    // ══════════════════════════════════════════════════════════════
    // Tab-Navigation
    // ══════════════════════════════════════════════════════════════

    fun selectTab(tab: CrewspaceTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }


    // ══════════════════════════════════════════════════════════════
    // Planung
    // ══════════════════════════════════════════════════════════════

    fun selectDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun navigateMonth(forward: Boolean) {
        _uiState.update {
            val newMonth = if (forward) {
                it.currentMonth.plusMonths(1)
            } else {
                it.currentMonth.minusMonths(1)
            }
            it.copy(currentMonth = newMonth)
        }
    }

    fun setCurrentMonth(date: LocalDate) {
        _uiState.update { it.copy(currentMonth = date.withDayOfMonth(1)) }
    }

    fun addPlannerEvent(event: PlannerEvent) {
        viewModelScope.launch {
            repository.insertPlannerEvent(event.toEntity())
        }
    }

    fun addPlannerEventWithDate(title: String, description: String, date: LocalDate) {
        val event = PlannerEvent(
            startDate = date,
            endDate = date,
            title = title,
            description = description
        )
        viewModelScope.launch {
            repository.insertPlannerEvent(event.toEntity())
        }
    }

    fun updatePlannerEvent(event: PlannerEvent) {
        viewModelScope.launch {
            repository.insertPlannerEvent(event.toEntity())
        }
    }

    fun deletePlannerEvent(event: PlannerEvent) {
        _uiState.update { it.copy(eventToDelete = event) }
    }

    fun confirmDeletePlannerEvent() {
        val event = _uiState.value.eventToDelete ?: return
        viewModelScope.launch {
            repository.deletePlannerEvent(event.toEntity())
            _uiState.update { it.copy(eventToDelete = null) }
        }
    }

    fun cancelDeletePlannerEvent() {
        _uiState.update { it.copy(eventToDelete = null) }
    }

    /** Events für das ausgewählte Datum (inkl. mehrtägige Termine) */
    fun eventsForSelectedDate(): List<PlannerEvent> {
        val state = _uiState.value
        val selected = state.selectedDate
        return state.plannerEvents.filter { event ->
            // Ein Event ist relevant, wenn das ausgewählte Datum zwischen Start und Ende liegt
            !selected.isBefore(event.startDate) && !selected.isAfter(event.endDate)
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Crew-Verwaltung
    // ══════════════════════════════════════════════════════════════

    fun updateAddName(name: String) {
        _uiState.update { it.copy(addName = name) }
    }

    fun updateAddSkipperId(id: String) {
        _uiState.update { it.copy(addSkipperId = id) }
    }

    fun updateAddSelectedRole(role: CrewRole) {
        _uiState.update { it.copy(addSelectedRole = role) }
    }

    fun updateAddEmergencyContact(contact: String) {
        _uiState.update { it.copy(addEmergencyContact = contact) }
    }

    fun updateAddPhone(phone: String) {
        _uiState.update { it.copy(addPhone = phone) }
    }

    fun updateAddMedicalNotes(notes: String) {
        _uiState.update { it.copy(addMedicalNotes = notes) }
    }

    fun addCrewMember() {
        val state = _uiState.value
        if (state.addName.isBlank() && state.addSkipperId.isBlank()) return

        // Dubletten-Prüfung
        if (state.addSkipperId.isNotBlank() && state.crewMembers.any { it.skipperId == state.addSkipperId }) {
            _uiState.update { it.copy(formError = "Dieses Mitglied (ID: ${state.addSkipperId}) ist bereits an Bord.") }
            return
        }

        val member = com.example.trnberechnung.logic.ValidationUtils.sanitizeCrewMember(
            CrewMember(
                name = state.addName.ifBlank { state.addSkipperId },
                rank = state.addSelectedRole.label,
                isOnBoard = true,
                medicalNote = state.addMedicalNotes,
                emergencyPhone = state.addPhone,
                skipperId = state.addSkipperId,
                role = state.addSelectedRole.name,
                emergencyContact = state.addEmergencyContact,
                phone = state.addPhone,
                medicalNotes = state.addMedicalNotes
            )
        )

        viewModelScope.launch {
            repository.insertCrew(member)
        }

        // Formular zurücksetzen
        _uiState.update {
            it.copy(
                addName = "",
                addSkipperId = "",
                addSelectedRole = CrewRole.SKIPPER,
                addEmergencyContact = "",
                addPhone = "",
                addMedicalNotes = "",
                formError = null
            )
        }
    }

    fun startEditingCrew(member: CrewMember) {
        _uiState.update { it.copy(editingMember = member) }
    }

    fun cancelEditingCrew() {
        _uiState.update { it.copy(editingMember = null) }
    }

    fun updateCrew(member: CrewMember) {
        viewModelScope.launch {
            repository.updateCrew(member)
        }
    }

    fun deleteCrew(member: CrewMember) {
        _uiState.update { it.copy(memberToDelete = member) }
    }

    fun confirmDeleteCrew() {
        val member = _uiState.value.memberToDelete ?: return
        viewModelScope.launch {
            repository.deleteCrew(member)
            _uiState.update { it.copy(memberToDelete = null) }
        }
    }

    fun cancelDeleteCrew() {
        _uiState.update { it.copy(memberToDelete = null) }
    }

}
