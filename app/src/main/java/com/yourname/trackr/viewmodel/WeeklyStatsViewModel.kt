package com.yourname.trackr.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yourname.trackr.data.local.SessionEntity
import com.yourname.trackr.model.DayDistance
import com.yourname.trackr.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WeeklyStatsViewModel(private val repository: SessionRepository) : ViewModel() {

    private val _weeklyDistances = MutableStateFlow<List<DayDistance>>(emptyList())
    val weeklyDistances: StateFlow<List<DayDistance>> = _weeklyDistances.asStateFlow()

    init {
        viewModelScope.launch {
            val sessions = repository.getAllSessionsOnce()
            _weeklyDistances.value = buildLastSevenDays(sessions)
        }
    }

    private fun buildLastSevenDays(sessions: List<SessionEntity>): List<DayDistance> {
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val calendar = Calendar.getInstance()
        // Start of "today" (midnight) so day boundaries are calendar days, not rolling 24h windows.
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayStart = calendar.timeInMillis

        return (6 downTo 0).map { daysAgo ->
            val dayStart = todayStart - daysAgo * DAY_MILLIS
            val dayEnd = dayStart + DAY_MILLIS
            val kmForDay = sessions
                .filter { it.startTime in dayStart until dayEnd }
                .sumOf { it.distanceMeters.toDouble() }
                .toFloat() / 1000f
            DayDistance(dayFormat.format(Date(dayStart)), kmForDay)
        }
    }

    class Factory(private val repository: SessionRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(WeeklyStatsViewModel::class.java)) {
                return WeeklyStatsViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
        }
    }

    companion object {
        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}
