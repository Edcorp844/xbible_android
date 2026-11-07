package com.example.xbible.data

import android.app.Application
import android.system.Os
import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import uniffi.xbible_engine.DictionaryQuery
import uniffi.xbible_engine.DictionaryResponse
import uniffi.xbible_engine.EntityRef
import uniffi.xbible_engine.EntitySummary
import uniffi.xbible_engine.EventDetail
import uniffi.xbible_engine.FreeBibleBook
import uniffi.xbible_engine.FreeBibleReader
import uniffi.xbible_engine.FreeBibleStore
import uniffi.xbible_engine.FreeBibleTaskStatus
import uniffi.xbible_engine.GroupDetail
import uniffi.xbible_engine.InstalledTranslation
import uniffi.xbible_engine.LanguageFacet
import uniffi.xbible_engine.LexiconQuery
import uniffi.xbible_engine.LexiconResponse
import uniffi.xbible_engine.ModuleBook
import uniffi.xbible_engine.Period
import uniffi.xbible_engine.PersonDetail
import uniffi.xbible_engine.PlaceDetail
import uniffi.xbible_engine.ReaderChapter
import uniffi.xbible_engine.SearchResults
import uniffi.xbible_engine.SearchType
import uniffi.xbible_engine.Section
import uniffi.xbible_engine.StoreItem
import uniffi.xbible_engine.StoreShelf
import uniffi.xbible_engine.SwordModule
import uniffi.xbible_engine.TaskStatus
import uniffi.xbible_engine.TimelineData
import uniffi.xbible_engine.VerseRange
import uniffi.xbible_engine.XBibleEngine
import uniffi.xbible_engine.initLogging
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

    @Volatile
    private var engine: XBibleEngine? = null
    @Volatile
    private var freeBible: uniffi.xbible_engine.FreeBibleEngine? = null
    @Volatile
    private var freeBibleStore: FreeBibleStore? = null
    @Volatile
    private var freeBibleReader: FreeBibleReader? = null

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

            initLogging()

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

            Log.d(TAG, "initialize: XBibleEngine + FreeBible constructed")
            Result.success(Unit)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize XBibleEngine", e)
            Result.failure(e)
        }
    }

    fun isInitialized(): Boolean = engine != null

    // ══════════════════════════════════════════════════════════════════
    // SWORD engine (unchanged — mirrors XBibleEngine's methods)
    // ══════════════════════════════════════════════════════════════════

    suspend fun getBibleModules(): List<SwordModule> = onEngine {
        engine?.getBibleModules() ?: emptyList()
    }

    suspend fun getBooks(moduleName: String): List<ModuleBook> = onEngine {
        engine?.getBooks(moduleName) ?: emptyList()
    }

    suspend fun getChapterContent(moduleName: String, reference: String): List<Section> =
        onEngine { engine?.getChapterContent(moduleName, reference) ?: emptyList() }

    suspend fun refreshModules(): List<SwordModule> = onEngine {
        engine?.refreshInstalledModules() ?: emptyList()
    }

    suspend fun getInstalledModules(): List<SwordModule> = onEngine {
        engine?.refreshInstalledModules() ?: emptyList()
    }

    suspend fun getRemoteSources(): List<String> = onEngine {
        engine?.getRemoteSources() ?: emptyList()
    }

    suspend fun fetchRemoteModules(sourceName: String): List<SwordModule> = onEngine {
        engine?.fetchRemoteModules(sourceName) ?: emptyList()
    }

    suspend fun installModuleAsync(source: String, moduleName: String): String = onEngine {
        engine?.installModuleAsync(source, moduleName) ?: ""
    }

    suspend fun getTaskStatus(taskId: String): TaskStatus? = onEngine {
        engine?.getTaskStatus(taskId)
    }

    suspend fun cancelTask(taskId: String) = onEngine {
        engine?.cancelTask(taskId); Unit
    }

    suspend fun isModuleInstalled(moduleName: String): Boolean = onEngine {
        engine?.isModuleInstalled(moduleName) ?: false
    }

    suspend fun uninstallModule(moduleName: String): Int = onEngine {
        engine?.uninstallModule(moduleName) ?: -1
    }

    suspend fun getDictionaryModules(): List<SwordModule> = onEngine {
        engine?.getDictionaryModules() ?: emptyList()
    }

    suspend fun getLexiconModules(): List<SwordModule> = onEngine {
        engine?.getLexiconModules() ?: emptyList()
    }

    suspend fun getCommentaryModules(): List<SwordModule> = onEngine {
        engine?.getCommentaryModules() ?: emptyList()
    }

    suspend fun lookupDictionary(query: DictionaryQuery): DictionaryResponse = onEngine {
        engine?.lookupDictionary(query) ?: DictionaryResponse(emptyList())
    }

    suspend fun lookupStrongsNumber(query: LexiconQuery): LexiconResponse = onEngine {
        engine?.lookupStrongsNumber(query) ?: LexiconResponse(emptyList())
    }

    suspend fun search(
        moduleName: String,
        query: String,
        searchType: SearchType,
    ): SearchResults = onEngine {
        engine?.search(moduleName, query, searchType)
            ?: SearchResults(query, moduleName, emptyList())
    }

    suspend fun getSingleEntry(module: SwordModule, reference: String): List<Section> = onEngine {
        engine?.getSingleEntry(module, reference) ?: emptyList()
    }

    suspend fun getTimelineData(): List<Period> = onEngine {
        TimelineData().use { it.getData() }
    }

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
        offset: Int,
        limit: Int,
    ): List<StoreItem> = onEngine {
        freeBibleStore?.getShelf(shelfId, language, offset.toUInt(), limit.toUInt())
            ?: emptyList()
    }

    suspend fun getFreeBibleShelfTotal(shelfId: String, language: String?): Int = onEngine {
        freeBibleStore?.shelfTotal(shelfId, language)?.toInt() ?: 0
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

    /**
     * The main reading API. Returns everything needed to render one chapter:
     * ordered blocks, footnotes, prev/next.
     */
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
}