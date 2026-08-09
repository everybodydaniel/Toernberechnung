package com.example.trnberechnung.nauti

import com.example.trnberechnung.BuildConfig
import com.example.trnberechnung.dto.GeminiGenerateContentResponseDto
import com.example.trnberechnung.network.GeminiApiService
import com.example.trnberechnung.network.RetrofitInstance
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * Nauti's Gemini transport.
 *
 * Talks to the REST endpoint through [GeminiApiService] instead of the archived
 * `com.google.ai.client.generativeai` SDK, which could not express `thinkingLevel`, shipped no
 * request timeout, and pinned older ktor/coroutines versions than the rest of the app.
 *
 * On failure the returned [Result] always carries a [NautiError], so callers get a message that is
 * safe to show to the skipper.
 */
class GeminiNautiClient(
    private val service: GeminiApiService = RetrofitInstance.geminiApi,
    private val apiKey: String = BuildConfig.GEMINI_API_KEY,
    private val model: String = GeminiRequestFactory.model,
) : NautiInferenceClient {
    override val isConfigured: Boolean = apiKey.isNotBlank()

    override suspend fun reply(messages: List<NautiPromptMessage>): Result<NautiReply> {
        if (!isConfigured) return Result.failure(NautiError.NotConfigured)
        if (messages.isEmpty()) return Result.failure(NautiError.MalformedReply)

        return runCatching {
            val response =
                service.generateContent(
                    model = model,
                    apiKey = apiKey,
                    request = GeminiRequestFactory.build(messages),
                )
            parseReply(response)
        }.recoverCatching { throwable ->
            throw NautiErrorMapper.map(throwable)
        }
    }

    private fun parseReply(response: GeminiGenerateContentResponseDto): NautiReply {
        NautiErrorMapper
            .fromFinishReason(
                finishReason = response.firstFinishReason,
                blockReason = response.promptFeedback?.blockReason,
            )?.let { throw it }

        val raw = response.firstText
        if (raw.isBlank()) throw NautiError.MalformedReply

        // responseMimeType=application/json plus a responseSchema means this is exactly one JSON
        // object - no fences to strip, no brace matching.
        val root =
            runCatching { JsonParser.parseString(raw).asJsonObject }
                .getOrElse { throw NautiError.MalformedReply }

        val text =
            root
                .string(GeminiRequestFactory.KEY_TEXT)
                .orEmpty()
                .ifBlank { "Ich konnte keine Antwort formulieren." }

        val actionObject =
            root
                .get(GeminiRequestFactory.KEY_ACTION)
                ?.takeIf { it.isJsonObject }
                ?.asJsonObject

        // Decoding goes through NautiActionCodec, the single JSON <-> action mapper. A second copy
        // here is what previously dropped the departure time.
        val action = actionObject?.let(::decodeAction)

        return NautiReply(text = text, action = action)
    }

    /**
     * Decodes a model-supplied action, tolerating an under-specified trip.
     *
     * The response schema cannot make `destination_harbour_id` mandatory for only some action types,
     * and the model has been observed emitting `start_voyage` with a start but no destination. Rather
     * than let the skipper's instruction vanish, an incomplete trip degrades to opening the planner
     * with whatever is known.
     *
     * The leniency lives here, at the model boundary, and deliberately not in [NautiActionCodec]:
     * that codec also round-trips actions through Room, where strictness is what keeps a persisted
     * action faithful to what the skipper confirmed.
     */
    private fun decodeAction(actionObject: JsonObject): NautiAction? {
        val type = actionObject.string(GeminiRequestFactory.KEY_TYPE)
        val decoded = NautiActionCodec.decode(type = type, payload = actionObject.toString())
        if (decoded != null) return decoded
        return if (type == NautiActionCodec.TYPE_PLAN_TRIP || type == NautiActionCodec.TYPE_START_VOYAGE) {
            NautiAction.OpenTripPlanner
        } else {
            null
        }
    }

    private fun JsonObject.string(name: String): String? =
        get(name)?.takeUnless { it.isJsonNull }?.asString?.trim()?.takeIf { it.isNotEmpty() }
}
