package com.example.trnberechnung.nauti

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser

object NautiActionCodec {
    fun type(action: NautiAction): String =
        when (action) {
            NautiAction.OpenTripPlanner -> "open_trip_planner"
            is NautiAction.PlanTrip -> "plan_trip"
            NautiAction.StartNavigation -> "start_navigation"
            NautiAction.ShowPassageWindow -> "show_passage_window"
            is NautiAction.ShowWeather -> "show_weather"
            is NautiAction.ShowTides -> "show_tides"
            is NautiAction.ShowBshWaterLevel -> "show_bsh_water_level"
        }

    fun payload(action: NautiAction): String {
        val root = JsonObject()
        when (action) {
            is NautiAction.PlanTrip -> {
                root.addProperty("start_harbour_id", action.startHarbourId)
                root.addProperty("destination_harbour_id", action.destinationHarbourId)
                root.add(
                    "intermediate_harbour_ids",
                    JsonArray().apply {
                        action.intermediateHarbourIds.forEach(::add)
                    },
                )
                action.departure?.let { root.addProperty("departure", it.toString()) }
            }
            is NautiAction.ShowWeather -> action.harbourId?.let { root.addProperty("harbour_id", it) }
            is NautiAction.ShowTides -> action.harbourId?.let { root.addProperty("harbour_id", it) }
            is NautiAction.ShowBshWaterLevel -> action.harbourId?.let { root.addProperty("harbour_id", it) }
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
            "open_trip_planner" -> NautiAction.OpenTripPlanner
            "start_navigation" -> NautiAction.StartNavigation
            "show_passage_window" -> NautiAction.ShowPassageWindow
            "show_weather" -> NautiAction.ShowWeather(root.string("harbour_id"))
            "show_tides" -> NautiAction.ShowTides(root.string("harbour_id"))
            "show_bsh_water_level" -> NautiAction.ShowBshWaterLevel(root.string("harbour_id"))
            "plan_trip" -> {
                val start = root.string("start_harbour_id") ?: return null
                val destination = root.string("destination_harbour_id") ?: return null
                val stops =
                    root.getAsJsonArray("intermediate_harbour_ids")
                        ?.mapNotNull { runCatching { it.asString }.getOrNull() }
                        .orEmpty()
                NautiAction.PlanTrip(start, destination, stops)
            }
            else -> null
        }
    }

    fun requiresConfirmation(action: NautiAction): Boolean =
        action is NautiAction.PlanTrip || action == NautiAction.StartNavigation
}

private fun JsonObject.string(name: String): String? =
    get(name)?.takeUnless { it.isJsonNull }?.asString?.trim()?.takeIf(String::isNotEmpty)
