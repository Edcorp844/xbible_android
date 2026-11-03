package com.example.xbible.feature.store

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.xbible.viewmodel.AudioStoreViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uniffi.xbible_engine.AudioAlbum
import uniffi.xbible_engine.StoreApiService

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CollectionAlbumsScreen(
    viewModel: AudioStoreViewModel,
    collectionId: String,
    collectionName: String,
    onBack: (() -> Unit)? = null,
    onOpenAlbum: (AudioAlbum) -> Unit,
) {
    var albums by remember(collectionId) { mutableStateOf<List<AudioAlbum>>(emptyList()) }
    var isLoading by remember(collectionId) { mutableStateOf(true) }
    var error by remember(collectionId) { mutableStateOf<String?>(null) }
    var searchText by remember { mutableStateOf("") }
    val service = remember { StoreApiService() }

    LaunchedEffect(collectionId) {
        if (albums.isNotEmpty()) return@LaunchedEffect
        isLoading = true
        try {
            albums = withContext(Dispatchers.IO) {
                service.fetchAlbumsByCollection(collectionId = collectionId, first = 100, skip = 0)
            }
        } catch (e: Throwable) {
            error = e.message
        } finally {
            isLoading = false
        }
    }

    val filtered = remember(albums, searchText) {
        if (searchText.isBlank()) albums
        else albums.filter { it.title.contains(searchText, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(collectionName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        }
    ) { padding ->
        Column(Modifier
            .fillMaxSize()
            .padding(padding)) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text("Search albums") },
                leadingIcon = {
                    Icon(androidx.compose.material.icons.Icons.Outlined.Search, null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                singleLine = true,
            )

            when {
                isLoading -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { LoadingIndicator() }

                albums.isEmpty() -> EmptyState(
                    title = "No Albums",
                    subtitle = "This collection has no albums yet.",
                    modifier = Modifier.fillMaxSize(),
                )

                filtered.isEmpty() -> EmptyState(
                    title = "No Results",
                    subtitle = "No albums match \"$searchText\"",
                    modifier = Modifier.fillMaxSize(),
                )

                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    contentPadding = PaddingValues(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    items(filtered, key = { it.id }) { album ->
                        AlbumCard(album = album, onClick = { onOpenAlbum(album) })
                    }
                }
            }
        }
    }

    error?.let {
        AlertDialog(
            onDismissRequest = { error = null },
            confirmButton = { TextButton(onClick = { error = null }) { Text("OK") } },
            title = { Text("Error") },
            text = { Text(it) },
        )
    }
}

@Composable
private fun AlbumCard(album: AudioAlbum, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
        ) {
            val url = album.artwork?.url
            if (!url.isNullOrBlank()) {
                AsyncImage(
                    model = url,
                    contentDescription = album.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Text(
            album.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            minLines = 2,
        )
        Text(
            "${album.modules.size} ${if (album.modules.size == 1) "module" else "modules"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}