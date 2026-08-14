package com.example.xbible.ui.components

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
import uniffi.xbible_engine.Verse
import uniffi.xbible_engine.Word

@Composable
fun VerseView(
    verse: Verse,
    modifier: Modifier = Modifier,
    onWordClick: ((Word) -> Unit)? = null,
    onStrongsClick: ((String) -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Verse Number Indicator
        Text(
            text = "${verse.number}",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 2.dp)
        )

        // Flow layout simulation for words inside a verse
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val commentaryTheme = WordConfig(fontSize = 18f)
            verse.words.forEach { word ->
                WordView(
                    word = word,
                    config = commentaryTheme,
                    onWordClick = { onWordClick?.invoke(word) },
                    onStrongsClick = { strong -> onStrongsClick?.invoke(strong) }
                )
            }
        }
    }
}
