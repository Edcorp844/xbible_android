package com.example.xbible.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class WordConfig(
    val fontSize: Float = 18f,
    val fontFamily: FontFamily = FontFamily.Serif,
    val verticalSpacing: Dp = 2.dp,
    val lineHeight: Float = 1.5f,
    val wordSpacing: Float = 0f,
    val justify: Boolean = false,
    val primaryTextColor: Color = Color.Unspecified, // Uses MaterialTheme.colorScheme.onBackground
    val showRedWords: Boolean = true,
    val redWordsColor: Color = Color.Red,
    val showStrongs: Boolean = true,
    val showLemma: Boolean = true,
    val showMorph: Boolean = true,
    val showVerseNotes: Boolean = true,
    val showWordNotes: Boolean = true,
    val strongsFontSize: Float = 9f,
    val morphFontSize: Float = 8f,
    val strongsTagColor: Color = Color.Unspecified,
    val morphTagColor: Color = Color.Unspecified,
    val tagBackgroundColor: Color = Color.LightGray.copy(alpha = 0.2f),
    val addedWordsStyle: AddedWordStyle = AddedWordStyle.Brackets
)
