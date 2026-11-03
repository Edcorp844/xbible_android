package com.example.xbible.feature.store

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
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
import androidx.compose.ui.text.style.TextOverflow
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

    // This triggers the Rust extraction (or fallback) you referenced
    LaunchedEffect(live.uniqueId) { viewModel.extractArtworkColors(live) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Full-screen Background Gradient
        Brush.verticalGradient(
            colors = listOf(
                Color(gradient.getOrNull(0) ?: 0x1A000000L).copy(alpha = 0.6f),
                Color(gradient.getOrNull(1) ?: 0x00000000L).copy(alpha = 0.3f),
                Color.Transparent,
            ),
            startY = 0f,
            endY = 1200f, // Stretches the gradient down the screen
        ).let { brush ->
            Box(
                Modifier
                    .fillMaxSize()
                    .background(brush)
            )
        }

        // 2. Main Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 40.dp), // Space for bottom nav
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Custom Top Bar (Clears status bar)
            CustomTopBar(
                title = live.displayTitle,
                onBack = onBack,
                modifier = Modifier.padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
            )

            // Artwork
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant) // Shadow fallback
            ) {
                val bytes = live.albumArtworkBytes ?: live.artworkBytes
                when {
                    bytes != null -> AsyncImage(
                        model = bytes, contentDescription = null,
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                    )
                    !live.artworkFile?.url.isNullOrBlank() -> AsyncImage(
                        model = live.artworkFile!!.url, contentDescription = null,
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                    )
                    else -> ArtworkPlaceholder(Modifier.fillMaxSize())
                }
            }

            Spacer(Modifier.height(20.dp))

            // Title with subtle shadow for readability over gradient
            Text(
                text = live.displayTitle,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    shadow = Shadow(color = Color.Black.copy(alpha = 0.2f), blurRadius = 4f)
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            live.contributor?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        shadow = Shadow(color = Color.Black.copy(alpha = 0.1f), blurRadius = 2f)
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(20.dp))
            ModuleInstallSplitControl(live, viewModel)
            Spacer(Modifier.height(32.dp))

            // Content Sections
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                MetadataGrid(live)
                if (live.features.features.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Features",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            live.features.features.forEach { f ->
                                SuggestionChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            f,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold
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
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            d,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }
    }
}

// Custom top bar to match the iOS design
@Composable
private fun CustomTopBar(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.width(16.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ModuleInstallSplitControl(
    live: RemoteAudioModuleInfo,
    viewModel: AudioStoreViewModel,
) {
    when (val s = live.status) {
        is ModuleStatus.Idle -> {
            Button(
                onClick = { viewModel.installModule(live.uniqueId) },
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 10.dp),
            ) { Text("GET", fontWeight = FontWeight.Bold) }
        }
        is ModuleStatus.Downloading -> {
            AssistChip(
                onClick = {},
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularWavyProgressIndicator(modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${(s.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
            )
        }
        is ModuleStatus.Installed -> {
            OutlinedButton(
                onClick = {},
                shape = CircleShape,
            ) { Text("Installed", fontWeight = FontWeight.SemiBold) }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MetadataGrid(live: RemoteAudioModuleInfo) {
    val items = listOf(
        "Duration" to formatDuration(live.durationMs),
        "Version" to "v${live.version}"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Details",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items.forEachIndexed { index, (label, value) ->
                SegmentedListItem(
                    onClick = { /* visual only */ },
                    modifier = Modifier.height(IntrinsicSize.Min),
                    shapes = ListItemDefaults.segmentedShapes(index = index, count = items.size),
                    colors = ListItemDefaults.segmentedColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    trailingContent = {
                        Box(
                            modifier = Modifier.fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = value,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
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