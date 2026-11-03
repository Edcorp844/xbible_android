package com.example.xbible.feature.tools.audiobible

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.xbible.viewmodel.AudioBibleViewModel

@Composable
fun AudioBiblePlayerScreen(
    viewModel: AudioBibleViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val selectedModule by viewModel.selectedModule.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentTimeMs by viewModel.currentTimeMs.collectAsState()
    val currentActiveTitle by viewModel.currentActiveTitle.collectAsState()
    val decodedArtwork by viewModel.decodedArtwork.collectAsState()
    val chapters by remember { derivedStateOf { viewModel.cachedChaptersList } }
    val activeText by viewModel.activeText.collectAsState()

    val gradient by viewModel.backgroundGradientColors.collectAsState()
    val color1 = Color(gradient.getOrNull(0) ?: 0xFF000000)
    val color2 = Color(gradient.getOrNull(1) ?: 0xFF000000)

    val listState = rememberLazyListState()

    // Auto-scroll lyrics
    LaunchedEffect(activeText) {
        if (activeText.isNotEmpty()) {
            val targetIndex = chapters.indexOfFirst { it.text == activeText }
            if (targetIndex != -1) {
                listState.animateScrollToItem(targetIndex + 1) // +1 for header
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Background Gradient
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to color1.copy(alpha = 0.8f),
                            0.4f to color2.copy(alpha = 0.4f),
                            0.8f to Color.Transparent
                        )
                    )
                )
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentPadding = PaddingValues(bottom = 120.dp) // Space for player controls
        ) {
            // Top Bar & Artwork
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 16.dp, bottom = 32.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.background(
                                Color.Black.copy(alpha = 0.2f),
                                CircleShape
                            )
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Artwork
                    if (decodedArtwork != null) {
                        Image(
                            bitmap = decodedArtwork!!.asImageBitmap(),
                            contentDescription = "Album Art",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(240.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                    } else {
                        Box(
                            Modifier
                                .size(240.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MusicNote, null, Modifier.size(64.dp))
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Text(
                        text = currentActiveTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    Text(
                        text = selectedModule?.metadata?.contributor ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Lyrics / Chapters
            items(chapters, key = { it.id }) { chapter ->
                val isActive = activeText == chapter.text

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    if (chapter.title.isNotEmpty()) {
                        Text(
                            text = chapter.title.uppercase(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Text(
                        text = chapter.text ?: "",
                        fontSize = if (isActive) 22.sp else 18.sp,
                        fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                        color = if (isActive) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = 0.5f
                        ),
                        lineHeight = 32.sp
                    )
                }
            }
        }

        // Bottom Floating Controls
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Slider
                    val durationMs = selectedModule?.metadata?.durationMs ?: 1L
                    Slider(
                        value = currentTimeMs.toFloat(),
                        onValueChange = { viewModel.seekToTime(it.toLong()) },
                        valueRange = 0f..durationMs.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            viewModel.formatTime(currentTimeMs),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            viewModel.formatTime(durationMs),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.skipBackward() }) {
                            Icon(
                                Icons.Default.Replay10,
                                contentDescription = "Rewind",
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        FilledIconButton(
                            onClick = { viewModel.togglePlayback() },
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        IconButton(onClick = { viewModel.skipForward() }) {
                            Icon(
                                Icons.Default.Forward10,
                                contentDescription = "Forward",
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}