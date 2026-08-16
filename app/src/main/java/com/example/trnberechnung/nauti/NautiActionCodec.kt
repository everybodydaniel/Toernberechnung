package com.example.trnberechnung.nauti

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.time.ZonedDateTime

/**
 * The single JSON <-> [NautiAction] mapper.
 *
 * Both the model reply and the Room persistence layer go through here. They used to have separate,
 * copy-pasted `when (type)` blocks, and both copies silently dropped `departure` - so a departure
 * time the skipper had asked for could never reach the planner. Keeping one mapper is what stops
 * that from happening again.
 */
object NautiActionCodec {
    fun type(action: NautiAction): String =
        when (action) {
            NautiAction.OpenTripPlanner -> TYPE_OPEN_TRIP_PLANNER
            is NautiAction.PlanTrip -> TYPE_PLAN_TRIP
            NautiAction.StartNavigation -> TYPE_START_NAVIGATION
            is NautiAction.StartVoyage -> TYPE_START_VOYAGE
            NautiAction.ShowPassageWindow -> TYPE_SHOW_PASSAGE_WINDOW
            is NautiAction.ShowWeather -> TYPE_SHOW_WEATHER
            is NautiAction.ShowTides -> TYPE_SHOW_TIDES
            is NautiAction.ShowBshWaterLevel -> TYPE_SHOW_BSH_WATER_LEVEL
        }

    fun payload(action: NautiAction): String {
        val root = JsonObject()
        when (action) {
            is NautiAction.PlanTrip ->
                root.writeTrip(
                    action.startHarbourId,
                    action.destinationHarbourId,
                    action.intermediateHarbourIds,
                    action.departure,
                )

            is NautiAction.StartVoyage ->
                root.writeTrip(
                    action.startHarbourId,
                    action.destinationHarbourId,
                    action.intermediateHarbourIds,
                    action.departure,
                )

            is NautiAction.ShowWeather -> action.harbourId?.let { root.addProperty(KEY_HARBOUR_ID, it) }
            is NautiAction.ShowTides -> action.harbourId?.let { root.addProperty(KEY_HARBOUR_ID, it) }
            is NautiAction.ShowBshWaterLevel -> action.harbourId?.let { root.addProperty(KEY_HARBOUR_ID, it) }
            else -> Unit
        }
        return root.toString()
    }

    fun decode(
        type: String?,
        payload: String?,
    ): NautiAction? {
        val root =
            payload
                ?.takeIf(String::isNotBlank)
                ?.let {
                    runCatching { JsonParser.parseString(it).asJsonObject }.getOrNull()
                } ?: JsonObject()
        return when (type) {
            TYPE_OPEN_TRIP_PLANNER -> NautiAction.OpenTripPlanner
            TYPE_START_NAVIGATION -> NautiAction.StartNavigation
            TYPE_SHOW_PASSAGE_WINDOW -> NautiAction.ShowPassageWindow
            TYPE_SHOW_WEATHER -> NautiAction.ShowWeather(root.string(KEY_HARBOUR_ID))
            TYPE_SHOW_TIDES -> NautiAction.ShowTides(root.string(KEY_HARBOUR_ID))
            TYPE_SHOW_BSH_WATER_LEVEL -> NautiAction.ShowBshWaterLevel(root.string(KEY_HARBOUR_ID))
            TYPE_PLAN_TRIP -> {
                val trip = root.readTrip() ?: return null
                NautiAction.PlanTrip(
                    startHarbourId = trip.start,
                    destinationHarbourId = trip.destination,
                    intermediateHarbourIds = trip.stops,
                    departure = trip.departure,
                )
            }
            TYPE_START_VOYAGE -> {
                val trip = root.readTrip() ?: return null
                NautiAction.StartVoyage(
                    startHarbourId = trip.start,
                    destinationHarbourId = trip.destination,
                    intermediateHarbourIds = trip.stops,
                    departure = trip.departure,
                )
            }
            else -> null
        }
    }

