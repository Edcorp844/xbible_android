package com.example.xbible.feature.store.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.xbible.core.ui.components.utils.AudioCollectionColorPalate
import com.example.xbible.viewmodel.InstalledCollectionNode
import uniffi.xbible_engine.AudioCollection

@Composable
fun CollectionThumbnail(
    seed: String,
    name: String,
    modifier: Modifier = Modifier,
) {
    val (a, b) = AudioCollectionColorPalate.gradientFor(seed)
    val initials = name.split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.linearGradient(listOf(a, b))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initials,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.9f),
        )
    }
}

@Composable
fun CollectionThumbnail(collection: AudioCollection, modifier: Modifier = Modifier) =
    CollectionThumbnail(seed = collection.id, name = collection.name, modifier = modifier)

@Composable
fun CollectionThumbnail(node: InstalledCollectionNode, modifier: Modifier = Modifier) =
    CollectionThumbnail(seed = node.id, name = node.name, modifier = modifier)