package com.example.xbible.feature.study

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.xbible.ui.icons.XBibleIcons
import com.example.xbible.viewmodel.ReaderTab
import uniffi.xbible_engine.ReaderChapter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchView(
    tab: ReaderTab,
    onSearch: (String) -> Unit,
    onReferenceClick: (String) -> Unit,
    onCopyVerse: (String, ReaderChapter) -> Unit,
    onOpenInNewTab: (String) -> Unit,
    onPreviewChapter: (String) -> Unit,
    onLexiconLookup: (String) -> Unit,
) {
    var query by remember(tab.id) { mutableStateOf(tab.searchQuery) }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = {
                Text("Search ${tab.translation?.shortName ?: "Bible"}...")
            },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            } else null,
            shape = CircleShape,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            ),
            singleLine = true,
            keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        )

        if (tab.isSearching) {
            LinearProgressIndicator(
                progress = { tab.searchLoadProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }

        if (tab.searchHits.isEmpty() && tab.searchQuery.isNotEmpty() && !tab.isSearching) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No results for \"${tab.searchQuery}\"")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(tab.searchHits, key = { it }) { hitKey ->
                    SearchHitItem(
                        hitKey = hitKey,
                        chapter = tab.searchHitsContent[hitKey],
                        query = tab.searchQuery,
                        onReferenceClick = { onReferenceClick(hitKey) },
                        onCopyVerse = { chapter -> onCopyVerse(hitKey, chapter) },
                        onOpenInNewTab = { onOpenInNewTab(hitKey) },
                        onPreviewChapter = { onPreviewChapter(hitKey) },
                        onLexiconLookup = { onLexiconLookup(hitKey) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchHitItem(
    hitKey: String,
    chapter: ReaderChapter?,
    query: String,
    onReferenceClick: () -> Unit,
    onCopyVerse: (ReaderChapter) -> Unit,
    onOpenInNewTab: () -> Unit,
    onPreviewChapter: () -> Unit,
    onLexiconLookup: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = chapter != null) { onReferenceClick() }
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = hitKey,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (chapter != null) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (chapter != null) {
            // Flatten the chapter into per-word pieces so we can highlight matches.
            val pieces = collectWords(chapter)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                pieces.forEach { piece ->
                    val isMatch = query.split(" ")
                        .any { q -> q.isNotEmpty() && piece.text.contains(q, ignoreCase = true) }

                    Text(
                        text = piece.text,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = if (isMatch) {
                            Modifier
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                    RoundedCornerShape(size = 8.dp),
                                )
                                .padding(horizontal = 4.dp)
                        } else Modifier,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPreviewChapter) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = "Preview Chapter",
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = onOpenInNewTab) {
                    Icon(
                        XBibleIcons.OpenInTab,
                        contentDescription = "Open in New Tab",
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = { onCopyVerse(chapter) }) {
                    Icon(
                        XBibleIcons.Copy,
                        contentDescription = "Copy Verse",
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = onLexiconLookup) {
                    Icon(
                        XBibleIcons.Lexicon,
                        contentDescription = "Lexicon",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        RoundedCornerShape(4.dp),
                    ),
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider()
    }
}

/**
 * Collect every renderable word from a chapter for the hit preview.
 *
 * `ReaderChapter.blocks` alternates between headings, verse blocks,
 * poems, and line breaks. For the search preview we flatten everything
 * into a linear sequence of `Piece` so the `FlowRow` can wrap them.
 */
private data class WordPiece(val text: String, val isWordsOfJesus: Boolean)

private fun collectWords(chapter: ReaderChapter): List<WordPiece> {
    val out = mutableListOf<WordPiece>()

    chapter.blocks.forEach { block ->
        when (block.itemType) {
            "verse" -> {
                block.verses.forEach { verse ->
                    if (verse.words.isNotEmpty()) {
                        verse.words.forEach { w ->
                            out += WordPiece(w.text, w.isWordsOfJesus)
                        }
                    } else {
                        // Fallback: split plain text so highlight still works.
                        verse.text.split(" ").forEach { tok ->
                            if (tok.isNotBlank()) out += WordPiece(tok, false)
                        }
                    }
                }
            }
            "heading", "hebrew_subtitle" -> {
                block.text.split(" ").forEach { tok ->
                    if (tok.isNotBlank()) out += WordPiece(tok, false)
                }
            }
            else -> Unit // line_break / unknown — nothing to render
        }
    }

    return out
}