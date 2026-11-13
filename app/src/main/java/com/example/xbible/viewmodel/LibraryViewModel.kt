package com.example.xbible.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.xbible.data.BibleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uniffi.xbible_engine.InstalledTranslation
import javax.inject.Inject

private const val TAG = "LibraryViewModel"

@HiltViewModel
class LibraryViewModel @Inject constructor(
    application: Application,
    private val repository: BibleRepository
) : AndroidViewModel(application) {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** All installed FreeBible translations. */
    private val _installedTranslations = MutableStateFlow<List<InstalledTranslation>>(emptyList())
    val installedTranslations: StateFlow<List<InstalledTranslation>> =
        _installedTranslations.asStateFlow()

    /** Emits when a translation is deleted so other VMs (like Reader) can react. */
    private val _deletedTranslationId = MutableSharedFlow<String>()
    val deletedTranslationId: SharedFlow<String> = _deletedTranslationId.asSharedFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var isRefreshing = false

    init {
        refreshLibrary()
    }

    /**
     * Fetches the list of installed translations from the FreeBible engine.
     */
    fun refreshLibrary() {
        if (isRefreshing) {
            Log.d(TAG, "refreshLibrary() called while already refreshing — skipping")
            return
        }

        viewModelScope.launch {
            isRefreshing = true
            _isLoading.value = true
            _errorMessage.value = null
            Log.d(TAG, "refreshLibrary() started")

            try {
                if (!repository.isInitialized()) {
                    Log.d(TAG, "Initializing repository...")
                    repository.initialize().onFailure { e ->
                        Log.e(TAG, "Repository initialization failed", e)
                        throw e
                    }
                }

                val translations = withContext(Dispatchers.IO) {
                    repository.getFreeBibleInstalledTranslations()
                }
                Log.d(TAG, "Loaded ${translations.size} installed translation(s)")

                _installedTranslations.value = translations
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to load library", e)
                _errorMessage.value = "Failed to load library: ${e.message}"
            } finally {
                _isLoading.value = false
                isRefreshing = false
                Log.d(TAG, "refreshLibrary() finished")
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Deletes a translation from the database and refreshes the list.
     */
    fun deleteTranslation(translationId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                withContext(Dispatchers.IO) {
                    repository.uninstallFreeBibleTranslation(translationId)
                }
                Log.d(TAG, "Uninstalled translation: $translationId")

                // Notify listeners (e.g. ReaderViewModel) that this was deleted
                _deletedTranslationId.emit(translationId)

                // Refresh the list immediately to reflect the removal
                refreshLibrary()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to uninstall translation: $translationId", e)
                _errorMessage.value = "Failed to uninstall translation: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}