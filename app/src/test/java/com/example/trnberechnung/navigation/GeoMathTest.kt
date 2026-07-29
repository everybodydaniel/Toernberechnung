package com.example.trnberechnung.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoMathTest {
    @Test
    fun `distance uses great circle metres`() {
        val distance =
            GeoMath.distanceMeters(
                from = GeoPoint(0.0, 0.0),
                to = GeoPoint(1.0, 0.0),
            )

        assertEquals(111_195.0, distance, 100.0)
        assertEquals(60.04, distance / METERS_PER_NAUTICAL_MILE, 0.1)
    }

    @Test
    fun `bearing is normalized to true compass degrees`() {
        assertEquals(
            90.0,
            GeoMath.bearingDegrees(
                from = GeoPoint(53.0, 7.0),
                to = GeoPoint(53.0, 8.0),
            ),
            0.5,
        )
        assertTrue(
            GeoMath.bearingDegrees(
                from = GeoPoint(53.0, 7.0),
                to = GeoPoint(53.0, 6.0),
            ) in 269.0..271.0,
        )
    }

    @Test
    fun `perpendicular distance clamps projection to segment`() {
        val distanceBesideSegment =
            GeoMath.perpendicularDistanceMeters(
                point = GeoPoint(0.001, 0.005),
                segmentStart = GeoPoint(0.0, 0.0),
                segmentEnd = GeoPoint(0.0, 0.01),
            )
        val distancePastSegment =
            GeoMath.perpendicularDistanceMeters(
                point = GeoPoint(0.0, 0.02),
                segmentStart = GeoPoint(0.0, 0.0),
                segmentEnd = GeoPoint(0.0, 0.01),
            )

        assertEquals(111.32, distanceBesideSegment, 0.5)
        assertEquals(1_111.95, distancePastSegment, 2.0)
    }
}
