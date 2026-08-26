package com.example.xbible.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uniffi.xbible_engine.Section
import uniffi.xbible_engine.Word

@Composable
fun PageView(
    sections: List<Section>,
    modifier: Modifier = Modifier,
    parallelSections: List<Section> = emptyList(),
    config: WordConfig = WordConfig(),
    state: LazyListState = rememberLazyListState(),
    onWordClick: ((Word) -> Unit)? = null,
    onStrongsClick: ((String) -> Unit)? = null
) {
    val isParallel = parallelSections.isNotEmpty()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = state,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!isParallel) {
            items(sections) { section ->
                SectionView(
                    section = section,
                    config = config,
                    onWordClick = onWordClick,
                    onStrongsClick = onStrongsClick
                )
            }
        } else {
            val count = maxOf(sections.size, parallelSections.size)
            items(count) { index ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        sections.getOrNull(index)?.let {
                            SectionView(
                                section = it,
                                config = config,
                                onWordClick = onWordClick,
                                onStrongsClick = onStrongsClick
                            )
                        }
                    }
                    VerticalDivider(modifier = Modifier.height(IntrinsicSize.Min))
                    Box(modifier = Modifier.weight(1f)) {
                        parallelSections.getOrNull(index)?.let {
                            SectionView(
                                section = it,
                                config = config,
                                onWordClick = onWordClick,
                                onStrongsClick = onStrongsClick
                            )
                        }
                    }
                }
            }
        }
    }
}
