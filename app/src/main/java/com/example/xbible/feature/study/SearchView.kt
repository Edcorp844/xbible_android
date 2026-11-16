package com.example.xbible.feature.study

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.xbible.ui.icons.XBibleIcons
import com.example.xbible.viewmodel.ReaderTab
import uniffi.xbible_engine.SearchHit

@Composable
fun SearchView(
    tab: ReaderTab,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onClearSearch: () -> Unit,
    onCancelSearch: () -> Unit,
    onHitClick: (SearchHit) -> Unit,
    onHitOpenInNewTab: (SearchHit) -> Unit,
    onHitCopy: (SearchHit) -> Unit,
) {
    // Local state for the text field. We sync it from the tab on first
    // composition and when the user clears the search, but we don't
    // overwrite it on every keystroke (that causes cursor jumping).
    var query by remember(tab.id) { mutableStateOf(tab.searchQuery) }

    // If the VM clears the search (e.g. a new tab is active), sync down.
    LaunchedEffect(tab.searchQuery) {
        if (tab.searchQuery.isEmpty() && query.isNotEmpty()) {
            query = ""
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                onQueryChange(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = {
                Text("Search ${tab.translation?.shortName ?: "Bible"}...")
            },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = {
                        query = ""
                        onClearSearch()
                    }) {
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
            keyboardActions = KeyboardActions(onSearch = {
                onSearch(query)
            }),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        )

        // ── States ──────────────────────────────────────────────────
        val results = tab.searchResults

        when {
            tab.searchError != null -> {
                ErrorState(message = tab.searchError)
            }

            tab.searchQuery.isEmpty() -> {
                EmptyState()
            }

            tab.isSearching && results == null -> {
                LoadingState(progress = tab.searchProgress)
            }

            results != null && results.totalHits == 0 -> {
                NoResultsState(query = tab.searchQuery)
            }

            results != null -> {
                ResultsList(
                    results = results,
                    onHitClick = onHitClick,
                    onHitOpenInNewTab = onHitOpenInNewTab,
                    onHitCopy = onHitCopy,
                )
            }
        }
    }
}

// ── States ──────────────────────────────────────────────────────────

@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Search text, a reference (JHN 3:16), or a Strong's number (H430).",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(32.dp),
        )
    }
}

@Composable
private fun LoadingState(progress: Float) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(48.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NoResultsState(query: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "No verses matched \"$query\".",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ErrorState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(32.dp),
        )
    }
}

// ── Results List ────────────────────────────────────────────────────

@Composable
private fun ResultsList(
    results: uniffi.xbible_engine.SearchResults,
    onHitClick: (SearchHit) -> Unit,
    onHitOpenInNewTab: (SearchHit) -> Unit,
    onHitCopy: (SearchHit) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${results.totalHits} result${if (results.totalHits == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${results.tookMs} ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(results.hits, key = { "${it.translationId}-${it.bookId}-${it.chapter}-${it.verse}" }) { hit ->
            SearchHitItem(
                hit = hit,
                onClick = { onHitClick(hit) },
                onOpenInNewTab = { onHitOpenInNewTab(hit) },
                onCopy = { onHitCopy(hit) },
            )
        }
    }
}

@Composable
private fun SearchHitItem(
    hit: SearchHit,
    onClick: () -> Unit,
    onOpenInNewTab: () -> Unit,
    onCopy: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${hit.bookName} ${hit.chapter}:${hit.verse}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = hit.translationId.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f),
                        RoundedCornerShape(4.dp),
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // The highlighted verse text
        Text(
            text = buildHighlightedText(hit),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 4,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onOpenInNewTab) {
                Icon(
                    XBibleIcons.OpenInTab,
                    contentDescription = "Open in New Tab",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onCopy) {
                Icon(
                    XBibleIcons.Copy,
                    contentDescription = "Copy Verse",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        )
    }
}

// ── Highlight Rendering ─────────────────────────────────────────────

/**
 * Renders a SearchHit's text with its highlight spans applied as a
 * tinted background.
 *
 * The spans are **byte** offsets in Rust's String. For ASCII and
 * Latin-1 text (BSB, KJV, ach_abr), byte count equals character count,
 * so we can slice the Kotlin String directly. For text with combining
 * marks, the offsets would need a mapping layer in Rust — that is a
 * search.rs change, not a UI one.
 */
@Composable
private fun buildHighlightedText(hit: SearchHit) = buildAnnotatedString {
    if (hit.highlights.isEmpty()) {
        append(hit.text)
        return@buildAnnotatedString
    }

    val text = hit.text
    var lastByte = 0

    for (span in hit.highlights) {
        val startByte = span.start.toInt().coerceIn(0, text.length)
        val endByte = span.end.toInt().coerceIn(0, text.length)

        if (startByte >= endByte) continue

        if (startByte > lastByte) {
            append(text.substring(lastByte, startByte))
        }

        withStyle(
            SpanStyle(
                background = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                color = MaterialTheme.colorScheme.onSurface,
            )
        ) {
            append(text.substring(startByte, endByte))
        }

        lastByte = endByte
    }

    if (lastByte < text.length) {
        append(text.substring(lastByte))
    }
}