package com.example.xbible.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.xbible.data.BibleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    /**
     * Translations grouped by language code → language name → list.
     * Structure: Map<languageCode, Map<languageName, List<InstalledTranslation>>>
     */
    private val _organizedTranslations =
        MutableStateFlow<Map<String, Map<String, List<InstalledTranslation>>>>(emptyMap())
    val organizedTranslations: StateFlow<Map<String, Map<String, List<InstalledTranslation>>>> =
        _organizedTranslations.asStateFlow()

    /** Language codes present in the installed set, sorted. */
    private val _languages = MutableStateFlow<List<String>>(emptyList())
    val languages: StateFlow<List<String>> = _languages.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var isRefreshing = false

    init {
        refreshLibrary()
    }

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
                translations.forEach {
                    Log.d(
                        TAG,
                        "  translation: id=${it.id}, name=${it.name}, " +
                                "lang=${it.language}, books=${it.bookCount}, " +
                                "chapters=${it.chapterCount}, verses=${it.verseCount}",
                    )
                }

                _installedTranslations.value = translations
                filterTranslations()

                if (translations.isEmpty()) {
                    Log.w(
                        TAG,
                        "No installed translations returned — check FreeBible install path / DB contents",
                    )
                }
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
        filterTranslations()
    }

    private fun filterTranslations() {
        val query = _searchQuery.value.lowercase()
        val translations = _installedTranslations.value

        val filtered = if (query.isEmpty()) {
            translations
        } else {
            translations.filter {
                it.name.lowercase().contains(query) ||
                        it.englishName.lowercase().contains(query) ||
                        it.shortName.lowercase().contains(query) ||
                        (it.languageName?.lowercase()?.contains(query) == true) ||
                        it.language.lowercase().contains(query)
            }
        }

        Log.d(
            TAG,
            "filterTranslations(): query='$query' -> ${filtered.size}/${translations.size} match",
        )

        // Organize: languageCode -> languageName -> list
        val organized = filtered
            .groupBy { it.language }
            .mapValues { (_, group) ->
                group.groupBy { it.languageName ?: it.language }
            }

        _organizedTranslations.value = organized
        _languages.value = organized.keys.sorted()

        Log.d(TAG, "Organized into languages: ${_languages.value}")
    }

    fun deleteTranslation(translationId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.uninstallFreeBibleTranslation(translationId)
                }
                Log.d(TAG, "Uninstalled translation: $translationId")
                refreshLibrary()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to uninstall translation: $translationId", e)
                _errorMessage.value = "Failed to uninstall translation: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}