package com.example.trnberechnung.viewmodel

import com.example.trnberechnung.database.NautiConversationEntity
import com.example.trnberechnung.database.NautiDao
import com.example.trnberechnung.nauti.NautiInferenceClient
import com.example.trnberechnung.nauti.NautiPromptMessage
import com.example.trnberechnung.nauti.NautiReply
import com.example.trnberechnung.repository.NautiConversationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class NautiViewModelTest {
    @get:Rule
    val mainDispatcherRule = NautiMainDispatcherRule()

    @Test
    fun `restoring an existing conversation keeps panel compact`() =
        runTest {
            val existing =
                NautiConversationEntity(
                    ownerId = OWNER_ID,
                    id = "existing-chat",
                    title = "Letzter Törn",
                    createdAt = 1L,
                    updatedAt = 2L,
                    draft = "Mein Entwurf",
                )
            val fixture = repositoryFixture(listOf(existing))

            val viewModel = NautiViewModel(fixture.repository, UnusedInferenceClient)
            advanceUntilIdle()

            assertEquals(NautiPanelMode.COMPACT, viewModel.uiState.value.mode)
            assertEquals(existing.id, viewModel.uiState.value.activeConversationId)
            assertEquals(existing.draft, viewModel.uiState.value.draft)
            coVerify(exactly = 0) { fixture.dao.upsertConversation(any()) }
        }

    @Test
    fun `creating the initial conversation keeps panel compact until user opens it`() =
        runTest {
            val fixture = repositoryFixture(emptyList())

            val viewModel = NautiViewModel(fixture.repository, UnusedInferenceClient)
            advanceUntilIdle()

            assertEquals(NautiPanelMode.COMPACT, viewModel.uiState.value.mode)
            assertNotNull(viewModel.uiState.value.activeConversationId)
            coVerify(exactly = 1) { fixture.dao.upsertConversation(any()) }
            coVerify(exactly = 1) {
                fixture.dao.insertAndTrim(
                    match { it.content == NautiViewModel.WELCOME_MESSAGE },
                    NautiConversationRepository.MESSAGE_LIMIT,
                )
            }

            viewModel.showChat()

            assertEquals(NautiPanelMode.CHAT, viewModel.uiState.value.mode)
        }

    private fun repositoryFixture(
        initialConversations: List<NautiConversationEntity>,
    ): RepositoryFixture {
        val conversations = MutableStateFlow(initialConversations)
        val dao = mockk<NautiDao>(relaxed = true)
        every { dao.observeConversations(OWNER_ID) } returns conversations
        every {
            dao.observeMessages(
                OWNER_ID,
                any(),
                NautiConversationRepository.MESSAGE_LIMIT,
            )
        } returns flowOf(emptyList())
        coEvery { dao.upsertConversation(any()) } answers {
            val conversation = firstArg<NautiConversationEntity>()
            conversations.value = listOf(conversation)
        }
        coEvery { dao.getConversation(OWNER_ID, any()) } answers {
            val conversationId = secondArg<String>()
            conversations.value.firstOrNull { it.id == conversationId }
        }
        coEvery { dao.insertAndTrim(any(), any()) } returns Unit

        return RepositoryFixture(
            dao = dao,
            repository =
                NautiConversationRepository(
                    dao = dao,
                    ownerIdProvider = { OWNER_ID },
                    now = { 100L },
                    idFactory = sequenceOf("initial-chat", "welcome-message").iterator()::next,
                ),
        )
    }

    private data class RepositoryFixture(
        val dao: NautiDao,
        val repository: NautiConversationRepository,
    )

    private object UnusedInferenceClient : NautiInferenceClient {
        override val isConfigured = false

        override suspend fun reply(messages: List<NautiPromptMessage>): Result<NautiReply> =
            error("Inference must not run while restoring the panel.")
    }

    private companion object {
        const val OWNER_ID = "skipper-1"
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class NautiMainDispatcherRule(
    private val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
