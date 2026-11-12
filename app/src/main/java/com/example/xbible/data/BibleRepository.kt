package com.example.xbible.data

import android.app.Application
import android.system.Os
import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import uniffi.xbible_engine.AudioEngine
import uniffi.xbible_engine.AudioModuleInfo
import uniffi.xbible_engine.AudioNode
import uniffi.xbible_engine.EntityRef
import uniffi.xbible_engine.EntitySummary
import uniffi.xbible_engine.EventDetail
import uniffi.xbible_engine.FreeBibleBook
import uniffi.xbible_engine.FreeBibleEngine
import uniffi.xbible_engine.FreeBibleReader
import uniffi.xbible_engine.FreeBibleStore
import uniffi.xbible_engine.FreeBibleTaskStatus
import uniffi.xbible_engine.GroupDetail
import uniffi.xbible_engine.InstalledTranslation
import uniffi.xbible_engine.LanguageFacet
import uniffi.xbible_engine.ModuleMetadata
import uniffi.xbible_engine.Period
import uniffi.xbible_engine.PersonDetail
import uniffi.xbible_engine.PlaceDetail
import uniffi.xbible_engine.PlaybackState
import uniffi.xbible_engine.ReaderChapter
import uniffi.xbible_engine.RepeatMode
import uniffi.xbible_engine.StoreItem
import uniffi.xbible_engine.StoreShelf
import uniffi.xbible_engine.TimelineData
import uniffi.xbible_engine.VerseRange
import uniffi.xbible_engine.XBibleEngine
import java.io.File
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "BibleRepo"

/**
 * Repository that abstracts the XBibleEngine (Rust) and provides data to the
 * ViewModels.
 *
 * ## Threading contract
 *
 * The Rust engine is **not internally synchronized**. All calls into it must
 * happen on a single thread. Every public method here is `suspend` and
 * dispatches onto [engineDispatcher] — a dedicated single-thread executor.
 *
 * ## Logging
 *
 * Platform logging is initialised by the Rust side inside `XBibleEngine::new()`
 * (see `initialization.rs`). A free `initLogging()` FFI export also exists but
 * is not called here — the constructor is sufficient.
 *
 * ## FFI surface (as generated)
 *
 *  - `XBibleEngine`     → `audioEngine()`, `freebible()`
 *  - `AudioEngine`      → playback + audio-module metadata
 *  - `FreeBibleEngine`  → `setDbPath()`, `store()`, `reader()`
 *  - `FreeBibleStore`   → catalog / shelves / installs / tasks
 *  - `FreeBibleReader`  → books / chapters / verses / entities
 *
 * SWORD-family methods (getBibleModules, getBooks, getChapterContent, search,
 * lookupDictionary, …) are not part of the current FFI and are therefore not
 * wrapped here. Re-export them from Rust if the app needs them back.
 */
@Singleton
class BibleRepository @Inject constructor(private val application: Application) {

    private val engineDispatcher: CoroutineDispatcher =
        Executors.newSingleThreadExecutor { r ->
            Thread(r, "xbible-engine").apply {
                isDaemon = true
                priority = Thread.NORM_PRIORITY
            }
        }.asCoroutineDispatcher()

    @Volatile private var engine: XBibleEngine? = null
    @Volatile private var audioEngine: AudioEngine? = null
    @Volatile private var freeBible: FreeBibleEngine? = null
    @Volatile private var freeBibleStore: FreeBibleStore? = null
    @Volatile private var freeBibleReader: FreeBibleReader? = null

    private suspend fun <T> onEngine(block: () -> T): T =
        withContext(engineDispatcher) { block() }

    // ══════════════════════════════════════════════════════════════════
    // Lifecycle
    // ══════════════════════════════════════════════════════════════════

