package com.example.trnberechnung.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class MaritimeNoticeDateTest {
    @Test
    fun parsesGoTimestampsWithAndWithoutFractionalSeconds() {
        assertEquals(
            Instant.parse("2026-07-22T01:00:00Z").toEpochMilli(),
            "2026-07-22T01:00:00Z".toEpochMillisOrNull(),
        )
        assertEquals(
            Instant.parse("2026-07-22T01:00:00.123Z").toEpochMilli(),
            "2026-07-22T01:00:00.123Z".toEpochMillisOrNull(),
        )
    }

    @Test
    fun invalidOrEmptyTimestampIsAbsent() {
        assertNull("".toEpochMillisOrNull())
        assertNull("not-a-date".toEpochMillisOrNull())
        assertNull((null as String?).toEpochMillisOrNull())
    }
}
