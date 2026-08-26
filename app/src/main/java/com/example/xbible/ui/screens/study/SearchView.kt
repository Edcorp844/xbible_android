package com.example.xbible.ui.screens.study

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.xbible.ui.icons.XBibleIcons
import com.example.xbible.viewmodel.StudyTab
import uniffi.xbible_engine.SearchHit
import uniffi.xbible_engine.Section

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchView(
    tab: StudyTab,
    onSearch: (String) -> Unit,
    onReferenceClick: (String) -> Unit,
    onCopyVerse: (String, List<Section>) -> Unit,
    onOpenInNewTab: (SearchHit) -> Unit,
    onPreviewChapter: (String) -> Unit,
    onLexiconLookup: (String) -> Unit
) {
    var query by remember(tab.id) { mutableStateOf(tab.searchQuery) }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search ${tab.module?.name ?: "Bible"}...") },
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
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            ),
            singleLine = true,
            keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
        )

        if (tab.isSearching) {
            LinearProgressIndicator(
                progress = { tab.searchLoadProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }

        if (tab.searchResults.isEmpty() && tab.searchQuery.isNotEmpty() && !tab.isSearching) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No results for \"${tab.searchQuery}\"")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tab.searchResults) { hit ->
                    SearchHitItem(
                        hit = hit,
                        content = tab.searchHitsContent[hit.key],
                        query = tab.searchQuery,
                        onReferenceClick = { onReferenceClick(hit.key) },
                        onCopyVerse = { sections -> onCopyVerse(hit.key, sections) },
                        onOpenInNewTab = { onOpenInNewTab(hit) },
                        onPreviewChapter = { onPreviewChapter(hit.key) },
                        onLexiconLookup = { onLexiconLookup(hit.key) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchHitItem(
    hit: SearchHit,
    content: List<Section>?,
    query: String,
    onReferenceClick: () -> Unit,
    onCopyVerse: (List<Section>) -> Unit,
    onOpenInNewTab: () -> Unit,
    onPreviewChapter: () -> Unit,
    onLexiconLookup: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = content != null) { onReferenceClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = hit.key,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (content != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (content != null) {
            val words = content.flatMap { it.verses }.flatMap { it.words }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                words.forEach { word ->
                    val isMatch = query.split(" ").any { q ->
                        q.isNotEmpty() && word.text.contains(q, ignoreCase = true)
                    }
                    Text(
                        text = word.text,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = if (isMatch) {
                            Modifier
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer.copy(
                                        alpha = 0.7f
                                    ),
                                    RoundedCornerShape(size = 8.dp)
                                )
                                .padding(horizontal = 4.dp)
                        } else Modifier
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviewChapter) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = "Preview Chapter",
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onOpenInNewTab) {
                    Icon(
                        XBibleIcons.OpenInTab,
                        contentDescription = "Open in New Tab",
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = { onCopyVerse(content) }) {
                    Icon(
                        XBibleIcons.Copy,
                        contentDescription = "Copy Verse",
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onLexiconLookup) {
                    Icon(
                        XBibleIcons.Lexicon,
                        contentDescription = "Lexicon",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else {
            // Placeholder/Living space
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        RoundedCornerShape(4.dp)
                    )
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider()
    }
}
