package com.example.trnberechnung.ui.map

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FullBleedMapStylesTest {
    @Test
    fun onlineStyleContainsBaseMapAndNauticalOverlay() {
        val style = JsonParser.parseString(FullBleedMapStyles.online).asJsonObject
        val sources = style.getAsJsonObject("sources")
        val layerIds =
            style
                .getAsJsonArray("layers")
                .map { it.asJsonObject.get("id").asString }

        assertEquals(8, style.get("version").asInt)
        assertTrue(sources.has("osm"))
        assertTrue(sources.has("openseamap"))
        assertEquals(
            listOf("sea-background", "osm-layer", "openseamap-layer"),
            layerIds,
        )
    }

    @Test
    fun offlineStyleHasNoRemoteSourcesAndKeepsVisibleBackground() {
        val style = JsonParser.parseString(FullBleedMapStyles.offline).asJsonObject
        val sources = style.getAsJsonObject("sources")
        val layers = style.getAsJsonArray("layers")

        assertTrue(sources.entrySet().isEmpty())
        assertEquals(1, layers.size())
        assertEquals(
            "offline-sea-background",
            layers.first().asJsonObject.get("id").asString,
        )
    }
}
