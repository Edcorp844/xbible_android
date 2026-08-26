package com.example.xbible.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uniffi.xbible_engine.TextDirection
import uniffi.xbible_engine.Verse
import uniffi.xbible_engine.Word

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VerseView(
    verse: Verse,
    modifier: Modifier = Modifier,
    config: WordConfig = WordConfig(),
    onWordClick: ((Word) -> Unit)? = null,
    onStrongsClick: ((String) -> Unit)? = null,
    textDirection: TextDirection
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Verse Number Indicator
        if (textDirection == TextDirection.LTR) {
            Text(
                text = "${verse.number}",
                fontSize = (config.fontSize * 0.6f).sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Flow layout simulation for words inside a verse
        val horizontalSpacing = 6.dp + config.wordSpacing.dp
        val verticalSpacing = (config.lineHeight * 10).dp // Heuristic for line spacing in FlowRow

        FlowRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = if (textDirection == TextDirection.RTL) {
                Arrangement.spacedBy(horizontalSpacing, Alignment.End)
            } else {
                Arrangement.spacedBy(horizontalSpacing, Alignment.Start)
            },
            verticalArrangement = Arrangement.spacedBy(verticalSpacing)
        ) {
            verse.words.forEach { word ->
                WordView(
                    word = word,
                    config = config,
                    onWordClick = { onWordClick?.invoke(word) },
                    onStrongsClick = { strong -> onStrongsClick?.invoke(strong) },
                    isTitle = false
                )
            }
        }

        // Verse Number Indicator
        if (textDirection == TextDirection.RTL) {
            Text(
                text = "${verse.number}",
                fontSize = (config.fontSize * 0.6f).sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
