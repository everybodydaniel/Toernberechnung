package com.example.trnberechnung.nauti

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

            is NautiAction.PlanTrip -> validateTrip(action)
            is NautiAction.ShowWeather -> validateOptionalHarbour(action.harbourId)
            is NautiAction.ShowTides -> validateOptionalHarbour(action.harbourId)
            is NautiAction.ShowBshWaterLevel -> validateOptionalHarbour(action.harbourId)
        }

    private fun validateTrip(action: NautiAction.PlanTrip): NautiActionValidation {
        val all =
            listOf(action.startHarbourId, action.destinationHarbourId) +
                action.intermediateHarbourIds
        return when {
            all.any { it !in ALLOWED_HARBOUR_IDS } ->
                NautiActionValidation(false, "Nauti hat eine unbekannte Hafen-ID geliefert.")
            action.startHarbourId == action.destinationHarbourId ->
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

    val ALLOWED_HARBOUR_IDS =
        setOf(
            "borkum_harbor",
            "emden_harbor",
            "juist_harbor",
            "norderney_harbor",
            "baltrum_harbor",
            "langeoog_harbor",
            "spiekeroog_harbor",
            "wangerooge_harbor",
        )
}
