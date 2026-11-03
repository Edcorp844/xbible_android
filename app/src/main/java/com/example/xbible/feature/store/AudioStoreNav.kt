package com.example.xbible.feature.store

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.xbible.viewmodel.AudioStoreViewModel
import uniffi.xbible_engine.AudioCollection
import uniffi.xbible_engine.AudioLanguage
import uniffi.xbible_engine.RemoteAudioModuleInfo

private const val TAG = "AudioStoreVM"

// ── Stack routes (plain Kotlin, no serialization plugin) ─────────────

sealed interface AudioRoute {
    data object Home : AudioRoute
    data class Language(val languageId: String) : AudioRoute
    data class CollectionAlbums(val collectionId: String, val collectionName: String) : AudioRoute
    data class AlbumModules(val albumId: String, val albumTitle: String) : AudioRoute
    data class ModuleDetail(val uniqueId: String) : AudioRoute
    data class Narrator(val contributorId: String) : AudioRoute
    data object AllRecents : AudioRoute
    data object AllInstalled : AudioRoute
    data object AllCollections : AudioRoute
}

class AudioStack(initial: AudioRoute) {
    var current by mutableStateOf(initial)
        private set
    val back = mutableListOf<AudioRoute>()

    fun push(route: AudioRoute) {
        Log.d(TAG, "AudioStack.push: ${describe(current)} → ${describe(route)} (depth=${back.size + 1})")
        back.add(current)
        current = route
    }

    fun pop(): Boolean {
        if (back.isEmpty()) {
            Log.d(TAG, "AudioStack.pop: at root (${describe(current)}), nothing to pop")
            return false
        }
        val from = current
        current = back.removeAt(back.lastIndex)
        Log.d(TAG, "AudioStack.pop: ${describe(from)} → ${describe(current)} (depth=${back.size})")
        return true
    }

    fun snapshot(): List<AudioRoute> = back + current
}

private fun describe(route: AudioRoute): String = when (route) {
    is AudioRoute.Home -> "Home"
    is AudioRoute.Language -> "Language(${route.languageId})"
    is AudioRoute.CollectionAlbums -> "Collection(${route.collectionName})"
    is AudioRoute.AlbumModules -> "Album(${route.albumTitle})"
    is AudioRoute.ModuleDetail -> "Module(${route.uniqueId})"
    is AudioRoute.Narrator -> "Narrator(${route.contributorId})"
    is AudioRoute.AllRecents -> "AllRecents"
    is AudioRoute.AllInstalled -> "AllInstalled"
    is AudioRoute.AllCollections -> "AllCollections"
}

