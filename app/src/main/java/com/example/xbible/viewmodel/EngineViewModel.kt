package com.example.xbible.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.xbible.data.BibleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uniffi.xbible_engine.Section

class EngineViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: BibleRepository = BibleRepository(application)
) : AndroidViewModel(application) {

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _currentChapterContent = MutableStateFlow<List<Section>>(emptyList())
    val currentChapterContent: StateFlow<List<Section>> = _currentChapterContent.asStateFlow()

    init {
        setupEngine()
    }

    private fun setupEngine() {
        viewModelScope.launch {
            val result = repository.initialize()
            result.onSuccess {
                _isReady.value = true
            }.onFailure { e ->
                _errorMessage.value = "Failed to initialize Bible engine: ${e.message}"
            }
        }
    }

    fun refreshEngine() {
        if (repository.isInitialized()) {
            repository.refreshModules()
        }
    }
}
