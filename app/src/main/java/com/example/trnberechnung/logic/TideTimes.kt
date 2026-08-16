package com.example.trnberechnung.logic

import com.example.trnberechnung.model.TideEvent
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Parsing of BSH tide timestamps.
 *
 * The BSH forecast mixes formats: `yyyy-MM-dd HH:mm:ss`, ISO with a `T`, and either flavour with a
 * trailing `Z` or a `+02:00` / `+02` offset. This used to be re-implemented at every call site
 * (`TideViewModel.updateTideEvents`, `TideContent` in the Revier screen), which is how a "next high
 * water" could differ between two screens showing the same station. One parser, one behaviour.
 *
 * The offset is deliberately dropped rather than converted: the BSH publishes local German times,
 * and the rest of the app compares against [LocalDateTime.now].
 */
object TideTimes {
    private val SPACE_SEPARATED = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val TRAILING_ZONE = Regex("(Z|[+-]\\d{2}(:?\\d{2})?)$")

    /** Returns the event time, or `null` if the timestamp cannot be read at all. */
    fun parse(timestamp: String?): LocalDateTime? {
        val cleaned =
            timestamp
                ?.replace("T", " ")
                ?.replace(TRAILING_ZONE, "")
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?: return null
        return runCatching { LocalDateTime.parse(cleaned, SPACE_SEPARATED) }
            .recoverCatching { LocalDateTime.parse(cleaned.replace(" ", "T")) }
            .getOrNull()
    }

    /** Events paired with their parsed time, unreadable ones dropped, oldest first. */
    fun sortedByTime(events: List<TideEvent>): List<Pair<TideEvent, LocalDateTime>> =
        events
            .mapNotNull { event -> parse(event.timestamp)?.let { event to it } }
            .sortedBy { it.second }
}