@Composable
fun AudioStoreRoot(
    onExitToStudy: () -> Unit = {},
    viewModel: AudioStoreViewModel = hiltViewModel(),
) {
    Log.d(TAG, "AudioStoreRoot: composed")

    // Kick the VM once (Swift `.task { bootstrap(); loadNarrators() }`).
    LaunchedEffect(Unit) {
        val existing = viewModel.availableModules.value.size
        Log.d(TAG, "AudioStoreRoot.LaunchedEffect: existingModules=$existing")
        if (existing == 0) {
            Log.d(TAG, "AudioStoreRoot.LaunchedEffect: triggering bootstrap() + loadNarrators()")
            viewModel.bootstrap()
            viewModel.loadNarrators()
        } else {
            Log.d(TAG, "AudioStoreRoot.LaunchedEffect: skipping bootstrap, catalog already populated")
        }
    }

    val stack = rememberSaveable(saver = AudioStackSaver) {
        Log.d(TAG, "AudioStoreRoot: creating fresh AudioStack(Home)")
        AudioStack(AudioRoute.Home)
    }

    BackHandler(enabled = stack.current !is AudioRoute.Home) {
        Log.d(TAG, "AudioStoreRoot: hardware back pressed (current=${describe(stack.current)})")
        if (!stack.pop()) {
            Log.d(TAG, "AudioStoreRoot: pop failed → onExitToStudy()")
            onExitToStudy()
        }
    }

    // Observe flows once at the root so children just consume them.
    val modules by viewModel.availableModules.collectAsState()
    val languages by viewModel.languages.collectAsState()
    val narrators by viewModel.narrators.collectAsState()

    // Log whenever the underlying catalog changes (debounced by size).
    LaunchedEffect(modules.size) {
        Log.d(
            TAG,
            "AudioStoreRoot: modules.size changed → ${modules.size} " +
                    "(installed=${modules.count { it.isInstalled }}, " +
                    "downloading=${modules.count { it.status is uniffi.xbible_engine.ModuleStatus.Downloading }})"
        )
        if (modules.isNotEmpty() && modules.size <= 5) {
            modules.forEach { m ->
                Log.d(TAG, "  • ${m.uniqueId} | installed=${m.isInstalled} | status=${m.status}")
            }
        } else if (modules.isNotEmpty()) {
            Log.d(TAG, "  • first: ${modules.first().uniqueId} | status=${modules.first().status}")
            Log.d(TAG, "  • last:  ${modules.last().uniqueId} | status=${modules.last().status}")
        }
    }
    LaunchedEffect(languages.size) {
        Log.d(TAG, "AudioStoreRoot: languages.size changed → ${languages.size}")
        if (languages.isNotEmpty()) {
            Log.d(TAG, "  languages = ${languages.map { "${it.code}:${it.name}" }}")
        }
    }
    LaunchedEffect(narrators.size) {
        Log.d(TAG, "AudioStoreRoot: narrators.size changed → ${narrators.size}")
        if (narrators.isNotEmpty()) {
            Log.d(TAG, "  narrators = ${narrators.map { it.name }}")
        }
    }

    when (val route = stack.current) {
        is AudioRoute.Home -> {
            Log.d(TAG, "AudioStoreRoot: rendering Home")
            StoreHomeScreen(
                viewModel = viewModel,
                onOpenLanguage = { l: AudioLanguage ->
                    Log.d(TAG, "Home.onOpenLanguage: ${l.code}/${l.name}")
                    stack.push(AudioRoute.Language(l.id))
                },
                onOpenContributor = { c ->
                    Log.d(TAG, "Home.onOpenContributor: ${c.name}")
                    stack.push(AudioRoute.Narrator(c.id))
                },
                onOpenCollection = { c: AudioCollection ->
                    Log.d(TAG, "Home.onOpenCollection: ${c.name} (albums=${c.albums.size})")
                    stack.push(AudioRoute.CollectionAlbums(c.id, c.name))
                },
                onOpenAlbum = { a ->
                    Log.d(TAG, "Home.onOpenAlbum: ${a.title} (modules=${a.modules.size})")
                    stack.push(AudioRoute.AlbumModules(a.id, a.title))
                },
                onOpenModule = { m: RemoteAudioModuleInfo ->
                    Log.d(TAG, "Home.onOpenModule: ${m.uniqueId} '${m.displayTitle}'")
                    stack.push(AudioRoute.ModuleDetail(m.uniqueId))
                },
                onOpenAllRecents = {
                    val recents = modules.filter { !it.isInstalled }
                    Log.d(TAG, "Home.onOpenAllRecents: ${recents.size} recents")
                    stack.push(AudioRoute.AllRecents)
                },
                onOpenAllInstalled = {
                    val installed = modules.filter { it.isInstalled }
                    Log.d(TAG, "Home.onOpenAllInstalled: ${installed.size} installed")
                    stack.push(AudioRoute.AllInstalled)
                },
                onOpenAllCollections = {
                    Log.d(TAG, "Home.onOpenAllCollections")
                    stack.push(AudioRoute.AllCollections)
                },
            )
        }

        is AudioRoute.Language -> {
            val lang = languages.firstOrNull { it.id == route.languageId }
            Log.d(
                TAG,
                "AudioStoreRoot: route=Language(${route.languageId}) resolved=${lang?.code ?: "<NOT FOUND>"} " +
                        "(available=${languages.size})"
            )
            if (lang != null) {
                LanguageCollectionsScreen(
                    viewModel = viewModel,
                    language = lang,
                    onBack = { stack.pop() },
                    onOpenCollection = { c ->
                        Log.d(TAG, "Language.onOpenCollection: ${c.name}")
                        stack.push(AudioRoute.CollectionAlbums(c.id, c.name))
                    },
                )
            }
        }

        is AudioRoute.CollectionAlbums -> {
            Log.d(TAG, "AudioStoreRoot: rendering CollectionAlbums(${route.collectionName})")
            CollectionAlbumsScreen(
                viewModel = viewModel,
                collectionId = route.collectionId,
                collectionName = route.collectionName,
                onBack = { stack.pop() },
                onOpenAlbum = { a ->
                    Log.d(TAG, "Collection.onOpenAlbum: ${a.title} (modules=${a.modules.size})")
                    stack.push(AudioRoute.AlbumModules(a.id, a.title))
                },
            )
        }

        is AudioRoute.AlbumModules -> {
            Log.d(TAG, "AudioStoreRoot: rendering AlbumModules(${route.albumTitle})")
            AlbumModulesScreen(
                viewModel = viewModel,
                albumId = route.albumId,
                albumTitle = route.albumTitle,
                onBack = { stack.pop() },
                onOpenModule = { m ->
                    Log.d(TAG, "Album.onOpenModule: ${m.uniqueId}")
                    stack.push(AudioRoute.ModuleDetail(m.uniqueId))
                },
            )
        }

        is AudioRoute.ModuleDetail -> {
            val module = modules.firstOrNull { it.uniqueId == route.uniqueId }
            Log.d(
                TAG,
                "AudioStoreRoot: route=ModuleDetail(${route.uniqueId}) resolved=" +
                        "${module?.displayTitle ?: "<NOT FOUND>"} (catalog=${modules.size})"
            )
            if (module != null) {
                ModuleDetailScreen(
                    viewModel = viewModel,
                    module = module,
                    onBack = { stack.pop() },
                )
            }
        }

        is AudioRoute.Narrator -> {
            val narrator = narrators.firstOrNull { it.id == route.contributorId }
            Log.d(
                TAG,
                "AudioStoreRoot: route=Narrator(${route.contributorId}) resolved=" +
                        "${narrator?.name ?: "<NOT FOUND>"} (available=${narrators.size})"
            )
            if (narrator != null) {
                NarratorDetailScreen(
                    viewModel = viewModel,
                    narrator = narrator,
                    onBack = { stack.pop() },
                    onOpenModule = { m ->
                        Log.d(TAG, "Narrator.onOpenModule: ${m.uniqueId}")
                        stack.push(AudioRoute.ModuleDetail(m.uniqueId))
                    },
                )
            }
        }

        is AudioRoute.AllRecents -> {
            val recents = modules
                .filter { !it.isInstalled }
                .sortedByDescending { it.updatedAt }
            Log.d(TAG, "AudioStoreRoot: rendering AllRecents (count=${recents.size})")
            AllModulesGridScreen(
                title = "Recent Modules",
                modules = recents,
                viewModel = viewModel,
                onBack = { stack.pop() },
                onOpenModule = { m -> stack.push(AudioRoute.ModuleDetail(m.uniqueId)) },
            )
        }

        is AudioRoute.AllInstalled -> {
            val installed = modules.filter { it.isInstalled }
            Log.d(TAG, "AudioStoreRoot: rendering AllInstalled (count=${installed.size})")
            AllModulesGridScreen(
                title = "Installed Modules",
                modules = installed,
                viewModel = viewModel,
                onBack = { stack.pop() },
                onOpenModule = { m -> stack.push(AudioRoute.ModuleDetail(m.uniqueId)) },
            )
        }

        is AudioRoute.AllCollections -> {
            val collections = viewModel.collections.collectAsState().value
            Log.d(TAG, "AudioStoreRoot: rendering AllCollections (count=${collections.size})")
            AllCollectionsScreen(
                viewModel = viewModel,
                onBack = { stack.pop() },
                onOpenCollection = { c ->
                    Log.d(TAG, "AllCollections.onOpenCollection: ${c.name}")
                    stack.push(AudioRoute.CollectionAlbums(c.id, c.name))
                },
            )
        }
    }
}

