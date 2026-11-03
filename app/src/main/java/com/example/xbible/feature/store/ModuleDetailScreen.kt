package com.example.xbible.feature.store

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.xbible.feature.store.components.ArtworkPlaceholder
import com.example.xbible.viewmodel.AudioStoreViewModel
import uniffi.xbible_engine.ModuleStatus
import uniffi.xbible_engine.RemoteAudioModuleInfo

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ModuleDetailScreen(
    viewModel: AudioStoreViewModel,
    module: RemoteAudioModuleInfo,
    onBack: (() -> Unit)? = null,
) {
    val catalog by viewModel.availableModules.collectAsState()
    val gradient by viewModel.backgroundGradientColors.collectAsState()
    val live = catalog.firstOrNull { it.uniqueId == module.uniqueId } ?: module

    // Keyed by module ID so status updates don't restart the extraction.
    LaunchedEffect(module.uniqueId) {
        viewModel.extractArtworkColors(module)
    }

    val color1 by animateColorAsState(
        targetValue = Color(gradient.getOrNull(0) ?: 0x1A000000L),
        animationSpec = tween(600),
        label = "color1",
    )
    val color2 by animateColorAsState(
        targetValue = Color(gradient.getOrNull(1) ?: 0x00000000L),
        animationSpec = tween(600),
        label = "color2",
    )

    val pageBackground = MaterialTheme.colorScheme.background

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBackground),
    ) {
        // 1) Hero gradient — Apple Music style.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to color1.copy(alpha = 0.85f),
                            0.4f to color1.copy(alpha = 0.6f),
                            0.55f to color2.copy(alpha = 0.3f),
                            0.85f to Color.Transparent,
                            1.0f to Color.Transparent,
                        ),
                    ),
                ),
        )

        // 2) Scrollable content.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(64.dp)) // clearance for the sticky back button

            // ── Artwork: untouched. No progress overlay, no scrim. ──
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                val bytes = live.albumArtworkBytes ?: live.artworkBytes
                when {
                    bytes != null -> AsyncImage(
                        model = bytes,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    !live.artworkFile?.url.isNullOrBlank() -> AsyncImage(
                        model = live.artworkFile!!.url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    else -> ArtworkPlaceholder(Modifier.fillMaxSize())
                }
            }

            Spacer(Modifier.height(20.dp))

            // Title
            Text(
                text = live.displayTitle,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    shadow = Shadow(color = Color.Black.copy(alpha = 0.2f), blurRadius = 6f),
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 24.dp),
            )

            live.contributor?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        shadow = Shadow(color = Color.Black.copy(alpha = 0.1f), blurRadius = 2f),
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(20.dp))

            ModuleInstallControl(live, viewModel)

            Spacer(Modifier.height(32.dp))

            // Content sections
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                MetadataGrid(live)

                if (live.features.features.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Features",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            live.features.features.forEach { f ->
                                SuggestionChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            f,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    },
                                )
                            }
                        }
                    }
                }

                live.description?.takeIf { it.isNotBlank() }?.let { d ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "About",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            d,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 22.sp,
                        )
                    }
                }
            }
        }

        // 3) Sticky back button.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .align(Alignment.TopStart),
        ) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.Black.copy(alpha = 0.2f), CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                    )
                }
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
    when (val s = live.status) {
        is ModuleStatus.Idle -> {
            Button(
                onClick = { viewModel.installModule(live.uniqueId) },
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 10.dp),
            ) {
                Text("GET", fontWeight = FontWeight.Bold)
            }
        }

        is ModuleStatus.Downloading -> {
            DownloadingChip(progress = s.progress)
        }

        is ModuleStatus.Installed -> {
            OutlinedButton(
                onClick = {},
                shape = CircleShape,
            ) {
                Text("Installed", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DownloadingChip(progress: Double) {
    AssistChip(
        onClick = {},
        shape = CircleShape,
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
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
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = "Downloading",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MetadataGrid(live: RemoteAudioModuleInfo) {
    val items = listOf(
        "Duration" to formatDuration(live.durationMs),
        "Version" to "v${live.version}",
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Details",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items.forEachIndexed { index, (label, value) ->
                SegmentedListItem(
                    onClick = { /* visual only */ },
                    modifier = Modifier.height(IntrinsicSize.Min),
                    shapes = ListItemDefaults.segmentedShapes(index = index, count = items.size),
                    colors = ListItemDefaults.segmentedColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                    trailingContent = {
                        Box(
                            modifier = Modifier.fillMaxHeight(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = value,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

internal fun formatDuration(ms: Long): String {
    val s = ms / 1000
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) {
        "%dh %02dm %02ds".format(h, m, sec)
    } else {
        "%dm %02ds".format(m, sec)
    }
}