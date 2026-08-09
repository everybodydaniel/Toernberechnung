package com.example.trnberechnung.nauti

import com.example.trnberechnung.mapplanning.HarbourId
import java.text.Normalizer
import java.time.Clock
import java.time.ZonedDateTime
import java.util.Locale

/**
 * Handles high-confidence nautical commands before a model is contacted.
 * Safety status is intentionally absent: only the deterministic route engine
 * may decide whether a route is navigable.
 *
 * This router is also the app's main defence against the Gemini free-tier budget of 20 requests per
 * day: every intent answered here costs no quota at all. It should only ever gain branches.
 */
class NautiDeterministicIntentRouter(
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    fun route(input: String): NautiReply? {
        val normalized = input.normalizedCommand()
        if (normalized.isBlank()) return null

        val harbourIds = HARBOURS.filterValues { names -> names.any(normalized::contains) }.keys.toList()

        // Must be tested before the plain "navigation starten" branch below, which would otherwise
        // swallow "starte eine Fahrt von X nach Y" and lose the harbours. Requires a resolvable
        // von/nach pair, so a bare "navigation starten" still falls through to StartNavigation.
        val asksToStartVoyage =
            normalized.contains("starte") ||
                normalized.contains("los ") ||
                normalized == "los" ||
                normalized.contains("losfahren") ||
                normalized.contains("ablegen")
        if (asksToStartVoyage) {
            val trip = parseTrip(normalized, harbourIds)
            if (trip != null) {
                return NautiReply(
                    text =
                        "Ich habe den Törn vorbereitet. Prüfe Route und Status - " +
                            "die Fahrt startet erst, wenn du bestätigst.",
                    action =
                        NautiAction.StartVoyage(
                            startHarbourId = trip.start,
                            destinationHarbourId = trip.destination,
                            intermediateHarbourIds = trip.stops,
                            departure = departureFrom(normalized),
                        ),
                )
            }
        }

        if (
            normalized.contains("passagefenster") ||
            normalized.contains("abfahrtsfenster") ||
            normalized.contains("sicheres fenster")
        ) {
            return NautiReply(
                text = "Ich öffne das sichere Passagefenster der aktuell geplanten Route.",
                action = NautiAction.ShowPassageWindow,
            )
        }

        if (
            normalized.contains("navigation starten") ||
            normalized.contains("fahrt starten") ||
            normalized.contains("navigieren")
        ) {
            return NautiReply(
                text = "Ich bereite die Navigation für die berechnete Route vor.",
                action = NautiAction.StartNavigation,
            )
        }

        if (normalized.contains("wasserstand") || normalized.contains("bsh")) {
            return NautiReply(
                text = "Ich öffne die BSH-Wasserstandsdaten.",
                action = NautiAction.ShowBshWaterLevel(harbourIds.firstOrNull()),
            )
        }

        if (
            normalized.contains("gezeiten") ||
            normalized.contains("tide") ||
            normalized.contains("hoch wasser") ||
            normalized.contains("niedrig wasser")
        ) {
            return NautiReply(
                text = "Ich öffne die Gezeiten für das gewünschte Revier.",
                action = NautiAction.ShowTides(harbourIds.firstOrNull()),
            )
        }

        if (
            normalized.contains("wetter") ||
            normalized.contains("wind") ||
            normalized.contains("boen") ||
            normalized.contains("sicht")
        ) {
            return NautiReply(
                text = "Ich öffne die maritimen Wetterdaten.",
                action = NautiAction.ShowWeather(harbourIds.firstOrNull()),
            )
        }

        val asksToPlan =
            normalized.contains("torn planen") ||
                normalized.contains("route planen") ||
                normalized.startsWith("plane ") ||
                normalized.contains("plane einen torn")
        if (asksToPlan) {
            val trip = parseTrip(normalized, harbourIds)
            if (trip != null) {
                return NautiReply(
                    text = "Ich habe die Route vorbereitet. Bitte prüfe Abfahrt und Zwischenstopps.",
                    action =
                        NautiAction.PlanTrip(
                            startHarbourId = trip.start,
                            destinationHarbourId = trip.destination,
                            intermediateHarbourIds = trip.stops,
                            departure = departureFrom(normalized),
                        ),
                )
            }
            return NautiReply(
                text = "Ich öffne die Törnplanung. Wähle dort Start, Ziel und Abfahrt.",
                action = NautiAction.OpenTripPlanner,
            )
        }

        return null
    }

    private class ParsedTrip(
        val start: String,
        val destination: String,
        val stops: List<String>,
    )

    /**
     * Reads a "von X nach Y" pair out of the normalized command. Shared by the plan and start
     * branches so both understand exactly the same phrasings.
     */
    private fun parseTrip(
        normalized: String,
        harbourIds: List<String>,
    ): ParsedTrip? {
        if (harbourIds.size < 2) return null
        val fromIndex = normalized.indexOf(" von ")
        val toIndex = normalized.indexOf(" nach ")
        if (fromIndex < 0 || toIndex <= fromIndex) return null
        val start = findHarbourIn(normalized.substring(fromIndex + 5, toIndex)) ?: return null
        val destination = findHarbourIn(normalized.substring(toIndex + 6)) ?: return null
        if (start == destination) return null
        return ParsedTrip(
            start = start,
            destination = destination,
            stops = harbourIds.filterNot { it == start || it == destination },
        )
    }

    /**
     * Only the unambiguous "now" wordings are resolved here. Anything richer ("morgen um 08:00",
     * "in zwei Stunden") is deliberately left to the model rather than reimplemented as a German
     * date parser, which would be a large surface for silently wrong departure times.
     */
    private fun departureFrom(normalized: String): ZonedDateTime? =
        if (
            normalized.contains("jetzt") ||
            normalized.contains("sofort") ||
            normalized.contains("gleich")
        ) {
            ZonedDateTime.now(clock)
        } else {
            null
        }

    private fun findHarbourIn(text: String): String? =
        HARBOURS.entries.firstOrNull { (_, names) -> names.any(text::contains) }?.key

    companion object {
        /**
         * Keyed by [HarbourId.rawValue] so the router can never resolve a harbour the deterministic
         * planner does not know. The values are the spoken/typed aliases for each harbour.
         */
        private val HARBOURS: Map<String, List<String>> =
            linkedMapOf(
                HarbourId.BORKUM_HARBOR.rawValue to listOf("borkum", "fischerbalje"),
                HarbourId.EMDEN_HARBOR.rawValue to listOf("emden", "grosse seeschleuse"),
                HarbourId.JUIST_HARBOR.rawValue to listOf("juist"),
                HarbourId.NORDERNEY_HARBOR.rawValue to listOf("norderney"),
                HarbourId.BALTRUM_HARBOR.rawValue to listOf("baltrum"),
                HarbourId.LANGEOOG_HARBOR.rawValue to listOf("langeoog"),
                HarbourId.SPIEKEROOG_HARBOR.rawValue to listOf("spiekeroog"),
                HarbourId.WANGEROOGE_HARBOR.rawValue to listOf("wangerooge"),
            )
    }
}

private fun String.normalizedCommand(): String {
    val decomposed = Normalizer.normalize(lowercase(Locale.GERMANY), Normalizer.Form.NFD)
    return decomposed
        .replace(Regex("\\p{M}+"), "")
        .replace("ß", "ss")
        .replace("ö", "o")
        .replace("ä", "a")
        .replace("ü", "u")
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
}
