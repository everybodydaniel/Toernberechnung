package com.example.trnberechnung.network

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrewspaceContractTest {
    private val gson = Gson()

    @Test
    fun sendRequestIncludesStableClientMessageId() {
        val json =
            gson.toJson(
                ApiCreateMessageRequest(
                    clientMessageId = "client-123",
                    text = "Moin",
                ),
            )

        assertTrue(json.contains("\"client_message_id\":\"client-123\""))
        assertFalse(json.contains("sender_id"))
    }

    @Test
    fun pageDecodesOpaqueRecoveryCursors() {
        val page =
            gson.fromJson(
                """
                {
                  "messages": [],
                  "next_before_cursor": "opaque-before",
                  "next_after_cursor": "opaque-after",
                  "has_more": true
                }
                """.trimIndent(),
                ApiMessagePage::class.java,
            )

        assertEquals("opaque-before", page.nextBeforeCursor)
        assertEquals("opaque-after", page.nextAfterCursor)
        assertTrue(page.hasMore)
    }

    @Test
    fun absentChatAvailabilityIsBackwardCompatible() {
        val conversation =
            gson.fromJson(
                """
                {
                  "id": "conversation",
                  "title": "Crew",
                  "kind": "direct"
                }
                """.trimIndent(),
                ApiCrewspaceConversation::class.java,
            )

        assertTrue(conversation.chatAvailable)
    }
}
