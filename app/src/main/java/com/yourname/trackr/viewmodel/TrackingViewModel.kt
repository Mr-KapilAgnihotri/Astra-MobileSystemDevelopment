package com.yourname.trackr.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yourname.trackr.data.local.SessionEntity
import com.yourname.trackr.data.sensors.LocationTracker
import com.yourname.trackr.data.sensors.MilestoneChime
import com.yourname.trackr.data.sensors.StepSensor
import com.yourname.trackr.model.CalorieCalculator
import com.yourname.trackr.model.TrackPoint
import com.yourname.trackr.model.WeatherReading
import com.yourname.trackr.model.toAccelJson
import com.yourname.trackr.model.toPathJson
import com.yourname.trackr.repository.SessionRepository
import com.yourname.trackr.repository.WeatherRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Owns the tracking session lifecycle: registers the step/location sensors
 * when tracking starts, unregisters them on stop or when the ViewModel is
 * cleared, and accumulates the live stats (elapsed time, steps, distance,
 * calories, path, weather, raw accelerometer samples) that TrackingActivity
 * renders. Also plays a short chime every MILESTONE_METERS of distance.
 */
class TrackingViewModel(
    private val repository: SessionRepository,
    private val weatherRepository: WeatherRepository,
    private val stepSensor: StepSensor,
    private val locationTracker: LocationTracker,
    private val milestoneChime: MilestoneChime,
    private val activityType: String,
    private val weightKg: Float
) : ViewModel() {

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private val _stepCount = MutableStateFlow(0)
    val stepCount: StateFlow<Int> = _stepCount.asStateFlow()

    private val _distanceMeters = MutableStateFlow(0f)
    val distanceMeters: StateFlow<Float> = _distanceMeters.asStateFlow()

    private val _caloriesEstimate = MutableStateFlow(0f)
    val caloriesEstimate: StateFlow<Float> = _caloriesEstimate.asStateFlow()

    private val _pathPoints = MutableStateFlow<List<TrackPoint>>(emptyList())
    val pathPoints: StateFlow<List<TrackPoint>> = _pathPoints.asStateFlow()

    private val _weather = MutableStateFlow<WeatherReading?>(null)
    val weather: StateFlow<WeatherReading?> = _weather.asStateFlow()

    var hasLocationPermission: Boolean = false
        private set

    private var startTime: Long = 0L
    private var timerJob: Job? = null
    private var weatherFetchStarted = false
    private val distanceResult = FloatArray(1)

    private val accelSamples = mutableListOf<Float>()
    private var lastMilestoneCount: Int = 0

    fun onLocationPermissionResult(granted: Boolean) {
        hasLocationPermission = granted
    }

    /** OS-level Location Services toggle, independent of whether we hold the permission. */
    fun isDeviceLocationEnabled(): Boolean = locationTracker.isLocationEnabled()

    fun startTracking() {
        if (_isTracking.value) return
        _isTracking.value = true
        startTime = System.currentTimeMillis()
        _elapsedSeconds.value = 0
        _stepCount.value = 0
        _distanceMeters.value = 0f
        _caloriesEstimate.value = 0f
        _pathPoints.value = emptyList()
        _weather.value = null
        weatherFetchStarted = false
        accelSamples.clear()
        lastMilestoneCount = 0

        stepSensor.start(
            onStepDetected = { _stepCount.value += 1 },
            onSample = { magnitude -> accelSamples.add(magnitude) }
        )
        if (hasLocationPermission) {
            locationTracker.start { lat, lng -> onNewLocation(lat, lng) }
        }

        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val elapsedMs = System.currentTimeMillis() - startTime
                _elapsedSeconds.value = elapsedMs / 1000
                _caloriesEstimate.value = CalorieCalculator.estimateCalories(activityType, weightKg, elapsedMs)
            }
        }
    }

    private fun onNewLocation(lat: Double, lng: Double) {
        val previous = _pathPoints.value.lastOrNull()
        if (previous != null) {
            Location.distanceBetween(previous.lat, previous.lng, lat, lng, distanceResult)
            _distanceMeters.value += distanceResult[0]
            checkMilestone()
        }
        _pathPoints.value = _pathPoints.value + TrackPoint(lat, lng, System.currentTimeMillis())

        if (!weatherFetchStarted) {
            weatherFetchStarted = true
            viewModelScope.launch {
                _weather.value = weatherRepository.fetchWeather(lat, lng)
            }
        }
    }

    private fun checkMilestone() {
        val currentMilestone = (_distanceMeters.value / MILESTONE_METERS).toInt()
        if (currentMilestone > lastMilestoneCount) {
            lastMilestoneCount = currentMilestone
            milestoneChime.play()
        }
    }

    /** Stops sensors, persists the session to Room, and hands the new session's id back. */
    fun stopTracking(onSaved: (sessionId: Long) -> Unit) {
        if (!_isTracking.value) return
        _isTracking.value = false
        stopSensors()

        val weatherReading = _weather.value
        val session = SessionEntity(
            type = activityType,
            startTime = startTime,
            endTime = System.currentTimeMillis(),
            stepCount = _stepCount.value,
            distanceMeters = _distanceMeters.value,
            pathJson = _pathPoints.value.toPathJson(),
            weatherTempC = weatherReading?.tempC,
            weatherWeatherCode = weatherReading?.weatherCode,
            accelSamplesJson = accelSamples.toAccelJson()
        )

        viewModelScope.launch {
            val id = repository.insert(session)
            onSaved(id)
        }
    }

    private fun stopSensors() {
        stepSensor.stop()
        locationTracker.stop()
        timerJob?.cancel()
        timerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopSensors()
        milestoneChime.release()
    }

    class Factory(
        private val repository: SessionRepository,
        private val weatherRepository: WeatherRepository,
        private val stepSensor: StepSensor,
        private val locationTracker: LocationTracker,
        private val milestoneChime: MilestoneChime,
        private val activityType: String,
        private val weightKg: Float
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TrackingViewModel::class.java)) {
                return TrackingViewModel(
                    repository, weatherRepository, stepSensor, locationTracker,
                    milestoneChime, activityType, weightKg
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
        }
    }

    companion object {
        /** Easy to change to 1000f for 1km milestones later. */
        private const val MILESTONE_METERS = 500f
    }
}
