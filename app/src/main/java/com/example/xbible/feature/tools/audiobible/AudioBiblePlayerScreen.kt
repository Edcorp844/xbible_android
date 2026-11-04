package com.example.xbible.feature.tools.audiobible

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButtonShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xbible.core.ui.layout.LayoutMode
import com.example.xbible.core.ui.layout.LocalLayoutInfo
import com.example.xbible.viewmodel.AudioBibleViewModel
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.material3.rememberDynamicColorScheme
import kotlinx.coroutines.launch
import uniffi.xbible_engine.AudioModuleInfo
import uniffi.xbible_engine.AudioNode

/**
 * Audio Bible player.
 *
 * Layout is chosen once from [LocalLayoutInfo.mode]:
 *
 *  - **[LayoutMode.PhonePortrait]** — vertical: artwork, title, progress,
 *    controls. The lyrics toggle replaces the middle stack when active.
 *  - **[LayoutMode.PhoneLandscape]** — horizontal single pane: artwork on
 *    the left, controls (or lyrics, toggled) on the right.
 *  - **[LayoutMode.TabletPortrait] / [LayoutMode.TabletLandscape]** — two
 *    panes: artwork + controls on the left, lyrics always visible on the
 *    right. Rotating a tablet only tunes the split ratio; the structure is
 *    identical in both orientations.
 *
 * The background gradient is seeded from the album artwork; the surrounding
 * [MaterialTheme] is re-tinted from that seed so sliders/buttons match.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AudioBiblePlayerScreen(
    viewModel: AudioBibleViewModel,
    onBack: () -> Unit,
) {
    val layout = LocalLayoutInfo.current

    val selectedModule by viewModel.selectedModule.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentTimeMs by viewModel.currentTimeMs.collectAsState()
    val currentActiveTitle by viewModel.currentActiveTitle.collectAsState()
    val decodedArtwork by viewModel.decodedArtwork.collectAsState()
    val gradient by viewModel.backgroundGradientColors.collectAsState()
    val activeText by viewModel.activeText.collectAsState()
    val selectedNodeId by viewModel.selectedNodeId.collectAsState()

    // Lyrics toggle is only meaningful on phones — tablets always show them.
    var showLyrics by remember { mutableStateOf(false) }

    val displayGradient = remember(gradient) {
        when {
            gradient.size >= 2 -> gradient.map { Color(it) }
            gradient.size == 1 -> listOf(Color(gradient[0]), Color(gradient[0]))
            else -> listOf(Color(0xFF1A1A1A), Color(0xFF000000))
        }
    }

    val seedColor = remember(displayGradient) { displayGradient.first() }
    val expressiveColorScheme = rememberDynamicColorScheme(
        seedColor = seedColor,
        isDark = true,
        style = PaletteStyle.Expressive,
        specVersion = ColorSpec.SpecVersion.SPEC_2025,
    )

    val durationMs = selectedModule?.metadata?.durationMs ?: 1L

    MaterialTheme(colorScheme = expressiveColorScheme) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                    ),
                )
            },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(colors = displayGradient)),
            ) {
                // Dark scrim for text legibility over the gradient.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                )

                when (layout.mode) {
                    // Tablets: two panes in *both* orientations. The split
                    // composable adjusts its internal weights from the mode.
                    LayoutMode.TabletPortrait,
                    LayoutMode.TabletLandscape -> TabletSplitContent(
                        padding = padding,
                        viewModel = viewModel,
                        decodedArtwork = decodedArtwork,
                        selectedModule = selectedModule,
                        currentActiveTitle = currentActiveTitle,
                        isPlaying = isPlaying,
                        currentTimeMs = currentTimeMs,
                        durationMs = durationMs,
                        activeText = activeText,
                        selectedNodeId = selectedNodeId,
                        mode = layout.mode,
                    )

                    // Phones in landscape: single-pane split, lyrics toggled.
                    LayoutMode.PhoneLandscape -> LandscapeContent(
                        padding = padding,
                        viewModel = viewModel,
                        decodedArtwork = decodedArtwork,
                        selectedModule = selectedModule,
                        currentActiveTitle = currentActiveTitle,
                        isPlaying = isPlaying,
                        currentTimeMs = currentTimeMs,
                        durationMs = durationMs,
                        activeText = activeText,
                        selectedNodeId = selectedNodeId,
                        showLyrics = showLyrics,
                        onToggleLyrics = { showLyrics = !showLyrics },
                    )

                    // Phones in portrait: vertical flow.
                    LayoutMode.PhonePortrait -> PortraitContent(
                        padding = padding,
                        viewModel = viewModel,
                        decodedArtwork = decodedArtwork,
                        selectedModule = selectedModule,
                        currentActiveTitle = currentActiveTitle,
                        isPlaying = isPlaying,
                        currentTimeMs = currentTimeMs,
                        durationMs = durationMs,
                        activeText = activeText,
                        selectedNodeId = selectedNodeId,
                        showLyrics = showLyrics,
                        onToggleLyrics = { showLyrics = !showLyrics },
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// Layouts
// ─────────────────────────────────────────────────────────────────────

/**
 * Vertical layout for portrait phones.
 */
