package com.yourname.trackr

import android.app.Application
import com.yourname.trackr.data.UserPrefs
import com.yourname.trackr.data.local.AppDatabase
import com.yourname.trackr.repository.SessionRepository
import com.yourname.trackr.repository.WeatherRepository
import org.osmdroid.config.Configuration
import java.io.File

class TrackrApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val sessionRepository: SessionRepository by lazy { SessionRepository(database.sessionDao()) }
    val weatherRepository: WeatherRepository by lazy { WeatherRepository() }
    val userPrefs: UserPrefs by lazy { UserPrefs(this) }

    override fun onCreate() {
        super.onCreate()
        configureOsmdroid()
    }

    /**
     * osmdroid requires a one-time global config before any MapView is created: a user agent
     * (tile servers block requests without one) and a cache location. Pointing the cache at the
     * app's private cache dir avoids needing WRITE_EXTERNAL_STORAGE on any API level.
     */
    private fun configureOsmdroid() {
        val config = Configuration.getInstance()
        config.load(this, getSharedPreferences("osmdroid_prefs", MODE_PRIVATE))
        config.userAgentValue = packageName
        val tileDir = File(cacheDir, "osmdroid")
        config.osmdroidBasePath = tileDir
        config.osmdroidTileCache = File(tileDir, "tiles")
    }
}
