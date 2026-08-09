package com.example.trnberechnung.nauti

import com.google.gson.JsonParseException
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Every way the Nauti inference call can fail, each with a German message fit to show the skipper.
 *
 * Previously any failure was turned into `NautiReply(text = throwable.message)`, so a stack trace or
 * an HTTP error string was persisted as if Nauti had said it - and then replayed into later prompts
 * as conversation context. Typed errors plus the `isError` flag on the message row are what stop
 * that.
 */
sealed class NautiError(
    val userMessage: String,
) : Exception(userMessage) {
    data object NotConfigured : NautiError(
        "Nauti ist nicht eingerichtet. Hinterlege GEMINI_API_KEY als Gradle-Property.",
    )

    /**
     * The expected steady state on the Gemini free tier, which allows only 20 requests per day and
     * model - hence the reassurance that the deterministic features keep working.
     */
    data object QuotaExceeded : NautiError(
        "Nauti hat das Tageslimit für freie Antworten erreicht. " +
            "Törnplanung, Gezeiten und Wetter funktionieren weiter.",
    )

    data object ModelUnavailable : NautiError(
        "Das Nauti-Sprachmodell ist nicht mehr verfügbar. Bitte die App aktualisieren.",
    )

    data object Unauthorized : NautiError(
        "Der Nauti-Zugangsschlüssel wurde abgelehnt.",
    )

    data object Offline : NautiError(
        "Keine Verbindung. Nauti braucht Internet - Törnplanung und Gezeiten laufen offline weiter.",
    )

    data object Timeout : NautiError(
        "Nauti antwortet nicht. Bitte später erneut versuchen.",
    )

    data object Truncated : NautiError(
        "Nautis Antwort wurde abgeschnitten. Formuliere die Frage etwas kürzer.",
    )

    data object Blocked : NautiError(
        "Nauti konnte diese Anfrage nicht beantworten.",
    )

    data object MalformedReply : NautiError(
        "Nauti hat eine unlesbare Antwort geliefert.",
    )

    data class Server(
        val code: Int,
    ) : NautiError("Der Nauti-Dienst ist gerade gestört (HTTP $code).")
}

object NautiErrorMapper {
    const val FINISH_REASON_MAX_TOKENS = "MAX_TOKENS"
    const val FINISH_REASON_SAFETY = "SAFETY"
    const val FINISH_REASON_RECITATION = "RECITATION"

    fun map(error: Throwable): NautiError =
        when (error) {
            is NautiError -> error
            is HttpException -> fromHttpStatus(error.code())
            is SocketTimeoutException -> NautiError.Timeout
            is UnknownHostException, is ConnectException -> NautiError.Offline
            is JsonParseException -> NautiError.MalformedReply
            is IOException -> NautiError.Offline
            else -> NautiError.MalformedReply
        }

    fun fromHttpStatus(code: Int): NautiError =
        when {
            code == 401 || code == 403 -> NautiError.Unauthorized
            code == 404 -> NautiError.ModelUnavailable
            code == 429 -> NautiError.QuotaExceeded
            code >= 500 -> NautiError.Server(code)
            else -> NautiError.Server(code)
        }

    /**
     * Maps a 200 response that still carries no usable answer. Checked before parsing, because a
     * truncated reply is not malformed JSON - it is a complete failure with a different remedy.
     */
    fun fromFinishReason(
        finishReason: String?,
        blockReason: String?,
    ): NautiError? =
        when {
            blockReason != null -> NautiError.Blocked
            finishReason == FINISH_REASON_MAX_TOKENS -> NautiError.Truncated
            finishReason == FINISH_REASON_SAFETY -> NautiError.Blocked
            finishReason == FINISH_REASON_RECITATION -> NautiError.Blocked
            else -> null
        }
}
