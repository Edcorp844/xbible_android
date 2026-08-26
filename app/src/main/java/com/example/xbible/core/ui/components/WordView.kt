package com.example.xbible.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uniffi.xbible_engine.Word

@Composable
fun WordView(
    word: Word,
    modifier: Modifier = Modifier,
    config: WordConfig = WordConfig(),
    onWordClick: (() -> Unit)? = null,
    onStrongsClick: ((String) -> Unit)? = null,
    isTitle: Boolean = false
) {
    Column(
        modifier = modifier.wrapContentWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(config.verticalSpacing)
    ) {
        // Main Scripture Word Text
        val textColor =
            if (word.isRed && config.showRedWords) config.redWordsColor 
            else if (config.primaryTextColor == Color.Unspecified) MaterialTheme.colorScheme.onBackground 
            else config.primaryTextColor


        if (word.isAdded) {
            when (config.addedWordsStyle) {
                AddedWordStyle.Italics -> {
                    Text(
                        text = word.text,
                        fontSize = if (isTitle) config.fontSize.sp * 1.5 else config.fontSize.sp,
                        fontFamily = config.fontFamily,
                        fontWeight = if (word.isBoldText) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = FontStyle.Italic,
                        color = textColor,
                        modifier = Modifier.clickable(enabled = onWordClick != null) {
                            onWordClick?.invoke()
                        }
                    )
                }

                AddedWordStyle.Brackets -> {
                    val text = if (word.isFirstAdded && word.isLastAdded) {
                        "[" + word.text + "]"
                    } else if (word.isFirstAdded) {
                        "[" + word.text
                    } else if (word.isLastAdded) {
                        word.text + "]"
                    } else {
                        word.text
                    }

                    Text(
                        text = text,
                        fontSize = if (isTitle) config.fontSize.sp * 1.5 else config.fontSize.sp,
                        fontFamily = config.fontFamily,
                        fontWeight = if (word.isBoldText) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (word.isItalic) FontStyle.Italic else FontStyle.Normal,
                        color = textColor,
                        modifier = Modifier.clickable(enabled = onWordClick != null) {
                            onWordClick?.invoke()
                        }
                    )

                }

            }
        } else {
            Text(
                text = word.text,
                fontSize = if (isTitle) config.fontSize.sp * 1.5 else config.fontSize.sp,
                fontFamily = config.fontFamily,
                fontWeight = if (word.isBoldText) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (word.isItalic) FontStyle.Italic else FontStyle.Normal,
                color = textColor,
                modifier = Modifier.clickable(enabled = onWordClick != null) {
                    onWordClick?.invoke()
                }
            )
        }

        // Strong's, Lemma, and Morph Tag Metadata Containers
        if (word.lex != null) {
            val lex = word.lex!!
            
            // Strongs
            if (config.showStrongs && lex.strongs.isNotEmpty()) {
                lex.strongs.forEach { strong ->
                    TagContainer(
                        text = strong,
                        config = config,
                        color = if (config.strongsTagColor == Color.Unspecified) MaterialTheme.colorScheme.primary else config.strongsTagColor,
                        fontFamily = FontFamily.Monospace,
                        onClick = { onStrongsClick?.invoke(strong) }
                    )
                }
            }
            
            // Lemma
            if (config.showLemma && !lex.lemma.isNullOrEmpty()) {
                TagContainer(
                    text = lex.lemma!!,
                    config = config,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontFamily = config.fontFamily,
                    onClick = { /* Could open lexicon for lemma too */ }
                )
            }
            
            // Morph
            if (config.showMorph && lex.morph.isNotEmpty()) {
                lex.morph.forEach { morph ->
                    TagContainer(
                        text = morph,
                        config = config,
                        color = if (config.morphTagColor == Color.Unspecified) MaterialTheme.colorScheme.secondary else config.morphTagColor,
                        fontFamily = FontFamily.Default,
                        onClick = { /* Could open lexicon for morph too */ }
                    )
                }
            }
        }
    }
}

@Composable
fun TagContainer(
    text: String,
    config: WordConfig,
    color: Color,
    fontFamily: FontFamily,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(config.tagBackgroundColor)
            .padding(horizontal = 4.dp, vertical = 1.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontSize = config.strongsFontSize.sp,
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
