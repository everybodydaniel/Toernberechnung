package com.example.trnberechnung.navigation

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

object GeoMath {
    private const val EARTH_RADIUS_METERS = 6_371_008.8
    private const val METERS_PER_DEGREE_LATITUDE = 111_320.0

    fun distanceMeters(
        from: GeoPoint,
        to: GeoPoint,
    ): Double {
        val latitude1 = from.latitude.toRadians()
        val latitude2 = to.latitude.toRadians()
        val latitudeDelta = (to.latitude - from.latitude).toRadians()
        val longitudeDelta = (to.longitude - from.longitude).toRadians()
        val a =
            sin(latitudeDelta / 2.0).let { it * it } +
                cos(latitude1) * cos(latitude2) *
                sin(longitudeDelta / 2.0).let { it * it }
        val normalizedA = a.coerceIn(0.0, 1.0)
        return 2.0 * EARTH_RADIUS_METERS *
            atan2(sqrt(normalizedA), sqrt(1.0 - normalizedA))
    }

    fun bearingDegrees(
        from: GeoPoint,
        to: GeoPoint,
    ): Double {
        val latitude1 = from.latitude.toRadians()
        val latitude2 = to.latitude.toRadians()
        val longitudeDelta = (to.longitude - from.longitude).toRadians()
        val y = sin(longitudeDelta) * cos(latitude2)
        val x =
            cos(latitude1) * sin(latitude2) -
                sin(latitude1) * cos(latitude2) * cos(longitudeDelta)
        return normalizeDegrees(atan2(y, x) * 180.0 / PI)
    }

    fun perpendicularDistanceMeters(
        point: GeoPoint,
        segmentStart: GeoPoint,
        segmentEnd: GeoPoint,
    ): Double {
        val middleLatitude = (segmentStart.latitude + segmentEnd.latitude) / 2.0
        val metersPerDegreeLongitude =
            METERS_PER_DEGREE_LATITUDE * cos(middleLatitude.toRadians())
        val startX = segmentStart.longitude * metersPerDegreeLongitude
        val startY = segmentStart.latitude * METERS_PER_DEGREE_LATITUDE
        val endX = segmentEnd.longitude * metersPerDegreeLongitude
        val endY = segmentEnd.latitude * METERS_PER_DEGREE_LATITUDE
        val pointX = point.longitude * metersPerDegreeLongitude
        val pointY = point.latitude * METERS_PER_DEGREE_LATITUDE

        val deltaX = endX - startX
        val deltaY = endY - startY
        val lengthSquared = deltaX * deltaX + deltaY * deltaY
        if (lengthSquared <= 0.0) {
            return sqrt(
                (pointX - startX) * (pointX - startX) +
                    (pointY - startY) * (pointY - startY),
            )
        }

        val projection =
            (
                (pointX - startX) * deltaX +
                    (pointY - startY) * deltaY
            ) / lengthSquared
        val clampedProjection = max(0.0, min(1.0, projection))
        val closestX = startX + clampedProjection * deltaX
        val closestY = startY + clampedProjection * deltaY
        return sqrt(
            (pointX - closestX) * (pointX - closestX) +
                (pointY - closestY) * (pointY - closestY),
        )
    }

    fun routeDistanceMeters(points: List<GeoPoint>): Double =
        points.zipWithNext().sumOf { (start, end) -> distanceMeters(start, end) }

    fun normalizeDegrees(degrees: Double): Double =
        ((degrees % 360.0) + 360.0) % 360.0

    private fun Double.toRadians(): Double = this * PI / 180.0
}
