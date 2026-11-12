package com.example.xbible.feature.study

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedToggleButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.xbible.R
import com.example.xbible.viewmodel.SearchType
import uniffi.xbible_engine.FreeBibleBook
import uniffi.xbible_engine.InstalledTranslation

// ── Translation selection ────────────────────────────────────────

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ModuleSelectionContent(
    translations: List<InstalledTranslation>,
    current: InstalledTranslation?,
    onTranslationSelected: (InstalledTranslation) -> Unit,
) {
    val groupedByLanguage = remember(translations) {
        translations.groupBy { it.language }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp),
    ) {
        Text(
            text = stringResource(R.string.select_bible_version),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp),
            fontWeight = FontWeight.Bold,
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            groupedByLanguage.keys.sorted().forEach { langCode ->
                val items = groupedByLanguage[langCode] ?: emptyList()

                // Extract the language name from the first translation in the group.
                // Fallback to uppercase code if the name is missing.
                val languageName = items.firstOrNull()?.languageName?.takeIf { it.isNotBlank() }
                    ?: langCode.uppercase()

                item(key = "lang:$langCode") {
                    SettingsGroup(title = languageName) {
                        items.forEach { t ->
                            val isSelected = current?.id == t.id
                            ListItem(
                                modifier = Modifier.clickable { onTranslationSelected(t) },
                                colors = ListItemDefaults.colors(
                                    containerColor = Color.Transparent,
                                ),
                                headlineContent = {
                                    Text(
                                        text = t.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold
                                        else FontWeight.Normal,
                                    )
                                },
                                supportingContent = {
                                    val subtitle = buildString {
                                        if (t.englishName.isNotBlank() &&
                                            t.englishName != t.name
                                        ) {
                                            append(t.englishName)
                                            append(" · ")
                                        }
                                        append("${t.shortName} · ")
                                        append("${t.bookCount} books · ")
                                        append("${t.verseCount} verses")
                                    }
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                trailingContent = {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = null,
                                    )
                                },
                            )
                        }
                    }
                }
                item(key = "lang-gap:$langCode") { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }
}

// ── Search settings ──────────────────────────────────────────────

@Composable
fun SearchSettingsContent(
    currentSearchType: SearchType,
    onSearchTypeSelected: (SearchType) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp),
    ) {
        Text(
            text = stringResource(R.string.search_settings),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp),
            fontWeight = FontWeight.Bold,
        )

        SettingsGroup(title = stringResource(R.string.search_type_label)) {
            SearchType.entries.forEach { type ->
                val isSelected = currentSearchType == type
                ListItem(
                    modifier = Modifier.clickable { onSearchTypeSelected(type) },
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent,
                    ),
                    headlineContent = {
                        Text(
                            text = type.name
                                .replace("_", " ")
                                .lowercase()
                                .replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isSelected) FontWeight.Bold
                            else FontWeight.Normal,
                        )
                    },
                    trailingContent = {
                        RadioButton(selected = isSelected, onClick = null)
                    },
                )
            }
        }
    }
}

// ── Reference picker (books + chapters) ─────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferencePickerContent(
    books: List<FreeBibleBook>,
    currentBookId: String?,
    currentChapter: Int,
    onSelectBook: (String) -> Unit,
    onSelectChapter: (Int) -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    // Derive the currently selected index from the id
    val selectedBookIndex = remember(books, currentBookId) {
        books.indexOfFirst { it.id == currentBookId }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ButtonGroup(
            overflowIndicator = { menuState ->
                IconButton(onClick = { menuState.show() }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(
                ButtonGroupDefaults.ConnectedSpaceBetween,
                Alignment.CenterHorizontally,
            ),
        ) {
            customItem(
                buttonGroupContent = {
                    OutlinedToggleButton(
                        checked = selectedTab == 0,
                        onCheckedChange = { if (it) selectedTab = 0 },
                        shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                    ) {
                        Text(stringResource(R.string.books))
                    }
                },
                menuContent = { menuState ->
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.books)) },
                        onClick = {
                            selectedTab = 0
                            menuState.dismiss()
                        },
                    )
                },
            )
            customItem(
                buttonGroupContent = {
                    OutlinedToggleButton(
                        checked = selectedTab == 1,
                        onCheckedChange = { if (it) selectedTab = 1 },
                        shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                    ) {
                        Text(stringResource(R.string.chapters))
                    }
                },
                menuContent = { menuState ->
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.chapters)) },
                        onClick = {
                            selectedTab = 1
                            menuState.dismiss()
                        },
                    )
                },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.weight(1f)) {
            if (selectedTab == 0) {
                BookGrid(
                    books = books,
                    currentBookId = currentBookId,
                    onSelect = { book ->
                        onSelectBook(book.id)
                        selectedTab = 1
                    },
                )
            } else {
                if (selectedBookIndex in books.indices) {
                    val book = books[selectedBookIndex]
                    ChapterGrid(
                        book = book,
                        currentChapter = currentChapter,
                        onSelect = onSelectChapter,
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(stringResource(R.string.select_book_first))
                    }
                }
            }
        }
    }
}

@Composable
private fun BookGrid(
    books: List<FreeBibleBook>,
    currentBookId: String?,
    onSelect: (FreeBibleBook) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        gridItemsIndexed(books, key = { _, b -> b.id }) { _, book ->
            val isSelected = book.id == currentBookId
            Surface(
                modifier = Modifier
                    .aspectRatio(2f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelect(book) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = book.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.onSecondary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChapterGrid(
    book: FreeBibleBook,
    currentChapter: Int,
    onSelect: (Int) -> Unit,
) {
    val chapterCount = book.numberOfChapters
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        gridItemsIndexed((1..chapterCount).toList(), key = { _, ch -> ch }) { _, ch ->
            val isSelected = ch == currentChapter
            Surface(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelect(ch) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = ch.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.onSecondary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ── Shared group container ──────────────────────────────────────

@Composable
fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
            )
        }
        Surface(
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(content = content)
        }
    }
}