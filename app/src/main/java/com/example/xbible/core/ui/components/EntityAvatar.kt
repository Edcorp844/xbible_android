package com.example.xbible.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uniffi.xbible_engine.EntityRef
import kotlin.math.absoluteValue

/**
 * Deterministic avatar for a biblical entity.
 *
 * Has no image assets and no network: the color and glyph are derived
 * from the entity's kind + id, so the same person/place/event always
 * renders the same avatar on every device and every install.
 */
@Composable
fun EntityAvatar(
    entity: EntityRef,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
) {
    val seed = remember(entity.kind, entity.id) {
        stableHash("${entity.kind.lowercase()}:${entity.id}")
    }
    val palette = avatarPaletteFor(entity.kind.lowercase())
    val base = palette[seed % palette.size]

    // Slight lightness variation so two adjacent chips with the same
    // kind don't look identical.
    val shade = (seed / palette.size) % 4
    val container = when (shade) {
        0 -> base.copy(alpha = 0.18f)
        1 -> base.copy(alpha = 0.24f)
        2 -> base.copy(alpha = 0.30f)
        else -> base.copy(alpha = 0.36f)
    }
    val glyph = glyphFor(entity, seed)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = glyph,
            color = base,
            fontWeight = FontWeight.SemiBold,
            fontSize = (size.value * 0.42f).sp,
            maxLines = 1,
        )
    }
}

/**
 * Two-letter glyph: prefer initials of the first two words
 * ("Mary Magdalene" → "MM"), otherwise the first letter + kind.
 */
private fun glyphFor(entity: EntityRef, seed: Int): String {
    val words = entity.name.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .filterNot { it.lowercase() in STOP_WORDS }

    val letters = when {
        words.isEmpty() -> "?"
        words.size == 1 -> words[0].take(1).uppercase()
        else -> (words[0].take(1) + words[1].take(1)).uppercase()
    }

    // Places/events with a single letter get a subtle disambiguator
    // from the seed so "Egypt" and "Edom" don't both read "E".
    return if (letters.length == 1 && entity.kind.lowercase() in setOf("place", "event")) {
        letters + ('A' + (seed % 26)).toString()
    } else {
        letters
    }
}

private val STOP_WORDS = setOf(
    "the", "of", "a", "an", "and", "in", "on", "at", "for", "to"
)

/**
 * One palette per entity kind so the reader can visually scan
 * "who vs. where vs. when" without reading the label.
 */
private fun avatarPaletteFor(kind: String): List<Color> = when (kind) {
    "person" -> listOf(
        Color(0xFF1E88E5), // blue
        Color(0xFF3949AB),
        Color(0xFF00897B),
        Color(0xFF5E35B1),
    )
    "place" -> listOf(
        Color(0xFF2E7D32), // green
        Color(0xFF558B2F),
        Color(0xFF00695C),
        Color(0xFF6D4C41),
    )
    "event" -> listOf(
        Color(0xFFEF6C00), // amber/orange
        Color(0xFFD84315),
        Color(0xFF8D6E63),
        Color(0xFFF9A825),
    )
    "group" -> listOf(
        Color(0xFF6A1B9A), // purple
        Color(0xFFAD1457),
        Color(0xFF4527A0),
        Color(0xFF283593),
    )
    else -> listOf(
        Color(0xFF546E7A),
        Color(0xFF455A64),
        Color(0xFF607D8B),
    )
}

/**
 * Stable 32-bit hash (FNV-1a). Avoids `String.hashCode()` because
 * that is JVM-specified but still surprises people across platforms,
 * and we want "same entity → same avatar" to be an explicit guarantee.
 */
private fun stableHash(s: String): Int {
    var h = -0x7ee3623b // 2166136261 as signed int
    for (c in s) {
        h = h xor c.code
        h *= 16777619
    }
    return h.absoluteValue
}