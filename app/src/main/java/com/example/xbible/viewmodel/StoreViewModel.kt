package com.example.xbible.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.xbible.data.BibleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uniffi.xbible_engine.SwordModule
import uniffi.xbible_engine.TaskState
import javax.inject.Inject

private const val TAG = "StoreVM"

sealed class InstallationStatus {
    object Idle : InstallationStatus()
    object Pending : InstallationStatus()
    data class Installing(val progress: Float) : InstallationStatus()
    object Installed : InstallationStatus()
    object Cancelled : InstallationStatus()
    data class Failed(val message: String) : InstallationStatus()
}

@HiltViewModel
class StoreViewModel @Inject constructor(
    application: Application,
    private val repository: BibleRepository
) : AndroidViewModel(application) {

    // ── Raw state (mirrors Swift @Published raw state) ─────────────────

    private val _remoteSources = MutableStateFlow<List<String>>(emptyList())
    val remoteSources: StateFlow<List<String>> = _remoteSources.asStateFlow()

    private val _selectedSource = MutableStateFlow<String?>(null)
    val selectedSource: StateFlow<String?> = _selectedSource.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // The "modulesForCurrentSource" equivalent: raw, unfiltered.
    private val _modulesForCurrentSource = MutableStateFlow<List<SwordModule>>(emptyList())

    private val _installationStates =
        MutableStateFlow<Map<String, InstallationStatus>>(emptyMap())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Swift owns selectedCategory in the VM. Do the same.
    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // ── Derived state (view-facing; mirrors Swift @Published private(set)) ──

    private val _availableCategories = MutableStateFlow<List<String>>(listOf("All"))
    val availableCategories: StateFlow<List<String>> = _availableCategories.asStateFlow()

    /** Grouped-by-language, category- and search-filtered modules. This is what the UI renders. */
    private val _filteredLanguages = MutableStateFlow<Map<String, List<SwordModule>>>(emptyMap())
    val filteredLanguages: StateFlow<Map<String, List<SwordModule>>> =
        _filteredLanguages.asStateFlow()

    /** Snapshot of installation states for currently visible modules. */
    private val _visibleStatuses = MutableStateFlow<Map<String, InstallationStatus>>(emptyMap())
    val visibleStatuses: StateFlow<Map<String, InstallationStatus>> =
        _visibleStatuses.asStateFlow()

    // ── Book-keeping ───────────────────────────────────────────────────

    private val activeTaskIds = mutableMapOf<String, String>()
    private var sourceLoadJob: Job? = null
    private var initCompleted = false

    init {
        Log.d(TAG, "init: StoreViewModel created")
        loadSources()
    }

    // ── Sources ────────────────────────────────────────────────────────

    private fun loadSources() {
        Log.d(TAG, "loadSources: start")
        viewModelScope.launch {
            try {
                if (!repository.isInitialized()) {
                    Log.d(TAG, "loadSources: repository not initialized, calling initialize()")
                    repository.initialize()
                }
                Log.d(TAG, "loadSources: calling repository.getRemoteSources()…")
                val sources = withContext(Dispatchers.IO) { repository.getRemoteSources() }
                Log.d(TAG, "loadSources: got ${sources.size} sources: $sources")
                _remoteSources.value = sources
                if (!initCompleted && sources.isNotEmpty()) {
                    initCompleted = true
                    Log.d(TAG, "loadSources: auto-selecting '${sources.first()}'")
                    selectSource(sources.first())
                } else {
                    Log.d(
                        TAG,
                        "loadSources: NOT auto-selecting (initCompleted=$initCompleted, empty=${sources.isEmpty()})"
                    )
                }
            } catch (e: Throwable) {
                Log.e(TAG, "loadSources: FAILED", e)
                e.printStackTrace()
                _errorMessage.value = "Failed to load sources: ${e.message}"
            }
        }
    }

    fun selectSource(source: String) {
        Log.d(TAG, "selectSource('$source'): current=${_selectedSource.value}")
        if (_selectedSource.value == source) {
            Log.d(TAG, "selectSource: same source, early return")
            return
        }
        _selectedSource.value = source
        _searchQuery.value = ""          // matches Swift reset-on-source-change
        _selectedCategory.value = "All"  // also reset the tab
        refreshStore()
    }

    fun refreshStore() {
        val source = _selectedSource.value
        Log.d(TAG, "refreshStore: source=$source")
        if (source == null) {
            Log.d(TAG, "refreshStore: EARLY RETURN, no source selected")
            return
        }
        // Cancel any in-flight refresh so stale results don't clobber fresh ones.
        sourceLoadJob?.cancel()
        sourceLoadJob = viewModelScope.launch {
            // Clear the current list up front — matches the Swift
            // `modulesForCurrentSource = []` before kickoff.
            _modulesForCurrentSource.value = emptyList()
            _filteredLanguages.value = emptyMap()
            _isLoading.value = true
            _errorMessage.value = null
            Log.d(TAG, "refreshStore: cleared state, isLoading=true")

            try {
                Log.d(TAG, "refreshStore: calling repository.fetchRemoteModules('$source')…")
                val modules = withContext(Dispatchers.IO) {
                    repository.fetchRemoteModules(source)
                }
                Log.d(TAG, "refreshStore: fetch returned ${modules.size} modules")
                if (modules.isNotEmpty()) {
                    val sample = modules.first()
                    Log.d(
                        TAG,
                        "refreshStore: first module = name=${sample.name}, lang=${sample.language}, " +
                                "cat=${sample.category}, src=${sample.source}"
                    )
                } else {
                    Log.w(
                        TAG,
                        "refreshStore: ENGINE RETURNED ZERO MODULES for source '$source'"
                    )
                }

                // Ignore the result if the user switched away mid-flight.
                if (_selectedSource.value != source) {
                    Log.d(
                        TAG,
                        "refreshStore: source changed mid-flight (now ${_selectedSource.value}), discarding"
                    )
                    return@launch
                }

                _modulesForCurrentSource.value = modules
                _isLoading.value = false

                // Sync installed statuses *after* we know what modules exist,
                // exactly like Swift's syncAllInstallationStatuses().
                Log.d(TAG, "refreshStore: calling syncAllInstallationStatuses()")
                syncAllInstallationStatuses()
                Log.d(TAG, "refreshStore: calling recomputeDerived()")
                recomputeDerived()
                Log.d(TAG, "refreshStore: DONE")
            } catch (e: Throwable) {
                if (_selectedSource.value != source) return@launch
                Log.e(TAG, "refreshStore: FETCH THREW", e)
                e.printStackTrace()
                _errorMessage.value =
                    "Network error: ${e.message}. Ensure your device has internet access."
                _modulesForCurrentSource.value = emptyList()
                _isLoading.value = false
                recomputeDerived()
            }
        }
    }

    private suspend fun syncAllInstallationStatuses() {
        try {
            Log.d(TAG, "syncAll: calling repository.refreshModules()…")
            val installed = withContext(Dispatchers.IO) { repository.refreshModules() }
            Log.d(
                TAG,
                "syncAll: ${installed.size} installed modules: ${installed.map { it.name }}"
            )
            val installedNames = installed.map { it.name }.toSet()
            val current = _installationStates.value.toMutableMap()

            for (module in _modulesForCurrentSource.value) {
                if (installedNames.contains(module.name)) {
                    if (current[module.name] != InstallationStatus.Installed) {
                        current[module.name] = InstallationStatus.Installed
                    }
                } else {
                    when (current[module.name]) {
                        is InstallationStatus.Installing,
                        InstallationStatus.Pending -> { /* keep */ }
                        else -> current[module.name] = InstallationStatus.Idle
                    }
                }
            }
            _installationStates.value = current
            Log.d(TAG, "syncAll: installationStates now has ${current.size} entries")
        } catch (e: Throwable) {
            Log.e(TAG, "syncAll: FAILED", e)
            e.printStackTrace()
        }
    }

    // ── Search + category ─────────────────────────────────────────────

    fun updateSearchQuery(query: String) {
        Log.d(TAG, "updateSearchQuery('$query')")
        _searchQuery.value = query
        recomputeDerived()
    }

    fun setCategory(category: String) {
        Log.d(TAG, "setCategory('$category') (current=${_selectedCategory.value})")
        if (_selectedCategory.value == category) return
        _selectedCategory.value = category
        recomputeDerived()
    }

    // ── The single derived-state computation ──────────────────────────
    // Mirrors Swift recomputeDerived(): one place, pure function of inputs.

    private fun recomputeDerived() {
        val allModules = _modulesForCurrentSource.value
        Log.d(
            TAG,
            "recomputeDerived: allModules=${allModules.size}, " +
                    "selectedCategory=${_selectedCategory.value}, search='${_searchQuery.value}'"
        )

        // 1) Categories, with "All" prepended and stale-selection correction.
        val categories = listOf("All") + allModules.map { it.category }.distinct().sorted()
        Log.d(TAG, "recomputeDerived: categories=$categories")

        val effectiveCategory = if (categories.contains(_selectedCategory.value)) {
            _selectedCategory.value
        } else {
            Log.d(
                TAG,
                "recomputeDerived: selectedCategory '${_selectedCategory.value}' not in categories → forcing 'All'"
            )
            _selectedCategory.value = "All"
            "All"
        }

        // 2) Category filter.
        val base = if (effectiveCategory == "All") {
            allModules
        } else {
            allModules.filter { it.category == effectiveCategory }
        }
        Log.d(TAG, "recomputeDerived: after category filter → ${base.size}")

        // 3) Search filter.
        val query = _searchQuery.value.trim().lowercase()
        val searched = if (query.isEmpty()) {
            base
        } else {
            base.filter { matches(it, query) }
        }
        Log.d(TAG, "recomputeDerived: after search filter → ${searched.size}")

        // 4) Group by language.
        val grouped = searched.groupBy { it.language }
        Log.d(
            TAG,
            "recomputeDerived: grouped by language → keys=${grouped.keys}, " +
                    "sizes=${grouped.mapValues { it.value.size }}"
        )

        // 5) Snapshot of statuses for visible modules only.
        val currentStates = _installationStates.value
        val statuses = HashMap<String, InstallationStatus>(searched.size)
        for (m in searched) {
            statuses[m.name] = currentStates[m.name] ?: InstallationStatus.Idle
        }

        // Assign everything atomically from the collector's perspective.
        _availableCategories.value = categories
        _filteredLanguages.value = grouped
        _visibleStatuses.value = statuses
        Log.d(
            TAG,
            "recomputeDerived: ASSIGNED filteredLanguages.size=${grouped.size}, " +
                    "availableCategories=$categories"
        )
    }

    private fun matches(module: SwordModule, query: String): Boolean {
        if (module.name.lowercase().contains(query)) return true
        if (module.description.lowercase().contains(query)) return true
        if (module.category.lowercase().contains(query)) return true
        if (module.language.lowercase().contains(query)) return true
        if (module.source.lowercase().contains(query)) return true
        return false
    }

    /** Convenience for the view: mirrors Swift `status(for:)`. */
    fun statusFor(module: SwordModule): InstallationStatus =
        _visibleStatuses.value[module.name] ?: InstallationStatus.Idle

    // ── Installation ──────────────────────────────────────────────────

    fun install(module: SwordModule) {
        val moduleName = module.name
        val source = module.source
        Log.d(TAG, "install('$moduleName') from source='$source'")

        viewModelScope.launch {
            _installationStates.value += (moduleName to InstallationStatus.Pending)
            recomputeDerived()

            Log.d(TAG, "install('$moduleName'): calling repository.installModuleAsync(…)")
            val taskId = withContext(Dispatchers.IO) {
                repository.installModuleAsync(source, moduleName)
            }
            Log.d(TAG, "install('$moduleName'): taskId='$taskId'")

            if (taskId.isEmpty()) {
                Log.w(TAG, "install('$moduleName'): EMPTY taskId, marking Failed")
                _installationStates.value +=
                    (moduleName to InstallationStatus.Failed("Task failed to start"))
                recomputeDerived()
                return@launch
            }

            activeTaskIds[moduleName] = taskId
            monitorInstallation(moduleName, taskId)
        }
    }

    private suspend fun monitorInstallation(moduleName: String, taskId: String) {
        Log.d(TAG, "monitorInstallation('$moduleName', taskId='$taskId'): start")
        while (true) {
            val status = withContext(Dispatchers.IO) { repository.getTaskStatus(taskId) }
            if (status == null) {
                Log.w(TAG, "monitorInstallation('$moduleName'): status == null, marking Failed")
                _installationStates.value +=
                    (moduleName to InstallationStatus.Failed("Status not found"))
                activeTaskIds.remove(moduleName)
                recomputeDerived()
                return
            }

            when (val state = status.state) {
                is TaskState.Running, is TaskState.Queued -> {
                    Log.d(
                        TAG,
                        "monitorInstallation('$moduleName'): progress=${status.progress}"
                    )
                    _installationStates.value +=
                        (moduleName to InstallationStatus.Installing(status.progress.toFloat()))
                    // Only the statuses snapshot needs refreshing here; the
                    // module list itself hasn't changed. Updating just that
                    // avoids thrashing the grouped map.
                    refreshVisibleStatusesOnly()
                }
                is TaskState.Completed -> {
                    Log.d(TAG, "monitorInstallation('$moduleName'): Completed")
                    _installationStates.value += (moduleName to InstallationStatus.Installed)
                    activeTaskIds.remove(moduleName)
                    // Matches Swift: refresh installed list, then recompute.
                    syncAllInstallationStatuses()
                    recomputeDerived()
                    return
                }
                is TaskState.Failed -> {
                    Log.w(
                        TAG,
                        "monitorInstallation('$moduleName'): Failed — ${state.error}"
                    )
                    _installationStates.value +=
                        (moduleName to InstallationStatus.Failed(state.error))
                    activeTaskIds.remove(moduleName)
                    recomputeDerived()
                    return
                }
                else -> {
                    Log.d(
                        TAG,
                        "monitorInstallation('$moduleName'): unknown state=$state, marking Cancelled"
                    )
                    _installationStates.value += (moduleName to InstallationStatus.Cancelled)
                    activeTaskIds.remove(moduleName)
                    recomputeDerived()
                    return
                }
            }
            delay(100)
        }
    }

    private fun refreshVisibleStatusesOnly() {
        val visible = _filteredLanguages.value.values.flatten()
        val currentStates = _installationStates.value
        val statuses = HashMap<String, InstallationStatus>(visible.size)
        for (m in visible) {
            statuses[m.name] = currentStates[m.name] ?: InstallationStatus.Idle
        }
        _visibleStatuses.value = statuses
    }

    fun cancelInstall(moduleName: String) {
        Log.d(TAG, "cancelInstall('$moduleName')")
        activeTaskIds[moduleName]?.let { taskId ->
            Log.d(TAG, "cancelInstall('$moduleName'): calling repository.cancelTask('$taskId')")
            viewModelScope.launch(Dispatchers.IO) {
                repository.cancelTask(taskId)
            }
        } ?: Log.d(TAG, "cancelInstall('$moduleName'): no active taskId")
        _installationStates.value += (moduleName to InstallationStatus.Idle)
        recomputeDerived()
    }
}