package com.example.trnberechnung

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.math.*

@DisplayName("JUnit 5 Unit-Test Suite für TideNode Nautik-Logik")
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
    @DisplayName("TC01: Haversine Distanz zwischen Borkum und Norderney")
    fun testHaversineDistanceCalculation() {
        val dist = calculateHaversineNm(53.60, 6.67, 53.70, 7.15)
        assertTrue(dist in 16.0..20.0, "Distanz sollte zwischen 16.0 und 20.0 NM liegen (Errechnet: $dist)")
    }

    @ParameterizedTest(name = "Distanz {0} NM bei {1} kn = {2} Min")
    @CsvSource(
        "13.0, 6.5, 120",
        "6.5, 6.5, 60",
        "26.0, 13.0, 120",
        "0.0, 5.0, 0"
    )
    @DisplayName("TC02: Parameterisierte Fahrzeitberechnung")
    fun testTravelTimeCalculation(distanceNm: Double, speedKnots: Double, expectedMinutes: Int) {
        val mins = calculateTravelTimeMinutes(distanceNm, speedKnots)
        assertEquals(expectedMinutes, mins, "Fahrzeit in Minuten für $distanceNm NM bei $speedKnots kn ist nicht korrekt")
    }

    @ParameterizedTest(name = "Wassertiefe {0}m, Tiefgang {1}m, Marge {2}m -> Status: {3}")
    @CsvSource(
        "3.0, 1.2, 0.5, BEFAHRBAR",
        "1.4, 1.2, 0.5, EINGESCHRAENKT",
        "1.0, 1.2, 0.5, NICHT_BEFAHRBAR"
    )
    @DisplayName("TC03-TC05: Parameterisierte Routen-Sicherheitsbewertung")
    fun testRouteStatusEvaluation(depth: Double, draft: Double, margin: Double, expectedStatus: String) {
        val status = evaluateRouteStatus(depth, draft, margin)
        assertEquals(expectedStatus, status, "Status-Bewertung unterscheidet sich vom erwarteten Ergebnis")
    }

    @Test
    @DisplayName("TC06: Gezeiten Wasserstand Zwölftel-Regel 50% Hub")
    fun testTideTwelfthsRule() {
        val lowWater = 0.8
        val highWater = 3.2
        val range = highWater - lowWater
        val waterAtHour3 = lowWater + (6.0 / 12.0) * range
        assertEquals(2.0, waterAtHour3, 0.01, "Wasserstand zur 3. Tidenstunde sollte 2.0m betragen")
    }

    @Test
    @DisplayName("TC09: Alphabetische Sortierung der Stationen A-Z")
    fun testAlphabeticalStationSorting() {
        val stations = listOf("Borkum", "Juist", "Baltrum", "Norderney", "Emden", "Harlesiel")
        val sorted = stations.sorted()
        assertEquals("Baltrum", sorted[0], "Erster Ort muss Baltrum sein")
        assertEquals("Borkum", sorted[1], "Zweiter Ort muss Borkum sein")
        assertEquals("Emden", sorted[2], "Dritter Ort muss Emden sein")
    }
}
