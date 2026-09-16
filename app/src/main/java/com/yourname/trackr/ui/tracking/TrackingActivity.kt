package com.yourname.trackr.ui.tracking

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.yourname.trackr.R
import com.yourname.trackr.TrackrApplication
import com.yourname.trackr.data.sensors.LocationTracker
import com.yourname.trackr.data.sensors.MilestoneChime
import com.yourname.trackr.data.sensors.StepSensor
import com.yourname.trackr.databinding.ActivityTrackingBinding
import com.yourname.trackr.ui.WeatherPresenter
import com.yourname.trackr.ui.fadeIn
import com.yourname.trackr.ui.photo.PhotoCaptureActivity
import com.yourname.trackr.ui.setUpBasicMap
import com.yourname.trackr.ui.updateLiveRoute
import com.yourname.trackr.viewmodel.TrackingViewModel
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import java.util.concurrent.TimeUnit

/**
 * Live tracking screen. Start registers the step + location sensors through
 * TrackingViewModel; Stop unregisters them, persists the finished session to
 * Room, and hands the saved SessionEntity back to HomeActivity.
 */
class TrackingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTrackingBinding
    private var activityType: String = "Run"
    private val routeColor: Int by lazy { ContextCompat.getColor(this, R.color.trackr_accent) }

    private val viewModel: TrackingViewModel by lazy {
        val app = application as TrackrApplication
        ViewModelProvider(
            this,
            TrackingViewModel.Factory(
                app.sessionRepository,
                app.weatherRepository,
                StepSensor(applicationContext),
                LocationTracker(applicationContext),
                MilestoneChime(),
                activityType,
                app.userPrefs.weightKg
            )
        )[TrackingViewModel::class.java]
    }

    private val requestLocationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onLocationPermissionResult(granted)
        refreshTrackingAvailability()
        if (!granted) {
            Toast.makeText(this, R.string.location_permission_denied, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityType = intent.getStringExtra(EXTRA_ACTIVITY_TYPE) ?: "Run"

        binding = ActivityTrackingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.mapView.setUpBasicMap()

        binding.textActivityType.text = activityType
        binding.statElapsed.textStatLabel.text = getString(R.string.stat_label_time)
        binding.statSteps.textStatLabel.text = getString(R.string.stat_label_steps)
        binding.statDistance.textStatLabel.text = getString(R.string.stat_label_distance)
        binding.statCalories.textStatLabel.text = getString(R.string.stat_label_calories)
        binding.statsGrid.fadeIn()

        requestLocationPermissionIfNeeded()

        binding.buttonStart.setOnClickListener { viewModel.startTracking() }
        binding.buttonStop.setOnClickListener { stopAndReturn() }

        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
        // The user may have granted the permission or flipped Location Services on/off in
        // Settings and come back here, so re-check rather than trusting stale state.
        refreshTrackingAvailability()
    }

    override fun onPause() {
        binding.mapView.onPause()
        super.onPause()
    }

    private fun requestLocationPermissionIfNeeded() {
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        viewModel.onLocationPermissionResult(granted)
        if (!granted) {
            requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        refreshTrackingAvailability()
    }

    /** Single source of truth for whether Start can be pressed and what the warning banner says. */
    private fun refreshTrackingAvailability() {
        val permissionGranted = viewModel.hasLocationPermission
        val servicesEnabled = viewModel.isDeviceLocationEnabled()
        val tracking = viewModel.isTracking.value

        binding.buttonStart.isEnabled = permissionGranted && servicesEnabled && !tracking
        binding.buttonStop.isEnabled = tracking

        val warning = when {
            !permissionGranted -> getString(R.string.location_permission_required)
            !servicesEnabled -> getString(R.string.location_services_disabled)
            else -> null
        }
        binding.textPermissionWarning.text = warning ?: ""
        binding.textPermissionWarning.visibility = if (warning != null && !tracking) View.VISIBLE else View.GONE
    }

    private fun stopAndReturn() {
        viewModel.stopTracking { sessionId ->
            val intent = Intent(this, PhotoCaptureActivity::class.java).apply {
                putExtra(PhotoCaptureActivity.EXTRA_SESSION_ID, sessionId)
            }
            startActivity(intent)
            finish()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.isTracking.collect { refreshTrackingAvailability() }
        }
        lifecycleScope.launch {
            viewModel.elapsedSeconds.collect { seconds ->
                binding.statElapsed.textStatValue.text = formatElapsed(seconds)
            }
        }
        lifecycleScope.launch {
            viewModel.stepCount.collect { steps ->
                binding.statSteps.textStatValue.text = steps.toString()
            }
        }
        lifecycleScope.launch {
            viewModel.distanceMeters.collect { meters ->
                binding.statDistance.textStatValue.text = getString(R.string.distance_km_format, meters / 1000f)
            }
        }
        lifecycleScope.launch {
            viewModel.caloriesEstimate.collect { calories ->
                binding.statCalories.textStatValue.text = getString(R.string.calories_format, calories)
            }
        }
        lifecycleScope.launch {
            viewModel.pathPoints.collect { points ->
                val isTracking = viewModel.isTracking.value
                binding.textRouteEmpty.visibility = if (isTracking && points.isEmpty()) View.VISIBLE else View.GONE
                if (points.isNotEmpty()) {
                    binding.mapView.updateLiveRoute(points.map { GeoPoint(it.lat, it.lng) }, routeColor)
                }
            }
        }
        lifecycleScope.launch {
            viewModel.weather.collect { weather ->
                if (weather != null) {
                    val presentation = WeatherPresenter.present(weather.weatherCode)
                    binding.weatherCard.textWeatherTemp.text = getString(R.string.temp_c_format, weather.tempC)
                    binding.weatherCard.textWeatherCondition.text = presentation.label
                    binding.weatherCard.imageWeatherIcon.setImageResource(presentation.iconRes)
                    if (binding.weatherCard.root.visibility != View.VISIBLE) {
                        binding.weatherCard.root.fadeIn()
                    }
                }
            }
        }
    }

    private fun formatElapsed(totalSeconds: Long): String {
        val hours = TimeUnit.SECONDS.toHours(totalSeconds)
        val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    companion object {
        const val EXTRA_ACTIVITY_TYPE = "activity_type"
    }
}