    /**
     * Whether the skipper has to tap the action before anything happens.
     *
     * [NautiAction.StartVoyage] must stay in this set: it is the only action that can put the boat
     * under GPS recording from a chat sentence.
     */
    fun requiresConfirmation(action: NautiAction): Boolean =
        action is NautiAction.PlanTrip ||
            action is NautiAction.StartVoyage ||
            action == NautiAction.StartNavigation

    /**
     * Whether the action may fire the moment the reply arrives, without any tap.
     *
     * Deliberately narrower than `!requiresConfirmation`: every data request - weather, tides and
     * BSH water level - is answered by a widget inside the chat, so none of them may yank the
     * skipper off the map. The widget carries its own "Im Revier öffnen" affordance instead.
     */
    fun autoDispatches(action: NautiAction): Boolean =
        !requiresConfirmation(action) &&
            action !is NautiAction.ShowWeather &&
            action !is NautiAction.ShowTides &&
            action !is NautiAction.ShowBshWaterLevel

    const val TYPE_OPEN_TRIP_PLANNER = "open_trip_planner"
    const val TYPE_PLAN_TRIP = "plan_trip"
    const val TYPE_START_NAVIGATION = "start_navigation"
    const val TYPE_START_VOYAGE = "start_voyage"
    const val TYPE_SHOW_PASSAGE_WINDOW = "show_passage_window"
    const val TYPE_SHOW_WEATHER = "show_weather"
    const val TYPE_SHOW_TIDES = "show_tides"
    const val TYPE_SHOW_BSH_WATER_LEVEL = "show_bsh_water_level"

    /** Every action type the model is allowed to emit, for the response schema and the prompt. */
    val ALL_TYPES: List<String> =
        listOf(
            TYPE_OPEN_TRIP_PLANNER,
            TYPE_PLAN_TRIP,
            TYPE_START_NAVIGATION,
            TYPE_START_VOYAGE,
            TYPE_SHOW_PASSAGE_WINDOW,
            TYPE_SHOW_WEATHER,
            TYPE_SHOW_TIDES,
            TYPE_SHOW_BSH_WATER_LEVEL,
        )

    const val KEY_HARBOUR_ID = "harbour_id"
    const val KEY_START_HARBOUR_ID = "start_harbour_id"
    const val KEY_DESTINATION_HARBOUR_ID = "destination_harbour_id"
    const val KEY_INTERMEDIATE_HARBOUR_IDS = "intermediate_harbour_ids"
    const val KEY_DEPARTURE = "departure"
}

private class TripFields(
    val start: String,
    val destination: String,
    val stops: List<String>,
    val departure: ZonedDateTime?,
)

private fun JsonObject.readTrip(): TripFields? {
    val start = string(NautiActionCodec.KEY_START_HARBOUR_ID) ?: return null
    val destination = string(NautiActionCodec.KEY_DESTINATION_HARBOUR_ID) ?: return null
    val stops =
        getAsJsonArray(NautiActionCodec.KEY_INTERMEDIATE_HARBOUR_IDS)
            ?.mapNotNull { runCatching { it.asString }.getOrNull() }
            .orEmpty()
    return TripFields(start, destination, stops, departure())
}

private fun JsonObject.writeTrip(
    start: String,
    destination: String,
    stops: List<String>,
    departure: ZonedDateTime?,
) {
    addProperty(NautiActionCodec.KEY_START_HARBOUR_ID, start)
    addProperty(NautiActionCodec.KEY_DESTINATION_HARBOUR_ID, destination)
    add(
        NautiActionCodec.KEY_INTERMEDIATE_HARBOUR_IDS,
        JsonArray().apply { stops.forEach(::add) },
    )
    departure?.let { addProperty(NautiActionCodec.KEY_DEPARTURE, it.toString()) }
}

/**
 * Tolerant ISO-8601 parse. A departure the model formatted oddly must degrade to "no departure
 * given" rather than discarding the whole action - the skipper can still adjust it in the planner.
 */
private fun JsonObject.departure(): ZonedDateTime? =
    string(NautiActionCodec.KEY_DEPARTURE)?.let {
        runCatching { ZonedDateTime.parse(it) }.getOrNull()
    }

private fun JsonObject.string(name: String): String? =
    get(name)?.takeUnless { it.isJsonNull }?.asString?.trim()?.takeIf(String::isNotEmpty)
