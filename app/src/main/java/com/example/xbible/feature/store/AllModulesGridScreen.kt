package com.example.xbible.feature.store

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.xbible.feature.store.components.ModuleCard
import com.example.xbible.viewmodel.AudioStoreViewModel
import uniffi.xbible_engine.RemoteAudioModuleInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllModulesGridScreen(
    title: String,
    modules: List<RemoteAudioModuleInfo>,
    viewModel: AudioStoreViewModel,
    onBack: (() -> Unit)? = null,
    onOpenModule: (RemoteAudioModuleInfo) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 180.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            items(modules, key = { it.uniqueId }) { module ->
                ModuleCard(
                    module = module,
                    viewModel = viewModel,
                    onClick = { onOpenModule(module) },
                )
            }
        }
    }
}