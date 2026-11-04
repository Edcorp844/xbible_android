package com.example.xbible.feature.tools.audiobible

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.xbible.R
import com.example.xbible.feature.tools.audiobible.components.LocalAudioModuleCard
import com.example.xbible.viewmodel.AudioBibleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioBibleHomeScreen(
    viewModel: AudioBibleViewModel,
    onBack: () -> Unit,
    onNavigateToPlaylists: () -> Unit,
    onNavigateToCollections: () -> Unit,
    onNavigateToAlbums: () -> Unit,
    onNavigateToAudioModules: () -> Unit
) {
    val availableModules by remember { derivedStateOf { viewModel.availableModules } }
    val recentModules = remember(availableModules) {
        availableModules.sortedByDescending { it.metadata?.version ?: 0 }.take(20)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.audio_bible_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Create Playlist Alert */ }) {
                        Icon(Icons.Default.PlaylistAdd, contentDescription = "New Playlist")
                    }
                }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Directory Links
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    DirectoryRow(
                        icon = Icons.AutoMirrored.Outlined.QueueMusic,
                        title = stringResource(R.string.playlists),
                        onClick = onNavigateToPlaylists
                    )
                    DirectoryRow(
                        icon = Icons.Outlined.Collections,
                        title = stringResource(R.string.collections),
                        onClick = onNavigateToCollections
                    )
                    DirectoryRow(
                        icon = Icons.Outlined.Album,
                        title = stringResource(R.string.albums),
                        onClick = onNavigateToAlbums
                    )
                    DirectoryRow(
                        icon = Icons.Outlined.GraphicEq,
                        title = stringResource(R.string.audio_modules),
                        onClick = onNavigateToAudioModules
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        stringResource(R.string.recently_added),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            // Recent Modules
            items(recentModules, key = { it.fileName }) { module ->
                LocalAudioModuleCard(module) {
                    viewModel.selectModule(module)
                }
            }
        }
    }
}

@Composable
private fun DirectoryRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.weight(1f))
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

