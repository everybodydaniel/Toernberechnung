package com.example.trnberechnung.nauti

import com.example.trnberechnung.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.google.gson.JsonObject
import com.google.gson.JsonParser

class GeminiNautiClient(
    private val apiKey: String = BuildConfig.GEMINI_API_KEY,
) : NautiInferenceClient {
    override val isConfigured: Boolean = apiKey.isNotBlank()

    private val model: GenerativeModel? by lazy {
        apiKey.takeIf(String::isNotBlank)?.let { key ->
            GenerativeModel(
                modelName = "gemini-2.5-flash",
                apiKey = key,
                generationConfig =
                    generationConfig {
                        temperature = 0.45f
                        topP = 0.9f
                        maxOutputTokens = 900
                    },
                systemInstruction =
                    content {
                        text(SYSTEM_INSTRUCTION)
                    },
            )
        }
    }

    override suspend fun reply(messages: List<NautiPromptMessage>): Result<NautiReply> =
        runCatching {
            val configuredModel =
                model ?: error(
                    "Gemini ist nicht konfiguriert. Hinterlege GEMINI_API_KEY als lokale Gradle-Property.",
                )
            val conversation =
                messages
                    .takeLast(24)
                    .joinToString("\n") {
                        val role = if (it.role == NautiRole.USER) "SKIPPER" else "NAUTI"
                        "$role: ${it.text}"
                    }
            val response =
                configuredModel.generateContent(
                    "$conversation\n\nAntworte jetzt ausschließlich im vereinbarten JSON-Format.",
                ).text.orEmpty()
            parseReply(response)
        }

    private fun parseReply(raw: String): NautiReply {
        val jsonText =
            raw
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
                .let(::extractFirstJsonObject)
        val root = JsonParser.parseString(jsonText).asJsonObject
        val text = root.string("text").orEmpty().ifBlank { "Ich konnte keine Antwort formulieren." }
        val action =
            root.get("action")
                ?.takeIf { it.isJsonObject }
                ?.asJsonObject
                ?.toAction()
        return NautiReply(text = text, action = action)
    }

    private fun JsonObject.toAction(): NautiAction? {
        return when (string("type")?.lowercase()) {
            "open_trip_planner" -> NautiAction.OpenTripPlanner
            "start_navigation" -> NautiAction.StartNavigation
            "show_passage_window" -> NautiAction.ShowPassageWindow
            "show_weather" -> NautiAction.ShowWeather(string("harbour_id"))
            "show_tides" -> NautiAction.ShowTides(string("harbour_id"))
            "show_bsh_water_level" -> NautiAction.ShowBshWaterLevel(string("harbour_id"))
            "plan_trip" -> {
                val start = string("start_harbour_id") ?: return null
                val destination = string("destination_harbour_id") ?: return null
                val stops =
                    getAsJsonArray("intermediate_harbour_ids")
                        ?.mapNotNull { runCatching { it.asString }.getOrNull() }
                        .orEmpty()
                NautiAction.PlanTrip(start, destination, stops)
            }
            else -> null
        }
    }

    private fun JsonObject.string(name: String): String? =
        get(name)?.takeUnless { it.isJsonNull }?.asString?.trim()?.takeIf { it.isNotEmpty() }

    private fun extractFirstJsonObject(value: String): String {
        val start = value.indexOf('{')
        if (start < 0) error("Gemini lieferte kein JSON.")
        var depth = 0
        var quoted = false
        var escaped = false
        for (index in start until value.length) {
            val character = value[index]
            if (escaped) {
                escaped = false
                continue
            }
            if (character == '\\' && quoted) {
                escaped = true
                continue
            }
            if (character == '"') quoted = !quoted
            if (!quoted) {
                if (character == '{') depth += 1
                if (character == '}') {
                    depth -= 1
                    if (depth == 0) return value.substring(start, index + 1)
                }
            }
        }
        error("Gemini lieferte unvollständiges JSON.")
    }

    companion object {
        private const val SYSTEM_INSTRUCTION =
            """
            Du bist Nauti, der deutschsprachige nautische Assistent von TideNode.
            Antworte kompakt und praxisnah. Du darfst niemals selbst entscheiden,
            ob eine Route befahrbar ist: Go/No-Go, WuK, Wetterstatus und
            Passagefenster kommen ausschließlich aus der deterministischen App.

            Erlaubte Aktionen:
            open_trip_planner, plan_trip, start_navigation, show_passage_window,
            show_weather, show_tides, show_bsh_water_level.
            Erlaubte Hafen-IDs:
            borkum_harbor, emden_harbor, juist_harbor, norderney_harbor,
            baltrum_harbor, langeoog_harbor, spiekeroog_harbor, wangerooge_harbor.

            Ausgabeformat:
            {"text":"Antwort","action":null}
            oder
            {"text":"Antwort","action":{"type":"show_weather","harbour_id":"juist_harbor"}}
            """
    }
}
