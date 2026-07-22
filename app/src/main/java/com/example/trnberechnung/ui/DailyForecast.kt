package com.example.trnberechnung.ui

data class DailyForecast(
    val dayLabel: String,
    val condition: String,
    val highTemp: Int,
    val lowTemp: Int,
    val maxWind: Int,
    val totalPrecip: Double,
    val maxPrecipProb: Int,
    val minVisibility: Int?
)
