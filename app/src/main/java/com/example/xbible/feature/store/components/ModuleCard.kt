package com.example.xbible.feature.store.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.xbible.viewmodel.AudioStoreViewModel
import uniffi.xbible_engine.ModuleStatus
import uniffi.xbible_engine.RemoteAudioModuleInfo

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ModuleCard(
    module: RemoteAudioModuleInfo,
    viewModel: AudioStoreViewModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val catalog by viewModel.availableModules.collectAsState()
    val live = remember(catalog, module.uniqueId) {
        catalog.firstOrNull { it.uniqueId == module.uniqueId } ?: module
    }

    Column(
        modifier = modifier.clickable(onClick = onClick),
    ) {
        // 1:1 artwork
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            val bytes = live.albumArtworkBytes ?: live.artworkBytes
            when {
                bytes != null -> AsyncImage(
                    model = bytes,
                    contentDescription = live.displayTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp)),
                )

                !live.artworkFile?.url.isNullOrBlank() -> AsyncImage(
                    model = live.artworkFile!!.url,
                    contentDescription = live.displayTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp)),
                )

                else -> ArtworkPlaceholder()
            }
            if (live.status is ModuleStatus.Downloading) {
                val p = (live.status as ModuleStatus.Downloading).progress
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            "${(p * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            live.displayTitle,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            minLines = 2,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (live.contributor != null) {
                Text(
                    live.contributor!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(Modifier.weight(1f))
            }

            // Only show the control if it is NOT installed
            if (live.status !is ModuleStatus.Installed) {
                Spacer(Modifier.width(8.dp))
                ModuleInstallControl(live, viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ModuleInstallControl(
    live: RemoteAudioModuleInfo,
    viewModel: AudioStoreViewModel,
) {
    when (val status = live.status) {
        is ModuleStatus.Idle -> {
            FilledTonalButton(
                onClick = { viewModel.installModule(live.uniqueId) },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    "Get",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        is ModuleStatus.Downloading -> {
            CircularWavyProgressIndicator(
                progress = { status.progress.toFloat() },
                modifier = Modifier.size(24.dp),
            )
        }

        is ModuleStatus.Installed -> {
            // Do nothing, this is handled by the parent
        }
    }
}

@Composable
fun ArtworkPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                    )
                ),
                shape = RoundedCornerShape(12.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.Waves,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}