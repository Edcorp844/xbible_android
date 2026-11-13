package com.example.xbible.viewmodel

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.xbible.MainActivity
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
import uniffi.xbible_engine.EntitySummary
import uniffi.xbible_engine.FreeBibleTaskState
import uniffi.xbible_engine.FreeBibleTaskStatus
import uniffi.xbible_engine.LanguageFacet
import uniffi.xbible_engine.StoreItem
import uniffi.xbible_engine.StoreShelf
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "FreeBibleStoreVM"

data class InstallState(
    val translationId: String,
    val taskId: String,
    val fraction: Float = 0f,
    val message: String = "",
    val chaptersDone: Int = 0,
    val chaptersTotal: Int = 0,
    val versesDone: Int = 0,
    val currentBook: String = "",
    val currentChapter: Int = 0,
    val failed: String? = null,
    val done: Boolean = false,
)

data class BrowseState(
    val datasetId: String,
    val kind: String,
    val items: List<EntitySummary> = emptyList(),
    val loading: Boolean = false,
    val query: String = "",
    val offset: Int = 0,
    val endReached: Boolean = false,
)

@HiltViewModel
class FreeBibleStoreViewModel @Inject constructor(
    application: Application,
    private val repository: BibleRepository,
) : AndroidViewModel(application) {

    // ── Catalog state ────────────────────────────────────────────

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _shelves = MutableStateFlow<List<StoreShelf>>(emptyList())
    val shelves: StateFlow<List<StoreShelf>> = _shelves.asStateFlow()

    private val _languages = MutableStateFlow<List<LanguageFacet>>(emptyList())
    val languages: StateFlow<List<LanguageFacet>> = _languages.asStateFlow()

    private val _selectedLanguage = MutableStateFlow<String?>(null)
    val selectedLanguage: StateFlow<String?> = _selectedLanguage.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<StoreItem>>(emptyList())
    val searchResults: StateFlow<List<StoreItem>> = _searchResults.asStateFlow()

    // ── Install state ────────────────────────────────────────────

    private val _activeInstall = MutableStateFlow<InstallState?>(null)
    val activeInstall: StateFlow<InstallState?> = _activeInstall.asStateFlow()

    private val notificationManager by lazy {
        NotificationManagerCompat.from(getApplication())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                INSTALL_CHANNEL_ID,
                "Bible Downloads & Installations",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of downloading Bible translations and modules"
            }
            val manager = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun updateInstallNotification(state: InstallState) {
        createNotificationChannel()

        val context = getApplication<Application>()
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when {
            state.failed != null -> "Installation Failed: ${state.translationId.uppercase()}"
            state.done -> "${state.translationId.uppercase()} Installed"
            state.fraction <= 0f -> "Preparing ${state.translationId.uppercase()}"
            else -> "Downloading ${state.translationId.uppercase()}"
        }

        val contentText = when {
            state.failed != null -> state.failed
            state.done -> "Download complete"
            state.message.isNotBlank() -> state.message
            else -> "Downloading..."
        }

        val builder = NotificationCompat.Builder(context, INSTALL_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(contentText)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (state.done) {
            builder.setOngoing(false)
                .setAutoCancel(true)
                .setProgress(0, 0, false)
            if (state.failed != null) {
                builder.setSmallIcon(android.R.drawable.stat_notify_error)
            } else {
                builder.setSmallIcon(android.R.drawable.stat_sys_download_done)
            }
        } else {
            builder.setOngoing(true)
                .setAutoCancel(false)
            val progress = (state.fraction * 100).toInt()
            if (state.fraction <= 0f) {
                builder.setProgress(100, 0, true)
            } else {
                builder.setProgress(100, progress, false)
            }
        }

        try {
            notificationManager.notify(INSTALL_NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to post notification (permission missing?)", e)
        }
    }

    private fun cancelInstallNotification() {
        try {
            notificationManager.cancel(INSTALL_NOTIFICATION_ID)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel notification", e)
        }
    }

    private fun updateActiveInstall(state: InstallState?) {
        _activeInstall.value = state
        if (state != null) {
            updateInstallNotification(state)
        } else {
            cancelInstallNotification()
        }
    }

    // ── Browse state ─────────────────────────────────────────────

    private val _browseState = MutableStateFlow<BrowseState?>(null)
    val browseState: StateFlow<BrowseState?> = _browseState.asStateFlow()

    private var isRefreshing = false
    private var lastRefreshMs: Long = 0
    private var pollJob: Job? = null
    private var searchJob: Job? = null

    init {
        refreshCatalog()
    }

    // ── Catalog ──────────────────────────────────────────────────

    fun refreshCatalog(force: Boolean = false) {
        if (isRefreshing) {
            Log.d(TAG, "refreshCatalog() called while already refreshing — skipping")
            return
        }
        val now = System.currentTimeMillis()
        if (!force && now - lastRefreshMs < REFRESH_COOLDOWN_MS) {
            Log.d(TAG, "refreshCatalog() skipped — within cooldown")
            reloadFromCache()
            return
        }

        viewModelScope.launch {
            isRefreshing = true
            _isLoading.value = true
            _errorMessage.value = null
            Log.d(TAG, "refreshCatalog() started")

            try {
                repository.initialize()
                repository.refreshFreeBibleCatalog()

                val lang = _selectedLanguage.value
                val newShelves = withContext(Dispatchers.IO) {
                    repository.freeBibleHomeFeed(lang)
                }
                val newLanguages = withContext(Dispatchers.IO) {
                    repository.freeBibleLanguages()
                }

                _shelves.value = newShelves
                _languages.value = newLanguages
                lastRefreshMs = System.currentTimeMillis()

                Log.d(
                    TAG,
                    "refreshCatalog(): ${newShelves.size} shelves, ${newLanguages.size} languages"
                )
            } catch (e: Throwable) {
                Log.e(TAG, "refreshCatalog failed", e)
                _errorMessage.value = e.message ?: "Failed to load catalog"
            } finally {
                _isLoading.value = false
                isRefreshing = false
                Log.d(TAG, "refreshCatalog() finished")
            }
        }
    }

    private fun reloadFromCache() {
        viewModelScope.launch {
            try {
                val lang = _selectedLanguage.value
                val newShelves = withContext(Dispatchers.IO) {
                    repository.freeBibleHomeFeed(lang)
                }
                val newLanguages = withContext(Dispatchers.IO) {
                    repository.freeBibleLanguages()
                }
                _shelves.value = newShelves
                _languages.value = newLanguages
            } catch (e: Throwable) {
                Log.e(TAG, "reloadFromCache failed", e)
            }
        }
    }

    fun selectLanguage(languageCode: String?) {
        _selectedLanguage.value = languageCode
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val newShelves = withContext(Dispatchers.IO) {
                    repository.freeBibleHomeFeed(languageCode)
                }
                _shelves.value = newShelves
            } catch (e: Throwable) {
                Log.e(TAG, "selectLanguage failed", e)
                _errorMessage.value = e.message ?: "Failed to filter catalog"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ── Store search ─────────────────────────────────────────────

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()

        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        searchJob = viewModelScope.launch {
            delay(250.milliseconds)
            try {
                val results = withContext(Dispatchers.IO) {
                    repository.searchFreeBibleStore(query)
                }
                _searchResults.value = results
                Log.d(TAG, "search('$query') → ${results.size} results")
            } catch (e: Throwable) {
                Log.e(TAG, "search failed", e)
            }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _searchQuery.value = ""
        _searchResults.value = emptyList()
    }

    // ── Install ──────────────────────────────────────────────────

    /**
     * Installs any store item, dispatching on [StoreItem.kind].
     *
     * This is the *only* correct way to install from the UI. Passing
     * `item.id` alone is not enough — "adam-clarke" is a commentary id,
     * "theographic:person" is an entity-set id, "kjv" is a translation
     * id, and none of them are distinguishable from the id alone.
     */
    fun install(item: StoreItem) = installItem(itemId = item.id, kind = item.kind)

    /**
     * Installs by explicit id + kind. Prefer [install] when you already
     * have the [StoreItem].
     */
    fun installItem(itemId: String, kind: String) {
        if (_activeInstall.value?.done == false) {
            Log.w(TAG, "install already in progress")
            return
        }

        Log.d(TAG, "installItem id=$itemId kind=$kind")

        viewModelScope.launch {
            try {
                val taskId = withContext(Dispatchers.IO) {
                    when (kind) {
                        "translation" -> repository.installFreeBibleTranslation(itemId)

                        "entity_set" -> {
                            val (dataset, entityKind) = itemId.split(':', limit = 2)
                            repository.installFreeBibleEntitySet(dataset, entityKind)
                        }

                        "commentary" -> repository.installFreeBibleCommentary(itemId)

                        else -> error("unsupported store item kind: $kind (id=$itemId)")
                    }
                }
                updateActiveInstall(
                    InstallState(
                        translationId = itemId,
                        taskId = taskId,
                        message = "Queued",
                    )
                )
                startPolling(taskId, itemId)
            } catch (e: Throwable) {
                Log.e(TAG, "installItem failed to start", e)
                updateActiveInstall(
                    InstallState(
                        translationId = itemId,
                        taskId = "",
                        failed = e.message ?: "install failed",
                        done = true,
                    )
                )
            }
        }
    }

    fun uninstallTranslation(translationId: String) {
        viewModelScope.launch {
            try {
                val taskId = withContext(Dispatchers.IO) {
                    repository.uninstallFreeBibleTranslation(translationId)
                }
                updateActiveInstall(
                    InstallState(
                        translationId = translationId,
                        taskId = taskId,
                        message = "Queued",
                    )
                )
                startPolling(taskId, translationId)
            } catch (e: Throwable) {
                Log.e(TAG, "uninstallTranslation failed to start", e)
                _errorMessage.value = e.message ?: "Uninstall failed"
            }
        }
    }

    fun cancelActiveInstall() {
        val current = _activeInstall.value ?: return
        if (current.taskId.isNotEmpty()) {
            viewModelScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        repository.cancelFreeBibleTask(current.taskId)
                    }
                } catch (e: Throwable) {
                    Log.e(TAG, "cancel failed", e)
                }
            }
        }
    }

    fun dismissActiveInstall() {
        val current = _activeInstall.value ?: return
        if (!current.done) {
            Log.w(TAG, "dismissActiveInstall called while running; ignoring")
            return
        }
        pollJob?.cancel()
        updateActiveInstall(null)
    }

    private fun startPolling(taskId: String, itemId: String) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                delay(250)

                val status: FreeBibleTaskStatus? = try {
                    withContext(Dispatchers.IO) {
                        repository.pollFreeBibleTask(taskId)
                    }
                } catch (e: Throwable) {
                    Log.e(TAG, "poll failed", e)
                    null
                }

                if (status == null) {
                updateActiveInstall(
                    _activeInstall.value?.copy(
                        failed = "Task not found",
                        done = true,
                    )
                )
                    return@launch
                }

                val p = status.progress
                val state = status.state

                val updated = InstallState(
                    translationId = itemId,
                    taskId = taskId,
                    fraction = p.fraction.toFloat().coerceIn(0f, 1f),
                    message = p.message,
                    chaptersDone = p.chaptersDone,
                    chaptersTotal = p.chaptersTotal,
                    versesDone = p.versesDone,
                    currentBook = p.currentBookName,
                    currentChapter = p.currentChapter,
                    failed = when (state) {
                        is FreeBibleTaskState.Failed -> state.error
                        else -> null
                    },
                    done = when (state) {
                        FreeBibleTaskState.Completed,
                        FreeBibleTaskState.Cancelled,
                        is FreeBibleTaskState.Failed -> true

                        else -> false
                    },
                )

                updateActiveInstall(updated)

                if (updated.done) {
                    // Force-refresh so installed flags update on the cards.
                    refreshCatalog(force = true)
                    return@launch
                }
            }
        }
    }

    // ── Installed state (non-blocking) ───────────────────────────

    /**
     * Reads the [StoreItem.installed] flag from the currently loaded shelves.
     * No DB access — pure in-memory lookup. Called from composition, so it
     * must never block.
     */
    fun isInstalled(itemId: String): Boolean {
        val shelves = _shelves.value
        for (shelf in shelves) {
            for (item in shelf.items) {
                if (item.id == itemId) {
                    return item.installed
                }
            }
        }
        return false
    }

    fun itemDetail(id: String, kind: String): StoreItem? {
        val shelves = _shelves.value
        for (shelf in shelves) {
            for (item in shelf.items) {
                if (item.id == id && item.kind == kind) {
                    return item
                }
            }
        }
        return null
    }

    // ── Browse (entity sets) ─────────────────────────────────────

    fun openBrowse(datasetId: String, kind: String) {
        _browseState.value = BrowseState(
            datasetId = datasetId,
            kind = kind,
            items = emptyList(),
            loading = false,
            query = "",
            offset = 0,
            endReached = false,
        )
        loadBrowsePage(reset = true)
    }

    fun onBrowseQueryChange(query: String) {
        val current = _browseState.value ?: return
        if (current.query == query) return
        _browseState.value = current.copy(
            query = query,
            items = emptyList(),
            offset = 0,
            endReached = false,
        )
        loadBrowsePage(reset = true)
    }

    fun loadMoreBrowse() {
        val current = _browseState.value ?: return
        if (current.loading || current.endReached) return
        _browseState.value = current.copy(offset = current.items.size)
        loadBrowsePage(reset = false)
    }

    private fun loadBrowsePage(reset: Boolean) {
        val current = _browseState.value ?: return
        viewModelScope.launch {
            _browseState.value = _browseState.value?.copy(loading = true)
            try {
                val query = current.query.ifBlank { null }
                val page = withContext(Dispatchers.IO) {
                    when (current.kind) {
                        "person" -> repository.listFreeBiblePeople(
                            current.datasetId, current.offset, PAGE_SIZE, query
                        )

                        "event" -> repository.listFreeBibleEvents(
                            current.datasetId, current.offset, PAGE_SIZE, query
                        )

                        "place" -> repository.listFreeBiblePlaces(
                            current.datasetId, current.offset, PAGE_SIZE, query
                        )

                        "group" -> repository.listFreeBibleGroups(
                            current.datasetId, current.offset, PAGE_SIZE, query
                        )

                        else -> emptyList()
                    }
                }

                val base = _browseState.value ?: return@launch
                val merged = if (reset) page else base.items + page
                _browseState.value = base.copy(
                    items = merged,
                    loading = false,
                    endReached = page.isEmpty() || page.size < PAGE_SIZE,
                )
            } catch (e: Throwable) {
                Log.e(TAG, "loadBrowsePage failed", e)
                _browseState.value = _browseState.value?.copy(loading = false)
            }
        }
    }

    fun closeBrowse() {
        _browseState.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        pollJob?.cancel()
        searchJob?.cancel()
    }

    companion object {
        private const val REFRESH_COOLDOWN_MS = 5 * 60 * 1000L
        private const val PAGE_SIZE = 100
        private const val INSTALL_CHANNEL_ID = "bible_install_channel"
        private const val INSTALL_NOTIFICATION_ID = 1001
    }
}