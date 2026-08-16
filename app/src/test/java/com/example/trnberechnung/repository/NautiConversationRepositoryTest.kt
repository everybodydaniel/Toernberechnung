package com.example.trnberechnung.repository

import com.example.trnberechnung.database.NautiDao
import com.example.trnberechnung.database.NautiMessageEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NautiConversationRepositoryTest {
    private val dao = mockk<NautiDao>(relaxed = true)

    @Test
    fun conversationAndMessageAreScopedAndBounded() =
        runTest {
            val repository =
                NautiConversationRepository(
                    dao = dao,
                    ownerIdProvider = { "skipper-1" },
                    now = { 123L },
                    idFactory = sequenceOf("conversation-1", "message-1").iterator()::next,
                )
            val conversation = repository.createConversation("  Mein   Törn  ")
            coEvery { dao.getConversation("skipper-1", conversation.id) } returns conversation
            val inserted = slot<NautiMessageEntity>()
            coEvery { dao.insertAndTrim(capture(inserted), any()) } returns Unit

            repository.addMessage(
                conversationId = conversation.id,
                role = NautiConversationRepository.ROLE_USER,
                content = "Moin",
            )

            assertEquals("skipper-1", conversation.ownerId)
            assertEquals("Mein Törn", conversation.title)
            assertEquals("skipper-1", inserted.captured.ownerId)
            assertEquals("conversation-1", inserted.captured.conversationId)
            coVerify {
                dao.insertAndTrim(
                    match { it.id == "message-1" },
                    NautiConversationRepository.MESSAGE_LIMIT,
                )
            }
        }

    @Test
    fun generatedTitleNeverExceedsFortyEightCharacters() {
        val repository =
            NautiConversationRepository(
                dao = dao,
                ownerIdProvider = { "owner" },
            )

        val title = repository.titleFrom("A".repeat(80))

        assertEquals(NautiConversationRepository.MAX_TITLE_LENGTH, title.length)
        assertTrue(title.all { it == 'A' })
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankOwnerCannotAccessAnotherNamespace() =
        runTest {
            val repository =
                NautiConversationRepository(
                    dao = dao,
                    ownerIdProvider = { " " },
                )
            repository.createConversation()
        }
}
