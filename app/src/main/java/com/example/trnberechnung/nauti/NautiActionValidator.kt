package com.example.trnberechnung.nauti

import com.example.trnberechnung.mapplanning.HarbourId

data class NautiActionValidation(
    val isValid: Boolean,
    val message: String? = null,
)

object NautiActionValidator {
    fun validate(action: NautiAction): NautiActionValidation =
        when (action) {
            NautiAction.OpenTripPlanner,
            NautiAction.StartNavigation,
            NautiAction.ShowPassageWindow,
            -> NautiActionValidation(true)

            is NautiAction.PlanTrip ->
                validateTrip(
                    action.startHarbourId,
                    action.destinationHarbourId,
                    action.intermediateHarbourIds,
                )

            is NautiAction.StartVoyage ->
                validateTrip(
                    action.startHarbourId,
                    action.destinationHarbourId,
                    action.intermediateHarbourIds,
                )

            is NautiAction.ShowWeather -> validateOptionalHarbour(action.harbourId)
            is NautiAction.ShowTides -> validateOptionalHarbour(action.harbourId)
            is NautiAction.ShowBshWaterLevel -> validateOptionalHarbour(action.harbourId)
        }

    private fun validateTrip(
        startHarbourId: String,
        destinationHarbourId: String,
        intermediateHarbourIds: List<String>,
    ): NautiActionValidation {
        val all = listOf(startHarbourId, destinationHarbourId) + intermediateHarbourIds
        return when {
            all.any { it !in ALLOWED_HARBOUR_IDS } ->
                NautiActionValidation(false, "Nauti hat eine unbekannte Hafen-ID geliefert.")
            startHarbourId == destinationHarbourId ->
                NautiActionValidation(false, "Start und Ziel müssen verschieden sein.")
            all.distinct().size != all.size ->
                NautiActionValidation(false, "Häfen dürfen in einer Route nicht doppelt vorkommen.")
            else -> NautiActionValidation(true)
        }
    }

    private fun validateOptionalHarbour(harbourId: String?): NautiActionValidation =
        if (harbourId == null || harbourId in ALLOWED_HARBOUR_IDS) {
            NautiActionValidation(true)
        } else {
            NautiActionValidation(false, "Nauti hat eine unbekannte Hafen-ID geliefert.")
        }

    /**
     * Derived from [HarbourId] so the planner catalog stays the single source of truth. A harbour
     * the deterministic planner cannot route to must never pass validation, and duplicating the
     * list here previously allowed the two to drift apart.
     */
    val ALLOWED_HARBOUR_IDS: Set<String> =
        HarbourId.entries.mapTo(LinkedHashSet()) { it.rawValue }
}