@Composable
private fun PortraitContent(
    padding: PaddingValues,
    viewModel: AudioBibleViewModel,
    decodedArtwork: Bitmap?,
    selectedModule: AudioModuleInfo?,
    currentActiveTitle: String,
    isPlaying: Boolean,
    currentTimeMs: Long,
    durationMs: Long,
    activeText: String,
    selectedNodeId: String?,
    showLyrics: Boolean,
    onToggleLyrics: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showLyrics) {
            LyricsPane(
                viewModel = viewModel,
                activeText = activeText,
                currentActiveTitle = currentActiveTitle,
                selectedNodeId = selectedNodeId,
                currentTimeMs = currentTimeMs,
                theme = MaterialTheme,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        } else {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                ArtworkBlock(decodedArtwork, Modifier.weight(1f, fill = false))
                Spacer(Modifier.height(32.dp))
                TitleBlock(currentActiveTitle, selectedModule, viewModel::stopPlayback)
                Spacer(Modifier.height(32.dp))
                ProgressBlock(viewModel, currentTimeMs, durationMs)
                Spacer(Modifier.height(32.dp))
                PlaybackControls(viewModel, isPlaying)
            }
        }

        Spacer(Modifier.height(24.dp))
        BottomActionBar(showLyrics, onToggleLyrics)
        Spacer(Modifier.height(24.dp))
    }
}

/**
 * Horizontal single-pane layout for phones in landscape. Artwork on the left;
 * controls or lyrics on the right. The bottom action bar sits under the right
 * column so the toggle is always reachable.
 */
@Composable
private fun LandscapeContent(
    padding: PaddingValues,
    viewModel: AudioBibleViewModel,
    decodedArtwork: Bitmap?,
    selectedModule: AudioModuleInfo?,
    currentActiveTitle: String,
    isPlaying: Boolean,
    currentTimeMs: Long,
    durationMs: Long,
    activeText: String,
    selectedNodeId: String?,
    showLyrics: Boolean,
    onToggleLyrics: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // Left: artwork
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            ArtworkBlock(
                bitmap = decodedArtwork,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f),
            )
        }

        // Right: controls or lyrics
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
        ) {
            if (showLyrics) {
                LyricsPane(
                    viewModel = viewModel,
                    activeText = activeText,
                    currentActiveTitle = currentActiveTitle,
                    selectedNodeId = selectedNodeId,
                    currentTimeMs = currentTimeMs,
                    theme = MaterialTheme,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
            } else {
                // Top row: Title and Action Bar next to each other
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        TitleBlock(currentActiveTitle, selectedModule, viewModel::stopPlayback)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    CompactActionBar(showLyrics, onToggleLyrics)
                }

                Spacer(Modifier.height(20.dp))
                ProgressBlock(viewModel, currentTimeMs, durationMs)
                Spacer(Modifier.height(20.dp))
                PlaybackControls(viewModel, isPlaying)
            }
        }
    }
}



