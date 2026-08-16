package com.example.xbible.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.xbible.data.BibleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uniffi.xbible_engine.SwordModule

private const val TAG = "LibraryViewModel"

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BibleRepository(application)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _installedModules = MutableStateFlow<List<SwordModule>>(emptyList())
    val installedModules: StateFlow<List<SwordModule>> = _installedModules.asStateFlow()

    private val _organizedModules = MutableStateFlow<Map<String, Map<String, List<SwordModule>>>>(emptyMap())
    val organizedModules: StateFlow<Map<String, Map<String, List<SwordModule>>>> = _organizedModules.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

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

                val modules = withContext(Dispatchers.IO) {
                    repository.getInstalledModules()
                }
                Log.d(TAG, "Loaded ${modules.size} installed modules")
                modules.forEach {
                    Log.d(TAG, "  module: name=${it.name}, category=${it.category}, language=${it.language}")
                }

                _installedModules.value = modules
                filterModules()

                if (modules.isEmpty()) {
                    Log.w(TAG, "No modules returned from repository.getInstalledModules() — check module install path / repository logic")
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
        filterModules()
    }

    private fun filterModules() {
        val query = _searchQuery.value.lowercase()
        val modules = _installedModules.value

        val filtered = if (query.isEmpty()) {
            modules
        } else {
            modules.filter {
                it.name.lowercase().contains(query) ||
                        it.description.lowercase().contains(query) ||
                        it.language.lowercase().contains(query)
            }
        }

        Log.d(TAG, "filterModules(): query='$query' -> ${filtered.size}/${modules.size} modules match")

        // Organize modules by category and language
        val organized = filtered.groupBy { it.category }
            .mapValues { entry ->
                entry.value.groupBy { it.language }
            }

        _organizedModules.value = organized
        _categories.value = organized.keys.sorted()

        Log.d(TAG, "Organized into categories: ${_categories.value}")
    }

    fun deleteModule(moduleName: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.uninstallModule(moduleName)
                }
                Log.d(TAG, "Deleted module: $moduleName")
                refreshLibrary()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete module: $moduleName", e)
                _errorMessage.value = "Failed to delete module: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}