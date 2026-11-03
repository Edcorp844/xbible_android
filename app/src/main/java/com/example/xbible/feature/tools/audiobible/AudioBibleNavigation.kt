package com.example.xbible.feature.tools.audiobible


import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.xbible.viewmodel.AudioBibleViewModel
import com.example.xbible.viewmodel.InstalledCollectionNode

@Composable
fun AudioBibleNavigation(
    onBackToTools: () -> Unit,
    viewModel: AudioBibleViewModel
) {
    var currentScreen by remember { mutableStateOf("home") }
    var selectedCollection by remember { mutableStateOf<InstalledCollectionNode?>(null) }

    when (currentScreen) {
        "home" -> AudioBibleHomeScreen(
            viewModel = viewModel,
            onBack = onBackToTools,
            onNavigateToPlayer = { currentScreen = "player" },
            onNavigateToPlaylists = { /* TODO: Playlists Screen */ },
            onNavigateToCollections = { currentScreen = "collections" },
            onNavigateToAlbums = {
                selectedCollection = null
                currentScreen = "albums"
            }
        )

        "player" -> AudioBiblePlayerScreen(
            viewModel = viewModel,
            onBack = { currentScreen = "home" }
        )

        "collections" -> CollectionsDirectoryScreen(
            viewModel = viewModel,
            onBack = { currentScreen = "home" },
            onNavigateToAlbums = { collection ->
                selectedCollection = collection
                currentScreen = "albums"
            }
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
            onPlay = { album ->
                if (album.modules.isNotEmpty()) {
                    viewModel.selectModule(album.modules.first())
                    currentScreen = "player"
                }
            }
        )
    }
}