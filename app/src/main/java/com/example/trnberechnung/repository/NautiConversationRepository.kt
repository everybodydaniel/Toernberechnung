package com.example.trnberechnung.repository

import com.example.trnberechnung.database.NautiConversationEntity
import com.example.trnberechnung.database.NautiDao
import com.example.trnberechnung.database.NautiMessageEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class NautiConversationRepository(
    private val dao: NautiDao,
    private val ownerIdProvider: () -> String,
    private val now: () -> Long = System::currentTimeMillis,
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
) {
    val conversations: Flow<List<NautiConversationEntity>>
        get() = dao.observeConversations(ownerId())

    fun search(query: String): Flow<List<NautiConversationEntity>> =
        if (query.isBlank()) conversations else dao.searchConversations(ownerId(), query.trim())

    fun messages(conversationId: String): Flow<List<NautiMessageEntity>> =
        dao.observeMessages(ownerId(), conversationId.requireIdentifier(), MESSAGE_LIMIT)

    suspend fun createConversation(title: String = DEFAULT_TITLE): NautiConversationEntity {
        val timestamp = now()
        return NautiConversationEntity(
            ownerId = ownerId(),
            id = idFactory().requireIdentifier(),
            title = sanitizeTitle(title),
            createdAt = timestamp,
            updatedAt = timestamp,
        ).also { dao.upsertConversation(it) }
    }

    suspend fun addMessage(
        conversationId: String,
        role: String,
        content: String,
        actionType: String? = null,
        actionPayloadJson: String? = null,
        requiresConfirmation: Boolean = false,
        actionState: String? = null,
        isError: Boolean = false,
    ): NautiMessageEntity {
        val cleanConversationId = conversationId.requireIdentifier()
        val owner = ownerId()
        check(dao.getConversation(owner, cleanConversationId) != null) {
            "Die Nauti-Unterhaltung existiert nicht."
        }
        val message =
            NautiMessageEntity(
                ownerId = owner,
                id = idFactory().requireIdentifier(),
                conversationId = cleanConversationId,
                role = role.trim().lowercase().takeIf { it in ALLOWED_ROLES } ?: ROLE_ASSISTANT,
                content = content.trim(),
                createdAt = now(),
                actionType = actionType?.trim()?.takeIf(String::isNotEmpty),
                actionPayloadJson = actionPayloadJson?.trim()?.takeIf(String::isNotEmpty),
                requiresConfirmation = requiresConfirmation,
                actionState = actionState?.trim()?.takeIf(String::isNotEmpty),
                isError = isError,
            )
        dao.insertAndTrim(message, MESSAGE_LIMIT)
        return message
    }

    suspend fun updateDraft(
        conversationId: String,
        draft: String,
    ) {
        dao.updateDraft(ownerId(), conversationId.requireIdentifier(), draft, now())
    }

    suspend fun rename(
        conversationId: String,
        title: String,
    ) {
        dao.rename(
            ownerId(),
            conversationId.requireIdentifier(),
            sanitizeTitle(title),
            now(),
        )
    }

    suspend fun setPinned(
        conversationId: String,
        pinned: Boolean,
    ) {
        dao.setPinned(ownerId(), conversationId.requireIdentifier(), pinned, now())
    }

    suspend fun updateActionState(
        conversationId: String,
        messageId: String,
        state: String,
    ) {
        dao.updateActionState(
            ownerId = ownerId(),
            conversationId = conversationId.requireIdentifier(),
            messageId = messageId.requireIdentifier(),
            state = state.trim().ifBlank { "pending" },
        )
    }

    suspend fun delete(conversationId: String) {
        dao.deleteConversation(ownerId(), conversationId.requireIdentifier())
    }

    fun titleFrom(firstUserMessage: String): String = sanitizeTitle(firstUserMessage.ifBlank { DEFAULT_TITLE })

    private fun ownerId(): String = ownerIdProvider().trim().requireIdentifier()

    private fun sanitizeTitle(value: String): String =
        value
            .trim()
            .replace(Regex("\\s+"), " ")
            .ifBlank { DEFAULT_TITLE }
            .take(MAX_TITLE_LENGTH)

    private fun String.requireIdentifier(): String {
        val clean = trim()
        require(clean.isNotEmpty()) { "Eine stabile ID ist erforderlich." }
        return clean
    }

    companion object {
        const val DEFAULT_TITLE = "Neuer Chat"
        const val MAX_TITLE_LENGTH = 48
        const val MESSAGE_LIMIT = 100
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"
        const val ROLE_SYSTEM = "system"
        private val ALLOWED_ROLES = setOf(ROLE_USER, ROLE_ASSISTANT, ROLE_SYSTEM)
    }
}
