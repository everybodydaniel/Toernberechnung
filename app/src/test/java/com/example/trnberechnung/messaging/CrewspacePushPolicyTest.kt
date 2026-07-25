package com.example.trnberechnung.messaging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CrewspacePushPolicyTest {
    @Test
    fun `accepts only the three identifier fields from data-only FCM`() {
        val envelope =
            parseCrewspacePush(
                mapOf(
                    "conversation_id" to "conversation-1",
                    "message_id" to "message-1",
                    "message_type" to "audio",
                ),
            )

        assertEquals("conversation-1", envelope?.conversationId)
        assertEquals("message-1", envelope?.messageId)
        assertEquals("audio", envelope?.messageType)
        assertNull(
            parseCrewspacePush(
                mapOf(
                    "conversation_id" to "conversation-1",
                    "message_id" to "message-1",
                    "message_type" to "text",
                    "sender_name" to "Must not travel in Android push data",
                ),
            ),
        )
        assertNull(
            parseCrewspacePush(
                mapOf(
                    "conversation_id" to "conversation-1",
                    "message_id" to "message-1",
                ),
            ),
        )
    }

    @Test
    fun `requires owner and installation to match the server-confirmed binding`() {
        assertTrue(
            isCurrentPushRegistration(
                isLoggedIn = true,
                currentOwnerId = "owner-a",
                currentInstallationId = "fid-a",
                registeredOwnerId = "owner-a",
                registeredInstallationId = "fid-a",
            ),
        )
        assertFalse(
            isCurrentPushRegistration(
                isLoggedIn = true,
                currentOwnerId = "owner-b",
                currentInstallationId = "fid-a",
                registeredOwnerId = "owner-a",
                registeredInstallationId = "fid-a",
            ),
        )
        assertFalse(
            isCurrentPushRegistration(
                isLoggedIn = true,
                currentOwnerId = "owner-a",
                currentInstallationId = "fid-b",
                registeredOwnerId = "owner-a",
                registeredInstallationId = "fid-a",
            ),
        )
        assertFalse(
            isCurrentPushRegistration(
                isLoggedIn = false,
                currentOwnerId = "owner-a",
                currentInstallationId = "fid-a",
                registeredOwnerId = "owner-a",
                registeredInstallationId = "fid-a",
            ),
        )
    }

    @Test
    fun `redundant registration keeps only the same confirmed owner and FID`() {
        assertTrue(
            shouldKeepConfirmedPushRegistration(
                requestedOwnerId = "owner-a",
                currentInstallationId = "fid-a",
                registeredOwnerId = "owner-a",
                registeredInstallationId = "fid-a",
            ),
        )
        assertFalse(
            shouldKeepConfirmedPushRegistration(
                requestedOwnerId = "owner-b",
                currentInstallationId = "fid-a",
                registeredOwnerId = "owner-a",
                registeredInstallationId = "fid-a",
            ),
        )
        assertFalse(
            shouldKeepConfirmedPushRegistration(
                requestedOwnerId = "owner-a",
                currentInstallationId = "fid-b",
                registeredOwnerId = "owner-a",
                registeredInstallationId = "fid-a",
            ),
        )
    }

    @Test
    fun `uses one owner tag for enqueue and logout cancellation`() {
        assertEquals("crewspace-push-owner-owner-a", crewspacePushOwnerWorkTag("owner-a"))
        assertEquals(
            "crewspace-push-owner-a-message-1",
            crewspacePushUniqueWorkName("owner-a", "message-1"),
        )
    }

    @Test
    fun `notification title prefers canonical sender without message content`() {
        assertEquals("Mara", pushSenderTitle("Mara", "Crew"))
        assertEquals("Crew", pushSenderTitle("", "Crew"))
        assertEquals("Crewspace", pushSenderTitle(null, null))
    }
}
