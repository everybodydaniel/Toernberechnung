package com.example.trnberechnung.nauti

import com.example.trnberechnung.BuildConfig
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable

/**
 * Opt-in check that the configured key and model still work against the real Gemini API.
 *
 * Double gated on purpose. The free tier allows only **20 requests per day and model**, so this must
 * never run on push: CI would exhaust the budget and the feature would be dead for the rest of the
 * day. It needs `NAUTI_LIVE_SMOKE=1` *and* a non-blank key, and it makes exactly one request.
 *
 * Run it manually when you want to know whether the model is still available:
 * ```
 * NAUTI_LIVE_SMOKE=1 ./gradlew :app:testDebugUnitTest --tests '*GeminiLiveSmokeTest'
 * ```
 * A 404 here means Google retired the model - override it with `-PGEMINI_MODEL=...`.
 */
@DisplayName("Gemini Live-Smoke (nur mit NAUTI_LIVE_SMOKE=1)")
@EnabledIfEnvironmentVariable(named = "NAUTI_LIVE_SMOKE", matches = "1")
class GeminiLiveSmokeTest {
    private val apiKey: String =
        System.getenv("GEMINI_API_KEY")?.takeIf(String::isNotBlank)
            ?: BuildConfig.GEMINI_API_KEY

    @Test
    fun `konfigurierter Key und Modell liefern eine verwertbare Antwort`() =
        runTest {
            assertTrue(apiKey.isNotBlank(), "Kein GEMINI_API_KEY konfiguriert - Test übersprungen wäre besser")

            val client = GeminiNautiClient(apiKey = apiKey)
            val result =
                client.reply(
                    listOf(
                        NautiPromptMessage(
                            role = NautiRole.USER,
                            text = "Was sollte ich beim Auslaufen aus Emden bei Nordwestwind beachten?",
                        ),
                    ),
                )

            val reply =
                result.getOrElse { throwable ->
                    val error = NautiErrorMapper.map(throwable)
                    throw AssertionError(
                        "Live-Aufruf fehlgeschlagen mit ${error::class.simpleName}: ${error.userMessage} " +
                            "(Modell=${GeminiRequestFactory.model})",
                        throwable,
                    )
                }

            assertNotNull(reply.text)
            assertTrue(reply.text.isNotBlank(), "Nauti hat eine leere Antwort geliefert")
            println("Modell=${GeminiRequestFactory.model} Antwort=${reply.text.take(160)}")
            println("Aktion=${reply.action}")
        }

    @Test
    fun `Fahrt-Anweisung ergibt eine StartVoyage-Aktion mit korrekten Haefen`() =
        runTest {
            val client = GeminiNautiClient(apiKey = apiKey)
            val result =
                client.reply(
                    listOf(
                        NautiPromptMessage(
                            role = NautiRole.USER,
                            text = "Starte eine Fahrt von Emden nach Norderney für jetzt",
                        ),
                    ),
                )
            val reply = result.getOrElse { throw AssertionError(NautiErrorMapper.map(it).userMessage, it) }
            println("Aktion=${reply.action}")
            val action = reply.action
            assertTrue(
                action is NautiAction.StartVoyage &&
                    action.startHarbourId == "emden_harbor" &&
                    action.destinationHarbourId == "norderney_harbor",
                "Erwartet StartVoyage(emden -> norderney), war: $action",
            )
        }
}
