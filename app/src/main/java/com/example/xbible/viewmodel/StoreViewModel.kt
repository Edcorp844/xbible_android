package com.example.xbible.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.xbible.data.BibleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uniffi.xbible_engine.SwordModule
import uniffi.xbible_engine.TaskState

sealed class InstallationStatus {
    object Idle : InstallationStatus()
    object Pending : InstallationStatus()
    data class Installing(val progress: Float) : InstallationStatus()
    object Installed : InstallationStatus()
    object Cancelled : InstallationStatus()
    data class Failed(val message: String) : InstallationStatus()
}

class StoreViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BibleRepository(application)

    private val _remoteSources = MutableStateFlow<List<String>>(emptyList())
    val remoteSources: StateFlow<List<String>> = _remoteSources.asStateFlow()

    private val _selectedSource = MutableStateFlow<String?>(null)
    val selectedSource: StateFlow<String?> = _selectedSource.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _remoteModules = MutableStateFlow<List<SwordModule>>(emptyList())
    val remoteModules: StateFlow<List<SwordModule>> = _remoteModules.asStateFlow()

    private val _installationStates = MutableStateFlow<Map<String, InstallationStatus>>(emptyMap())
    val installationStates: StateFlow<Map<String, InstallationStatus>> = _installationStates.asStateFlow()

    private val _organizedModules = MutableStateFlow<Map<String, Map<String, List<SwordModule>>>>(emptyMap())
    val organizedModules: StateFlow<Map<String, Map<String, List<SwordModule>>>> = _organizedModules.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    private val activeTaskIds = mutableMapOf<String, String>()

    init {
        loadSources()
    }

    private fun loadSources() {
        viewModelScope.launch {
            try {
                if (!repository.isInitialized()) {
                    repository.initialize()
                }
                val sources = withContext(Dispatchers.IO) { repository.getRemoteSources() }
                _remoteSources.value = sources
                if (sources.isNotEmpty()) {
                    selectSource(sources.first())
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                _errorMessage.value = "Failed to load sources: ${e.message}"
            }
        }
    }

    fun selectSource(source: String) {
        if (_selectedSource.value == source) return
        _selectedSource.value = source
        refreshStore()
    }

    fun refreshStore() {
        val source = _selectedSource.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val modules = withContext(Dispatchers.IO) { repository.fetchRemoteModules(source) }
                _remoteModules.value = modules
                
                // Organize modules by category and language
                val organized = modules.groupBy { it.category }
                    .mapValues { entry ->
                        entry.value.groupBy { it.language }
                    }
                _organizedModules.value = organized
                _categories.value = organized.keys.sorted()
                
                // Update installation states for already installed modules
                val installedModules = withContext(Dispatchers.IO) { repository.refreshModules() }
                val currentStates = _installationStates.value.toMutableMap()
                installedModules.forEach { module ->
                    currentStates[module.name] = InstallationStatus.Installed
                }
                _installationStates.value = currentStates
                
            } catch (e: Throwable) {
                e.printStackTrace()
                _errorMessage.value = "Network error: ${e.message}. Ensure your device has internet access."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun install(module: SwordModule) {
        val moduleName = module.name
        val source = module.source
        
        viewModelScope.launch {
            _installationStates.value += (moduleName to InstallationStatus.Pending)
            
            val taskId = withContext(Dispatchers.IO) {
                repository.installModuleAsync(source, moduleName)
            }
            
            if (taskId.isEmpty()) {
                _installationStates.value += (moduleName to InstallationStatus.Failed("Task failed to start"))
                return@launch
            }
            
            activeTaskIds[moduleName] = taskId
            
            // Monitor progress
            monitorInstallation(moduleName, taskId)
        }
    }

    private suspend fun monitorInstallation(moduleName: String, taskId: String) {
        while (true) {
            val status = withContext(Dispatchers.IO) { repository.getTaskStatus(taskId) }
            if (status == null) {
                _installationStates.value += (moduleName to InstallationStatus.Failed("Status not found"))
                activeTaskIds.remove(moduleName)
                break
            }
            
            when (val state = status.state) {
                is TaskState.Running, is TaskState.Queued -> {
                    _installationStates.value += (moduleName to InstallationStatus.Installing(status.progress.toFloat()))
                }
                is TaskState.Completed -> {
                    _installationStates.value += (moduleName to InstallationStatus.Installed)
                    activeTaskIds.remove(moduleName)
                    break
                }
                is TaskState.Failed -> {
                    _installationStates.value += (moduleName to InstallationStatus.Failed(state.error))
                    activeTaskIds.remove(moduleName)
                    break
                }
                // Check if Cancelled exists - based on Swift code it might, but not in my grep. 
                // Let's re-check TaskState
                else -> {
                    // Fallback for unknown states or potential Cancellation
                    _installationStates.value += (moduleName to InstallationStatus.Cancelled)
                    activeTaskIds.remove(moduleName)
                    break
                }
            }
            delay(100)
        }
    }

    fun cancelInstall(moduleName: String) {
        activeTaskIds[moduleName]?.let { taskId ->
            viewModelScope.launch(Dispatchers.IO) {
                repository.cancelTask(taskId)
            }
        }
    }
}
