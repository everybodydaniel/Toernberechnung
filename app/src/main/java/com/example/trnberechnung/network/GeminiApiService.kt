package com.example.trnberechnung.network

import com.example.trnberechnung.dto.GeminiGenerateContentRequestDto
import com.example.trnberechnung.dto.GeminiGenerateContentResponseDto
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Gemini Developer API.
 *
 * The key travels in the `x-goog-api-key` header, never as a `?key=` query parameter: query strings
 * end up in proxy and crash logs.
 *
 * Declaring this as an interface is also what makes `GeminiNautiClient` unit-testable - a test
 * implements it directly, so no MockWebServer dependency is needed.
 */
interface GeminiApiService {
    /**
     * Returns the parsed body and therefore throws `retrofit2.HttpException` on any non-2xx status.
     * `NautiErrorMapper` depends on that: it reads `HttpException.code()` to tell a spent quota
     * (429) apart from a retired model (404).
     */
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GeminiGenerateContentRequestDto,
    ): GeminiGenerateContentResponseDto
}
