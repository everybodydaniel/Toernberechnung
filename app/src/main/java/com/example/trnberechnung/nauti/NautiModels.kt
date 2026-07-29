package com.example.trnberechnung.nauti

import java.time.ZonedDateTime

enum class NautiRole {
    USER,
    ASSISTANT,
}

data class NautiPromptMessage(
    val role: NautiRole,
    val text: String,
)

sealed interface NautiAction {
    data object OpenTripPlanner : NautiAction

    data class PlanTrip(
        val startHarbourId: String,
        val destinationHarbourId: String,
        val intermediateHarbourIds: List<String> = emptyList(),
        val departure: ZonedDateTime? = null,
    ) : NautiAction

    data object StartNavigation : NautiAction

    data object ShowPassageWindow : NautiAction

    data class ShowWeather(
        val harbourId: String? = null,
    ) : NautiAction

    data class ShowTides(
        val harbourId: String? = null,
    ) : NautiAction

    data class ShowBshWaterLevel(
        val harbourId: String? = null,
    ) : NautiAction
}

data class NautiReply(
    val text: String,
    val action: NautiAction? = null,
)

interface NautiInferenceClient {
    val isConfigured: Boolean

    suspend fun reply(messages: List<NautiPromptMessage>): Result<NautiReply>
}