/**
 * Two-pane layout for tablets — same structure in portrait and landscape.
 *
 * Orientation only adjusts the split ratio and outer padding:
 *  - Portrait: 0.40 / 0.60 (narrower artwork column, more horizontal room
 *    for the lyrics).
 *  - Landscape: 0.45 / 0.55 (more even, since width is abundant).
 *
 * The lyrics pane is transparent — the player gradient shows through, no
 * extra container, no rounded corners, no surface tint.
 */
@Composable
private fun TabletSplitContent(
    padding: PaddingValues,
    viewModel: AudioBibleViewModel,
    decodedArtwork: Bitmap?,
    selectedModule: AudioModuleInfo?,
    currentActiveTitle: String,
    isPlaying: Boolean,
    currentTimeMs: Long,
    durationMs: Long,
    activeText: String,
    selectedNodeId: String?,
    mode: LayoutMode,
) {
    val leftWeight = if (mode == LayoutMode.TabletPortrait) 0.40f else 0.45f
    val rightWeight = 1f - leftWeight
    val outerPadding = if (mode == LayoutMode.TabletPortrait) 20.dp else 32.dp

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = outerPadding, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(outerPadding),
    ) {
        // Left pane: artwork + controls.
        Column(
            modifier = Modifier
                .weight(leftWeight)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ArtworkBlock(
                bitmap = decodedArtwork,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            )
            Spacer(Modifier.height(24.dp))
            TitleBlock(currentActiveTitle, selectedModule, viewModel::stopPlayback)
            Spacer(Modifier.height(20.dp))
            ProgressBlock(viewModel, currentTimeMs, durationMs)
            Spacer(Modifier.height(20.dp))
            PlaybackControls(viewModel, isPlaying)
        }

        // Right pane: lyrics, no background — the gradient is the backdrop.
        Box(
            modifier = Modifier
                .weight(rightWeight)
                .fillMaxHeight(),
        ) {
            LyricsPane(
                viewModel = viewModel,
                activeText = activeText,
                currentActiveTitle = currentActiveTitle,
                selectedNodeId = selectedNodeId,
                currentTimeMs = currentTimeMs,
                theme = MaterialTheme,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// Shared sub-components
// ─────────────────────────────────────────────────────────────────────

@Composable
private fun ArtworkBlock(
    bitmap: Bitmap?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        val shape = RoundedCornerShape(20.dp)
        val artworkModifier = Modifier
            .fillMaxWidth(0.85f)
            .aspectRatio(1f)
            .clip(shape)
            .shadow(
                elevation = 20.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.25f),
                spotColor = Color.Black.copy(alpha = 0.35f),
            )

        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Album Art",
                contentScale = ContentScale.Crop,
                modifier = artworkModifier,
            )
        } else {
            Box(
                modifier = artworkModifier
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TitleBlock(
    currentActiveTitle: String,
    selectedModule: AudioModuleInfo?,
    onStop: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = currentActiveTitle.ifEmpty {
                    selectedModule?.metadata?.displayTitle ?: "Unknown Title"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = selectedModule?.metadata?.contributor ?: "Unknown Artist",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        FilledTonalIconButton(
            onClick = onStop,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceDim,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            Icon(Icons.Default.Stop, contentDescription = "Stop")
        }
    }
}

@Composable
private fun ProgressBlock(
    viewModel: AudioBibleViewModel,
    currentTimeMs: Long,
    durationMs: Long,
) {
    val sliderState = rememberSliderState(value = 0f, steps = 0, trackRange = 0f..1f)
    var isDragging by remember { mutableStateOf(false) }

    if (!isDragging) {
        sliderState.value =
            (currentTimeMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    }

    Slider(
        state = sliderState,
        onValueChange = { fraction ->
            isDragging = true
            sliderState.value = fraction
        },
        onValueChangeFinished = {
            isDragging = false
            viewModel.seekToTime((sliderState.value * durationMs).toLong())
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = durationMs > 0,
        colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
        ),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            viewModel.formatTime(currentTimeMs),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            viewModel.formatTime(durationMs),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PlaybackControls(
    viewModel: AudioBibleViewModel,
    isPlaying: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { /* TODO: playback speed */ }) {
            Icon(
                Icons.Default.Speed,
                contentDescription = "Playback Speed",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalIconButton(
                onClick = viewModel::skipBackward,
                modifier = Modifier.size(64.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceDim,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Icon(
                    Icons.Default.Replay10,
                    contentDescription = "Rewind",
                    modifier = Modifier.size(28.dp),
                )
            }

            FilledIconToggleButton(
                checked = isPlaying,
                onCheckedChange = { viewModel.togglePlayback() },
                modifier = Modifier.size(80.dp),
                shapes = IconToggleButtonShapes(
                    shape = RoundedCornerShape(24.dp),
                    pressedShape = RoundedCornerShape(20.dp),
                ),
                colors = IconButtonDefaults.filledIconToggleButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    checkedContainerColor = MaterialTheme.colorScheme.primary,
                    checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    modifier = Modifier.size(40.dp),
                )
            }

            FilledTonalIconButton(
                onClick = viewModel::skipForward,
                modifier = Modifier.size(64.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceDim,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Icon(
                    Icons.Default.Forward30,
                    contentDescription = "Forward",
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        IconButton(onClick = { /* TODO: repeat mode */ }) {
            Icon(
                Icons.Default.Repeat,
                contentDescription = "Repeat Mode",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun BottomActionBar(
    showLyrics: Boolean,
    onToggleLyrics: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(50.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp,
                        bottomStart = 20.dp,
                        topEnd = 8.dp,
                        bottomEnd = 8.dp,
                    ),
                )
                .background(
                    if (showLyrics) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surface,
                )
                .clickable(onClick = onToggleLyrics),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Lyrics,
                contentDescription = "Lyrics",
                tint = if (showLyrics) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp),
            )
        }

        Box(
            modifier = Modifier
                .width(100.dp)
                .height(50.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 8.dp,
                        bottomStart = 8.dp,
                        topEnd = 20.dp,
                        bottomEnd = 20.dp,
                    ),
                )
                .background(MaterialTheme.colorScheme.surface)
                .clickable { /* TODO: cast */ },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Cast,
                contentDescription = "Cast",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun CompactActionBar(
    showLyrics: Boolean,
    onToggleLyrics: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Lyrics Toggle
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (showLyrics) MaterialTheme.colorScheme.primary
                    else Color.Transparent
                )
                .clickable(onClick = onToggleLyrics),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Lyrics,
                contentDescription = "Lyrics",
                tint = if (showLyrics) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }

        // Cast
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable { /* TODO: cast */ },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Cast,
                contentDescription = "Cast",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// Lyrics pane
// ─────────────────────────────────────────────────────────────────────

@Composable
private fun LyricsPane(
    viewModel: AudioBibleViewModel,
    activeText: String,
    currentActiveTitle: String,
    selectedNodeId: String?,
    currentTimeMs: Long,
    theme: MaterialTheme,
    modifier: Modifier = Modifier,
) {
    val chapters = viewModel.cachedChaptersList
    val currentActiveSubtitle = viewModel.currentActiveSubtitle

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(activeText) {
        val cleaned = activeText.trim()
        if (cleaned.isEmpty()) return@LaunchedEffect

        val targetIndex = findActiveVerseIndex(chapters, cleaned)
        if (targetIndex >= 0) {
            scope.launch {
                listState.animateScrollToItem(index = targetIndex, scrollOffset = -180)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (chapters.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No lyrics data found",
                    color = Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    top = 110.dp,
                    bottom = 40.dp,
                    start = 24.dp,
                    end = 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(32.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                chapters.forEach { chapter ->
                    item(key = "header_${chapter.id}") {
                        Text(
                            text = chapter.title.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                            ),
                            color = Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                        )
                    }

                    items(
                        items = chapter.children,
                        key = { it.id },
                    ) { node ->
                        val verseText = node.text.orEmpty()
                        val isActive = activeText.isNotBlank() &&
                                activeText.trim() == verseText.trim()

                        VerseRow(
                            title = node.title,
                            text = verseText,
                            isActive = isActive,
                            onClick = { node.startMs?.let(viewModel::seekToTime) },
                        )
                    }
                }
            }
        }

        InteractiveNavigationCard(
            currentTitle = currentActiveTitle.ifEmpty { "Now Playing" },
            currentSubtitle = currentActiveSubtitle,
            chapters = chapters,
            selectedNodeId = selectedNodeId,
            currentTimeMs = currentTimeMs,
            onChapterSelected = viewModel::seekToChapter,
            color = theme.colorScheme.surfaceVariant,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(start = 20.dp, end = 20.dp),
        )
    }
}

@Composable
private fun VerseRow(
    title: String,
    text: String,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (isActive) Color(0xFFFF9800).copy(alpha = 0.85f)
            else Color.White.copy(alpha = 0.18f),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Medium,
                lineHeight = 32.sp,
            ),
            color = if (isActive) Color.White else Color.White.copy(alpha = 0.28f),
            modifier = Modifier.graphicsLayer {
                scaleX = if (isActive) 1.015f else 1f
                scaleY = if (isActive) 1.015f else 1f
            },
        )
    }
}

@Composable
private fun InteractiveNavigationCard(
    currentTitle: String,
    currentSubtitle: String,
    chapters: List<AudioNode>,
    selectedNodeId: String?,
    currentTimeMs: Long,
    onChapterSelected: (String) -> Unit,
    color: Color,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(color)
            .shadow(elevation = 16.dp, shape = RoundedCornerShape(20.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .background(color)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentTitle.ifEmpty { "Now Playing" },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = currentSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f),
                    maxLines = 1,
                )
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f),
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column {
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color)
                        .heightIn(max = 240.dp),
                ) {
                    items(chapters, key = { it.id }) { chapter ->
                        ChapterRow(
                            chapter = chapter,
                            chapterIndex = chapters.indexOf(chapter) + 1,
                            isSelected = selectedNodeId == chapter.id,
                            currentPlaybackMs = currentTimeMs,
                            onSelect = {
                                onChapterSelected(chapter.id)
                                isExpanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterRow(
    chapter: AudioNode,
    chapterIndex: Int,
    isSelected: Boolean,
    currentPlaybackMs: Long,
    onSelect: () -> Unit,
) {
    val start = chapter.startMs ?: 0L
    val end = chapter.endMs ?: 0L

    // 2 = currently playing, 0 = already passed, 1 = upcoming.
    val rowState = when {
        isSelected -> 2
        currentPlaybackMs >= start -> 0
        else -> 1
    }

    val titleColor = when (rowState) {
        2 -> Color.White
        0 -> Color.White.copy(alpha = 0.35f)
        else -> Color.White.copy(alpha = 0.7f)
    }
    val numberColor = when (rowState) {
        2 -> Color.White.copy(alpha = 0.9f)
        0 -> Color.White.copy(alpha = 0.25f)
        else -> Color.White.copy(alpha = 0.5f)
    }
    val durationColor = when (rowState) {
        2 -> Color.White.copy(alpha = 0.8f)
        0 -> Color.White.copy(alpha = 0.25f)
        else -> Color.White.copy(alpha = 0.4f)
    }

    val durationText = if (isSelected) {
        val remaining = maxOf(0L, end - currentPlaybackMs) / 1000
        "- %d:%02d".format(remaining / 60, remaining % 60)
    } else {
        val diff = maxOf(0L, (end - start) / 1000)
        if (diff == 0L) "10m" else "${diff / 60}m"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$chapterIndex",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = numberColor,
            modifier = Modifier.width(24.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = chapter.title,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (rowState == 2) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = titleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = durationText,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = durationColor,
        )
    }
}

private fun findActiveVerseIndex(
    chapters: List<AudioNode>,
    cleanedActiveText: String,
): Int {
    var index = 0
    for (chapter in chapters) {
        index++ // chapter header row
        for (node in chapter.children) {
            if (node.text?.trim() == cleanedActiveText) return index
            index++
        }
    }
    return -1
}