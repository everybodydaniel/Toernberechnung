package com.example.trnberechnung.nauti

import com.example.trnberechnung.mapplanning.HarbourCatalog
import com.example.trnberechnung.mapplanning.HarbourId
import com.example.trnberechnung.model.TideStationData

/**
 * Resolves a Nauti harbour id to the closest BSH tide station.
 *
 * Extracted unchanged from `MapTabScreen.selectNautiStation` so the map screen and the in-chat
 * widget resolver pick the same station for the same harbour - two implementations would let a
 * widget disagree with the Revier screen it links to.
 */
object NautiStationMatcher {
    fun nearestStation(
        rawHarbourId: String?,
        stations: List<TideStationData>,
    ): TideStationData? {
        if (stations.isEmpty()) return null
        val harbour = HarbourId.fromRawValue(rawHarbourId)?.let(HarbourCatalog::get)
            ?: return stations.firstOrNull()
        return stations.minByOrNull {
            val latitudeDelta = it.latitude - harbour.coordinate.latitude
            val longitudeDelta = it.longitude - harbour.coordinate.longitude
            latitudeDelta * latitudeDelta + longitudeDelta * longitudeDelta
        }
    }
}
