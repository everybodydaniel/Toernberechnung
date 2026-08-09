package com.example.trnberechnung.nauti

import com.example.trnberechnung.database.NautiMessageEntity
import com.example.trnberechnung.repository.NautiConversationRepository

/**
 * Turns the persisted conversation into the turns sent to the model.
 *
 * The `isError` filter here is load-bearing, not cosmetic. Failure notices are stored as assistant
 * rows so the skipper can see them in the transcript; replaying them into the next prompt would feed
 * the model its own error messages back as if they were nautical advice.
 */
object NautiPromptBuilder {
    const val MAX_TURNS = 24

    fun build(
        history: List<NautiMessageEntity>,
        newUserText: String,
    ): List<NautiPromptMessage> {
        val turns =
            history
                .asSequence()
                .filterNot { it.isError }
                .filter { it.role == NautiConversationRepository.ROLE_USER || it.role == NautiConversationRepository.ROLE_ASSISTANT }
                .filter { it.content.isNotBlank() }
                .map {
                    NautiPromptMessage(
                        role =
                            if (it.role == NautiConversationRepository.ROLE_USER) {
                                NautiRole.USER
                            } else {
                                NautiRole.ASSISTANT
                            },
                        text = it.content,
                    )
                }
                .toMutableList()

        turns.add(NautiPromptMessage(role = NautiRole.USER, text = newUserText))

        // Keep the most recent turns, then make sure the window still starts on a user turn: the
        // Gemini contents array must not begin with a model turn.
        val windowed = turns.takeLast(MAX_TURNS).toMutableList()
        while (windowed.size > 1 && windowed.first().role == NautiRole.ASSISTANT) {
            windowed.removeAt(0)
        }
        return windowed
    }
}
