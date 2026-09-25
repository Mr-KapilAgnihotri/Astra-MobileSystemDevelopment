package com.yourname.trackr.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yourname.trackr.model.DayDistance
import com.yourname.trackr.model.buildLastSevenDayDistances
import com.yourname.trackr.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WeeklyStatsViewModel(private val repository: SessionRepository) : ViewModel() {

    private val _weeklyDistances = MutableStateFlow<List<DayDistance>>(emptyList())
    val weeklyDistances: StateFlow<List<DayDistance>> = _weeklyDistances.asStateFlow()

    init {
        viewModelScope.launch {
            val sessions = repository.getAllSessionsOnce()
            _weeklyDistances.value = buildLastSevenDayDistances(sessions)
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
}
