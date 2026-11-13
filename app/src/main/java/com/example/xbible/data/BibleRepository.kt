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
 * Never bypass this: calling a `FreeBibleReader` / `FreeBibleStore` method
 * from a Compose thread while another call is in flight on the engine thread
 * is a data race, even though UniFFI won't stop you.
 *
 * ## Logging
 *
 * Platform logging is initialized by the Rust side inside `XBibleEngine::new()`.
 * A free `initLogging()` FFI export also exists but is not called here —
 * the constructor is sufficient.
 *
 * ## FFI surface
 *
 *  - `XBibleEngine`     → `audioEngine()`, `freebible()`
 *  - `AudioEngine`      → playback + audio-module metadata
 *  - `FreeBibleEngine`  → `setDbPath()`, `store()`, `reader()`
 *  - `FreeBibleStore`   → catalog / shelves / installs / tasks
 *  - `FreeBibleReader`  → books / chapters / verses / entities / commentary
 *
 * SWORD-family methods (getBibleModules, getBooks, getChapterContent,
 * search, lookupDictionary, …) are not part of the current FFI and are
 * therefore not wrapped here. Re-export them from Rust if the app needs
 * them back.
 */
@Singleton
class BibleRepository @Inject constructor(private val application: Application) {

    // ══════════════════════════════════════════════════════════════════
    // Threading
    // ══════════════════════════════════════════════════════════════════

    /**
     * Single-thread executor that owns every Rust call. The engine is not
     * internally synchronized; this dispatcher is the only legal thread
     * to touch it from.
     */
    private val engineDispatcher: CoroutineDispatcher =
        Executors.newSingleThreadExecutor { r ->
            Thread(r, "xbible-engine").apply {
                isDaemon = true
                priority = Thread.NORM_PRIORITY
            }
        }.asCoroutineDispatcher()

    /** Runs [block] on the engine thread. Every public method below uses this. */
    private suspend fun <T> onEngine(block: () -> T): T =
        withContext(engineDispatcher) { block() }

    // ══════════════════════════════════════════════════════════════════
    // Engine handles
    // ══════════════════════════════════════════════════════════════════

    @Volatile private var engine: XBibleEngine? = null
    @Volatile private var audioEngine: AudioEngine? = null
    @Volatile private var freeBible: FreeBibleEngine? = null
    @Volatile private var freeBibleStore: FreeBibleStore? = null
    @Volatile private var freeBibleReader: FreeBibleReader? = null

    // ══════════════════════════════════════════════════════════════════
    // Lifecycle
    // ══════════════════════════════════════════════════════════════════

    /**
     * Initializes the engine and opens the DB at the app's canonical path.
     *
     * Idempotent: calling twice is a no-op after the first successful call.
     *
     * Steps:
     *  1. `System.loadLibrary("xbible_engine")` — first time only.
     *  2. Set HOME / TMPDIR / SWORD_PATH to the app's files dir.
     *  3. Create the SWORD layout directories (`mods.d/`, `modules/`).
     *  4. Construct the top-level [XBibleEngine] (which also initializes
     *     Rust-side logging).
     *  5. Resolve `xbible.db` under `application.getDatabasePath(...)`,
     *     which is the same path every other caller will compute.
     *  6. `fb.setDbPath(dbPath)` — opens the connection, runs migrations,
     *     attaches the shared `Arc<Db>` to both store and reader.
     *  7. Grab the store, reader, and audio handles.
     */
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

            // Logging is initialized inside XBibleEngine::new() on the
            // Rust side. No separate initLogging() FFI call needed.
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

            // Grab the handles. These wrap the same underlying Rust
            // objects that fb.setDbPath just configured.
            val fbStore = fb.store()
            val fbReader = fb.reader()

            // Sanity check: what does the reader actually see?
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

    /** True once [initialize] has succeeded at least once. */
    fun isInitialized(): Boolean = engine != null

    // ══════════════════════════════════════════════════════════════════
    // Audio engine
    // ══════════════════════════════════════════════════════════════════

    // ── Audio modules ──────────────────────────────────────────────

    /** Every stamped audio module found under the modules directory. */
    suspend fun getAudioModules(): List<AudioModuleInfo> = onEngine {
        audioEngine?.getAudioModules() ?: emptyList()
    }

    /** Path that holds the installed audio modules. */
    suspend fun getAudioModulesPath(): String = onEngine {
        audioEngine?.getAudioModulesPath() ?: ""
    }

    /** Hierarchical navigation tree for the currently loaded module. */
    suspend fun getAudioNavigationTree(): AudioNode? = onEngine {
        audioEngine?.getNavigationTree()
    }

    /** Current playback state (time, text anchor, repeat mode, …). */
    suspend fun getAudioPlaybackState(): PlaybackState? = onEngine {
        audioEngine?.getPlaybackState()
    }

