package com.example.xbible.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uniffi.xbible_engine.Section
import uniffi.xbible_engine.Word

@Composable
fun SectionView(
    section: Section,
    modifier: Modifier = Modifier,
    onWordClick: ((Word) -> Unit)? = null,
    onStrongsClick: ((String) -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- 1. SECTION TITLE ---
        if (section.title.isNotEmpty()) {
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val titleTheme = WordConfig(fontSize = 14f)
                section.title.forEach { item ->
                    WordView(
                        word = item,
                        config = titleTheme,
                        onWordClick = { onWordClick?.invoke(item) },
                        isTitle = true
                    )
                }
            }
        }

        // --- 2. VERSE ROWS ---
        section.verses.forEach { verse ->
            VerseView(
                verse = verse,
                onWordClick = onWordClick,
                onStrongsClick = onStrongsClick,
                textDirection = section.textDirection
            )
        }
    }
}
