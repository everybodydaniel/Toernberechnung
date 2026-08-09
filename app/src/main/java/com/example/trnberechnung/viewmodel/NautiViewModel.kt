package com.example.trnberechnung.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.trnberechnung.database.NautiConversationEntity
import com.example.trnberechnung.database.NautiMessageEntity
import com.example.trnberechnung.nauti.NautiAction
import com.example.trnberechnung.nauti.NautiActionCodec
import com.example.trnberechnung.nauti.NautiActionValidator
import com.example.trnberechnung.nauti.NautiDeterministicIntentRouter
import com.example.trnberechnung.nauti.NautiErrorMapper
import com.example.trnberechnung.nauti.NautiInferenceClient
import com.example.trnberechnung.nauti.NautiPromptBuilder
import com.example.trnberechnung.nauti.NautiReply
import com.example.trnberechnung.repository.NautiConversationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class NautiPanelMode {
    COMPACT,
    CHAT,
    HISTORY,
}

data class NautiUiState(
    val mode: NautiPanelMode = NautiPanelMode.COMPACT,
    val activeConversationId: String? = null,
    val draft: String = "",
    val historyQuery: String = "",
    val isSending: Boolean = false,
    val error: String? = null,
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class NautiViewModel(
    private val repository: NautiConversationRepository,
    private val inferenceClient: NautiInferenceClient,
    private val deterministicRouter: NautiDeterministicIntentRouter =
        NautiDeterministicIntentRouter(),
) : ViewModel() {
    val conversations =
        repository.conversations.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList(),
        )

    private val _uiState = MutableStateFlow(NautiUiState())
    val uiState: StateFlow<NautiUiState> = _uiState.asStateFlow()

    val messages =
        _uiState
            .combine(conversations) { state, _ -> state.activeConversationId }
            .flatMapLatest { conversationId ->
                if (conversationId == null) flowOf(emptyList()) else repository.messages(conversationId)
            }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList(),
            )

    val filteredConversations =
        combine(conversations, _uiState) { conversations, state ->
            val query = state.historyQuery.trim()
            if (query.isBlank()) {
                conversations
            } else {
                conversations.filter { it.title.contains(query, ignoreCase = true) }
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList(),
        )

    private val _actions = MutableSharedFlow<NautiAction>(extraBufferCapacity = 4)
    val actions = _actions.asSharedFlow()
    private var draftWriteJob: Job? = null

    init {
        viewModelScope.launch {
            restoreInitialConversation()
        }
    }

    fun showCompact() {
        _uiState.update { it.copy(mode = NautiPanelMode.COMPACT) }
    }

    fun showChat() {
        _uiState.update { it.copy(mode = NautiPanelMode.CHAT) }
        if (_uiState.value.activeConversationId == null) {
            viewModelScope.launch { createNewChat() }
        }
    }

    fun showHistory() {
        _uiState.update { it.copy(mode = NautiPanelMode.HISTORY) }
    }

    fun updateHistoryQuery(value: String) {
        _uiState.update { it.copy(historyQuery = value) }
    }

    fun updateDraft(value: String) {
        _uiState.update { it.copy(draft = value) }
        val conversationId = _uiState.value.activeConversationId ?: return
        draftWriteJob?.cancel()
        draftWriteJob =
            viewModelScope.launch {
                kotlinx.coroutines.delay(250)
                repository.updateDraft(conversationId, value)
            }
    }

    fun createNewChat() {
        viewModelScope.launch {
            val conversation = repository.createConversation()
            repository.addMessage(
                conversationId = conversation.id,
                role = NautiConversationRepository.ROLE_ASSISTANT,
                content = WELCOME_MESSAGE,
            )
            _uiState.update {
                it.copy(
                    activeConversationId = conversation.id,
                    draft = "",
                    mode = NautiPanelMode.CHAT,
                    error = null,
                )
            }
        }
    }

    fun selectConversation(conversation: NautiConversationEntity) {
        _uiState.update {
            it.copy(
                activeConversationId = conversation.id,
                draft = conversation.draft,
                mode = NautiPanelMode.CHAT,
                error = null,
            )
        }
    }

    fun renameConversation(
        conversationId: String,
        title: String,
    ) {
        viewModelScope.launch { repository.rename(conversationId, title) }
    }

    fun setPinned(
        conversationId: String,
        pinned: Boolean,
    ) {
        viewModelScope.launch { repository.setPinned(conversationId, pinned) }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            repository.delete(conversationId)
            val next = repository.conversations.first().firstOrNull()
            if (next == null) {
                createNewChat()
            } else {
                selectConversation(next)
                showHistory()
            }
        }
    }

    fun send() {
        val state = _uiState.value
        val conversationId = state.activeConversationId ?: return
        val text = state.draft.trim()
        if (text.isBlank() || state.isSending) return
        _uiState.update { it.copy(draft = "", isSending = true, error = null) }
        viewModelScope.launch {
            try {
                // Read straight from Room rather than `messages.value`: that flow is
                // stateIn(WhileSubscribed), so it is empty whenever the panel is not collecting and
                // the model would silently lose the conversation context.
                val existingMessages = repository.messages(conversationId).first()
                repository.addMessage(
                    conversationId = conversationId,
                    role = NautiConversationRepository.ROLE_USER,
                    content = text,
                )
                repository.updateDraft(conversationId, "")
                if (
                    existingMessages.none {
                        it.role == NautiConversationRepository.ROLE_USER
                    }
                ) {
                    repository.rename(conversationId, repository.titleFrom(text))
                }

                val deterministic = deterministicRouter.route(text)
                if (deterministic != null) {
                    persistReply(conversationId, deterministic)
                } else {
                    inferenceClient
                        .reply(NautiPromptBuilder.build(existingMessages, text))
                        .fold(
                            onSuccess = { reply -> persistReply(conversationId, reply) },
                            onFailure = { throwable -> persistFailure(conversationId, throwable) },
                        )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(error = error.message ?: "Die Nachricht konnte nicht gesendet werden.")
                }
            } finally {
                _uiState.update { it.copy(isSending = false) }
            }
        }
    }

    fun confirmAction(message: NautiMessageEntity) {
        val action = NautiActionCodec.decode(message.actionType, message.actionPayloadJson) ?: return
        val accepted = emitValidated(action)
        viewModelScope.launch {
            repository.updateActionState(
                conversationId = message.conversationId,
                messageId = message.id,
                state = if (accepted) "executed" else "rejected",
            )
        }
    }

    fun dispatch(action: NautiAction) {
        emitValidated(action)
    }

    /**
     * Restores the persisted conversation context without changing how the panel is presented.
     *
     * Opening a conversation is an explicit user action. App startup must therefore keep the
     * default compact mode even when a previous conversation or draft is restored.
     */
    private suspend fun restoreInitialConversation() {
        val existing = repository.conversations.first().firstOrNull()
        val conversation =
            existing ?: repository.createConversation().also { created ->
                repository.addMessage(
                    conversationId = created.id,
                    role = NautiConversationRepository.ROLE_ASSISTANT,
                    content = WELCOME_MESSAGE,
                )
            }
        _uiState.update { current ->
            current.copy(
                activeConversationId = conversation.id,
                draft = conversation.draft,
                error = null,
            )
        }
    }

    private fun emitValidated(action: NautiAction): Boolean {
        val validation = NautiActionValidator.validate(action)
        if (validation.isValid) {
            _actions.tryEmit(action)
            return true
        } else {
            _uiState.update {
                it.copy(error = validation.message ?: "Die Nauti-Aktion ist ungültig.")
            }
            return false
        }
    }

    private suspend fun persistReply(
        conversationId: String,
        reply: NautiReply,
    ) {
        val action =
            reply.action?.takeIf { NautiActionValidator.validate(it).isValid }
        repository.addMessage(
            conversationId = conversationId,
            role = NautiConversationRepository.ROLE_ASSISTANT,
            content = reply.text,
            actionType = action?.let(NautiActionCodec::type),
            actionPayloadJson = action?.let(NautiActionCodec::payload),
            requiresConfirmation = action?.let(NautiActionCodec::requiresConfirmation) == true,
            actionState = if (action == null) null else "pending",
        )
        if (action != null && NautiActionCodec.autoDispatches(action)) {
            _actions.emit(action)
        }
    }

    /**
     * Records a failed inference as a visible, clearly marked error row.
     *
     * Stored with `isError = true` for two reasons: the bubble renders it as a warning rather than as
     * something Nauti claimed, and [NautiPromptBuilder] keeps it out of later prompts. Before this,
     * the raw exception text was persisted as an ordinary assistant answer and fed straight back into
     * the model as conversation context.
     */
    private suspend fun persistFailure(
        conversationId: String,
        throwable: Throwable,
    ) {
        val error = NautiErrorMapper.map(throwable)
        repository.addMessage(
            conversationId = conversationId,
            role = NautiConversationRepository.ROLE_ASSISTANT,
            content = error.userMessage,
            isError = true,
        )
        _uiState.update { it.copy(error = error.userMessage) }
    }

    class Factory(
        private val repository: NautiConversationRepository,
        private val inferenceClient: NautiInferenceClient,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(NautiViewModel::class.java))
            return NautiViewModel(repository, inferenceClient) as T
        }
    }

    companion object {
        const val WELCOME_MESSAGE = "Moin, ich bin Nauti. Wie kann ich dir helfen?"
    }
}
