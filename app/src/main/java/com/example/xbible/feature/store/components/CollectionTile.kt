package com.example.xbible.feature.store.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import uniffi.xbible_engine.AudioCollection
import uniffi.xbible_engine.AudioContributor
import uniffi.xbible_engine.ContributorRole

@Composable
fun CollectionTile(
    collection: AudioCollection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CollectionThumbnail(collection, Modifier.size(50.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(collection.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(
                collection.description ?: "Audio Collection",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CollectionThumbnail(collection: AudioCollection, modifier: Modifier = Modifier) {
    val (a, b) = CollectionPalette.gradientFor(collection.id)
    val initials = collection.name.split(" ").take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.linearGradient(listOf(a, b))),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.9f))
    }
}

object CollectionPalette {
    private val pairs = listOf(
        Color(0.98f, 0.87f, 0.80f) to Color(0.96f, 0.72f, 0.65f),
        Color(0.85f, 0.92f, 0.98f) to Color(0.68f, 0.82f, 0.94f),
        Color(0.90f, 0.96f, 0.88f) to Color(0.72f, 0.88f, 0.72f),
        Color(0.97f, 0.88f, 0.94f) to Color(0.92f, 0.74f, 0.85f),
        Color(0.92f, 0.90f, 0.98f) to Color(0.79f, 0.76f, 0.94f),
        Color(0.99f, 0.94f, 0.82f) to Color(0.95f, 0.84f, 0.62f),
        Color(0.86f, 0.94f, 0.95f) to Color(0.68f, 0.86f, 0.88f),
        Color(0.96f, 0.88f, 0.86f) to Color(0.90f, 0.72f, 0.70f),
        Color(0.90f, 0.93f, 0.86f) to Color(0.76f, 0.84f, 0.68f),
        Color(0.94f, 0.90f, 0.96f) to Color(0.82f, 0.76f, 0.90f),
    )
    fun gradientFor(id: String): Pair<Color, Color> {
        var h = 0xcbf29ce484222325UL
        for (b in id.toByteArray()) { h = h xor b.toULong(); h *= 0x100000001b3UL }
        return pairs[(h % pairs.size.toULong()).toInt()]
    }
}
