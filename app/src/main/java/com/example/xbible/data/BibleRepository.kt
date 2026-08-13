package com.example.xbible.data

import android.app.Application
import android.system.Os
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uniffi.xbible_engine.Section
import uniffi.xbible_engine.SwordModule
import uniffi.xbible_engine.TaskStatus
import uniffi.xbible_engine.XBibleEngine
import java.io.File

/**
 * Repository that abstracts the XBibleEngine (Rust) and provides data to the ViewModels.
 */
class BibleRepository(private val application: Application) {

    private var engine: XBibleEngine? = null

    /**
     * Initializes the native environment and the engine.
     */
    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Load native libraries
            System.loadLibrary("c++_shared")
            System.loadLibrary("xbible_engine")

            val filesDir = application.filesDir.absolutePath
            val cacheDir = application.cacheDir.absolutePath

            // Set environment variables for the Rust engine
            Os.setenv("HOME", filesDir, true)
            Os.setenv("TMPDIR", cacheDir, true)
            Os.setenv("SWORD_PATH", filesDir, true)

            // Ensure expected Sword directories exist
            File(filesDir, "mods.d").mkdirs()

            engine = XBibleEngine()
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    fun isInitialized(): Boolean = engine != null

    fun getBibleModules(): List<SwordModule> {
        return engine?.getBibleModules() ?: emptyList()
    }

    fun getChapterContent(moduleName: String, reference: String): List<Section> {
        return engine?.getChapterContent(moduleName, reference) ?: emptyList()
    }

    fun refreshModules(): List<SwordModule> {
        return engine?.refreshInstalledModules() ?: emptyList()
    }

    fun getRemoteSources(): List<String> {
        return engine?.getRemoteSources() ?: emptyList()
    }

    fun fetchRemoteModules(sourceName: String): List<SwordModule> {
        return engine?.fetchRemoteModules(sourceName) ?: emptyList()
    }

    fun installModuleAsync(source: String, moduleName: String): String {
        return engine?.installModuleAsync(source, moduleName) ?: ""
    }

    fun getTaskStatus(taskId: String): TaskStatus? {
        return engine?.getTaskStatus(taskId)
    }

    fun cancelTask(taskId: String) {
        engine?.cancelTask(taskId)
    }

    fun isModuleInstalled(moduleName: String): Boolean {
        return engine?.isModuleInstalled(moduleName) ?: false
    }

    // Add more methods here as needed to wrap XBibleEngine functionality
}
