package com.example.trnberechnung.routing.v2

import com.example.trnberechnung.logic.NauticalRouter
import com.example.trnberechnung.logic.RouterLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.maplibre.android.geometry.LatLng
import java.util.regex.Pattern

/**
 * Service zum Abruf aktueller Watthöhen (z.B. von Wattsegler.de oder ELWIS).
 * Diese Werte überschreiben die statischen Basiswerte in der SeaMask.
 */
object TidalDepthService {
    private const val TAG = "TidalDepthService"
    private val client = OkHttpClient()

    // Zentrale Punkte der bekanntesten Watthochs
    private val DEPTH_POINTS = mapOf(
        "Dornumer Nacken" to (LatLng(53.695, 7.410) to "watt_dornum"),
        "Langeooger Watt" to (LatLng(53.700, 7.520) to "watt_bensersiel"),
        "Spiekerooger Watt" to (LatLng(53.720, 7.650) to "watt_neuharling"),
        "Wangerooger Watt" to (LatLng(53.740, 7.920) to "watt_wangerooge"),
        "Baltrumer Watt" to (LatLng(53.700, 7.370) to "watt_nesskana"),
        "Memmert Watt" to (LatLng(53.640, 6.950) to "memmert_e3"),
        "Juister Watt" to (LatLng(53.665, 7.150) to "watt_norden")
    )

    /**
     * Startet den Abruf der Live-Daten und aktualisiert die SeaMask.
     */
    suspend fun refreshDepths() = withContext(Dispatchers.IO) {
        RouterLog.i(TAG, "Starte Live-Update der Watthöhen von externen Quellen...")

        try {
            val updates = fetchFromWattsegler()
            if (updates.isEmpty()) {
                RouterLog.w(TAG, "Keine Live-Daten empfangen, verwende statische Basiswerte.")
                return@withContext
            }

            updates.forEach { (name, depth) ->
                // Wir suchen nach Übereinstimmungen in unseren bekannten Punkten
                val entry = DEPTH_POINTS.entries.find { name.contains(it.key, ignoreCase = true) }
                if (entry != null) {
                    val (pos, wpId) = entry.value
                    updateSeaMaskArea(pos, depth)
                    NauticalRouter.updateWaypointDepth(wpId, depth)
                    RouterLog.i(TAG, "Live-Update erfolgreich: ${entry.key} ($wpId) auf $depth m (LAT) korrigiert.")
                }
            }
        } catch (e: Exception) {
            RouterLog.w(TAG, "Fehler beim Live-Update der Tiefen: ${e.message}")
        }
    }

    private fun fetchFromWattsegler(): Map<String, Double> {
        val results = mutableMapOf<String, Double>()

        // Hinweis: Dies ist ein Beispiel-URL. In der Realität müsste hier die
        // aktuellste Quelle für Watthöhen eingetragen werden.
        val url = "https://www.wattsegler.de/service/watthoehen.html"

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) TörnberechnungApp/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyMap()
                val html = response.body?.string() ?: return emptyMap()

                // Simpler Regex-Parser für Tabellenzeilen der Form: "Ort ... -1,2 m"
                // Sucht nach Namen und Dezimalzahlen mit Komma
                val pattern = Pattern.compile("([a-zA-Z\\s]{5,25})[\\s\\S]*?(-?\\d,\\d+)\\s*m", Pattern.CASE_INSENSITIVE)
                val matcher = pattern.matcher(html)

                while (matcher.find()) {
                    val name = matcher.group(1)?.trim() ?: continue
                    val depthStr = matcher.group(2)?.replace(",", ".") ?: continue
                    val depth = depthStr.toDoubleOrNull()
                    if (depth != null) {
                        results[name] = depth
                    }
                }
            }
        } catch (e: Exception) {
            RouterLog.w(TAG, "Konnte Wattsegler.de nicht erreichen - Fallback auf interne Werte")
            // Fallback: Wenn Netz weg, geben wir die statisch sichersten Werte zurück
            return mapOf(
                "Dornumer Nacken" to -1.2,
                "Langeooger Watt" to -1.4,
                "Spiekerooger Watt" to -1.5,
                "Wangerooger Watt" to -1.6
            )
        }

        return results
    }

    private fun updateSeaMaskArea(pos: LatLng, depthMeters: Double) {
        val row0 = GridConfig.latToRow(pos.latitude)
        val col0 = GridConfig.lonToCol(pos.longitude)

        // Wir aktualisieren ein Feld von ca. 300x300m um den Punkt herum
        val radius = 3
        for (r in (row0 - radius)..(row0 + radius)) {
            for (c in (col0 - radius)..(col0 + radius)) {
                if (GridConfig.inBounds(r, c)) {
                    SeaMask.setDepth(r, c, depthMeters)
                }
            }
        }
    }
}
