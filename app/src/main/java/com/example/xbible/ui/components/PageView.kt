package com.example.xbible.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uniffi.xbible_engine.Section
import uniffi.xbible_engine.Word

@Composable
fun PageView(
    sections: List<Section>,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    onWordClick: ((Word) -> Unit)? = null,
    onStrongsClick: ((String) -> Unit)? = null
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = state,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(sections) { section ->
            SectionView(
                section = section,
                onWordClick = onWordClick,
                onStrongsClick = onStrongsClick
            )
        }
    }
}