    suspend fun initialize(): Result<Unit> = onEngine {
        try {
            if (engine != null) {
                Log.d(TAG, "initialize: already initialized")
                return@onEngine Result.success(Unit)
            }

            System.loadLibrary("xbible_engine")

            val filesDir = application.filesDir.absolutePath
            val cacheDir = application.cacheDir.absolutePath

            Os.setenv("HOME", filesDir, true)
            Os.setenv("TMPDIR", cacheDir, true)
            Os.setenv("SWORD_PATH", filesDir, true)

            File(filesDir, "mods.d").mkdirs()
            File(filesDir, "modules").mkdirs()

            // Logging is initialised inside XBibleEngine::new() on the Rust
            // side. No separate initLogging() FFI call needed.
            val newEngine = XBibleEngine()

            // Grab the FreeBible sub-tree.
            val fb = newEngine.freebible()

            // Resolve the DB path from the application context so every
            // code path that uses getDatabasePath("xbible.db") agrees on it.
            val dbPath = application.getDatabasePath("xbible.db").absolutePath
            Log.d(TAG, "initialize: opening FreeBible DB at $dbPath")
            Log.d(TAG, "initialize: file exists before open = ${File(dbPath).exists()}")
            if (File(dbPath).exists()) {
                Log.d(TAG, "initialize: file size before open = ${File(dbPath).length()} bytes")
            }

            // IMPORTANT: setDbPath must run before any read queries.
            // It opens the connection, runs migrations, and attaches the
            // shared Arc to both store and reader.
            fb.setDbPath(dbPath)

            // Now grab the handles. These wrap the same underlying Rust
            // objects that fb.setDbPath just configured.
            val fbStore = fb.store()
            val fbReader = fb.reader()

            // ── Sanity check: what does the reader actually see? ──────
            val installed = fbReader.getInstalledTranslations()
            Log.d(
                TAG,
                "initialize: reader sees ${installed.size} installed translation(s) right after setDbPath",
            )
            installed.forEach { t ->
                Log.d(TAG, "initialize:   - ${t.id} (${t.shortName}) \"${t.name}\"")
            }

            // Also verify the store can query the same DB.
            val languages = fbStore.languages()
            Log.d(TAG, "initialize: store sees ${languages.size} language facet(s)")

            freeBibleReader = fbReader
            freeBibleStore = fbStore
            freeBible = fb
            engine = newEngine

            // AudioEngine is a real handle on XBibleEngine.
            audioEngine = newEngine.audioEngine()

            Result.success(Unit)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize XBibleEngine", e)
            Result.failure(e)
        }
    }

    fun isInitialized(): Boolean = engine != null

    // ══════════════════════════════════════════════════════════════════
    // Audio engine
    // ══════════════════════════════════════════════════════════════════

    suspend fun getAudioModules(): List<AudioModuleInfo> = onEngine {
        audioEngine?.getAudioModules() ?: emptyList()
    }

    suspend fun getAudioModulesPath(): String = onEngine {
        audioEngine?.getAudioModulesPath() ?: ""
    }

    suspend fun getAudioNavigationTree(): AudioNode? = onEngine {
        audioEngine?.getNavigationTree()
    }

    suspend fun getAudioPlaybackState(): PlaybackState? = onEngine {
        audioEngine?.getPlaybackState()
    }

    suspend fun peekAudioModuleMetadata(filePath: String): ModuleMetadata? = onEngine {
        runCatching { audioEngine?.peekModuleMetadata(filePath) }.getOrNull()
    }

    suspend fun loadAudioModule(filePath: String): ByteArray? = onEngine {
        runCatching { audioEngine?.loadAudioModule(filePath) }.getOrNull()
    }

    suspend fun getAudioModuleArtwork(filePath: String, kind: String): ByteArray? = onEngine {
        runCatching { audioEngine?.getModuleArtwork(filePath, kind) }.getOrNull()
    }

    suspend fun findActiveAudioNodeId(timeMs: Long): String? = onEngine {
        audioEngine?.findActiveNodeId(timeMs)
    }

    suspend fun audioTogglePlayback() = onEngine { audioEngine?.togglePlayback(); Unit }
    suspend fun audioStop() = onEngine { audioEngine?.stop(); Unit }
    suspend fun audioSeekToTime(targetMs: Long) = onEngine { audioEngine?.seekToTime(targetMs); Unit }
    suspend fun audioSeekToChapter(chapterId: String) = onEngine { audioEngine?.seekToChapter(chapterId); Unit }
    suspend fun audioSkipForward(seconds: Long) = onEngine { audioEngine?.skipForward(seconds); Unit }
    suspend fun audioSkipBackward(seconds: Long) = onEngine { audioEngine?.skipBackward(seconds); Unit }
    suspend fun audioSetRepeatMode(mode: RepeatMode) = onEngine { audioEngine?.setRepeatMode(mode); Unit }

    // ══════════════════════════════════════════════════════════════════
    // FreeBible store
    // ══════════════════════════════════════════════════════════════════

    suspend fun refreshFreeBibleCatalog() = onEngine {
        freeBibleStore?.refresh(); Unit
    }

    suspend fun freeBibleHomeFeed(languageCode: String?): List<StoreShelf> = onEngine {
        freeBibleStore?.homeFeed(languageCode) ?: emptyList()
    }

