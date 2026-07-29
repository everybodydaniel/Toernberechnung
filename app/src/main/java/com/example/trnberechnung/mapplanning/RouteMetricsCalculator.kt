package com.example.trnberechnung.mapplanning

import java.time.Duration
import java.time.ZonedDateTime
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt

object RouteMetricsCalculator {
    private const val EARTH_RADIUS_NM = 3_440.065
    private const val DIESEL_LITERS_PER_NM = 0.35

    fun calculate(
        routeGeometry: List<GeoPoint>,
        departure: ZonedDateTime,
        speedKnots: Double,
        worstClearanceMeters: Double? = null,
    ): RouteMetrics? {
        if (routeGeometry.size < 2 || speedKnots <= 0) return null

        val distanceNm =
            routeGeometry.zipWithNext().sumOf { (start, end) ->
                haversineNm(start, end)
            }
        return fromDistance(
            distanceNm = distanceNm,
            departure = departure,
            speedKnots = speedKnots,
            worstClearanceMeters = worstClearanceMeters,
        )
    }

    fun fromDistance(
        distanceNm: Double,
        departure: ZonedDateTime,
        speedKnots: Double,
        worstClearanceMeters: Double? = null,
    ): RouteMetrics {
        require(distanceNm >= 0) { "Die Distanz darf nicht negativ sein." }
        require(speedKnots > 0) { "Die Fahrtgeschwindigkeit muss größer als 0 sein." }

        val travelSeconds = (distanceNm / speedKnots * 3_600).roundToLong()
        val travelTime = Duration.ofSeconds(travelSeconds)
        val berlinDeparture = departure.withZoneSameInstant(MAP_PLANNING_ZONE_ID)
        return RouteMetrics(
            distanceNm = distanceNm,
            travelTime = travelTime,
            arrival = berlinDeparture.plus(travelTime),
            worstUnderKeelClearanceMeters = worstClearanceMeters,
            dieselLiters = distanceNm * DIESEL_LITERS_PER_NM,
        )
    }

    fun haversineNm(start: GeoPoint, end: GeoPoint): Double {
        val lat1 = Math.toRadians(start.latitude)
        val lat2 = Math.toRadians(end.latitude)
        val deltaLat = Math.toRadians(end.latitude - start.latitude)
        val deltaLon = Math.toRadians(end.longitude - start.longitude)
        val haversine =
            sin(deltaLat / 2).let { it * it } +
                cos(lat1) * cos(lat2) * sin(deltaLon / 2).let { it * it }
        return EARTH_RADIUS_NM * 2 * asin(sqrt(haversine.coerceIn(0.0, 1.0)))
    }
}
