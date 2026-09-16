package com.yourname.trackr.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yourname.trackr.repository.SessionRepository
import kotlinx.coroutines.launch

class PhotoViewModel(
    private val repository: SessionRepository,
    private val sessionId: Long
) : ViewModel() {

    fun savePhotoUri(photoUri: String?) {
        viewModelScope.launch {
            repository.updatePhoto(sessionId, photoUri)
        }
    }

    class Factory(
        private val repository: SessionRepository,
        private val sessionId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PhotoViewModel::class.java)) {
                return PhotoViewModel(repository, sessionId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
        }
    }
}
