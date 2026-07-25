package com.example.trnberechnung.model

data class TideStationData(
    val area: String,
    val region: String,
    val latitude: Double,
    val longitude: Double,
    val waterLevel: Double?,
    val meanHighWater: Double?,
    val meanLowWater: Double?,
    val gaugeLabel: String? = null,
    val gaugeZeroNhn: Double? = null,
    val chartDatumGauge: Double? = null,
    val forecastTimestamp: String,
    val temperature: Double? = null,
    val windSpeed: Double? = null,
    val windGustSpeed: Double? = null,
    val windDirection: Int? = null,
    val events: List<TideEvent>
)
