package com.example.xbible.feature.tools.audiobible

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.xbible.R
import com.example.xbible.viewmodel.AudioBibleViewModel
import com.example.xbible.viewmodel.InstalledAlbumNode
import com.example.xbible.viewmodel.InstalledCollectionNode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionsDirectoryScreen(
    viewModel: AudioBibleViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onNavigateToAlbums: (InstalledCollectionNode) -> Unit
) {
    val collections = viewModel.organizedLibrary.flatMap { it.collections }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.collections)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(padding)
        ) {
            items(collections, key = { it.id }) { collection ->
                Column(modifier = Modifier.clickable { onNavigateToAlbums(collection) }) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Collections, null, Modifier.size(48.dp))
                    }
                    Text(collection.name, fontWeight = FontWeight.Bold)
                    Text(
                        "${collection.albums.size} albums",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsDirectoryScreen(
    albums: List<InstalledAlbumNode>,
    title: String,
    onBack: () -> Unit,
    onPlay: (InstalledAlbumNode) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(padding)
        ) {
            items(albums, key = { it.id }) { album ->
                Column(modifier = Modifier.clickable { onPlay(album) }) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        val art = album.artworkBitmap
                        if (art != null) {
                            Image(
                                bitmap = art.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(Icons.Default.Album, null, Modifier.size(48.dp))
                        }
                    }
                    Text(album.title, fontWeight = FontWeight.Bold)
                    Text(
                        "${album.modules.size} modules",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}