package com.example.trnberechnung.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trnberechnung.model.*
import com.example.trnberechnung.repository.TideRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Job

class CrewspaceViewModel(
    private val repository: TideRepository,
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
            repository.getChatThreadsForUser(ownId).collect { threads ->
                _uiState.update { state ->
                    state.copy(chatThreads = threads.map { it.toModel(ownId) })
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
            repository.syncRemoteConversations(authRepo.idToken, _uiState.value.ownSkipperId)
            repository.syncRemoteEvents(authRepo.idToken)
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

    fun startChat(skipperId: String, displayName: String = "") {
        viewModelScope.launch {
            val ownId = _uiState.value.ownSkipperId
            val ownName = _uiState.value.ownDisplayName

            // 1. Try remote direct chat creation/fetch
            val remoteThread = repository.createRemoteDirectChat(authRepo.idToken, skipperId, ownId)
            val threadId = remoteThread?.id ?: listOf(ownId, skipperId).sorted().joinToString("_")
            
            val existingThread = _uiState.value.chatThreads.firstOrNull { it.id == threadId || it.participantSkipperId == skipperId }
            
            if (existingThread != null) {
                _uiState.update {
                    it.copy(showNewConversationSheet = false, newConversationSkipperId = "")
                }
                openChat(existingThread)
            } else {
                var resolvedName = when {
                    remoteThread != null && remoteThread.participant2Name.isNotBlank() && remoteThread.participant2Name != skipperId -> remoteThread.participant2Name
                    displayName.isNotBlank() -> displayName
                    else -> ""
                }

                // If name is still blank, try querying the skipper profile from backend
                if (resolvedName.isBlank() && authRepo.idToken.isNotBlank()) {
                    try {
                        val profileRes = com.example.trnberechnung.network.RetrofitInstance.socialFeedApi.getSkipper("Bearer ${authRepo.idToken}", skipperId)
                        if (profileRes.isSuccessful && profileRes.body() != null) {
                            resolvedName = profileRes.body()!!.name
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("CREWSPACE", "Failed to fetch skipper profile: ${e.message}")
                    }
                }

                if (resolvedName.isBlank()) {
                    resolvedName = skipperId
                }

                val newThread = ChatThread(
                    id = threadId,
                    type = ChatThreadType.DIRECT,
                    participantName = resolvedName,
                    participantSkipperId = skipperId
                )
                repository.insertChatThread(newThread.toEntity(ownId, ownName))
                
                _uiState.update {
                    it.copy(showNewConversationSheet = false, newConversationSkipperId = "")
                }
                openChat(newThread)
            }
        }
    }

    fun openChat(thread: ChatThread) {
        activeChatMessagesJob?.cancel()
        activeChatMessagesJob = viewModelScope.launch {
            // Polling job in parallel for remote messages
            launch {
                while (true) {
                    repository.syncRemoteMessages(authRepo.idToken, thread.id)
                    kotlinx.coroutines.delay(3000)
                }
            }

            repository.getMessagesForThread(thread.id).collect { messageEntities ->
                val ownId = _uiState.value.ownSkipperId
                val messages = messageEntities.map { it.toModel(ownId) }
                
                // Wir aktualisieren den aktiven Thread mit den neuen Nachrichten
                val currentThread = _uiState.value.chatThreads.find { it.id == thread.id } ?: thread
                val activeThread = currentThread.copy(unreadCount = 0, messages = messages)
                
                _uiState.update { it.copy(activeChatThread = activeThread) }
            }
        }
    }

    fun closeChat() {
        activeChatMessagesJob?.cancel()
        _uiState.update { it.copy(activeChatThread = null, chatInput = "") }
    }

    fun updateChatInput(text: String) {
        _uiState.update { it.copy(chatInput = text) }
    }

    fun sendMessage() {
        val state = _uiState.value
        val thread = state.activeChatThread ?: return
        val text = state.chatInput.trim()
        if (text.isBlank()) return

        viewModelScope.launch {
            repository.sendRemoteMessage(
                idToken = authRepo.idToken,
                threadId = thread.id,
                text = text,
                senderId = state.ownSkipperId,
                senderName = state.ownDisplayName
            )
            
            // Thread Preview aktualisieren
            val updatedThread = thread.copy(
                lastMessage = text,
                lastMessageTimestamp = System.currentTimeMillis()
            )
            repository.insertChatThread(updatedThread.toEntity(state.ownSkipperId, state.ownDisplayName))
        }
        
        _uiState.update { it.copy(chatInput = "") }
    }
    
    fun sendAttachmentMessage(type: ChatMessageType, content: String, duration: Int = 0) {
        val state = _uiState.value
        val thread = state.activeChatThread ?: return

        val message = ChatMessage(
            threadId = thread.id,
            senderId = state.ownSkipperId,
            senderName = state.ownDisplayName,
            content = content,
            type = type,
            voiceDurationSeconds = duration,
            isOwnMessage = true
        )

        viewModelScope.launch {
            repository.insertChatMessage(message.toEntity())
            
            val previewText = when(type) {
                ChatMessageType.VOICE -> "Sprachnachricht"
                ChatMessageType.IMAGE -> "Bild"
                ChatMessageType.EVENT -> {
                    val parts = content.split("|")
                    "Termin: ${parts.getOrNull(0) ?: ""}"
                }
                else -> content
            }
            
            val updatedThread = thread.copy(
                lastMessage = previewText,
                lastMessageTimestamp = message.timestamp
            )
            repository.insertChatThread(updatedThread.toEntity(state.ownSkipperId, state.ownDisplayName))
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

    fun addPlannerEvent(title: String, description: String = "") {
        val event = PlannerEvent(
            date = _uiState.value.selectedDate,
            title = title,
            description = description
        )
        viewModelScope.launch {
            repository.insertPlannerEvent(event.toEntity())
        }
    }
    
    fun addPlannerEventWithDate(title: String, description: String, date: LocalDate) {
        val event = PlannerEvent(
            date = date,
            title = title,
            description = description
        )
        viewModelScope.launch {
            repository.insertPlannerEvent(event.toEntity())
        }
    }
    
    fun updatePlannerEvent(event: PlannerEvent, newTitle: String, newDescription: String) {
        val updatedEvent = event.copy(title = newTitle, description = newDescription)
        viewModelScope.launch {
            repository.insertPlannerEvent(updatedEvent.toEntity())
        }
    }

    fun deletePlannerEvent(event: PlannerEvent) {
        viewModelScope.launch {
            repository.deletePlannerEvent(event.toEntity())
        }
    }

    /** Events für das ausgewählte Datum */
    fun eventsForSelectedDate(): List<PlannerEvent> {
        val state = _uiState.value
        return state.plannerEvents.filter { it.date == state.selectedDate }
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

        val member = CrewMember(
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
                addMedicalNotes = ""
            )
        }
    }

    fun deleteCrew(member: CrewMember) {
        viewModelScope.launch {
            repository.deleteCrew(member)
        }
    }

    fun updateCrew(member: CrewMember) {
        viewModelScope.launch {
            repository.updateCrew(member)
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Mock-Daten für Demo / Vorschau
    // ══════════════════════════════════════════════════════════════

    fun loadMockChatData() {
        val now = System.currentTimeMillis()
        val mockMessages = listOf(
            ChatMessage(
                threadId = "mock-thread-1",
                senderId = "external-skipper",
                senderName = "Skipper",
                content = "Hallo",
                type = ChatMessageType.TEXT,
                timestamp = now - 60_000,
                isOwnMessage = false
            ),
            ChatMessage(
                threadId = "mock-thread-1",
                senderId = "external-skipper",
                senderName = "Skipper",
                content = "Sprachnachricht",
                type = ChatMessageType.VOICE,
                voiceDurationSeconds = 8,
                timestamp = now,
                isOwnMessage = false
            )
        )

        val mockThread = ChatThread(
            id = "mock-thread-1",
            type = ChatThreadType.DIRECT,
            participantName = "Skipper",
            participantSkipperId = "ext-skipper-id",
            lastMessage = "Sprachnachricht",
            lastMessageTimestamp = now,
            unreadCount = 2,
            messages = mockMessages
        )

        _uiState.update {
            it.copy(chatThreads = listOf(mockThread))
        }
    }

    // ══════════════════════════════════════════════════════════════
    // AI Assistent (Gemini 2.5 Flash)
    // ══════════════════════════════════════════════════════════════

    fun updateAiInput(text: String) {
        _uiState.update { it.copy(aiInput = text) }
    }

    fun sendAiMessage() {
        val state = _uiState.value
        val text = state.aiInput.trim()
        if (text.isBlank() || state.aiIsLoading) return

        val userMessage = AiChatMessage(content = text, isFromUser = true)
        _uiState.update {
            it.copy(
                aiMessages = it.aiMessages + userMessage,
                aiInput = "",
                aiIsLoading = true
            )
        }

        viewModelScope.launch {
            val response = com.example.trnberechnung.network.GeminiHelper.askQuestion(text)
            val aiMessage = AiChatMessage(content = response, isFromUser = false)
            _uiState.update {
                it.copy(
                    aiMessages = it.aiMessages + aiMessage,
                    aiIsLoading = false
                )
            }
        }
    }
}
