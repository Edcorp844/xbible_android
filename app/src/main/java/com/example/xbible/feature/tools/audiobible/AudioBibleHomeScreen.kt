package com.example.xbible.feature.tools.audiobible

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.xbible.R
import com.example.xbible.viewmodel.AudioBibleViewModel
import uniffi.xbible_engine.AudioModuleInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioBibleHomeScreen(
    viewModel: AudioBibleViewModel,
    onBack: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    onNavigateToPlaylists: () -> Unit,
    onNavigateToCollections: () -> Unit,
    onNavigateToAlbums: () -> Unit
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
                        icon = Icons.Default.QueueMusic,
                        title = stringResource(R.string.playlists),
                        onClick = onNavigateToPlaylists
                    )
                    DirectoryRow(
                        icon = Icons.Default.Collections,
                        title = stringResource(R.string.collections),
                        onClick = onNavigateToCollections
                    )
                    DirectoryRow(
                        icon = Icons.Default.Album,
                        title = stringResource(R.string.albums),
                        onClick = onNavigateToAlbums
                    )
                    DirectoryRow(
                        icon = Icons.Default.GraphicEq,
                        title = stringResource(R.string.audio_modules),
                        onClick = { /* TODO */ })
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
                RecentModuleCard(module) {
                    viewModel.selectModule(module)
                    onNavigateToPlayer()
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

@Composable
private fun RecentModuleCard(module: AudioModuleInfo, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            val imageBytes = module.artwork.imageBytes()
            if (imageBytes != null) {
                val bitmap = remember(imageBytes) {
                    android.graphics.BitmapFactory.decodeByteArray(
                        imageBytes,
                        0,
                        imageBytes.size
                    )
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Icon(Icons.Default.MusicNote, null, Modifier
                    .align(Alignment.Center)
                    .size(48.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            module.metadata?.displayTitle ?: stringResource(R.string.unknown_title),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            module.metadata?.contributor ?: stringResource(R.string.unknown_contributor),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}