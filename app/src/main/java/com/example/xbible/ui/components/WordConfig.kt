package com.example.xbible.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class WordConfig(
    val fontSize: Float = 17f,
    val verticalSpacing: Dp = 2.dp,
    val primaryTextColor: Color = Color.Unspecified, // Uses MaterialTheme.colorScheme.onBackground
    val redWordsColor: Color = Color.Red,
    val showStrongsTags: Boolean = true,
    val strongsFontSize: Float = 9f,
    val morphFontSize: Float = 8f,
    val strongsTagColor: Color = Color.Unspecified,
    val morphTagColor: Color = Color.Unspecified,
    val tagBackgroundColor: Color = Color.LightGray.copy(alpha = 0.2f)
)
