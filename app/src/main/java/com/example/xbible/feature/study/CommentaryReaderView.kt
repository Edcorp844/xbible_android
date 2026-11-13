package com.example.xbible.feature.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xbible.core.ui.components.WordConfig
import uniffi.xbible_engine.CommentaryChapter

/**
 * Reader-surface renderer for a commentary chapter.
 *
 * Mirrors the translation [PageView] in layout behaviour — the caller
 * passes the same [LazyListState] it uses for scroll tracking — but
 * renders homiletical blocks rather than verses. Commentary prose has
 * no word-level data (no red-letter, no Strong's, no footnotes, no
 * parallel column), so this view is deliberately simpler than [PageView].
 */
@Composable
fun CommentaryReaderView(
    chapter: CommentaryChapter,
    commentaryName: String,
    config: WordConfig,
    state: LazyListState,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        state = state,
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item(key = "header") {
            Column {
                Text(
                    text = commentaryName,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${chapter.bookName} ${chapter.chapterNumber}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                )
            }
        }

        items(
            items = chapter.blocks,
            key = { block -> "v${block.verseNumber}-${block.text.hashCode()}" },
        ) { block ->
            CommentaryBlockView(block = block, config = config)
        }
    }
}

@Composable
private fun CommentaryBlockView(
    block: uniffi.xbible_engine.CommentaryBlock,
    config: WordConfig,
) {
    Column {
        Text(
            text = "Verse ${block.verseNumber}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = block.text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = config.fontSize.sp,
                fontFamily = config.fontFamily,
                lineHeight = (config.fontSize * config.lineHeight).sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}