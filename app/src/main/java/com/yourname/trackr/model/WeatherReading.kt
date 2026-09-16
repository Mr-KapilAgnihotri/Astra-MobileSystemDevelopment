package com.yourname.trackr.model

/** Raw current-conditions reading from the weather API: temperature plus an Open-Meteo WMO weather code. */
data class WeatherReading(
    val tempC: Float,
    val weatherCode: Int
)
