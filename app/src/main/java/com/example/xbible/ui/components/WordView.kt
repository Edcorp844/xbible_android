package com.example.xbible.ui.components

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
    onStrongsClick: ((String) -> Unit)? = null
) {
    Column(
        modifier = modifier.wrapContentWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(config.verticalSpacing)
    ) {
        // Main Scripture Word Text
        val textColor = if (word.isRed) config.redWordsColor else if (config.primaryTextColor == Color.Unspecified) MaterialTheme.colorScheme.onBackground else config.primaryTextColor

        Text(
            text = word.text,
            fontSize = config.fontSize.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = if (word.isBoldText) FontWeight.Bold else FontWeight.Normal,
            fontStyle = if (word.isItalic) FontStyle.Italic else FontStyle.Normal,
            color = textColor,
            modifier = Modifier.clickable(enabled = onWordClick != null) {
                onWordClick?.invoke()
            }
        )

        // Strong's Tag Metadata Container
        if (config.showStrongsTags && word.lex != null && (word.lex!!.strongs.isNotEmpty() || word.lex!!.morph.isNotEmpty())) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(config.tagBackgroundColor)
                    .padding(horizontal = 4.dp, vertical = 1.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                word.lex!!.strongs.firstOrNull()?.let { strong ->
                    val strongColor = if (config.strongsTagColor == Color.Unspecified) MaterialTheme.colorScheme.primary else config.strongsTagColor
                    Text(
                        text = strong,
                        fontSize = config.strongsFontSize.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = strongColor,
                        modifier = Modifier.clickable { onStrongsClick?.invoke(strong) }
                    )
                }
                word.lex!!.morph.firstOrNull()?.let { morph ->
                    val morphColor = if (config.morphTagColor == Color.Unspecified) MaterialTheme.colorScheme.secondary else config.morphTagColor
                    Text(
                        text = morph,
                        fontSize = config.morphFontSize.sp,
                        fontFamily = FontFamily.Default,
                        color = morphColor
                    )
                }
            }
        }
    }
}
