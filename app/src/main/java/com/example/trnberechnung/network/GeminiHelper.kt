package com.example.trnberechnung.network

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig

/**
 * Zentraler Helper für die Gemini AI-Integration.
 * Nutzt Gemini 2.5 Flash als Modell für schnelle Antworten.
 */
object GeminiHelper {

    private val API_KEY: String by lazy {
        String(android.util.Base64.decode("QVEuQWI4Uk42TEVER1BVSnZ0YWgwQzMtLUlXYk9XSGlab2l6UFJldXpBOTBmUmRpTWN3alE=", android.util.Base64.DEFAULT)).trim()
    }
    private const val MODEL_NAME = "gemini-2.5-flash"

    /**
     * Generatives Modell für allgemeine Text-Anfragen (Navigation, Wetter-Tipps, etc.)
     */
    val model: GenerativeModel by lazy {
        GenerativeModel(
            modelName = MODEL_NAME,
            apiKey = API_KEY,
            generationConfig = generationConfig {
                temperature = 0.7f
                topP = 0.95f
                maxOutputTokens = 1024
            },
            systemInstruction = com.google.ai.client.generativeai.type.content {
                text(
                    """Du bist ein freundlicher Segeltörn-Assistent in der App "Törnberechnung". 
                    |Du hilfst Skippern und Crew-Mitgliedern bei Fragen rund ums Segeln,
                    |Navigation, Wetter, Gezeiten, Revierplanung und Seemannschaft.
                    |Antworte immer auf Deutsch, kompakt und praxisnah.
                    |Wenn du dir unsicher bist, weise darauf hin, dass der Skipper 
                    |immer die letzte Entscheidung trifft.""".trimMargin()
                )
            }
        )
    }

    /**
     * Generiert eine Antwort auf eine Nutzeranfrage.
     * @param prompt Die Frage oder Eingabe des Nutzers.
     * @return Die generierte Antwort als String, oder eine Fehlermeldung.
     */
    suspend fun askQuestion(prompt: String): String {
        return try {
            val response = model.generateContent(prompt)
            response.text ?: "Keine Antwort erhalten."
        } catch (e: Exception) {
            "Fehler bei der Anfrage: ${e.message}"
        }
    }
}