    suspend fun freeBibleLanguages(): List<LanguageFacet> = onEngine {
        freeBibleStore?.languages() ?: emptyList()
    }

    suspend fun searchFreeBibleStore(query: String): List<StoreItem> = onEngine {
        freeBibleStore?.search(query) ?: emptyList()
    }

    suspend fun freeBibleStoreItem(id: String, kind: String): StoreItem? = onEngine {
        freeBibleStore?.item(id, kind)
    }

    suspend fun getFreeBibleShelf(
        shelfId: String,
        language: String?,
        offset: UInt,
        limit: UInt,
    ): List<StoreItem> = onEngine {
        freeBibleStore?.getShelf(shelfId, language, offset, limit) ?: emptyList()
    }

    suspend fun getFreeBibleShelfTotal(shelfId: String, language: String?): UInt = onEngine {
        freeBibleStore?.shelfTotal(shelfId, language) ?: 0u
    }

    suspend fun installFreeBibleTranslation(translationId: String): String = onEngine {
        freeBibleStore?.installTranslation(translationId) ?: ""
    }

    suspend fun uninstallFreeBibleTranslation(translationId: String): String = onEngine {
        freeBibleStore?.uninstallTranslation(translationId) ?: ""
    }

    suspend fun installFreeBibleEntitySet(datasetId: String, kind: String): String = onEngine {
        freeBibleStore?.installEntitySet(datasetId, kind) ?: ""
    }

    suspend fun listFreeBibleTasks(): List<FreeBibleTaskStatus> = onEngine {
        freeBibleStore?.listTasks() ?: emptyList()
    }

    suspend fun pollFreeBibleTask(taskId: String): FreeBibleTaskStatus? = onEngine {
        freeBibleStore?.pollTask(taskId)
    }

    suspend fun cancelFreeBibleTask(taskId: String) = onEngine {
        freeBibleStore?.cancelTask(taskId); Unit
    }

    // ══════════════════════════════════════════════════════════════════
    // FreeBible reader — reading page APIs
    // ══════════════════════════════════════════════════════════════════

    suspend fun getFreeBibleInstalledTranslations(): List<InstalledTranslation> = onEngine {
        freeBibleReader?.getInstalledTranslations() ?: emptyList()
    }

    suspend fun isFreeBibleInstalled(translationId: String): Boolean = onEngine {
        freeBibleReader?.isInstalled(translationId) ?: false
    }

    suspend fun getFreeBibleBooks(translationId: String): List<FreeBibleBook> = onEngine {
        freeBibleReader?.getBooks(translationId) ?: emptyList()
    }

    suspend fun getFirstFreeBibleBook(translationId: String): FreeBibleBook? = onEngine {
        freeBibleReader?.getBooks(translationId)?.firstOrNull()
    }

    suspend fun getFreeBibleBook(translationId: String, bookId: String): FreeBibleBook? =
        onEngine {
            freeBibleReader?.getBooks(translationId)?.firstOrNull { it.id == bookId }
        }

    suspend fun readFreeBibleChapter(
        translationId: String,
        bookId: String,
        chapter: Int,
    ): ReaderChapter? = onEngine {
        freeBibleReader?.readChapter(translationId, bookId, chapter)
    }

    suspend fun getFreeBibleVerse(
        translationId: String,
        bookId: String,
        chapter: Int,
        verse: Int,
    ): String? = onEngine {
        freeBibleReader?.getVerse(translationId, bookId, chapter, verse)
    }

    suspend fun getFreeBibleVerseRange(
        translationId: String,
        bookId: String,
        chapter: Int,
        startVerse: Int,
        endVerse: Int,
    ): List<Pair<Int, String>> = onEngine {
        val reader = freeBibleReader ?: return@onEngine emptyList()
        (startVerse..endVerse).mapNotNull { v ->
            reader.getVerse(translationId, bookId, chapter, v)?.let { v to it }
        }
    }

    suspend fun getFreeBibleNextChapter(
        translationId: String,
        bookId: String,
        chapter: Int,
    ): Triple<String, Int, String>? = onEngine {
        val reader = freeBibleReader ?: return@onEngine null
        val books = reader.getBooks(translationId)
        val idx = books.indexOfFirst { it.id == bookId }
        if (idx < 0) return@onEngine null
        val current = books[idx]
        if (chapter < current.numberOfChapters) {
            return@onEngine Triple(bookId, chapter + 1, current.name)
        }
        val next = books.getOrNull(idx + 1) ?: return@onEngine null
        Triple(next.id, 1, next.name)
    }