// ── RememberSaveable support ────────────────────────────────────────

private fun encodeRoute(route: AudioRoute): String = when (route) {
    is AudioRoute.Home -> "Home"
    is AudioRoute.Language -> "Language|${route.languageId}"
    is AudioRoute.CollectionAlbums -> "CollectionAlbums|${route.collectionId}|${route.collectionName}"
    is AudioRoute.AlbumModules -> "AlbumModules|${route.albumId}|${route.albumTitle}"
    is AudioRoute.ModuleDetail -> "ModuleDetail|${route.uniqueId}"
    is AudioRoute.Narrator -> "Narrator|${route.contributorId}"
    is AudioRoute.AllRecents -> "AllRecents"
    is AudioRoute.AllInstalled -> "AllInstalled"
    is AudioRoute.AllCollections -> "AllCollections"
}

private fun decodeRoute(raw: String): AudioRoute {
    val parts = raw.split("|")
    return when (parts.getOrNull(0)) {
        "Language" -> AudioRoute.Language(parts.getOrElse(1) { "" })
        "CollectionAlbums" -> AudioRoute.CollectionAlbums(parts.getOrElse(1) { "" }, parts.getOrElse(2) { "" })
        "AlbumModules" -> AudioRoute.AlbumModules(parts.getOrElse(1) { "" }, parts.getOrElse(2) { "" })
        "ModuleDetail" -> AudioRoute.ModuleDetail(parts.getOrElse(1) { "" })
        "Narrator" -> AudioRoute.Narrator(parts.getOrElse(1) { "" })
        "AllRecents" -> AudioRoute.AllRecents
        "AllInstalled" -> AudioRoute.AllInstalled
        "AllCollections" -> AudioRoute.AllCollections
        else -> AudioRoute.Home
    }
}

private val AudioStackSaver = Saver<AudioStack, List<String>>(
    save = { stack ->
        val list = mutableListOf<String>()
        stack.back.forEach { list.add(encodeRoute(it)) }
        list.add(encodeRoute(stack.current))
        Log.d(TAG, "AudioStackSaver.save: ${list.size} entries → $list")
        list
    },
    restore = { list ->
        Log.d(TAG, "AudioStackSaver.restore: ${list.size} entries ← $list")
        if (list.isEmpty()) return@Saver AudioStack(AudioRoute.Home)
        val stack = AudioStack(decodeRoute(list.last()))
        for (i in 0 until list.lastIndex) {
            stack.back.add(decodeRoute(list[i]))
        }
        stack
    }
)