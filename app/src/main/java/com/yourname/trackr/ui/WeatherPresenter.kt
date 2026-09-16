package com.yourname.trackr.ui

import androidx.annotation.DrawableRes
import com.yourname.trackr.R

data class WeatherPresentation(val label: String, @DrawableRes val iconRes: Int)

/**
 * Maps a small, common subset of Open-Meteo's WMO weather codes to a label and icon.
 * Not exhaustive by design - codes outside this list fall back to "Cloudy" rather than
 * needing every one of the ~30 WMO codes handled.
 */
object WeatherPresenter {

    fun present(weatherCode: Int): WeatherPresentation = when (weatherCode) {
        0 -> WeatherPresentation("Clear", R.drawable.ic_sunny)
        1, 2, 3 -> WeatherPresentation("Cloudy", R.drawable.ic_cloud)
        45, 48 -> WeatherPresentation("Fog", R.drawable.ic_cloud)
        51, 53, 55, 56, 57,
        61, 63, 65, 66, 67,
        80, 81, 82 -> WeatherPresentation("Rain", R.drawable.ic_rain)
        71, 73, 75, 77, 85, 86 -> WeatherPresentation("Snow", R.drawable.ic_snow)
        95, 96, 99 -> WeatherPresentation("Storm", R.drawable.ic_rain)
        else -> WeatherPresentation("Cloudy", R.drawable.ic_cloud)
    }
}
