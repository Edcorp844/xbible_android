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
import uniffi.xbible_engine.CommentaryChapter
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
import uniffi.xbible_engine.SearchFilter
import uniffi.xbible_engine.SearchProgress
import uniffi.xbible_engine.SearchProgressListener
import uniffi.xbible_engine.SearchResults
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
 * Repository that abstracts the XBibleEngine (Rust) and provides data to
 * the ViewModels.
 *
 * ## Threading contract
 *
 * The Rust engine is **not internally synchronized**. Every call into it
 * must happen on a single thread. All public methods here are `suspend` and
 * dispatch onto [engineDispatcher] — a dedicated single-thread executor.
 */
@Singleton
class BibleRepository @Inject constructor(private val application: Application) {

    // ══════════════════════════════════════════════════════════════════
    // Threading
    // ══════════════════════════════════════════════════════════════════

    private val engineDispatcher: CoroutineDispatcher =
        Executors.newSingleThreadExecutor { r ->
            Thread(r, "xbible-engine").apply {
                isDaemon = true
                priority = Thread.NORM_PRIORITY
            }
        }.asCoroutineDispatcher()

    private suspend fun <T> onEngine(block: () -> T): T =
        withContext(engineDispatcher) { block() }

    // ══════════════════════════════════════════════════════════════════
    // Engine handles
    // ══════════════════════════════════════════════════════════════════

    @Volatile
    private var engine: XBibleEngine? = null
    @Volatile
    private var audioEngine: AudioEngine? = null
    @Volatile
    private var freeBible: FreeBibleEngine? = null
    @Volatile
    private var freeBibleStore: FreeBibleStore? = null
    @Volatile
    private var freeBibleReader: FreeBibleReader? = null

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

            val newEngine = XBibleEngine()
            val fb = newEngine.freebible()

            val dbPath = application.getDatabasePath("xbible.db").absolutePath
            Log.d(TAG, "initialize: opening FreeBible DB at $dbPath")
            if (File(dbPath).exists()) {
                Log.d(TAG, "initialize: file size before open = ${File(dbPath).length()} bytes")
            }

            fb.setDbPath(dbPath)

            val fbStore = fb.store()
            val fbReader = fb.reader()

            val installed = fbReader.getInstalledTranslations()
            Log.d(TAG, "initialize: reader sees ${installed.size} installed translation(s)")

            freeBibleReader = fbReader
            freeBibleStore = fbStore
            freeBible = fb
            engine = newEngine
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
    suspend fun audioSeekToTime(targetMs: Long) =
        onEngine { audioEngine?.seekToTime(targetMs); Unit }

    suspend fun audioSeekToChapter(chapterId: String) =
        onEngine { audioEngine?.seekToChapter(chapterId); Unit }

    suspend fun audioSkipForward(seconds: Long) =
        onEngine { audioEngine?.skipForward(seconds); Unit }

    suspend fun audioSkipBackward(seconds: Long) =
        onEngine { audioEngine?.skipBackward(seconds); Unit }

    suspend fun audioSetRepeatMode(mode: RepeatMode) =
        onEngine { audioEngine?.setRepeatMode(mode); Unit }

    // ══════════════════════════════════════════════════════════════════
    // FreeBible — catalog
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

    suspend fun getInstalledCommentaries(): List<StoreItem> = onEngine {
        val store = freeBibleStore ?: return@onEngine emptyList()
        val byId = LinkedHashMap<String, StoreItem>()
        for (shelf in store.homeFeed(null)) {
            for (item in shelf.items) {
                if (item.kind == "commentary" && item.installed) {
                    byId.putIfAbsent(item.id, item)
                }
            }
        }
        byId.values.sortedBy { it.name }
    }

    // ══════════════════════════════════════════════════════════════════
    // FreeBible — install / uninstall
    // ══════════════════════════════════════════════════════════════════

    suspend fun installFreeBibleTranslation(translationId: String): String = onEngine {
        require(!translationId.contains(':')) {
            "$translationId looks like a store item id (dataset:kind), not a translation id — " +
                    "call installFreeBibleEntitySet instead"
        }
        freeBibleStore?.installTranslation(translationId) ?: ""
    }

    suspend fun uninstallFreeBibleTranslation(translationId: String): String = onEngine {
        require(!translationId.contains(':')) {
            "$translationId looks like a store item id (dataset:kind), not a translation id — " +
                    "there is no uninstall_entity_set yet"
        }
        freeBibleStore?.uninstallTranslation(translationId) ?: ""
    }

