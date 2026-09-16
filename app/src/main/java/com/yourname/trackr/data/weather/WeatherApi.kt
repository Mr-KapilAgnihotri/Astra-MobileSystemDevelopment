package com.yourname.trackr.data.weather

import android.util.Log
import com.yourname.trackr.model.WeatherReading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal Open-Meteo client: no API key required, a single current-conditions GET request,
 * parsed by hand with org.json (consistent with how the session path is already serialized)
 * rather than pulling in Retrofit/Gson for one endpoint. Any failure - no network, bad
 * response, timeout - is swallowed and reported as null; weather is a nice-to-have.
 */
class WeatherApi {

    suspend fun fetchCurrentWeather(lat: Double, lng: Double): WeatherReading? =
        withContext(Dispatchers.IO) {
            try {
                val url = URL(
                    "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lng" +
                        "&current=temperature_2m,weather_code&timezone=auto"
                )
                Log.d(TAG, "Fetching weather for $lat, $lng")
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = TIMEOUT_MS
                connection.readTimeout = TIMEOUT_MS
                connection.requestMethod = "GET"

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    Log.w(TAG, "Weather request failed: HTTP ${connection.responseCode}")
                    connection.disconnect()
                    return@withContext null
                }

                val body = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                val current = JSONObject(body).getJSONObject("current")
                WeatherReading(
                    tempC = current.getDouble("temperature_2m").toFloat(),
                    weatherCode = current.getInt("weather_code")
                ).also { Log.d(TAG, "Weather fetched: $it") }
            } catch (e: Exception) {
                // Weather is a nice-to-have (see class doc) - any failure here just means the
                // card stays hidden, but log it so it's debuggable via Logcat.
                Log.w(TAG, "Weather fetch failed", e)
                null
            }
        }

    companion object {
        private const val TAG = "WeatherApi"
        private const val TIMEOUT_MS = 8000
    }
}