    /** Reads a module's stamped metadata without loading audio. */
    suspend fun peekAudioModuleMetadata(filePath: String): ModuleMetadata? = onEngine {
        runCatching { audioEngine?.peekModuleMetadata(filePath) }.getOrNull()
    }

    /** Loads an audio module (audio bytes + metadata) into the engine. */
    suspend fun loadAudioModule(filePath: String): ByteArray? = onEngine {
        runCatching { audioEngine?.loadAudioModule(filePath) }.getOrNull()
    }

    /** Reads artwork bytes directly out of the archive.
     *  `kind` is `"server"` | `"contributor"` | `"album"`. */
    suspend fun getAudioModuleArtwork(filePath: String, kind: String): ByteArray? = onEngine {
        runCatching { audioEngine?.getModuleArtwork(filePath, kind) }.getOrNull()
    }

    /** Active navigation node id at [timeMs], if any. */
    suspend fun findActiveAudioNodeId(timeMs: Long): String? = onEngine {
        audioEngine?.findActiveNodeId(timeMs)
    }

    // ── Playback control ───────────────────────────────────────────

    suspend fun audioTogglePlayback() = onEngine { audioEngine?.togglePlayback(); Unit }
    suspend fun audioStop() = onEngine { audioEngine?.stop(); Unit }
    suspend fun audioSeekToTime(targetMs: Long) = onEngine { audioEngine?.seekToTime(targetMs); Unit }
    suspend fun audioSeekToChapter(chapterId: String) = onEngine { audioEngine?.seekToChapter(chapterId); Unit }
    suspend fun audioSkipForward(seconds: Long) = onEngine { audioEngine?.skipForward(seconds); Unit }
    suspend fun audioSkipBackward(seconds: Long) = onEngine { audioEngine?.skipBackward(seconds); Unit }
    suspend fun audioSetRepeatMode(mode: RepeatMode) = onEngine { audioEngine?.setRepeatMode(mode); Unit }

    // ══════════════════════════════════════════════════════════════════
    // FreeBible — catalog
    // ══════════════════════════════════════════════════════════════════

    /** Downloads and stores the current manifest. Blocking on the Rust side. */
    suspend fun refreshFreeBibleCatalog() = onEngine {
        freeBibleStore?.refresh(); Unit
    }

    /** Home-feed shelves, optionally filtered by language code. */
    suspend fun freeBibleHomeFeed(languageCode: String?): List<StoreShelf> = onEngine {
        freeBibleStore?.homeFeed(languageCode) ?: emptyList()
    }

    /** Every language facet the catalog knows about. */
    suspend fun freeBibleLanguages(): List<LanguageFacet> = onEngine {
        freeBibleStore?.languages() ?: emptyList()
    }

    /** Full-text search across every store item. */
    suspend fun searchFreeBibleStore(query: String): List<StoreItem> = onEngine {
        freeBibleStore?.search(query) ?: emptyList()
    }

    /** A single store item by id + kind. */
    suspend fun freeBibleStoreItem(id: String, kind: String): StoreItem? = onEngine {
        freeBibleStore?.item(id, kind)
    }

    /** Paginated shelf contents. */
    suspend fun getFreeBibleShelf(
        shelfId: String,
        language: String?,
        offset: UInt,
        limit: UInt,
    ): List<StoreItem> = onEngine {
        freeBibleStore?.getShelf(shelfId, language, offset, limit) ?: emptyList()
    }

    /** Total item count for a shelf, ignoring pagination. */
    suspend fun getFreeBibleShelfTotal(shelfId: String, language: String?): UInt = onEngine {
        freeBibleStore?.shelfTotal(shelfId, language) ?: 0u
    }

