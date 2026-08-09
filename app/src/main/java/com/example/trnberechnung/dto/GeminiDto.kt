package com.example.trnberechnung.dto

import com.google.gson.annotations.SerializedName

/**
 * Wire model for the Gemini Developer API `generateContent` endpoint.
 *
 * Hand-rolled rather than taken from an SDK because the two settings Nauti depends on -
 * `thinkingConfig.thinkingLevel` and `responseSchema` - are not expressible in the archived
 * `com.google.ai.client.generativeai` client. See `GeminiRequestFactory` for the measured evidence.
 *
 * Gson omits null fields by default, which this model relies on: sending an explicit
 * `"items": null` or `"thinkingConfig": null` makes the API answer HTTP 400.
 */
data class GeminiPartDto(
    val text: String,
)

data class GeminiContentDto(
    val role: String? = null,
    val parts: List<GeminiPartDto>,
)

data class GeminiThinkingConfigDto(
    @SerializedName("thinkingLevel") val thinkingLevel: String,
)

/** OpenAPI-3.0 subset accepted by Gemini's `responseSchema`. */
data class GeminiSchemaDto(
    val type: String,
    val properties: Map<String, GeminiSchemaDto>? = null,
    val items: GeminiSchemaDto? = null,
    val required: List<String>? = null,
    @SerializedName("enum") val enumValues: List<String>? = null,
    val nullable: Boolean? = null,
    val description: String? = null,
    /**
     * Gemini-specific hint for the order in which fields should be produced. Materially improves how
     * consistently dependent fields get filled in - see `GeminiRequestFactory.actionEnvelopeSchema`.
     */
    @SerializedName("propertyOrdering") val propertyOrdering: List<String>? = null,
)

data class GeminiGenerationConfigDto(
    val temperature: Double,
    val topP: Double,
    @SerializedName("maxOutputTokens") val maxOutputTokens: Int,
    @SerializedName("responseMimeType") val responseMimeType: String,
    @SerializedName("responseSchema") val responseSchema: GeminiSchemaDto? = null,
    @SerializedName("thinkingConfig") val thinkingConfig: GeminiThinkingConfigDto? = null,
)

data class GeminiGenerateContentRequestDto(
    val contents: List<GeminiContentDto>,
    @SerializedName("systemInstruction") val systemInstruction: GeminiContentDto? = null,
    @SerializedName("generationConfig") val generationConfig: GeminiGenerationConfigDto,
)

data class GeminiCandidateDto(
    val content: GeminiContentDto? = null,
    @SerializedName("finishReason") val finishReason: String? = null,
)

data class GeminiPromptFeedbackDto(
    @SerializedName("blockReason") val blockReason: String? = null,
)

data class GeminiUsageMetadataDto(
    @SerializedName("promptTokenCount") val promptTokenCount: Int? = null,
    @SerializedName("thoughtsTokenCount") val thoughtsTokenCount: Int? = null,
    @SerializedName("candidatesTokenCount") val candidatesTokenCount: Int? = null,
    @SerializedName("totalTokenCount") val totalTokenCount: Int? = null,
)

data class GeminiGenerateContentResponseDto(
    val candidates: List<GeminiCandidateDto>? = null,
    @SerializedName("promptFeedback") val promptFeedback: GeminiPromptFeedbackDto? = null,
    @SerializedName("usageMetadata") val usageMetadata: GeminiUsageMetadataDto? = null,
) {
    /** Concatenated text of the first candidate, or empty when the model returned no parts. */
    val firstText: String
        get() =
            candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.joinToString(separator = "") { it.text }
                .orEmpty()

    val firstFinishReason: String?
        get() = candidates?.firstOrNull()?.finishReason
}
