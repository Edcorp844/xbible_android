package com.example.xbible.feature.tools.audiobible

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.example.xbible.R
import com.example.xbible.viewmodel.AudioBibleViewModel
import com.example.xbible.viewmodel.InstalledCollectionNode
import uniffi.xbible_engine.AudioModuleInfo

@Composable
fun AudioBibleNavigation(
    onBackToTools: () -> Unit,
    viewModel: AudioBibleViewModel,
) {
    // Backstack: we only need the current + previous, so a simple enum + a
    // "return-to" pointer is enough. Replace with NavHost if this grows.
    var currentScreen by remember { mutableStateOf("home") }
    var selectedCollection by remember { mutableStateOf<InstalledCollectionNode?>(null) }

    // When we push the module list, remember where it came from so back
    // returns to the right place.
    var moduleListTitle by remember { mutableStateOf("") }
    var moduleListModules by remember { mutableStateOf<List<AudioModuleInfo>>(emptyList()) }
    var moduleListOrigin by remember { mutableStateOf("home") }

    /** Push the module list for an arbitrary set of modules. */
    fun openModules(title: String, modules: List<AudioModuleInfo>, from: String) {
        moduleListTitle = title
        moduleListModules = modules
        moduleListOrigin = from
        currentScreen = "modules"
    }

    when (currentScreen) {
        "home" -> AudioBibleHomeScreen(
            viewModel = viewModel,
            onBack = onBackToTools,
            onNavigateToPlaylists = { /* TODO: Playlists Screen */ },
            onNavigateToCollections = { currentScreen = "collections" },
            onNavigateToAlbums = {
                selectedCollection = null
                currentScreen = "albums"
            },
            onNavigateToAudioModules = {
                // "All audio modules" from home — origin is home.
                openModules(
                    title = "Audio Modules",
                    modules = viewModel.availableModules,
                    from = "home",
                )
            },
        )

        "collections" -> CollectionsDirectoryScreen(
            viewModel = viewModel,
            onBack = { currentScreen = "home" },
            onNavigateToAlbums = { collection ->
                selectedCollection = collection
                currentScreen = "albums"
            },
        )

        "albums" -> AlbumsDirectoryScreen(
            albums = selectedCollection?.albums
                ?: viewModel.organizedLibrary.flatMap { it.collections.flatMap { c -> c.albums } },
            title = selectedCollection?.name ?: "Albums",
            onBack = {
                if (selectedCollection != null) {
                    selectedCollection = null
                    currentScreen = "collections"
                } else {
                    currentScreen = "home"
                }
            },
            onAlbumselected = { title, modules ->
                // ── This is the new hop ──
                openModules(
                    title = title,
                    modules = modules,
                    from = "albums",
                )
            },
        )

        "modules" -> ModuleListScreen(
            title = moduleListTitle,
            modules = moduleListModules,
            onBack = {
                // Return to whichever screen pushed us here.
                currentScreen = moduleListOrigin
            },
            onModuleClick = { module ->
                // Play / open the module. If selectModule() already routes
                // to a player elsewhere in the app, just call it.
                viewModel.selectModule(module)
            },
        )
    }
}