    /**
     * Every installed commentary, as a [StoreItem], sorted by name.
     *
     * Filters `homeFeed` for `kind == "commentary" && installed` and
     * deduplicates by id. The study panel's commentary picker reads this.
     *
     * If the Rust side later exposes a dedicated `list_installed_commentaries`
     * method, swap the implementation — the signature stays.
     */
    suspend fun getInstalledCommentaries(): List<StoreItem> = onEngine {
        val store = freeBibleStore ?: return@onEngine emptyList()

        // Commentaries can appear on more than one shelf; keep one per id,
        // preserving the shelf traversal order until the final sort.
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
    //
    // Each install family hits a different Rust method and a different
    // URL shape. Do not route one item kind through another's method:
    //
    //   translation → install_translation → GET /api/{id}/complete.json
    //   entity_set  → install_entity_set  → dataset_people_list("theographic")
    //   commentary  → install_commentary  → GET /api/c/{id}/books.json
    //                                       + per-chapter fetches
    //
    // The store screen and reader ViewModel dispatch by StoreItem.kind; this
    // repository is the last line of defense, so the translation methods
    // refuse ids that look like entity-set ids.

    /**
     * Installs a translation. Builds `/api/{translationId}/complete.json`.
     *
     * Refuses ids containing `:`, which are entity-set store ids
     * (`dataset:kind`) — those must go through [installFreeBibleEntitySet].
     */
    suspend fun installFreeBibleTranslation(translationId: String): String = onEngine {
        require(!translationId.contains(':')) {
            "$translationId looks like a store item id (dataset:kind), not a translation id — " +
                    "call installFreeBibleEntitySet instead"
        }
        freeBibleStore?.installTranslation(translationId) ?: ""
    }

    /**
     * Uninstalls a translation. Refuses entity-set ids for the same
     * reason as [installFreeBibleTranslation].
     */
    suspend fun uninstallFreeBibleTranslation(translationId: String): String = onEngine {
        require(!translationId.contains(':')) {
            "$translationId looks like a store item id (dataset:kind), not a translation id — " +
                    "there is no uninstall_entity_set yet"
        }
        freeBibleStore?.uninstallTranslation(translationId) ?: ""
    }

    /**
     * Installs an entity set (dataset + kind, e.g. `("theographic", "person")`).
     *
     * Entity sets have no `complete.json`: the Rust engine calls
     * `dataset_people_list("theographic")` (or equivalent) for the given kind.
     */
    suspend fun installFreeBibleEntitySet(datasetId: String, kind: String): String = onEngine {
        freeBibleStore?.installEntitySet(datasetId, kind) ?: ""
    }

    /**
     * Installs a commentary. Builds `/api/c/{commentaryId}/books.json` for
     * the book list, then per-chapter `/api/c/{commentaryId}/{book}/{ch}.json`.
     *
     * Commentaries have no `complete.json` and must not be routed through
     * [installFreeBibleTranslation].
     */
    suspend fun installFreeBibleCommentary(commentaryId: String): String = onEngine {
        freeBibleStore?.installCommentary(commentaryId) ?: ""
    }

    /** Uninstalls a commentary. */
    suspend fun uninstallFreeBibleCommentary(commentaryId: String): String = onEngine {
        freeBibleStore?.uninstallCommentary(commentaryId) ?: ""
    }

    // ── Task polling ───────────────────────────────────────────────

    /** Every in-flight or completed install/uninstall task. */
    suspend fun listFreeBibleTasks(): List<FreeBibleTaskStatus> = onEngine {
        freeBibleStore?.listTasks() ?: emptyList()
    }

    /** Poll a single task by id. Returns `null` if the task has been
     *  reaped (typically after completion). */
    suspend fun pollFreeBibleTask(taskId: String): FreeBibleTaskStatus? = onEngine {
        freeBibleStore?.pollTask(taskId)
    }

    /** Request cancellation of a running task. Best-effort. */
    suspend fun cancelFreeBibleTask(taskId: String) = onEngine {
        freeBibleStore?.cancelTask(taskId); Unit
    }

    // ══════════════════════════════════════════════════════════════════
    // FreeBible — reading (translations)
    // ══════════════════════════════════════════════════════════════════

    // ── Installed translations ─────────────────────────────────────

    /** Every installed translation, ready for the module picker. */
    suspend fun getFreeBibleInstalledTranslations(): List<InstalledTranslation> = onEngine {
        freeBibleReader?.getInstalledTranslations() ?: emptyList()
    }

    /** True if [translationId] is installed. */
    suspend fun isFreeBibleInstalled(translationId: String): Boolean = onEngine {
        freeBibleReader?.isInstalled(translationId) ?: false
    }

    // ── Books ──────────────────────────────────────────────────────

    /** Book list for a translation. */
    suspend fun getFreeBibleBooks(translationId: String): List<FreeBibleBook> = onEngine {
        freeBibleReader?.getBooks(translationId) ?: emptyList()
    }

    /** First book in the translation, or `null` if the translation is empty. */
    suspend fun getFirstFreeBibleBook(translationId: String): FreeBibleBook? = onEngine {
        freeBibleReader?.getBooks(translationId)?.firstOrNull()
    }

    /** A single book by id. */
    suspend fun getFreeBibleBook(translationId: String, bookId: String): FreeBibleBook? =
        onEngine {
            freeBibleReader?.getBooks(translationId)?.firstOrNull { it.id == bookId }
        }

    // ── Chapters / verses ─────────────────────────────────────────

    /** Read a full chapter. Returns `null` if not found. */
    suspend fun readFreeBibleChapter(
        translationId: String,
        bookId: String,
        chapter: Int,
    ): ReaderChapter? = onEngine {
        freeBibleReader?.readChapter(translationId, bookId, chapter)
    }

    /** Single verse text. */
    suspend fun getFreeBibleVerse(
        translationId: String,
        bookId: String,
        chapter: Int,
        verse: Int,
    ): String? = onEngine {
        freeBibleReader?.getVerse(translationId, bookId, chapter, verse)
    }

    /** Fetch a contiguous range of verses. Missing verses are skipped. */
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

    /**
     * Compute the next chapter reference, crossing book boundaries.
     * Returns `Triple(bookId, chapter, bookName)` or `null` at the end.
     */
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

    /**
     * Compute the previous chapter reference, crossing book boundaries.
     * Returns `Triple(bookId, chapter, bookName)` or `null` at the start.
     */
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

    /** True if [commentaryId] is installed. */
    suspend fun isCommentaryInstalled(commentaryId: String): Boolean = onEngine {
        freeBibleReader?.isCommentaryInstalled(commentaryId) ?: false
    }

    /**
     * Read a commentary chapter.
     *
     * Commentaries share the translation book-id space (`GEN`, `EXO`, …)
     * but read via a distinct Rust method that returns homiletical
     * `CommentaryBlock`s, not verse ranges. Returns `null` if the
     * commentary has no notes for this chapter.
     */
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

    /** True if the entity set (dataset + kind) is installed. */
    suspend fun isFreeBibleEntitySetInstalled(datasetId: String, kind: String): Boolean =
        onEngine { freeBibleReader?.isEntitySetInstalled(datasetId, kind) ?: false }

    // ── Listing ────────────────────────────────────────────────────

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

    // ── Detail ─────────────────────────────────────────────────────

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

    // ── Search / cross-reference ───────────────────────────────────

    /** Full-text search across the installed translation chapters. */
    suspend fun searchBible(
        translationId: String,
        query: String,
        onProgress: (Float) -> Unit = {},
    ): List<Pair<String, ReaderChapter>> = onEngine {
        val results = mutableListOf<Pair<String, ReaderChapter>>()
        val reader = freeBibleReader ?: return@onEngine results
        val books = reader.getBooks(translationId)
        val q = query.trim().lowercase()
        if (q.isEmpty()) return@onEngine results

        val totalChapters = books.sumOf { it.numberOfChapters }.toFloat().coerceAtLeast(1f)
        var chaptersChecked = 0f

        for (book in books) {
            for (ch in 1..book.numberOfChapters) {
                val chapter = reader.readChapter(translationId, book.id, ch)
                chaptersChecked += 1f
                onProgress(chaptersChecked / totalChapters)

                if (chapter == null) continue
                val matches = chapter.blocks.any { block ->
                    when (block.itemType) {
                        "verse" -> block.verses.any { v -> v.text.lowercase().contains(q) }
                        "heading", "hebrew_subtitle" -> block.text.lowercase().contains(q)
                        else -> false
                    }
                }
                if (matches) {
                    val hitKey = "${book.id} $ch"
                    results.add(hitKey to chapter)
                }
            }
        }
        results
    }

    suspend fun searchFreeBibleEntities(
        datasetId: String, query: String,
    ): List<EntitySummary> = onEngine {
        freeBibleReader?.searchEntities(datasetId, query) ?: emptyList()
    }

    /** Every entity mentioned in a specific verse. */
    suspend fun freeBibleEntitiesForVerse(
        datasetId: String, book: String, chapter: Int, verse: Int,
    ): List<EntityRef> = onEngine {
        freeBibleReader?.getEntitiesForVerse(datasetId, book, chapter, verse) ?: emptyList()
    }

    /** Every verse reference to a given entity. */
    suspend fun getFreeBibleVerseRefs(
        datasetId: String, kind: String, id: String,
    ): List<VerseRange> = onEngine {
        freeBibleReader?.getVerseRefs(datasetId, kind, id) ?: emptyList()
    }

    /** Related entities for a given entity. */
    suspend fun getFreeBibleRelated(
        datasetId: String, kind: String, id: String, relation: String?,
    ): List<EntityRef> = onEngine {
        freeBibleReader?.getRelated(datasetId, kind, id, relation) ?: emptyList()
    }

    /**
     * Fan-out: every entity mentioned anywhere in [chapter], deduplicated
     * by `(kind, id)` and preserving first-seen order.
     *
     * Rust only exposes per-verse lookups; this loops over
     * [verseNumbers] on the engine thread so the loop stays cheap.
     */
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
    // Timeline
    // ══════════════════════════════════════════════════════════════════

    /**
     * Static timeline data (`Period`s + `Event`s). No engine dependency;
     * still runs on the engine thread because [TimelineData] allocates an
     * FFI handle whose lifecycle is tied to Rust state.
     */
    suspend fun getTimelineData(): List<Period> = onEngine {
        TimelineData().use { it.getData() }
    }
}