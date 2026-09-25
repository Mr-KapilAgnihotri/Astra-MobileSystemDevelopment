package com.yourname.trackr.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yourname.trackr.data.local.SessionEntity
import com.yourname.trackr.data.sensors.CompassSensor
import com.yourname.trackr.model.DayDistance
import com.yourname.trackr.model.buildLastSevenDayDistances
import com.yourname.trackr.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

class HomeViewModel(
    private val repository: SessionRepository,
    private val compassSensor: CompassSensor
) : ViewModel() {

    val sessions: StateFlow<List<SessionEntity>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Same underlying data as WeeklyStatsActivity, recomputed automatically whenever
     *  `sessions` changes - so a just-saved session updates this without a manual reload. */
    val weeklyDistances: StateFlow<List<DayDistance>> = sessions
        .map { buildLastSevenDayDistances(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Plain-text fallback for the weekly card if the chart view can't be shown. */
    val weeklySummaryText: StateFlow<String> = sessions
        .map { summarizeLastSevenDays(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private fun summarizeLastSevenDays(sessionList: List<SessionEntity>): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStart = calendar.timeInMillis
        val windowStart = todayStart - 6 * DAY_MILLIS
        val windowEnd = todayStart + DAY_MILLIS
        val weekSessions = sessionList.filter { it.startTime in windowStart until windowEnd }
        val totalKm = weekSessions.sumOf { it.distanceMeters.toDouble() }.toFloat() / 1000f
        val sessionWord = if (weekSessions.size == 1) "session" else "sessions"
        return "This week: %.1f km across %d %s".format(totalKm, weekSessions.size, sessionWord)
    }

    private val _heading = MutableStateFlow(0f)
    val heading: StateFlow<Float> = _heading.asStateFlow()

    /** Called from Activity.onResume() - no permission needed for the rotation sensor. */
    fun startCompass() {
        compassSensor.start { heading -> _heading.value = heading }
    }

    /** Called from Activity.onPause(). */
    fun stopCompass() {
        compassSensor.stop()
    }

    override fun onCleared() {
        super.onCleared()
        compassSensor.stop()
    }

    class Factory(
        private val repository: SessionRepository,
        private val compassSensor: CompassSensor
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                return HomeViewModel(repository, compassSensor) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
        }
    }

    companion object {
        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}
