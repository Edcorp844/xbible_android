package com.example.xbible.feature.store.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uniffi.xbible_engine.StoreItem

private object BookColors {
    // Slate navy — for flagship translations
    val Navy = Color(0xFF3A4A5C)
    // Deep teal — for word-study Bibles
    val Teal = Color(0xFF3A5552)
    // Forest — for classic public-domain
    val Forest = Color(0xFF44524A)
    // Warm taupe — for commentaries
    val Taupe = Color(0xFF5C5145)
    // Muted burgundy — for study tools
    val Burgundy = Color(0xFF5A3F42)
    // Dusty plum — for reference / entities
    val Plum = Color(0xFF4A3F52)
}

private fun colorFor(item: StoreItem): Color {
    // Category first, then a small per-category palette.
    return when {
        item.kind == "entity_set" -> when (item.id) {
            "theographic:person" -> BookColors.Plum
            "theographic:event"  -> BookColors.Burgundy
            "theographic:place"  -> BookColors.Taupe
            "theographic:group"  -> BookColors.Forest
            else -> BookColors.Plum
        }
        item.kind == "commentary" -> BookColors.Taupe
        item.kind == "dataset" -> BookColors.Burgundy
        item.kind == "translation" -> when {
            item.hasStrongs -> BookColors.Teal    // word-study Bibles
            item.badge == "Public domain" -> BookColors.Forest
            else -> BookColors.Navy
        }
        else -> BookColors.Navy
    }
}

@Composable
fun TranslationCover(
    item: StoreItem,
    modifier: Modifier = Modifier,
) {
    val baseColor = remember(item.id) { colorFor(item) }

    Box(
        modifier = modifier
            .width(140.dp)
            .height(200.dp)
            .shadow(
                8.dp,
                RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 8.dp, bottomEnd = 8.dp)
            )
            .clip(
                RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 8.dp, bottomEnd = 8.dp)
            )
            .background(baseColor)
    ) {
        // Spine crease / 3D effect
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(20.dp)
                .background(
                    Brush.horizontalGradient(
                        0f to Color.Black.copy(alpha = 0.35f),
                        0.25f to Color.White.copy(alpha = 0.15f),
                        1f to Color.Transparent
                    )
                )
        )

        // Spine border highlight
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(3.dp)
                .padding(start = 1.dp)
                .background(Color.White.copy(alpha = 0.2f))
        )

        // Cover content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 20.dp, end = 12.dp, top = 24.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            // Description under the title
            Text(
                text = item.name,
                color = Color.White.copy(alpha = 0.92f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.weight(1f))

            // Footer: language + badge
            Text(
                text = item.languageName
                    ?: item.language.uppercase(),
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}