package com.example.xbible.core.ui.components.utils

import androidx.compose.ui.graphics.Color

object AudioCollectionColorPalate {
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