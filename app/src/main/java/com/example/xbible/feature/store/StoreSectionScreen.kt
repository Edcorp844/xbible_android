package com.example.xbible.feature.store

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.xbible.feature.store.components.StoreFilter
import com.example.xbible.feature.store.components.StoreSearchBar
import com.example.xbible.feature.store.components.StoreSort
import com.example.xbible.feature.store.components.TranslationCard
import com.example.xbible.feature.store.components.TranslationCardStatus
import com.example.xbible.feature.store.components.applyFilterAndSort
import com.example.xbible.viewmodel.FreeBibleStoreViewModel
import com.example.xbible.viewmodel.InstallState
import uniffi.xbible_engine.StoreItem

/** Sentinel for "no kind filter selected" in the category chip row. */
private const val ALL_KINDS = "__all__"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreSectionScreen(
    title: String,
    items: List<StoreItem>,
    viewModel: FreeBibleStoreViewModel,
    onBack: () -> Unit,
    onOpenTranslation: (String, String) -> Unit,
    onOpenEntitySet: (String, String) -> Unit,
    onOpenItem: (StoreItem) -> Unit,
    languageFilter: String? = null,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(StoreFilter.ALL) }
    var sort by rememberSaveable { mutableStateOf(StoreSort.NAME_ASC) }
    var kindFilter by rememberSaveable { mutableStateOf(ALL_KINDS) }
    var filterMenuOpen by remember { mutableStateOf(false) }
    var sortMenuOpen by remember { mutableStateOf(false) }

    val activeInstall by viewModel.activeInstall.collectAsStateWithLifecycle()

    // 1) Language scope
    val scopedItems = remember(items, languageFilter) {
        if (languageFilter == null) items
        else items.filter { it.language == languageFilter }
    }

    // 2) Discover distinct kinds present in the scoped list.
    //    Order matters: translations first, then entity sets, then
    //    commentaries, then everything else.
    val kinds = remember(scopedItems) {
        val preferredOrder = listOf(
            "translation", "entity_set", "commentary", "dataset"
        )
        val present = scopedItems.map { it.kind }.distinct()
        present.sortedWith(
            compareBy(
                { preferredOrder.indexOf(it).takeIf { i -> i >= 0 } ?: Int.MAX_VALUE },
                { it },
            )
        )
    }

    // 3) Apply kind filter + search + user sort/filter
    val visible = remember(query, filter, sort, kindFilter, scopedItems) {
        val kindScoped = if (kindFilter == ALL_KINDS) scopedItems
        else scopedItems.filter { it.kind == kindFilter }

        val searched = if (query.isBlank()) kindScoped
        else kindScoped.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.englishName.contains(query, ignoreCase = true) ||
                    it.shortName.contains(query, ignoreCase = true) ||
                    it.languageName?.contains(query, ignoreCase = true) == true ||
                    it.id.contains(query, ignoreCase = true)
        }
        searched.applyFilterAndSort(filter, sort)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "${visible.size} of ${scopedItems.size} items",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    // Filter menu (installed / not installed)
                    Box {
                        IconButton(onClick = { filterMenuOpen = true }) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = "Filter",
                            )
                        }
                        DropdownMenu(
                            expanded = filterMenuOpen,
                            onDismissRequest = { filterMenuOpen = false },
                        ) {
                            StoreFilter.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        filter = option
                                        filterMenuOpen = false
                                    },
                                    trailingIcon = {
                                        if (filter == option) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Selected",
                                            )
                                        }
                                    },
                                )
                            }
                        }
                    }

                    // Sort menu
                    Box {
                        IconButton(onClick = { sortMenuOpen = true }) {
                            Icon(
                                Icons.Default.Sort,
                                contentDescription = "Sort",
                            )
                        }
                        DropdownMenu(
                            expanded = sortMenuOpen,
                            onDismissRequest = { sortMenuOpen = false },
                        ) {
                            StoreSort.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        sort = option
                                        sortMenuOpen = false
                                    },
                                    trailingIcon = {
                                        if (sort == option) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Selected",
                                            )
                                        }
                                    },
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            StoreSearchBar(
                query = query,
                onQueryChange = { query = it },
            )

            // ── Category chips: only when >1 kind present ─────
            if (kinds.size > 1) {
                CategoryChipsRow(
                    kinds = kinds,
                    selectedKind = kindFilter,
                    onSelect = { kindFilter = it },
                )
            }

            if (visible.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = when {
                            query.isNotBlank() -> "No matches for \"$query\"."
                            kinds.size > 1 -> "Nothing in this category."
                            else -> "Nothing here yet."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 24.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(visible, key = { "${it.kind}:${it.id}" }) { item ->
                        SectionItemCard(
                            item = item,
                            activeInstall = activeInstall,
                            viewModel = viewModel,
                            onOpenTranslation = onOpenTranslation,
                            onOpenEntitySet = onOpenEntitySet,
                            onOpenItem = onOpenItem,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryChipsRow(
    kinds: List<String>,
    selectedKind: String,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selectedKind == ALL_KINDS,
            onClick = { onSelect(ALL_KINDS) },
            label = { Text("All") },
            shape = CircleShape,
            colors = FilterChipDefaults.filterChipColors(),
        )
        kinds.forEach { kind ->
            FilterChip(
                selected = selectedKind == kind,
                onClick = { onSelect(kind) },
                label = { Text(kindLabel(kind)) },
                shape = CircleShape,
                colors = FilterChipDefaults.filterChipColors(),
            )
        }
    }
}

private fun kindLabel(kind: String): String = when (kind) {
    "translation" -> "Bibles"
    "entity_set" -> "Study Tools"
    "commentary" -> "Commentaries"
    "dataset" -> "Datasets"
    else -> kind.replace('_', ' ')
        .replaceFirstChar { it.uppercase() }
}

@Composable
private fun SectionItemCard(
    item: StoreItem,
    activeInstall: InstallState?,
    viewModel: FreeBibleStoreViewModel,
    onOpenTranslation: (String, String) -> Unit,
    onOpenEntitySet: (String, String) -> Unit,
    onOpenItem: (StoreItem) -> Unit,
) {
    val status = sectionStatusFor(
        item = item,
        activeInstall = activeInstall,
        isInstalled = viewModel.isInstalled(item.id),
    )

    TranslationCard(
        item = item,
        status = status,
        cardWidth = null,   // ← fill the grid cell
        onInstall = { viewModel.install(item) },
        onOpen = {
            when (item.kind) {
                "translation" -> onOpenTranslation(item.id, item.name)
                "entity_set" -> {
                    val (dataset, kind) = item.id.split(':', limit = 2)
                    onOpenEntitySet(dataset, kind)
                }
                else -> onOpenItem(item)
            }
        },
        onClick = { onOpenItem(item) },
    )
}

private fun sectionStatusFor(
    item: StoreItem,
    activeInstall: InstallState?,
    isInstalled: Boolean,
): TranslationCardStatus {
    if (activeInstall != null && activeInstall.translationId == item.id) {
        return when {
            activeInstall.failed != null -> TranslationCardStatus.Failed(activeInstall.failed)
            activeInstall.done -> TranslationCardStatus.Installed
            activeInstall.fraction <= 0f -> TranslationCardStatus.Pending
            else -> TranslationCardStatus.Installing(activeInstall.fraction)
        }
    }
    return if (isInstalled) TranslationCardStatus.Installed else TranslationCardStatus.Idle
}