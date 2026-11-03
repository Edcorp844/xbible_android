package com.example.xbible.feature.store

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.xbible.feature.store.components.ModuleCard
import com.example.xbible.viewmodel.AudioStoreViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uniffi.xbible_engine.AudioAlbum
import uniffi.xbible_engine.RemoteAudioModuleInfo
import uniffi.xbible_engine.StoreApiService

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AlbumModulesScreen(
    viewModel: AudioStoreViewModel,
    albumId: String,
    albumTitle: String,
    onBack: (() -> Unit)? = null,
    onOpenModule: (RemoteAudioModuleInfo) -> Unit,
) {
    var album by remember(albumId) { mutableStateOf<AudioAlbum?>(null) }
    var isLoading by remember(albumId) { mutableStateOf(true) }
    var error by remember(albumId) { mutableStateOf<String?>(null) }
    val service = remember { StoreApiService() }

    LaunchedEffect(albumId) {
        if (album != null) return@LaunchedEffect
        isLoading = true
        try {
            album = withContext(Dispatchers.IO) { service.fetchAlbum(albumId) }
        } catch (e: Throwable) {
            error = e.message
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(albumTitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
        when {
            isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { LoadingIndicator() }

            album == null || album!!.modules.isEmpty() -> EmptyState(
                title = "No Modules",
                subtitle = "This album has no modules yet.",
                modifier = Modifier.fillMaxSize().padding(padding),
            )

            else -> {
                val a = album!!
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        AlbumHeader(album = a)
                    }
                    items(a.modules, key = { it.uniqueId }) { module ->
                        ModuleCard(
                            module = module,
                            viewModel = viewModel,
                            onClick = { onOpenModule(module) },
                        )
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
private fun AlbumHeader(album: AudioAlbum) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Box(
            Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(12.dp)),
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
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ),
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                album.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${album.modules.size} ${if (album.modules.size == 1) "module" else "modules"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}