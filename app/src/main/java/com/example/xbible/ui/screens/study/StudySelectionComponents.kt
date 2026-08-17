package com.example.xbible.ui.screens.study

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import uniffi.xbible_engine.ModuleBook
import uniffi.xbible_engine.SwordModule

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ModuleSelectionContent(
    installedBibles: List<SwordModule>,
    currentModule: SwordModule?,
    onModuleSelected: (SwordModule) -> Unit
) {
    val groupedByLanguage = remember(installedBibles) {
        installedBibles.groupBy { it.language }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Select Bible Version",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp),
            fontWeight = FontWeight.Bold
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            groupedByLanguage.keys.sorted().forEach { langCode ->
                val modules = groupedByLanguage[langCode] ?: emptyList()
                item {
                    SettingsGroup(title = langCode.uppercase()) {
                        modules.forEach { module ->
                            val isSelected = currentModule?.name == module.name
                            ListItem(
                                modifier = Modifier.clickable { onModuleSelected(module) },
                                colors = ListItemDefaults.colors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    else Color.Transparent
                                ),
                                headlineContent = {
                                    Text(
                                        text = module.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                                    )
                                },
                                supportingContent = if (module.description.isNotEmpty()) {
                                    {
                                        Text(
                                            text = module.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                } else null,
                                trailingContent = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Layers,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                } else null
                            )
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
fun ReferencePickerContent(
    books: List<ModuleBook>,
    selectedBookIndex: Int,
    selectedChapterIndex: Int,
    onBookSelected: (Int) -> Unit,
    onChapterSelected: (Int) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
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
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween, Alignment.CenterHorizontally)
        ) {
            customItem(
                buttonGroupContent = {
                    OutlinedToggleButton(
                        checked = selectedTab == 0,
                        onCheckedChange = { if (it) selectedTab = 0 },
                        shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
                    ) {
                        Text("Books")
                    }
                },
                menuContent = { menuState ->
                    DropdownMenuItem(
                        text = { Text("Books") },
                        onClick = {
                            selectedTab = 0
                            menuState.dismiss()
                        }
                    )
                }
            )
            customItem(
                buttonGroupContent = {
                    OutlinedToggleButton(
                        checked = selectedTab == 1,
                        onCheckedChange = { if (it) selectedTab = 1 },
                        shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
                    ) {
                        Text("Chapters")
                    }
                },
                menuContent = { menuState ->
                    DropdownMenuItem(
                        text = { Text("Chapters") },
                        onClick = {
                            selectedTab = 1
                            menuState.dismiss()
                        }
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.weight(1f)) {
            if (selectedTab == 0) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    gridItemsIndexed(books) { index, book ->
                        val isSelected = index == selectedBookIndex
                        Surface(
                            modifier = Modifier
                                .aspectRatio(2f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onBookSelected(index)
                                    selectedTab = 1
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                if (selectedBookIndex in books.indices) {
                    val book = books[selectedBookIndex]
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        gridItemsIndexed(book.chapters) { index, chapter ->
                            val isSelected = index == selectedChapterIndex
                            Surface(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onChapterSelected(chapter.number) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = chapter.number.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSecondary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Select a book first")
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
            )
        }
        Surface(
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(content = content)
        }
    }
}
