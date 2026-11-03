package com.example.xbible.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.xbible.data.BibleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import uniffi.xbible_engine.SwordModule
import uniffi.xbible_engine.TaskState
import javax.inject.Inject

private const val TAG = "StoreVM"
private const val POLL_INTERVAL_MS = 100L

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

    // ── Raw state ─────────────────────────────────────────────────────

    private val _remoteSources = MutableStateFlow<List<String>>(emptyList())
    val remoteSources: StateFlow<List<String>> = _remoteSources.asStateFlow()

    private val _selectedSource = MutableStateFlow<String?>(null)
    val selectedSource: StateFlow<String?> = _selectedSource.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _modulesForCurrentSource = MutableStateFlow<List<SwordModule>>(emptyList())

    /**
     * The ONLY authoritative map of module install state.
     * Both the internal derived maps and the view read through this.
     */
    private val _installationStates =
        MutableStateFlow<Map<String, InstallationStatus>>(emptyMap())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // ── Derived state ─────────────────────────────────────────────────

    private val _availableCategories = MutableStateFlow<List<String>>(listOf("All"))
    val availableCategories: StateFlow<List<String>> = _availableCategories.asStateFlow()

    private val _filteredLanguages = MutableStateFlow<Map<String, List<SwordModule>>>(emptyMap())
    val filteredLanguages: StateFlow<Map<String, List<SwordModule>>> =
        _filteredLanguages.asStateFlow()

    /**
     * Snapshot of the statuses of *currently visible* modules. Rebuilt:
     *   - on every `recomputeDerived()` (source/search/category change)
     *   - on every install-progress tick (cheap: just the visible set)
     *   - on every terminal transition
     *
     * The Compose screen binds `statusFor(module)` → this map. Because it
     * updates on every tick, progress moves.
     */
    private val _visibleStatuses = MutableStateFlow<Map<String, InstallationStatus>>(emptyMap())
    val visibleStatuses: StateFlow<Map<String, InstallationStatus>> =
        _visibleStatuses.asStateFlow()

    // ── Book-keeping ──────────────────────────────────────────────────

    private val activeTaskIds = mutableMapOf<String, String>()
    private val installJobs = mutableMapOf<String, Job>()
    private var sourceLoadJob: Job? = null
    private var initCompleted = false

    /**
     * Serializes "engine walk" reconciliation (refreshModules) with install
     * completions. Prevents a slow refresh from clobbering a freshly-set
     * Installed state, and prevents two refreshes from overlapping.
     */
    private val reconcileMutex = Mutex()

    init {
        Log.d(TAG, "init: StoreViewModel created")
        loadSources()
    }

    // ── Sources ───────────────────────────────────────────────────────

    private fun loadSources() {
        viewModelScope.launch {
            try {
                if (!repository.isInitialized()) {
                    Log.d(TAG, "loadSources: initializing repository")
                    repository.initialize()
                }
                val sources = repository.getRemoteSources()
                Log.d(TAG, "loadSources: ${sources.size} sources")
                _remoteSources.value = sources
                if (!initCompleted && sources.isNotEmpty()) {
                    initCompleted = true
                    selectSource(sources.first())
                }
            } catch (e: Throwable) {
                Log.e(TAG, "loadSources failed", e)
                _errorMessage.value = "Failed to load sources: ${e.message}"
            }
        }
    }

    fun selectSource(source: String) {
        if (_selectedSource.value == source) return
        Log.d(TAG, "selectSource('$source')")
        _selectedSource.value = source
        _searchQuery.value = ""
        _selectedCategory.value = "All"
        refreshStore()
    }

    fun refreshStore() {
        val source = _selectedSource.value ?: return
        sourceLoadJob?.cancel()
        sourceLoadJob = viewModelScope.launch {
            _modulesForCurrentSource.value = emptyList()
            _filteredLanguages.value = emptyMap()
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val modules = repository.fetchRemoteModules(source)
                if (_selectedSource.value != source) return@launch

                _modulesForCurrentSource.value = modules
                _isLoading.value = false

                syncAllInstallationStatuses()
                recomputeDerived()
            } catch (e: Throwable) {
                if (_selectedSource.value != source) return@launch
                Log.e(TAG, "refreshStore failed", e)
                _errorMessage.value =
                    "Network error: ${e.message}. Ensure your device has internet access."
                _modulesForCurrentSource.value = emptyList()
                _isLoading.value = false
                recomputeDerived()
            }
        }
    }

    /**
     * Reconcile `_installationStates` with what the engine reports on disk.
     *
     * Runs serialized via `reconcileMutex` and never on the install hot path
     * while an install is in-flight for that module — otherwise a slow
     * `refreshModules()` could land *after* a completion and downgrade it.
     */
    private suspend fun syncAllInstallationStatuses() {
        reconcileMutex.withLock {
            try {
                val installed = repository.refreshModules()
                val installedNames = installed.map { it.name }.toSet()

                _installationStates.update { current ->
                    val next = current.toMutableMap()
                    for (module in _modulesForCurrentSource.value) {
                        if (installedNames.contains(module.name)) {
                            if (next[module.name] != InstallationStatus.Installed) {
                                next[module.name] = InstallationStatus.Installed
                            }
                        } else {
                            when (next[module.name]) {
                                is InstallationStatus.Installing,
                                InstallationStatus.Pending -> { /* keep in-flight */ }
                                else -> next[module.name] = InstallationStatus.Idle
                            }
                        }
                    }
                    next
                }
            } catch (e: Throwable) {
                Log.e(TAG, "syncAllInstallationStatuses failed", e)
            }
        }
    }

    // ── Search + category ─────────────────────────────────────────────

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        recomputeDerived()
    }

    fun setCategory(category: String) {
        if (_selectedCategory.value == category) return
        _selectedCategory.value = category
        recomputeDerived()
    }

    // ── Derived state ─────────────────────────────────────────────────

    private fun recomputeDerived() {
        val allModules = _modulesForCurrentSource.value

        val categories = listOf("All") + allModules.map { it.category }.distinct().sorted()
        val effectiveCategory = if (categories.contains(_selectedCategory.value)) {
            _selectedCategory.value
        } else {
            _selectedCategory.value = "All"
            "All"
        }

        val base = if (effectiveCategory == "All") allModules
        else allModules.filter { it.category == effectiveCategory }

        val query = _searchQuery.value.trim().lowercase()
        val searched = if (query.isEmpty()) base else base.filter { matches(it, query) }

        val grouped = searched.groupBy { it.language }

        // Snapshot statuses for the visible set from the single source of truth.
        val currentStates = _installationStates.value
        val statuses = HashMap<String, InstallationStatus>(searched.size)
        for (m in searched) statuses[m.name] = currentStates[m.name] ?: InstallationStatus.Idle

        _availableCategories.value = categories
        _filteredLanguages.value = grouped
        _visibleStatuses.value = statuses
    }

    /**
     * Cheap update of `_visibleStatuses` only — used during progress ticks.
     * Reads the current `filteredLanguages` (unchanged during an install),
     * overlays fresh statuses. Never touches `_filteredLanguages` itself, so
     * `LazyRow` items are not re-created.
     */
    private fun refreshVisibleStatusesOnly() {
        val visible = _filteredLanguages.value.values.flatten()
        if (visible.isEmpty()) return
        val currentStates = _installationStates.value
        val statuses = HashMap<String, InstallationStatus>(visible.size)
        for (m in visible) statuses[m.name] = currentStates[m.name] ?: InstallationStatus.Idle
        // `update` guarantees a new map instance so StateFlow emits.
        _visibleStatuses.value = statuses
    }

    private fun matches(module: SwordModule, query: String): Boolean =
        module.name.lowercase().contains(query) ||
                module.description.lowercase().contains(query) ||
                module.category.lowercase().contains(query) ||
                module.language.lowercase().contains(query) ||
                module.source.lowercase().contains(query)

    /** Convenience for the view. Backed by the freshly-updating snapshot. */
    fun statusFor(module: SwordModule): InstallationStatus =
        _visibleStatuses.value[module.name] ?: InstallationStatus.Idle

    // ── Installation ──────────────────────────────────────────────────

    fun install(module: SwordModule) {
        val moduleName = module.name
        val source = module.source
        if (installJobs[moduleName]?.isActive == true) {
            Log.d(TAG, "install('$moduleName'): already running")
            return
        }

        installJobs[moduleName] = viewModelScope.launch {
            // 1) Optimistically mark Pending and push to the view immediately.
            setInstallationState(moduleName, InstallationStatus.Pending)

            // 2) Register the task with the engine (serialized on engine thread).
            val taskId = try {
                repository.installModuleAsync(source, moduleName)
            } catch (e: Throwable) {
                Log.e(TAG, "install('$moduleName') threw", e)
                setInstallationState(moduleName, InstallationStatus.Failed("Task failed to start"))
                return@launch
            }

            if (taskId.isEmpty()) {
                Log.w(TAG, "install('$moduleName'): empty taskId")
                setInstallationState(moduleName, InstallationStatus.Failed("Task failed to start"))
                return@launch
            }
            activeTaskIds[moduleName] = taskId

            // 3) Poll until terminal. Each tick pushes progress to the UI.
            pollInstallTask(moduleName, taskId)

            activeTaskIds.remove(moduleName)
            installJobs.remove(moduleName)
        }
    }

    /**
     * Polling loop. Runs on the Main dispatcher (caller's scope). The only
     * engine call — `getTaskStatus` — hops onto the engine thread inside
     * the repository. Progress advances on `_visibleStatuses` immediately.
     */
    private suspend fun pollInstallTask(moduleName: String, taskId: String) {
        while (true) {
            val status = try {
                repository.getTaskStatus(taskId)
            } catch (e: Throwable) {
                Log.e(TAG, "getTaskStatus('$taskId') threw", e)
                null
            }

            if (status == null) {
                setInstallationState(moduleName, InstallationStatus.Failed("Status not found"))
                return
            }

            when (val state = status.state) {
                is TaskState.Running, is TaskState.Queued -> {
                    // Push *both* the authoritative map and the view snapshot.
                    setInstallationState(
                        moduleName,
                        InstallationStatus.Installing(status.progress.toFloat()),
                        updateView = false // batched below via refreshVisibleStatusesOnly
                    )
                    refreshVisibleStatusesOnly()
                }
                is TaskState.Completed -> {
                    // Do NOT call refreshModules() here. The Rust worker may
                    // still be finalizing on-disk writes; walking the module
                    // list now would race it. Swift's `.installCompleted`
                    // handler does exactly this: just mark .installed.
                    setInstallationState(moduleName, InstallationStatus.Installed)
                    return
                }
                is TaskState.Failed -> {
                    setInstallationState(
                        moduleName,
                        InstallationStatus.Failed(state.error)
                    )
                    return
                }
                else -> {
                    setInstallationState(moduleName, InstallationStatus.Cancelled)
                    return
                }
            }
            delay(POLL_INTERVAL_MS)
        }
    }

    /**
     * Set a module's status in `_installationStates` and (optionally) push
     * it into `_visibleStatuses` in one atomic update so the view reacts
     * immediately. Keeping the two maps in sync here — not just in
     * `recomputeDerived()` — is what un-sticks the progress bar.
     */
    private fun setInstallationState(
        moduleName: String,
        newStatus: InstallationStatus,
        updateView: Boolean = true,
    ) {
        _installationStates.update { it + (moduleName to newStatus) }
        if (updateView) refreshVisibleStatusesOnly()
    }

    fun cancelInstall(moduleName: String) {
        Log.d(TAG, "cancelInstall('$moduleName')")
        activeTaskIds[moduleName]?.let { taskId ->
            viewModelScope.launch { repository.cancelTask(taskId) }
        }
        // Cancel any in-flight poll loop; it will exit on next `status == null`
        // or when it observes a state change. Also cancel the job itself so
        // the coroutine tree is clean.
        installJobs.remove(moduleName)?.cancel()

        setInstallationState(moduleName, InstallationStatus.Idle)
    }
}