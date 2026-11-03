package com.example.xbible.feature.store

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.xbible.feature.store.components.CollectionTile
import com.example.xbible.viewmodel.AudioStoreViewModel
import uniffi.xbible_engine.AudioCollection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllCollectionsScreen(
    viewModel: AudioStoreViewModel,
    onBack: (() -> Unit)? = null,
    onOpenCollection: (AudioCollection) -> Unit,
) {
    val collections by viewModel.collections.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Collections", maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            items(collections, key = { it.id }) { c ->
                CollectionTile(collection = c, onClick = { onOpenCollection(c) })
                HorizontalDivider(
                    modifier = Modifier.padding(start = 64.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                )
            }
        }
    }
}