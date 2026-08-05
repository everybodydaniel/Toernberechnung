package com.example.trnberechnung

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.*

class TideNodeUnitTest {

    // 1. Haversine Distanzberechnung (Seemeilen)
    private fun calculateHaversineNm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val rNm = 3440.065
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return rNm * c
    }

    // 2. Fahrzeit-Berechnung in Minuten
    private fun calculateTravelTimeMinutes(distanceNm: Double, speedKnots: Double): Int {
        if (speedKnots <= 0.0) return 0
        return (distanceNm / speedKnots * 60.0).roundToInt()
    }

    // 3. Routen-Status Bewertung
    private fun evaluateRouteStatus(depth: Double, draft: Double, margin: Double): String {
        val required = draft + margin
        return when {
            depth < draft -> "NICHT_BEFAHRBAR"
            depth < required -> "EINGESCHRAENKT"
            else -> "BEFAHRBAR"
        }
    }

    @Test
    fun testHaversineDistanceCalculation() {
        // Borkum (53.60, 6.67) bis Norderney (53.70, 7.15)
        val dist = calculateHaversineNm(53.60, 6.67, 53.70, 7.15)
        assertTrue("Distanz sollte zwischen 16.0 und 20.0 NM liegen", dist in 16.0..20.0)
    }

    @Test
    fun testTravelTimeCalculation() {
        val mins = calculateTravelTimeMinutes(13.0, 6.5)
        assertEquals("13.0 NM bei 6.5 kn sollte 120 Minuten dauern", 120, mins)
    }

    @Test
    fun testRouteStatusBefahrbar() {
        val status = evaluateRouteStatus(3.0, 1.2, 0.5)
        assertEquals("Wassertiefe 3.0m mit Tiefgang 1.2m + 0.5m Marge muss BEFAHRBAR sein", "BEFAHRBAR", status)
    }

    @Test
    fun testRouteStatusEingeschraenkt() {
        val status = evaluateRouteStatus(1.4, 1.2, 0.5)
        assertEquals("Wassertiefe 1.4m mit Tiefgang 1.2m + 0.5m Marge muss EINGESCHRAENKT sein", "EINGESCHRAENKT", status)
    }

    @Test
    fun testRouteStatusNichtBefahrbar() {
        val status = evaluateRouteStatus(1.0, 1.2, 0.5)
        assertEquals("Wassertiefe 1.0m unter Tiefgang 1.2m muss NICHT_BEFAHRBAR sein", "NICHT_BEFAHRBAR", status)
    }

    @Test
    fun testAlphabeticalStationSorting() {
        val stations = listOf("Borkum", "Juist", "Baltrum", "Norderney", "Emden", "Harlesiel")
        val sorted = stations.sorted()
        assertEquals("Erster Ort muss Baltrum sein", "Baltrum", sorted[0])
        assertEquals("Zweiter Ort muss Borkum sein", "Borkum", sorted[1])
        assertEquals("Dritter Ort muss Emden sein", "Emden", sorted[2])
    }
}
