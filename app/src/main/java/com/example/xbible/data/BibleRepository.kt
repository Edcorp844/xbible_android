package com.example.xbible.data

import android.app.Application
import android.system.Os
import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import uniffi.xbible_engine.DictionaryQuery
import uniffi.xbible_engine.DictionaryResponse
import uniffi.xbible_engine.LexiconQuery
import uniffi.xbible_engine.LexiconResponse
import uniffi.xbible_engine.ModuleBook
import uniffi.xbible_engine.Period
import uniffi.xbible_engine.SearchResults
import uniffi.xbible_engine.SearchType
import uniffi.xbible_engine.Section
import uniffi.xbible_engine.SwordModule
import uniffi.xbible_engine.TaskStatus
import uniffi.xbible_engine.TimelineData
import uniffi.xbible_engine.XBibleEngine
import uniffi.xbible_engine.initLogging
import java.io.File
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "BibleRepo"

/**
 * Repository that abstracts the XBibleEngine (Rust) and provides data to the ViewModels.
 *
 * ## Threading contract
 *
 * The Rust engine is **not internally synchronized**. All calls into it must
 * happen on a single thread — otherwise we hit the exact SIGSEGV that Swift
 * avoids by routing everything through `engineQueue`.
 *
 * Every public method here is `suspend` and internally dispatches onto
 * [engineDispatcher], which is a dedicated single-thread executor. Callers may
 * invoke these methods from any coroutine context (Main, IO, Default); the
 * repository serializes them.
 *
 * Do **not** expose the raw `engine` field. Everything goes through
 * [onEngine].
 */
@Singleton
class BibleRepository @Inject constructor(private val application: Application) {

    /**
     * The one and only thread that ever touches the Rust engine.
     * Mirrors Swift's `DispatchQueue` labelled `engineQueue`.
     */
    private val engineDispatcher: CoroutineDispatcher =
        Executors.newSingleThreadExecutor { r ->
            Thread(r, "xbible-engine").apply {
                isDaemon = true
                // Give the engine thread a predictable priority. UI/coroutines
                // stay responsive; engine work is CPU-bound (parsing, extracting).
                priority = Thread.NORM_PRIORITY
            }
        }.asCoroutineDispatcher()

    @Volatile
    private var engine: XBibleEngine? = null

    /**
     * Run [block] serialized on the engine thread. All engine access MUST go
     * through this helper. If you add a new method below, wrap the body in
     * `onEngine { ... }`.
     */
    private suspend fun <T> onEngine(block: () -> T): T =
        withContext(engineDispatcher) { block() }

    // ── Lifecycle ─────────────────────────────────────────────────────

    /**
     * Initializes the native environment and the engine.
     * Idempotent — safe to call multiple times.
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

            initLogging()
            engine = XBibleEngine()
            Log.d(TAG, "initialize: XBibleEngine constructed")
            Result.success(Unit)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize XBibleEngine", e)
            Result.failure(e)
        }
    }

    fun isInitialized(): Boolean = engine != null

    // ── Module discovery ──────────────────────────────────────────────

    suspend fun getBibleModules(): List<SwordModule> = onEngine {
        engine?.getBibleModules() ?: emptyList()
    }

    suspend fun getBooks(moduleName: String): List<ModuleBook> = onEngine {
        engine?.getBooks(moduleName) ?: emptyList()
    }

    suspend fun getChapterContent(moduleName: String, reference: String): List<Section> = onEngine {
        engine?.getChapterContent(moduleName, reference) ?: emptyList()
    }

    suspend fun refreshModules(): List<SwordModule> = onEngine {
        engine?.refreshInstalledModules() ?: emptyList()
    }

    suspend fun getInstalledModules(): List<SwordModule> = onEngine {
        val e = engine
        if (e == null) {
            Log.w(TAG, "getInstalledModules: engine not initialized")
            emptyList()
        } else {
            e.refreshInstalledModules()
        }
    }

    // ── Remote / store ────────────────────────────────────────────────

    suspend fun getRemoteSources(): List<String> = onEngine {
        engine?.getRemoteSources() ?: emptyList()
    }

    suspend fun fetchRemoteModules(sourceName: String): List<SwordModule> = onEngine {
        engine?.fetchRemoteModules(sourceName) ?: emptyList()
    }

    // ── Install pipeline ──────────────────────────────────────────────

    /**
     * Kicks off an async install. Returns a task id that can be polled via
     * [getTaskStatus]. The engine spawns its own worker for the actual
     * download/extract; this call only registers the task.
     */
    suspend fun installModuleAsync(source: String, moduleName: String): String = onEngine {
        engine?.installModuleAsync(source, moduleName) ?: ""
    }

    suspend fun getTaskStatus(taskId: String): TaskStatus? = onEngine {
        engine?.getTaskStatus(taskId)
    }

    suspend fun cancelTask(taskId: String) = onEngine {
        engine?.cancelTask(taskId)
        Unit
    }

    // ── Module management ─────────────────────────────────────────────

    suspend fun isModuleInstalled(moduleName: String): Boolean = onEngine {
        engine?.isModuleInstalled(moduleName) ?: false
    }

    suspend fun uninstallModule(moduleName: String): Int = onEngine {
        engine?.uninstallModule(moduleName) ?: -1
    }

    // ── Typed module lookups ──────────────────────────────────────────

    suspend fun getDictionaryModules(): List<SwordModule> = onEngine {
        engine?.getDictionaryModules() ?: emptyList()
    }

    suspend fun getLexiconModules(): List<SwordModule> = onEngine {
        engine?.getLexiconModules() ?: emptyList()
    }

    suspend fun getCommentaryModules(): List<SwordModule> = onEngine {
        engine?.getCommentaryModules() ?: emptyList()
    }

    // ── Queries ───────────────────────────────────────────────────────

    suspend fun lookupDictionary(query: DictionaryQuery): DictionaryResponse = onEngine {
        engine?.lookupDictionary(query) ?: DictionaryResponse(emptyList())
    }

    suspend fun lookupStrongsNumber(query: LexiconQuery): LexiconResponse = onEngine {
        engine?.lookupStrongsNumber(query) ?: LexiconResponse(emptyList())
    }

    suspend fun search(
        moduleName: String,
        query: String,
        searchType: SearchType
    ): SearchResults = onEngine {
        engine?.search(moduleName, query, searchType)
            ?: SearchResults(query, moduleName, emptyList())
    }

    suspend fun getSingleEntry(module: SwordModule, reference: String): List<Section> = onEngine {
        engine?.getSingleEntry(module, reference) ?: emptyList()
    }

    // ── Timeline ──────────────────────────────────────────────────────
    //
    // TimelineData() is a *separate* Rust object — its `use {}` closes it
    // immediately. Whether it needs engine-thread affinity depends on whether
    // it shares engine state internally; keep it on the engine thread to be
    // safe. Cheap operation, no reason to risk it.

    suspend fun getTimelineData(): List<Period> = onEngine {
        TimelineData().use { it.getData() }
    }
}