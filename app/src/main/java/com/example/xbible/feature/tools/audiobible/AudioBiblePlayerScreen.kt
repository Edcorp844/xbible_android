package com.example.xbible.feature.tools.audiobible

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xbible.viewmodel.AudioBibleViewModel
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.material3.rememberDynamicColorScheme
import kotlinx.coroutines.launch
import uniffi.xbible_engine.AudioNode

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AudioBiblePlayerScreen(
    viewModel: AudioBibleViewModel,
    onBack: () -> Unit
) {
    val selectedModule by viewModel.selectedModule.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentTimeMs by viewModel.currentTimeMs.collectAsState()
    val currentActiveTitle by viewModel.currentActiveTitle.collectAsState()
    val decodedArtwork by viewModel.decodedArtwork.collectAsState()
    val gradient by viewModel.backgroundGradientColors.collectAsState()
    val activeText by viewModel.activeText.collectAsState()
    val selectedNodeId by viewModel.selectedNodeId.collectAsState()

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
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(colors = displayGradient))
            ) {
                // Dark overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // ──────────────────────────────────────────────
                    // MIDDLE SECTION
                    // ──────────────────────────────────────────────
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
                                .fillMaxWidth()
                        )
                    } else {
                        // Artwork + Controls (original layout)
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {

                            // 1. Large Artwork
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false)
                                    .aspectRatio(1f)
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (decodedArtwork != null) {
                                    Image(
                                        bitmap = decodedArtwork!!.asImageBitmap(),
                                        contentDescription = "Album Art",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth(0.85f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(20.dp))
                                            .shadow(
                                                elevation = 20.dp,
                                                shape = RoundedCornerShape(20.dp),
                                                ambientColor = Color.Black.copy(alpha = 0.25f),
                                                spotColor = Color.Black.copy(alpha = 0.35f)
                                            )
                                    )
                                } else {
                                    Box(
                                        Modifier
                                            .fillMaxWidth(0.85f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .shadow(
                                                elevation = 20.dp,
                                                shape = RoundedCornerShape(20.dp),
                                                ambientColor = Color.Black.copy(alpha = 0.25f),
                                                spotColor = Color.Black.copy(alpha = 0.35f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.MusicNote,
                                            null,
                                            Modifier.size(80.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            // 2. Title & Subtitle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
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
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = selectedModule?.metadata?.contributor ?: "Unknown Artist",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                FilledTonalIconButton(
                                    onClick = { viewModel.stopPlayback() },
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceDim,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = "Stop")
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            // 3. Progress Bar
                            val sliderState = rememberSliderState(
                                value = 0f,
                                steps = 0,
                                trackRange = 0f..1f
                            )
                            var isDragging by remember { mutableStateOf(false) }

                            if (!isDragging) {
                                val targetValue =
                                    (currentTimeMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                                sliderState.value = targetValue
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
                                    inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                )
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = viewModel.formatTime(currentTimeMs),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = viewModel.formatTime(durationMs),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            // 4. Playback Controls
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { /* TODO: Speed */ }) {
                                    Icon(
                                        Icons.Default.Speed,
                                        contentDescription = "Playback Speed",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilledTonalIconButton(
                                        onClick = { viewModel.skipBackward() },
                                        modifier = Modifier.size(64.dp),
                                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceDim,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    ) {
                                        Icon(
                                            Icons.Default.Replay10,
                                            contentDescription = "Rewind",
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }

                                    FilledIconToggleButton(
                                        checked = isPlaying,
                                        onCheckedChange = { viewModel.togglePlayback() },
                                        modifier = Modifier.size(80.dp),
                                        shapes = IconToggleButtonShapes(
                                            shape = RoundedCornerShape(24.dp),
                                            pressedShape = RoundedCornerShape(20.dp)
                                        ),
                                        colors = IconButtonDefaults.filledIconToggleButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                            checkedContainerColor = MaterialTheme.colorScheme.primary,
                                            checkedContentColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Play/Pause",
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }

                                    FilledTonalIconButton(
                                        onClick = { viewModel.skipForward() },
                                        modifier = Modifier.size(64.dp),
                                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceDim,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    ) {
                                        Icon(
                                            Icons.Default.Forward30,
                                            contentDescription = "Forward",
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                IconButton(onClick = { /* TODO: Repeat */ }) {
                                    Icon(
                                        Icons.Default.Repeat,
                                        contentDescription = "Repeat Mode",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ──────────────────────────────────────────────
                    // BOTTOM ACTION BAR
                    // ──────────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(28.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Lyrics button
                        Box(
                            modifier = Modifier
                                .width(100.dp)
                                .height(50.dp)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 20.dp,
                                        bottomStart = 20.dp,
                                        topEnd = 8.dp,
                                        bottomEnd = 8.dp
                                    )
                                )
                                .background(
                                    if (showLyrics) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surface
                                )
                                .clickable { showLyrics = !showLyrics },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lyrics,
                                contentDescription = "Lyrics",
                                tint = if (showLyrics) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Cast button
                        Box(
                            modifier = Modifier
                                .width(100.dp)
                                .height(50.dp)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 8.dp,
                                        bottomStart = 8.dp,
                                        topEnd = 20.dp,
                                        bottomEnd = 20.dp
                                    )
                                )
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { /* TODO: Cast */ },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cast,
                                contentDescription = "Cast",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Lyrics Pane
// ─────────────────────────────────────────────────────────────

@Composable
private fun LyricsPane(
    viewModel: AudioBibleViewModel,
    activeText: String,
    currentActiveTitle: String,
    selectedNodeId: String?,
    currentTimeMs: Long,
    theme: MaterialTheme,
    modifier: Modifier = Modifier
) {
    val chapters = viewModel.cachedChaptersList
    val currentActiveSubtitle = viewModel.currentActiveSubtitle

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Auto-scroll when the spoken text changes
    LaunchedEffect(activeText) {
        val cleaned = activeText.trim()
        if (cleaned.isEmpty()) return@LaunchedEffect

        val targetIndex = findActiveVerseIndex(chapters, cleaned)
        if (targetIndex >= 0) {
            scope.launch {
                listState.animateScrollToItem(
                    index = targetIndex,
                    scrollOffset = -180
                )
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {

        if (chapters.isEmpty()) {
            // Temporary debug – remove once data is flowing
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No lyrics data found",
                    color = Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    top = 110.dp,
                    bottom = 40.dp,
                    start = 24.dp,
                    end = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(32.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                chapters.forEach { chapter ->
                    // Chapter header
                    item(key = "header_${chapter.id}") {
                        Text(
                            text = chapter.title.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }

                    // Verses / sentences
                    items(
                        items = chapter.children,
                        key = { it.id }
                    ) { node ->
                        val verseText = node.text.orEmpty()
                        val isActive = activeText.isNotBlank() &&
                                activeText.trim() == verseText.trim()

                        VerseRow(
                            title = node.title,
                            text = verseText,
                            isActive = isActive,
                            onClick = {
                                node.startMs?.let { viewModel.seekToTime(it) }
                            }
                        )
                    }
                }
            }
        }

        // Floating Navigation Card (same position as iOS)
        InteractiveNavigationCard(
            currentTitle = currentActiveTitle.ifEmpty { "Now Playing" },
            currentSubtitle = currentActiveSubtitle,
            chapters = chapters,
            selectedNodeId = selectedNodeId,
            currentTimeMs = currentTimeMs,
            onChapterSelected = { viewModel.seekToChapter(it) },
            color= theme.colorScheme.surfaceVariant,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(start = 20.dp, end = 20.dp)
        )
    }
}

@Composable
private fun VerseRow(
    title: String,
    text: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Small “Verse X” label
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = if (isActive)
                Color(0xFFFF9800).copy(alpha = 0.85f)
            else
                Color.White.copy(alpha = 0.18f)
        )

        // Main scripture text
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Medium,
                lineHeight = 32.sp
            ),
            color = if (isActive) Color.White else Color.White.copy(alpha = 0.28f),
            modifier = Modifier.graphicsLayer {
                scaleX = if (isActive) 1.015f else 1f
                scaleY = if (isActive) 1.015f else 1f
            }
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
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(color)
            .shadow(elevation = 16.dp, shape = RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .background(color)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentTitle.ifEmpty { "Now Playing" },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = currentSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f),
                    maxLines = 1
                )
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f)
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color)
                        .heightIn(max = 240.dp)
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
                            }
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
    onSelect: () -> Unit
) {
    val start = chapter.startMs ?: 0L
    val end = chapter.endMs ?: 0L

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
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$chapterIndex",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = numberColor,
            modifier = Modifier.width(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = chapter.title,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (rowState == 2) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = titleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = durationText,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = durationColor
        )
    }
}

private fun findActiveVerseIndex(
    chapters: List<AudioNode>,
    cleanedActiveText: String
): Int {
    var index = 0
    for (chapter in chapters) {
        index++ // header
        for (node in chapter.children) {
            if (node.text?.trim() == cleanedActiveText) return index
            index++
        }
    }
    return -1
}