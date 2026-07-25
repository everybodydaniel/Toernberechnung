package com.example.trnberechnung.messaging

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ChatNavigationState {
    private val _pendingConversationId = MutableStateFlow<String?>(null)
    val pendingConversationId: StateFlow<String?> = _pendingConversationId.asStateFlow()

    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    fun requestConversation(conversationId: String?) {
        _pendingConversationId.value = conversationId?.takeIf { it.isNotBlank() }
    }

    fun consumeConversation(conversationId: String) {
        if (_pendingConversationId.value == conversationId) {
            _pendingConversationId.value = null
        }
    }

    fun setActiveConversation(conversationId: String?) {
        _activeConversationId.value = conversationId
    }
}
