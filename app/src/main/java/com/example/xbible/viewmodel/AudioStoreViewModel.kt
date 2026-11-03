package com.example.xbible.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uniffi.xbible_engine.AudioAlbum
import uniffi.xbible_engine.AudioCollection
import uniffi.xbible_engine.AudioContributor
import uniffi.xbible_engine.AudioLanguage
import uniffi.xbible_engine.ContributorRole
import uniffi.xbible_engine.ModuleStatus
import uniffi.xbible_engine.RemoteAudioModuleInfo
import uniffi.xbible_engine.StoreApiService
import uniffi.xbible_engine.StoreDownloadProgressListener
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "AudioStoreVM"

enum class ModuleFilter(val label: String) {
    ALL("All"),
    INSTALLED("Installed"),
    NOT_INSTALLED("Not Installed"),
    DOWNLOADING("Downloading"),
}

@HiltViewModel
class AudioStoreViewModel @Inject constructor(
    application: Application,
) : AndroidViewModel(application) {

    private val storeService: StoreApiService = StoreApiService()
    private val imageLoader = ImageLoader(application)

    // ── Catalog (flat modules list) ──────────────────────────────────
    private val _availableModules = MutableStateFlow<List<RemoteAudioModuleInfo>>(emptyList())
    val availableModules: StateFlow<List<RemoteAudioModuleInfo>> = _availableModules.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ── Hierarchy ────────────────────────────────────────────────────
    private val _languages = MutableStateFlow<List<AudioLanguage>>(emptyList())
    val languages: StateFlow<List<AudioLanguage>> = _languages.asStateFlow()

    private val _selectedLanguageId = MutableStateFlow<String?>(null)
    val selectedLanguageId: StateFlow<String?> = _selectedLanguageId.asStateFlow()

    private val _collections = MutableStateFlow<List<AudioCollection>>(emptyList())
    val collections: StateFlow<List<AudioCollection>> = _collections.asStateFlow()

    private val _selectedCollectionId = MutableStateFlow<String?>(null)
    val selectedCollectionId: StateFlow<String?> = _selectedCollectionId.asStateFlow()

    private val _albums = MutableStateFlow<List<AudioAlbum>>(emptyList())
    val albums: StateFlow<List<AudioAlbum>> = _albums.asStateFlow()

    private val _selectedAlbumId = MutableStateFlow<String?>(null)
    val selectedAlbumId: StateFlow<String?> = _selectedAlbumId.asStateFlow()

    private val _modulesInAlbum = MutableStateFlow<List<RemoteAudioModuleInfo>>(emptyList())
    val modulesInAlbum: StateFlow<List<RemoteAudioModuleInfo>> = _modulesInAlbum.asStateFlow()

    // ── Contributors ─────────────────────────────────────────────────
    private val _narrators = MutableStateFlow<List<AudioContributor>>(emptyList())
    val narrators: StateFlow<List<AudioContributor>> = _narrators.asStateFlow()

    // ── Search / filter ──────────────────────────────────────────────
    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    private val _selectedFilter = MutableStateFlow(ModuleFilter.ALL)
    val selectedFilter: StateFlow<ModuleFilter> = _selectedFilter.asStateFlow()

    // ── Selection ────────────────────────────────────────────────────
    private val _selectedModuleId = MutableStateFlow<String?>(null)
    val selectedModuleId: StateFlow<String?> = _selectedModuleId.asStateFlow()

    // ── Artwork gradient (ARGB longs so the VM stays UI-agnostic) ────
    private val _backgroundGradientColors = MutableStateFlow(
        listOf(DEFAULT_ACCENT_TINT, TRANSPARENT)
    )
    val backgroundGradientColors: StateFlow<List<Long>> =
        _backgroundGradientColors.asStateFlow()
    private var gradientOwnerUniqueId: String? = null

    // ── Jobs & constants ─────────────────────────────────────────────
    private var trackingJob: Job? = null
    private var installJob: Job? = null
    private val maximumPageSize: Int = 100

    // ── Derived StateFlows ──────────────────────────────────────────

    val recentModules: StateFlow<List<RemoteAudioModuleInfo>> =
        combine(_availableModules, _searchText) { modules, _ ->
            modules
                .filter { !it.isInstalled }
                .sortedByDescending { parseIso(it.updatedAt) }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList(),
        )

    val filteredModules: StateFlow<List<RemoteAudioModuleInfo>> =
        combine(_availableModules, _searchText, _selectedFilter) { modules, query, filter ->
            filterModules(modules, query, filter)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList(),
        )

    val filteredLanguages: StateFlow<List<AudioLanguage>> =
        combine(_languages, _searchText) { langs, query ->
            val q = query.trim().lowercase()
            if (q.isEmpty()) langs
            else langs.filter {
                it.name.lowercase().contains(q) || it.code.lowercase().contains(q)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList(),
        )

    val filteredNarrators: StateFlow<List<AudioContributor>> =
        combine(_narrators, _searchText) { list, query ->
            val q = query.trim().lowercase()
            if (q.isEmpty()) list
            else list.filter { it.name.lowercase().contains(q) }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList(),
        )

    // ── Bootstrap ────────────────────────────────────────────────────

    fun bootstrap() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                loadCatalogInternal()
                loadLanguagesInternal()
                loadNarratorsInternal()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadCatalogInternal() {
        try {
            Log.d(TAG, "loadCatalog: fetching…")
            val raw = withContext(Dispatchers.IO) { storeService.loadCatalog() }
            Log.d(TAG, "loadCatalog: got ${raw.size}")
            val refreshed = withContext(Dispatchers.IO) {
                raw.map { module ->
                    val installed = storeService.isModuleInstalled(
                        moduleId = module.uniqueId,
                        version = module.version,
                    )
                    module.copy(isInstalled = installed)
                }
            }
            _availableModules.value = refreshed
        } catch (e: Throwable) {
            Log.e(TAG, "loadCatalog failed", e)
            _errorMessage.value = e.message ?: "loadCatalog failed"
        }
    }

    private suspend fun loadLanguagesInternal() {
        try {
            Log.d(TAG, "loadLanguages: fetching…")
            val langs = withContext(Dispatchers.IO) {
                storeService.fetchLanguages(first = maximumPageSize, skip = 0)
            }
            Log.d(TAG, "loadLanguages: got ${langs.size}")
            _languages.value = langs
            if (_selectedLanguageId.value == null && langs.isNotEmpty()) {
                val first = langs.first()
                _selectedLanguageId.value = first.id
                loadCollectionsForLanguage(first.id)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "loadLanguages failed", e)
            _errorMessage.value = e.message ?: "loadLanguages failed"
        }
    }

    private suspend fun loadNarratorsInternal() {
        try {
            Log.d(TAG, "loadNarrators…")
            val list = withContext(Dispatchers.IO) {
                storeService.fetchContributorsByRole(
                    role = ContributorRole.NARRATOR,
                    first = maximumPageSize,
                    skip = 0,
                )
            }
            _narrators.value = list
        } catch (e: Throwable) {
            Log.e(TAG, "loadNarrators failed", e)
            _errorMessage.value = e.message ?: "loadNarrators failed"
        }
    }

    fun reloadCatalog() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                loadCatalogInternal()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadNarrators() {
        viewModelScope.launch { loadNarratorsInternal() }
    }

    // ── Drill-down ───────────────────────────────────────────────────

    fun selectLanguage(languageId: String) {
        if (_selectedLanguageId.value == languageId) return
        _selectedLanguageId.value = languageId
        viewModelScope.launch {
            _isLoading.value = true
            try {
                loadCollectionsForLanguage(languageId)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadCollectionsForLanguage(languageId: String) {
        try {
            val language = _languages.value.firstOrNull { it.id == languageId } ?: return
            Log.d(TAG, "loadCollections(${language.code})…")
            val colls = withContext(Dispatchers.IO) {
                storeService.fetchCollectionsByLanguage(
                    code = language.code,
                    first = maximumPageSize,
                    skip = 0,
                )
            }
            _collections.value = colls
            _albums.value = emptyList()
            _modulesInAlbum.value = emptyList()
            _selectedCollectionId.value = colls.firstOrNull()?.id
            _selectedAlbumId.value = null
            colls.firstOrNull()?.let { loadAlbumsForCollection(it.id) }
        } catch (e: Throwable) {
            Log.e(TAG, "loadCollections failed", e)
            _errorMessage.value = e.message ?: "loadCollections failed"
        }
    }

    fun selectCollection(collectionId: String) {
        if (_selectedCollectionId.value == collectionId) return
        _selectedCollectionId.value = collectionId
        viewModelScope.launch {
            _isLoading.value = true
            try {
                loadAlbumsForCollection(collectionId)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadAlbumsForCollection(collectionId: String) {
        try {
            Log.d(TAG, "loadAlbums($collectionId)…")
            val albs = withContext(Dispatchers.IO) {
                storeService.fetchAlbumsByCollection(
                    collectionId = collectionId,
                    first = maximumPageSize,
                    skip = 0,
                )
            }
            _albums.value = albs
            _modulesInAlbum.value = emptyList()
            _selectedAlbumId.value = albs.firstOrNull()?.id
            albs.firstOrNull()?.let { loadModulesForAlbum(it.id) }
        } catch (e: Throwable) {
            Log.e(TAG, "loadAlbums failed", e)
            _errorMessage.value = e.message ?: "loadAlbums failed"
        }
    }

    fun selectAlbum(albumId: String) {
        if (_selectedAlbumId.value == albumId) return
        _selectedAlbumId.value = albumId
        viewModelScope.launch {
            _isLoading.value = true
            try {
                loadModulesForAlbum(albumId)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadModulesForAlbum(albumId: String) {
        try {
            Log.d(TAG, "loadModules($albumId)…")
            val album = withContext(Dispatchers.IO) { storeService.fetchAlbum(albumId) }
            _modulesInAlbum.value = album.modules
        } catch (e: Throwable) {
            Log.e(TAG, "loadModules failed", e)
            _errorMessage.value = e.message ?: "loadModules failed"
        }
    }

    // ── Search / filter setters ──────────────────────────────────────

    fun updateSearchText(text: String) {
        _searchText.value = text
    }

    fun setFilter(filter: ModuleFilter) {
        _selectedFilter.value = filter
    }

    fun setSelectedModule(id: String?) {
        _selectedModuleId.value = id
    }

    // ── Artwork gradient ─────────────────────────────────────────────

    fun extractArtworkColors(module: RemoteAudioModuleInfo) {
        val requestId = module.uniqueId
        _backgroundGradientColors.value = listOf(DEFAULT_ACCENT_TINT, TRANSPARENT)
        gradientOwnerUniqueId = requestId

        viewModelScope.launch {
            // 1. Determine the best image source
            val model: Any? = run {
                val albumBytes = module.albumArtworkBytes
                val plainBytes = module.artworkBytes
                val urlString = module.artworkFile?.url

                when {
                    albumBytes != null && albumBytes.isNotEmpty() -> albumBytes
                    plainBytes != null && plainBytes.isNotEmpty() -> plainBytes
                    !urlString.isNullOrBlank() -> urlString
                    else -> null
                }
            }

            if (model == null) {
                Log.d(TAG, "🎨 [$requestId] no artwork data → accent fallback")
                return@launch
            }

            // 2. Ask Coil for the image (it will use the memory/disk cache if the UI already loaded it)
            val request = ImageRequest.Builder(getApplication())
                .data(model)
                .allowHardware(false) // MUST be false to read pixels
                .size(64, 64)         // Shrink to 64x64 for instant processing
                .build()

            val result = imageLoader.execute(request)

            if (result !is SuccessResult) {
                Log.e(TAG, "🎨 [$requestId] Coil failed to load image")
                return@launch
            }

            val drawable = result.drawable
            if (drawable !is android.graphics.drawable.BitmapDrawable) {
                Log.e(TAG, "🎨 [$requestId] Coil returned non-bitmap drawable")
                return@launch
            }

            val bitmap = drawable.bitmap

            // 3. Extract using the Swift-equivalent Band Averaging logic
            val extracted: List<Long> = withContext(Dispatchers.Default) {
                try {
                    val count = 4
                    val width = bitmap.width
                    val height = bitmap.height
                    val bandHeight = height / count
                    val colors = mutableListOf<Long>()

                    // Loop through 4 horizontal bands
                    for (i in 0 until count) {
                        val y = i * bandHeight
                        val h = if (i == count - 1) height - y else bandHeight

                        if (h <= 0) continue

                        // Get the average color of this band
                        val avgColor = getAverageColor(bitmap, 0, y, width, h)

                        val r = android.graphics.Color.red(avgColor) / 255.0
                        val g = android.graphics.Color.green(avgColor) / 255.0
                        val b = android.graphics.Color.blue(avgColor) / 255.0

                        val brightness = (r + g + b) / 3.0

                        // Skip near-white or near-black bands
                        if (brightness > 0.92 || brightness < 0.05) {
                            continue
                        }

                        colors.add(avgColor.toLong() or 0xFF000000L)
                    }

                    // If filtering left us with nothing, fall back to raw averages
                    if (colors.isEmpty()) {
                        for (i in 0 until count) {
                            val y = i * bandHeight
                            val h = if (i == count - 1) height - y else bandHeight
                            if (h <= 0) continue
                            val avgColor = getAverageColor(bitmap, 0, y, width, h)
                            colors.add(avgColor.toLong() or 0xFF000000L)
                        }
                    }

                    colors
                } catch (e: Throwable) {
                    Log.e(TAG, "🎨 [$requestId] extraction failed", e)
                    emptyList()
                }
            }

            // 4. Drop stale results
            if (gradientOwnerUniqueId != requestId) {
                Log.d(TAG, "🎨 [$requestId] stale → dropped")
                return@launch
            }

            if (extracted.isEmpty()) return@launch

            _backgroundGradientColors.value = extracted
            Log.d(TAG, "[$requestId] published ${extracted.size} colors")
        }
    }

    // Helper to compute the average color of a specific rectangle in a Bitmap
    private fun getAverageColor(
        bitmap: android.graphics.Bitmap,
        x: Int,
        y: Int,
        w: Int,
        h: Int
    ): Int {
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, x, y, w, h)

        var r = 0L
        var g = 0L
        var b = 0L

        for (pixel in pixels) {
            r += android.graphics.Color.red(pixel)
            g += android.graphics.Color.green(pixel)
            b += android.graphics.Color.blue(pixel)
        }

        val total = pixels.size.toLong()
        if (total == 0L) return android.graphics.Color.BLACK

        return android.graphics.Color.rgb(
            (r / total).toInt(),
            (g / total).toInt(),
            (b / total).toInt()
        )
    }

    fun resetArtworkColors() {
        gradientOwnerUniqueId = null
        _backgroundGradientColors.value = listOf(DEFAULT_ACCENT_TINT, TRANSPARENT)
    }

    // ── Install + progress ──────────────────────────────────────────

    fun installModule(id: String) {
        trackingJob?.cancel()
        installJob?.cancel()
        updateModuleProgress(id, 0.0)

        val listener = StoreDownloadProgressListener(uniqueIdFilter = id)

        installJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val localPath = storeService.installModule(
                    moduleId = id,
                    progressListener = listener,
                )
                Log.d(TAG, "installModule('$id'): localPath=$localPath")
                withContext(Dispatchers.Main) {
                    trackingJob?.cancel()
                    updateModuleStatusToInstalled(id)
                }
            } catch (e: Throwable) {
                Log.e(TAG, "installModule('$id') failed", e)
                withContext(Dispatchers.Main) {
                    trackingJob?.cancel()
                    _errorMessage.value = e.message ?: "install failed"
                    resetModuleStatus(id)
                }
            }
        }

        trackingJob = viewModelScope.launch {
            while (true) {
                delay(250.milliseconds)
                val live = withContext(Dispatchers.IO) {
                    storeService.getCachedCatalog()
                }
                val liveModule = live.firstOrNull { it.uniqueId == id } ?: continue
                when (val s = liveModule.status) {
                    is ModuleStatus.Downloading -> updateModuleProgress(id, s.progress)
                    else -> Unit
                }
                if (liveModule.isInstalled) {
                    updateModuleStatusToInstalled(id)
                    break
                }
                if (installJob?.isActive == false) {
                    break
                }
            }
        }
    }

    private fun updateModuleProgress(id: String, progress: Double) {
        val newStatus = ModuleStatus.Downloading(progress)
        _availableModules.value = _availableModules.value.map {
            if (it.uniqueId == id) it.copy(status = newStatus) else it
        }
        _modulesInAlbum.value = _modulesInAlbum.value.map {
            if (it.uniqueId == id) it.copy(status = newStatus) else it
        }
    }

    private fun updateModuleStatusToInstalled(id: String) {
        _availableModules.value = _availableModules.value.map {
            if (it.uniqueId == id) {
                it.copy(status = ModuleStatus.Installed, isInstalled = true)
            } else it
        }
        _modulesInAlbum.value = _modulesInAlbum.value.map {
            if (it.uniqueId == id) {
                it.copy(status = ModuleStatus.Installed, isInstalled = true)
            } else it
        }
    }

    private fun resetModuleStatus(id: String) {
        _availableModules.value = _availableModules.value.map {
            if (it.uniqueId == id) it.copy(status = ModuleStatus.Idle) else it
        }
        _modulesInAlbum.value = _modulesInAlbum.value.map {
            if (it.uniqueId == id) it.copy(status = ModuleStatus.Idle) else it
        }
    }

    // ── Filter helper ───────────────────────────────────────────────

    private fun filterModules(
        modules: List<RemoteAudioModuleInfo>,
        searchText: String,
        filter: ModuleFilter,
    ): List<RemoteAudioModuleInfo> {
        val q = searchText.trim().lowercase()
        return modules.filter { m ->
            when (filter) {
                ModuleFilter.ALL -> Unit
                ModuleFilter.INSTALLED -> if (!m.isInstalled) return@filter false
                ModuleFilter.NOT_INSTALLED -> if (m.isInstalled) return@filter false
                ModuleFilter.DOWNLOADING ->
                    if (m.status !is ModuleStatus.Downloading) return@filter false
            }
            if (q.isEmpty()) return@filter true
            if (m.displayTitle.lowercase().contains(q)) return@filter true
            if (m.uniqueId.lowercase().contains(q)) return@filter true
            m.contributor?.let { if (it.lowercase().contains(q)) return@filter true }
            if (m.features.features.any { it.lowercase().contains(q) }) return@filter true
            false
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        trackingJob?.cancel()
        installJob?.cancel()
        runCatching { storeService.destroy() }
    }

    companion object {
        private const val DEFAULT_ACCENT_TINT = 0x1A_00_00_00L
        private const val TRANSPARENT = 0x00000000L

        private fun parseIso(s: String): Long =
            runCatching { java.time.Instant.parse(s).toEpochMilli() }.getOrDefault(0L)
    }
}