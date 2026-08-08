package com.example.trnberechnung.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trnberechnung.model.*
import com.example.trnberechnung.messaging.ChatNavigationState
import com.example.trnberechnung.repository.ChatRepository
import com.example.trnberechnung.repository.TideRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class CrewspaceViewModel(
    private val repository: TideRepository,
    private val chatRepository: ChatRepository,
    private val authRepo: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CrewspaceUiState(
            ownSkipperId = authRepo.skipperId.ifBlank { "Unbekannt" },
            ownDisplayName = authRepo.userName.ifBlank { "Gast" },
            ownEmail = authRepo.userEmail
        )
    )
    val uiState: StateFlow<CrewspaceUiState> = _uiState.asStateFlow()

    private var activeChatMessagesJob: Job? = null
    private var markReadJob: Job? = null

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

        // Chats beobachten
        viewModelScope.launch {
            val ownId = _uiState.value.ownSkipperId
            chatRepository.threads(ownId).collect { threads ->
                val activeId = _uiState.value.activeChatThread?.id
                _uiState.update { state ->
                    val models =
                        threads.map {
                            it.toModel(ownId).let { model ->
                                if (model.id == activeId) model.copy(unreadCount = 0) else model
                            }
                        }
                    val active =
                        state.activeChatThread?.let { selected ->
                            models.firstOrNull { it.id == selected.id }
                                ?.copy(messages = selected.messages)
                                ?: selected
                        }
                    state.copy(chatThreads = models, activeChatThread = active)
                }
                if (
                    activeId != null &&
                    threads.firstOrNull { it.id == activeId }?.unreadCount?.let { it > 0 } == true
                ) {
                    scheduleMarkRead(activeId)
                }
            }
        }

        viewModelScope.launch {
            chatRepository.connectionState.collect { connection ->
                _uiState.update { it.copy(chatConnectionState = connection.name) }
            }
        }

        viewModelScope.launch {
            ChatNavigationState.pendingConversationId
                .filterNotNull()
                .collect { conversationId ->
                    val ownerId = _uiState.value.ownSkipperId
                    var stored = chatRepository.thread(ownerId, conversationId)
                    if (stored == null) {
                        runCatching { chatRepository.syncConversations() }
                        stored = chatRepository.thread(ownerId, conversationId)
                    }
                    stored?.let {
                        openChat(it.toModel(ownerId))
                        ChatNavigationState.consumeConversation(conversationId)
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

        // Server-Sync beim Start
        viewModelScope.launch {
            runCatching { chatRepository.activate() }
                .onFailure { showChatError(it) }
            runCatching { repository.syncRemoteEvents(authRepo.getIdToken()) }
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

    /**
     * Refresh hook used by the single app header. Room-backed UI stays intact
     * while the remote conversation/event state is synchronized.
     */
    fun refresh() {
        viewModelScope.launch {
            runCatching { chatRepository.activate() }
                .onFailure { showChatError(it) }
            runCatching { chatRepository.syncConversations() }
                .onFailure { showChatError(it) }
            runCatching { repository.syncRemoteEvents(authRepo.getIdToken()) }
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Chats
    // ══════════════════════════════════════════════════════════════

    fun showNewConversationSheet() {
        _uiState.update { it.copy(showNewConversationSheet = true) }
    }

    fun hideNewConversationSheet() {
        _uiState.update {
            it.copy(
                showNewConversationSheet = false,
                newConversationSkipperId = "",
                newConversationTab = NewConversationTab.DIRECT
            )
        }
    }

    fun setNewConversationTab(tab: NewConversationTab) {
        _uiState.update { it.copy(newConversationTab = tab) }
    }

    fun updateNewConversationSkipperId(id: String) {
        _uiState.update { it.copy(newConversationSkipperId = id) }
    }

    fun startChat(skipperId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(chatBusy = true, chatError = null) }
            runCatching { chatRepository.createDirectChat(skipperId) }
                .onSuccess { remoteThread ->
                    val ownId = _uiState.value.ownSkipperId
                    val thread = remoteThread.toModel(ownId)
                    _uiState.update {
                        it.copy(
                            showNewConversationSheet = false,
                            newConversationSkipperId = "",
                            chatBusy = false,
                        )
                    }
                    openChat(thread)
                }
                .onFailure {
                    _uiState.update { state -> state.copy(chatBusy = false) }
                    showChatError(it)
                }
        }
    }

    fun openChat(thread: ChatThread) {
        activeChatMessagesJob?.cancel()
        ChatNavigationState.setActiveConversation(thread.id)
        _uiState.update { it.copy(activeChatThread = thread, chatError = null) }
        activeChatMessagesJob =
            viewModelScope.launch {
                val ownerId = _uiState.value.ownSkipperId
                var latestReadIncomingId: String? = null
                chatRepository.thread(ownerId, thread.id)?.let { stored ->
                    launch {
                        runCatching { chatRepository.synchronizeMessages(stored) }
                            .onFailure { showChatError(it) }
                    }
                }
                scheduleMarkRead(thread.id)
                chatRepository.messages(ownerId, thread.id).collect { messageEntities ->
                    val messages = messageEntities.map { it.toModel(ownerId) }
                    messages.lastOrNull { !it.isOwnMessage }?.let { newestIncoming ->
                        val identity = newestIncoming.serverId ?: newestIncoming.id
                        if (identity != latestReadIncomingId) {
                            latestReadIncomingId = identity
                            scheduleMarkRead(thread.id)
                        }
                    }
                    val currentThread =
                        _uiState.value.chatThreads.find { it.id == thread.id } ?: thread
                    val activeThread = currentThread.copy(unreadCount = 0, messages = messages)
                    _uiState.update { it.copy(activeChatThread = activeThread) }
                }
            }
    }

    fun closeChat() {
        activeChatMessagesJob?.cancel()
        markReadJob?.cancel()
        ChatNavigationState.setActiveConversation(null)
        _uiState.update { it.copy(activeChatThread = null, chatInput = "", chatError = null) }
    }

    fun updateChatInput(text: String) {
        _uiState.update { it.copy(chatInput = text) }
    }

    fun sendMessage() {
        val state = _uiState.value
        val thread = state.activeChatThread ?: return
        val text = state.chatInput.trim()
        if (text.isBlank() || !thread.isChatAvailable) return

        // Puffer den Text, falls das Senden fehlschlägt
        val originalText = state.chatInput
        _uiState.update { it.copy(chatInput = "") }

        viewModelScope.launch {
            runCatching { chatRepository.queueText(thread.id, text) }
                .onFailure { error ->
                    // Bei Fehler Text wiederherstellen (optional, hier zeigen wir eher den Fehler an)
                    // Wir lassen ihn leer, damit der User nicht verwirrt ist, bieten aber Retry über die Message-Liste an.
                    // Falls wir ihn wiederherstellen wollen:
                    // _uiState.update { it.copy(chatInput = originalText) }
                    showChatError(error)
                }
        }
    }

    fun sendAttachmentMessage(type: ChatMessageType, content: String, duration: Int = 0) {
        val thread = _uiState.value.activeChatThread ?: return
        if (!thread.isChatAvailable) return
        viewModelScope.launch {
            val result =
                if (type == ChatMessageType.IMAGE || type == ChatMessageType.VOICE) {
                    runCatching {
                        chatRepository.queueAttachment(
                            conversationId = thread.id,
                            source = content,
                            type = type,
                            durationSeconds = duration,
                        )
                    }
                } else {
                    runCatching { chatRepository.queueText(thread.id, content, type) }
                }
            result.onFailure(::showChatError)
        }
    }

    fun retryMessage(localId: String) {
        viewModelScope.launch {
            runCatching { chatRepository.retryMessage(localId) }
                .onFailure(::showChatError)
        }
    }

    fun loadOlderMessages() {
        val conversationId = _uiState.value.activeChatThread?.id ?: return
        viewModelScope.launch {
            runCatching { chatRepository.loadOlderMessages(conversationId) }
                .onFailure(::showChatError)
        }
    }

    fun blockActiveChat() {
        val uid = _uiState.value.activeChatThread?.participantSkipperId ?: return
        viewModelScope.launch {
            runCatching { chatRepository.block(uid) }
                .onFailure(::showChatError)
        }
    }

    fun unblockActiveChat() {
        val uid = _uiState.value.activeChatThread?.participantSkipperId ?: return
        viewModelScope.launch {
            runCatching { chatRepository.unblock(uid) }
                .onFailure(::showChatError)
        }
    }

    fun clearChatError() {
        _uiState.update { it.copy(chatError = null) }
    }

    private fun showChatError(error: Throwable) {
        _uiState.update {
            it.copy(chatError = error.localizedMessage ?: "Chat-Aktion fehlgeschlagen.")
        }
    }

    private fun scheduleMarkRead(conversationId: String) {
        markReadJob?.cancel()
        markReadJob =
            viewModelScope.launch {
                delay(250)
                if (_uiState.value.activeChatThread?.id == conversationId) {
                    runCatching { chatRepository.markConversationRead(conversationId) }
                }
            }
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
            _uiState.update { it.copy(chatError = "Dieses Mitglied (ID: ${state.addSkipperId}) ist bereits an Bord.") }
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
                chatError = null
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
