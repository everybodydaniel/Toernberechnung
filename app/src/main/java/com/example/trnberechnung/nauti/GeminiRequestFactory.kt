package com.example.trnberechnung.nauti

import com.example.trnberechnung.BuildConfig
import com.example.trnberechnung.dto.GeminiContentDto
import com.example.trnberechnung.dto.GeminiGenerateContentRequestDto
import com.example.trnberechnung.dto.GeminiGenerationConfigDto
import com.example.trnberechnung.dto.GeminiPartDto
import com.example.trnberechnung.dto.GeminiSchemaDto
import com.example.trnberechnung.dto.GeminiThinkingConfigDto
import com.example.trnberechnung.mapplanning.HarbourId

/**
 * Builds the `generateContent` request.
 *
 * Every constant below was measured against the live API. Do not "tidy" them without re-measuring:
 *
 *  - `gemini-2.5-flash` (the previous hardcoded model) answers **HTTP 404 "no longer available to
 *    new users"**. The model must stay configurable, hence [BuildConfig.GEMINI_MODEL].
 *  - `maxOutputTokens = 900` (the previous value) produced `finishReason = MAX_TOKENS` with 865
 *    thinking tokens and only 31 answer tokens, i.e. truncated garbage. 2048 leaves real headroom.
 *  - `thinkingConfig.thinkingBudget = 0` is rejected with **HTTP 400** on current flash models.
 *    Only `thinkingLevel` is accepted; `"low"` brought thinking tokens down to zero in testing.
 *  - `responseMimeType` + [actionEnvelopeSchema] make the reply exactly one JSON object, which is
 *    what allowed the previous hand-rolled brace-matching extractor to be deleted.
 */
object GeminiRequestFactory {
    val model: String = BuildConfig.GEMINI_MODEL

    const val MAX_OUTPUT_TOKENS = 2048
    const val THINKING_LEVEL = "low"
    const val RESPONSE_MIME_TYPE = "application/json"
    const val TEMPERATURE = 0.45
    const val TOP_P = 0.9

    const val KEY_TEXT = "text"
    const val KEY_ACTION = "action"
    const val KEY_TYPE = "type"

    private const val TYPE_OBJECT = "object"
    private const val TYPE_STRING = "string"
    private const val TYPE_ARRAY = "array"

    fun build(messages: List<NautiPromptMessage>): GeminiGenerateContentRequestDto =
        GeminiGenerateContentRequestDto(
            contents =
                messages.map { message ->
                    GeminiContentDto(
                        role = if (message.role == NautiRole.USER) ROLE_USER else ROLE_MODEL,
                        parts = listOf(GeminiPartDto(message.text)),
                    )
                },
            systemInstruction = GeminiContentDto(parts = listOf(GeminiPartDto(NautiSystemPrompt.text))),
            generationConfig =
                GeminiGenerationConfigDto(
                    temperature = TEMPERATURE,
                    topP = TOP_P,
                    maxOutputTokens = MAX_OUTPUT_TOKENS,
                    responseMimeType = RESPONSE_MIME_TYPE,
                    responseSchema = actionEnvelopeSchema,
                    thinkingConfig = GeminiThinkingConfigDto(THINKING_LEVEL),
                ),
        )

    /** Harbour ids the model may emit, straight from the planner catalog. */
    val harbourIds: List<String> = HarbourId.entries.map { it.rawValue }

    /**
     * Mirrors [NautiReply]. A single `action` object with a `type` enum rather than a union, because
     * Gemini's schema dialect is an OpenAPI 3.0 subset without dependable `oneOf` support.
     *
     * That flattening has one real consequence: `start_harbour_id` and `destination_harbour_id` can
     * only be required for *some* action types, which a single `required` list cannot express. In
     * testing the model did emit `start_voyage` with the start but no destination. Two mitigations:
     * the descriptions below say plainly which fields are mandatory for which type, and
     * `propertyOrdering` makes the model emit the harbours right after the type instead of last.
     * `GeminiNautiClient` additionally degrades an under-specified trip to `open_trip_planner` rather
     * than dropping the skipper's instruction on the floor.
     *
     * The schema reduces the malformed-reply rate; it is not a trust boundary.
     * [NautiActionValidator] remains the authority on whether an action may run.
     */
    val actionEnvelopeSchema: GeminiSchemaDto by lazy {
        fun harbourField(description: String) =
            GeminiSchemaDto(
                type = TYPE_STRING,
                enumValues = harbourIds,
                nullable = true,
                description = description,
            )
        GeminiSchemaDto(
            type = TYPE_OBJECT,
            properties =
                mapOf(
                    KEY_TEXT to
                        GeminiSchemaDto(
                            type = TYPE_STRING,
                            description = "Kurze deutsche Antwort an den Skipper, ohne Messwerte.",
                        ),
                    KEY_ACTION to
                        GeminiSchemaDto(
                            type = TYPE_OBJECT,
                            nullable = true,
                            properties =
                                mapOf(
                                    KEY_TYPE to
                                        GeminiSchemaDto(
                                            type = TYPE_STRING,
                                            enumValues = NautiActionCodec.ALL_TYPES,
                                        ),
                                    NautiActionCodec.KEY_START_HARBOUR_ID to
                                        harbourField(
                                            "Pflicht bei plan_trip und start_voyage: Starthafen.",
                                        ),
                                    NautiActionCodec.KEY_DESTINATION_HARBOUR_ID to
                                        harbourField(
                                            "Pflicht bei plan_trip und start_voyage: Zielhafen. " +
                                                "Niemals weglassen, sonst ist die Aktion unbrauchbar.",
                                        ),
                                    NautiActionCodec.KEY_INTERMEDIATE_HARBOUR_IDS to
                                        GeminiSchemaDto(
                                            type = TYPE_ARRAY,
                                            nullable = true,
                                            description = "Optionale Zwischenstopps in Reihenfolge.",
                                            items =
                                                GeminiSchemaDto(
                                                    type = TYPE_STRING,
                                                    enumValues = harbourIds,
                                                ),
                                        ),
                                    NautiActionCodec.KEY_HARBOUR_ID to
                                        harbourField(
                                            "Nur bei show_weather, show_tides und show_bsh_water_level.",
                                        ),
                                    NautiActionCodec.KEY_DEPARTURE to
                                        GeminiSchemaDto(
                                            type = TYPE_STRING,
                                            nullable = true,
                                            description = "ISO-8601 mit Offset, z. B. 2026-08-10T08:00:00+02:00",
                                        ),
                                ),
                            required = listOf(KEY_TYPE),
                            propertyOrdering =
                                listOf(
                                    KEY_TYPE,
                                    NautiActionCodec.KEY_START_HARBOUR_ID,
                                    NautiActionCodec.KEY_DESTINATION_HARBOUR_ID,
                                    NautiActionCodec.KEY_INTERMEDIATE_HARBOUR_IDS,
                                    NautiActionCodec.KEY_HARBOUR_ID,
                                    NautiActionCodec.KEY_DEPARTURE,
                                ),
                        ),
                ),
            required = listOf(KEY_TEXT),
            propertyOrdering = listOf(KEY_TEXT, KEY_ACTION),
        )
    }

    private const val ROLE_USER = "user"
    private const val ROLE_MODEL = "model"
}
