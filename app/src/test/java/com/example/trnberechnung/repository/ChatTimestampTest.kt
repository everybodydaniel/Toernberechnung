package com.example.trnberechnung.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatTimestampTest {
    @Test
    fun parsesCanonicalUtcTimestamp() {
        assertEquals(1_750_000_000_000L, parseServerTimestamp("2025-06-15T15:06:40Z"))
    }

    @Test
    fun parsesOffsetTimestampAsSameInstant() {
        assertEquals(
            parseServerTimestamp("2025-06-15T15:06:40Z"),
            parseServerTimestamp("2025-06-15T17:06:40+02:00"),
        )
    }

    @Test
    fun rejectsMissingOrMalformedTimestamp() {
        assertNull(parseServerTimestamp(null))
        assertNull(parseServerTimestamp("not-a-date"))
    }
}
