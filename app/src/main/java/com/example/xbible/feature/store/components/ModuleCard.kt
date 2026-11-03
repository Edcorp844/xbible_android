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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Waves
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.xbible.viewmodel.AudioStoreViewModel
import uniffi.xbible_engine.ModuleStatus
import uniffi.xbible_engine.RemoteAudioModuleInfo

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
        // ── 1:1 artwork — no overlays, no progress badge ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
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

            // Only show the control when the module is NOT installed.
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
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    "Get",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        is ModuleStatus.Downloading -> {
            DownloadingRing(progress = status.progress)
        }

        is ModuleStatus.Installed -> {
            // Handled by the parent (control is hidden).
        }
    }
}

/**
 * Small determinate ring with the percentage drawn inside it.
 * This is the only place progress is shown on the card.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DownloadingRing(progress: Double) {
    Box(
        modifier = Modifier.size(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularWavyProgressIndicator(
            progress = { progress.toFloat() },
            modifier = Modifier.size(28.dp),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "${(progress * 100).toInt()}",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun ArtworkPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                    ),
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