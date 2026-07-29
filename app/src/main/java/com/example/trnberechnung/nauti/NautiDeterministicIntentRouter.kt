package com.example.trnberechnung.nauti

import java.text.Normalizer
import java.util.Locale

/**
 * Handles high-confidence nautical commands before a model is contacted.
 * Safety status is intentionally absent: only the deterministic route engine
 * may decide whether a route is navigable.
 */
class NautiDeterministicIntentRouter {
    fun route(input: String): NautiReply? {
        val normalized = input.normalizedCommand()
        if (normalized.isBlank()) return null

        val harbourIds = HARBOURS.filterValues { names -> names.any(normalized::contains) }.keys.toList()

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
            val fromIndex = normalized.indexOf(" von ")
            val toIndex = normalized.indexOf(" nach ")
            if (fromIndex >= 0 && toIndex > fromIndex && harbourIds.size >= 2) {
                val start = findHarbourIn(normalized.substring(fromIndex + 5, toIndex))
                val destination = findHarbourIn(normalized.substring(toIndex + 6))
                if (start != null && destination != null && start != destination) {
                    val stops =
                        harbourIds.filterNot { it == start || it == destination }
                    return NautiReply(
                        text = "Ich habe die Route vorbereitet. Bitte prüfe Abfahrt und Zwischenstopps.",
                        action =
                            NautiAction.PlanTrip(
                                startHarbourId = start,
                                destinationHarbourId = destination,
                                intermediateHarbourIds = stops,
                            ),
                    )
                }
            }
            return NautiReply(
                text = "Ich öffne die Törnplanung. Wähle dort Start, Ziel und Abfahrt.",
                action = NautiAction.OpenTripPlanner,
            )
        }

        return null
    }

    private fun findHarbourIn(text: String): String? =
        HARBOURS.entries.firstOrNull { (_, names) -> names.any(text::contains) }?.key

    companion object {
        private val HARBOURS =
            linkedMapOf(
                "borkum_harbor" to listOf("borkum", "fischerbalje"),
                "emden_harbor" to listOf("emden", "grosse seeschleuse"),
                "juist_harbor" to listOf("juist"),
                "norderney_harbor" to listOf("norderney"),
                "baltrum_harbor" to listOf("baltrum"),
                "langeoog_harbor" to listOf("langeoog"),
                "spiekeroog_harbor" to listOf("spiekeroog"),
                "wangerooge_harbor" to listOf("wangerooge"),
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
