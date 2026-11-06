package com.example.xbible.core.ui.components

import androidx.compose.ui.graphics.Color

// 1. The colors available for highlighting
enum class HighlightColor(val label: String, val color: Color) {
    Yellow("Yellow", Color(0xFFFFF59D)),
    Green("Green", Color(0xFFA5D6A7)),
    Blue("Blue", Color(0xFF90CAF9)),
    Pink("Pink", Color(0xFFF48FB1)),
    Orange("Orange", Color(0xFFFFCC80))
}

// 2. How the highlight is drawn (Solid block, marker, or underline)
enum class HighlightStyle {
    Solid,
    Marker,
    Underline
}

// 3. The data class that holds the user's choice for a specific verse
data class VerseHighlight(
    val color: HighlightColor,
    val style: HighlightStyle
)