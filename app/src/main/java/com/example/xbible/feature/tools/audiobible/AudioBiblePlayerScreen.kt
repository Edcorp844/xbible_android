package com.example.xbible.feature.tools.audiobible

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.xbible.viewmodel.AudioBibleViewModel
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.material3.rememberDynamicColorScheme

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

    // Gradient colors from Rust
    val gradient by viewModel.backgroundGradientColors.collectAsState()

    // Safe gradient: ensure at least 2 colors (Compose Brush requires it)
    val displayGradient = remember(gradient) {
        when {
            gradient.size >= 2 -> gradient.map { Color(it) }
            gradient.size == 1 -> listOf(Color(gradient[0]), Color(gradient[0]))
            else -> listOf(Color(0xFF1A1A1A), Color(0xFF000000))
        }
    }

    // Extract seed color for the Expressive scheme
    val seedColor = remember(displayGradient) { displayGradient.first() }

    // Generate the Expressive color scheme locally for this screen ONLY
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
                // Dark overlay to tone down the brightness, matching iOS
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                )

                // MAIN CONTENT: Centered vertically
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 24.dp),
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
                            )
                        } else {
                            Box(
                                Modifier
                                    .fillMaxWidth(0.85f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
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
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // 3. Progress Bar (Slider)
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

                        Spacer(modifier = Modifier.width(16.dp))

                        FilledIconToggleButton(
                            checked = isPlaying,
                            onCheckedChange = { viewModel.togglePlayback() },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
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

                        Spacer(modifier = Modifier.width(16.dp))

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

                    Spacer(modifier = Modifier.height(32.dp))

                    // 5. Bottom Action Bar (Lyrics, Cast)
                    // ... (rest of the file remains the same until the bottom action bar) ...

                    // 5. Bottom Action Bar (Lyrics, Cast)
                    // The Wrapping Background Container
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(28.dp)) // Outer container very rounded
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp), // Space between buttons
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Individual Button 1 (Lyrics)
                        Box(
                            modifier = Modifier
                                .width(100.dp) // Increased length
                                .height(50.dp)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 20.dp,    // Very rounded outer left
                                        bottomStart = 20.dp, // Very rounded outer left
                                        topEnd = 8.dp,      // Slightly less rounded inner right
                                        bottomEnd = 8.dp    // Slightly less rounded inner right
                                    )
                                )
                                .background(MaterialTheme.colorScheme.surface) // Inner lighter background
                                .clickable { /* TODO: Open Lyrics */ },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lyrics,
                                contentDescription = "Lyrics",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Individual Button 2 (Cast)
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

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}