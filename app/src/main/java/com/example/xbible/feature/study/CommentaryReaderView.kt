package com.example.xbible.feature.study

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xbible.core.ui.components.WordConfig
import com.example.xbible.feature.study.components.ScriptureReferenceText
import uniffi.xbible_engine.CommentaryBlock
import uniffi.xbible_engine.CommentaryChapter

@Composable
fun CommentaryReaderView(
    chapter: CommentaryChapter,
    commentaryName: String,
    config: WordConfig,
    state: LazyListState,
    modifier: Modifier = Modifier,
    onReferenceClick: (bookId: String, chapter: Int, verseStart: Int?, verseEnd: Int?) -> Unit = { _, _, _, _ -> },
    selectedVerseNumber: Int? = null,
    onVerseLongPress: ((Int) -> Unit)? = null,
) {
    // EXACTLY match PageView:
    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        fontFamily = config.fontFamily,
        fontSize = config.fontSize.sp,
        lineHeight = (config.fontSize * config.lineHeight).sp,
        textAlign = if (config.justify) TextAlign.Justify else TextAlign.Start,
    )

    val textColor = config.primaryTextColor.takeIf { it != Color.Unspecified }
        ?: MaterialTheme.colorScheme.onSurface

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp), // Changed to 16.dp to match PageView
        state = state,
        contentPadding = PaddingValues(vertical = 16.dp),
        // Removed verticalArrangement.spacedBy(20.dp) to match PageView's flow
    ) {
        item(key = "header") {
            CommentaryHeader(
                commentaryName = commentaryName,
                chapter = chapter,
                config = config,
                textColor = textColor,
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        items(
            items = chapter.blocks,
            key = { block -> "v${block.verseNumber}-${block.text.hashCode()}" },
        ) { block ->
            CommentaryBlockView(
                block = block,
                bodyStyle = textStyle,
                bodyColor = textColor,
                onReferenceClick = onReferenceClick,
                isSelected = block.verseNumber == selectedVerseNumber,
                onVerseLongPress = onVerseLongPress,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CommentaryHeader(
    commentaryName: String,
    chapter: CommentaryChapter,
    config: WordConfig,
    textColor: Color,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = commentaryName,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = config.fontFamily,
                fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${chapter.bookName} ${chapter.chapterNumber}".uppercase(),
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = config.fontFamily,
                letterSpacing = 1.sp,
            ),
            color = textColor,
        )
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        )
    }
}

@Composable
private fun CommentaryBlockView(
    block: CommentaryBlock,
    bodyStyle: TextStyle,
    bodyColor: Color,
    onReferenceClick: (bookId: String, chapter: Int, verseStart: Int?, verseEnd: Int?) -> Unit,
    isSelected: Boolean,
    onVerseLongPress: ((Int) -> Unit)?,
) {
    val backgroundModifier = if (isSelected) {
        Modifier.background(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            RoundedCornerShape(4.dp)
        )
    } else {
        Modifier
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(backgroundModifier)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .pointerInput(block.verseNumber) {
                detectTapGestures(
                    onLongPress = { onVerseLongPress?.invoke(block.verseNumber) }
                )
            }
    ) {
        // Match the verse number style of PageView
        Text(
            text = "${block.verseNumber}",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = bodyStyle.fontFamily,
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 2.dp),
        )

        // Pass BOTH style and color to ScriptureReferenceText, exactly like PageView passes to Text
        ScriptureReferenceText(
            text = block.text,
            style = bodyStyle,
            onReferenceClick = onReferenceClick,
        )
    }
}