    suspend fun installFreeBibleEntitySet(datasetId: String, kind: String): String = onEngine {
        freeBibleStore?.installEntitySet(datasetId, kind) ?: ""
    }

    suspend fun installFreeBibleCommentary(commentaryId: String): String = onEngine {
        freeBibleStore?.installCommentary(commentaryId) ?: ""
    }

    suspend fun uninstallFreeBibleCommentary(commentaryId: String): String = onEngine {
        freeBibleStore?.uninstallCommentary(commentaryId) ?: ""
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
    // FreeBible — canon selection
    // ══════════════════════════════════════════════════════════════════

    /**
     * Set which canonical family a translation shows.
     *
     * Valid canon keys are `"protestant"`, `"catholic"`, `"orthodox"`,
     * `"ethiopian"`, `"all"`. Anything else throws from the Rust side.
     *
     * The change is reflected in the next [getFreeBibleBooks] call —
     * books whose `book_metadata.section` is not visible under the
     * chosen canon disappear from the list, but are not deleted.
     */
    suspend fun setSelectedCanon(translationId: String, canon: String) = onEngine {
        freeBibleReader?.setSelectedCanon(translationId, canon); Unit
    }

    /**
     * True if this translation was installed before `book_metadata.section`
     * existed (v8 migration). The UI should show an "Update Required"
     * banner and offer [rebuildSections].
     */
    suspend fun needsSectionUpdate(translationId: String): Boolean = onEngine {
        freeBibleReader?.needsSectionUpdate(translationId) ?: false
    }

    /**
     * Fill in `book_metadata.section` for every book of a translation
     * using the current section lookup. Returns the number of books
     * updated. No-op for books the lookup does not know — they stay
     * tagged `"unknown"` and the UI shows them under "Other".
     */
    suspend fun rebuildSections(translationId: String): Int = onEngine {
        freeBibleReader?.rebuildSections(translationId) ?: 0
    }

    // ══════════════════════════════════════════════════════════════════
    // FreeBible — reading (translations)
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
    // FreeBible — reading (commentary)
    // ══════════════════════════════════════════════════════════════════

    suspend fun isCommentaryInstalled(commentaryId: String): Boolean = onEngine {
        freeBibleReader?.isCommentaryInstalled(commentaryId) ?: false
    }

    suspend fun readCommentaryChapter(
        commentaryId: String,
        bookId: String,
        chapter: Int,
    ): CommentaryChapter? = onEngine {
        freeBibleReader?.readCommentaryChapter(commentaryId, bookId, chapter)
    }

    // ══════════════════════════════════════════════════════════════════
    // FreeBible — entities
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

    // ══════════════════════════════════════════════════════════════════
    // Search
    // ══════════════════════════════════════════════════════════════════

    /**
     * Performs a search using the Rust FTS5 / regex engine.
     *
     * @param query The user's raw query (e.g., "shepherd", "JHN 3:16", "H430").
     * @param translationId Optional translation ID to limit the search to.
     * @param limit Maximum number of hits to return.
     * @param filter Book filter and search mode. `SearchFilter()` with empty
     *   `bookIds` and `mode = MultiWord` searches everything.
     * @param onProgress Optional callback for search progress.
     */
    suspend fun searchBible(
        query: String,
        translationId: String? = null,
        limit: Int = 200,
        filter: SearchFilter = SearchFilter(
            bookIds = emptyList(),
            mode = uniffi.xbible_engine.SearchMode.MULTI_WORD
        ),
        onProgress: (SearchProgress) -> Unit = {}
    ): SearchResults = onEngine {
        val reader = freeBibleReader ?: return@onEngine SearchResults(query, emptyList(), 0, 0)

        val listener = object : SearchProgressListener {
            override fun onProgress(progress: SearchProgress) {
                onProgress(progress)
            }
        }

        reader.search(
            query = query,
            translationId = translationId,
            limit = limit,
            filter = filter,
            listener = listener
        )
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
        val seen = LinkedHashMap<String, EntityRef>()
        for (v in verseNumbers) {
            reader.getEntitiesForVerse(datasetId, book, chapter, v).forEach { ref ->
                seen.putIfAbsent("${ref.kind}:${ref.id}", ref)
            }
        }
        seen.values.toList()
    }

    // ══════════════════════════════════════════════════════════════════
    // Timeline
    // ══════════════════════════════════════════════════════════════════

    suspend fun getTimelineData(): List<Period> = onEngine {
        TimelineData().use { it.getData() }
    }
}