    suspend fun getFreeBiblePrevChapter(
        translationId: String,
        bookId: String,
        chapter: Int,
    ): Triple<String, Int, String>? = onEngine {
        val reader = freeBibleReader ?: return@onEngine null
        val books = reader.getBooks(translationId)
        val idx = books.indexOfFirst { it.id == bookId }
        if (idx < 0) return@onEngine null
        if (chapter > 1) {
            return@onEngine Triple(bookId, chapter - 1, books[idx].name)
        }
        if (idx == 0) return@onEngine null
        val prev = books[idx - 1]
        Triple(prev.id, prev.numberOfChapters, prev.name)
    }

    // ══════════════════════════════════════════════════════════════════
    // FreeBible reader — entities
    // ══════════════════════════════════════════════════════════════════

    suspend fun isFreeBibleEntitySetInstalled(datasetId: String, kind: String): Boolean =
        onEngine { freeBibleReader?.isEntitySetInstalled(datasetId, kind) ?: false }

    suspend fun listFreeBiblePeople(
        datasetId: String, offset: Int, limit: Int, query: String?,
    ): List<EntitySummary> = onEngine {
        freeBibleReader?.listPeople(datasetId, offset, limit, query) ?: emptyList()
    }

    suspend fun listFreeBibleEvents(
        datasetId: String, offset: Int, limit: Int, query: String?,
    ): List<EntitySummary> = onEngine {
        freeBibleReader?.listEvents(datasetId, offset, limit, query) ?: emptyList()
    }

    suspend fun listFreeBiblePlaces(
        datasetId: String, offset: Int, limit: Int, query: String?,
    ): List<EntitySummary> = onEngine {
        freeBibleReader?.listPlaces(datasetId, offset, limit, query) ?: emptyList()
    }

    suspend fun listFreeBibleGroups(
        datasetId: String, offset: Int, limit: Int, query: String?,
    ): List<EntitySummary> = onEngine {
        freeBibleReader?.listGroups(datasetId, offset, limit, query) ?: emptyList()
    }

    suspend fun getFreeBiblePerson(datasetId: String, id: String): PersonDetail? = onEngine {
        freeBibleReader?.getPerson(datasetId, id)
    }

    suspend fun getFreeBibleEvent(datasetId: String, id: String): EventDetail? = onEngine {
        freeBibleReader?.getEvent(datasetId, id)
    }

    suspend fun getFreeBiblePlace(datasetId: String, id: String): PlaceDetail? = onEngine {
        freeBibleReader?.getPlace(datasetId, id)
    }

    suspend fun getFreeBibleGroup(datasetId: String, id: String): GroupDetail? = onEngine {
        freeBibleReader?.getGroup(datasetId, id)
    }

    suspend fun searchFreeBibleEntities(
        datasetId: String, query: String,
    ): List<EntitySummary> = onEngine {
        freeBibleReader?.searchEntities(datasetId, query) ?: emptyList()
    }

    suspend fun freeBibleEntitiesForVerse(
        datasetId: String, book: String, chapter: Int, verse: Int,
    ): List<EntityRef> = onEngine {
        freeBibleReader?.getEntitiesForVerse(datasetId, book, chapter, verse) ?: emptyList()
    }

    suspend fun getFreeBibleVerseRefs(
        datasetId: String, kind: String, id: String,
    ): List<VerseRange> = onEngine {
        freeBibleReader?.getVerseRefs(datasetId, kind, id) ?: emptyList()
    }

    suspend fun getFreeBibleRelated(
        datasetId: String, kind: String, id: String, relation: String?,
    ): List<EntityRef> = onEngine {
        freeBibleReader?.getRelated(datasetId, kind, id, relation) ?: emptyList()
    }

    suspend fun getFreeBibleEntitiesForChapter(
        datasetId: String,
        book: String,
        chapter: Int,
        verseNumbers: List<Int>,
    ): List<EntityRef> = onEngine {
        val reader = freeBibleReader ?: return@onEngine emptyList()
        val seen = LinkedHashMap<String, EntityRef>()   // kind:id → ref, preserves order
        for (v in verseNumbers) {
            reader.getEntitiesForVerse(datasetId, book, chapter, v).forEach { ref ->
                seen.putIfAbsent("${ref.kind}:${ref.id}", ref)
            }
        }
        seen.values.toList()
    }

    // ══════════════════════════════════════════════════════════════════
    // Timeline (standalone object, no engine dependency)
    // ══════════════════════════════════════════════════════════════════

    suspend fun getTimelineData(): List<Period> = onEngine {
        TimelineData().use { it.getData() }
    }
}