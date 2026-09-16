package com.yourname.trackr.repository

import com.yourname.trackr.data.weather.WeatherApi
import com.yourname.trackr.model.WeatherReading

class WeatherRepository(private val weatherApi: WeatherApi = WeatherApi()) {

    suspend fun fetchWeather(lat: Double, lng: Double): WeatherReading? =
        weatherApi.fetchCurrentWeather(lat, lng)
}
