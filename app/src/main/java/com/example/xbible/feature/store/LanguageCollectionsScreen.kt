package com.example.xbible.feature.store

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.xbible.feature.store.components.CollectionTile
import com.example.xbible.viewmodel.AudioStoreViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uniffi.xbible_engine.AudioCollection
import uniffi.xbible_engine.AudioLanguage
import uniffi.xbible_engine.StoreApiService

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LanguageCollectionsScreen(
    viewModel: AudioStoreViewModel,
    language: AudioLanguage,
    onBack: (() -> Unit)? = null,
    onOpenCollection: (AudioCollection) -> Unit,
) {
    var collections by remember(language.id) { mutableStateOf<List<AudioCollection>>(emptyList()) }
    var isLoading by remember(language.id) { mutableStateOf(true) }
    var error by remember(language.id) { mutableStateOf<String?>(null) }
    val service = remember { StoreApiService() }

    LaunchedEffect(language.id) {
        if (collections.isNotEmpty()) return@LaunchedEffect
        isLoading = true
        try {
            collections = withContext(Dispatchers.IO) {
                service.fetchCollectionsByLanguage(code = language.code, first = 100, skip = 0)
            }
        } catch (e: Throwable) {
            error = e.message
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(language.name) },
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

            collections.isEmpty() -> EmptyState(
                title = "No Collections",
                subtitle = "This language has no collections yet.",
                modifier = Modifier.fillMaxSize().padding(padding),
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                items(collections, key = { it.id }) { c ->
                    CollectionTile(
                        collection = c,
                        onClick = { onOpenCollection(c) },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 64.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    )
                }
            }
        }
    }

    error?.let {
        AlertDialog(
            onDismissRequest = { error = null },
            confirmButton = {
                TextButton(onClick = { error = null }) { Text("OK") }
            },
            title = { Text("Error") },
            text = { Text(it) },
        )
    }
}

@Composable
internal fun EmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}