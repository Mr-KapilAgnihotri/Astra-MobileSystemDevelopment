package com.yourname.trackr.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yourname.trackr.model.toFloatSamples
import com.yourname.trackr.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AccelGraphViewModel(
    private val repository: SessionRepository,
    private val sessionId: Long
) : ViewModel() {

    private val _samples = MutableStateFlow<List<Float>>(emptyList())
    val samples: StateFlow<List<Float>> = _samples.asStateFlow()

    val peakValue: Float get() = _samples.value.maxOrNull() ?: 0f
    val averageValue: Float get() = _samples.value.let { if (it.isEmpty()) 0f else it.average().toFloat() }

    init {
        viewModelScope.launch {
            val session = repository.getSessionById(sessionId)
            _samples.value = session?.accelSamplesJson?.toFloatSamples() ?: emptyList()
        }
    }

    class Factory(
        private val repository: SessionRepository,
        private val sessionId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AccelGraphViewModel::class.java)) {
                return AccelGraphViewModel(repository, sessionId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
        }
    }
}
