package com.example.xbible.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uniffi.xbible_engine.Section
import uniffi.xbible_engine.Word

@Composable
fun PageView(
    sections: List<Section>,
    modifier: Modifier = Modifier,
    onWordClick: ((Word) -> Unit)? = null,
    onStrongsClick: ((String) -> Unit)? = null
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        items(sections.size) { index ->
            SectionView(
                section = sections[index],
                onWordClick = onWordClick,
                onStrongsClick = onStrongsClick
            )
        }
    }
}
