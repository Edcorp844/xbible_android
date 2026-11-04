package com.example.xbible.feature.tools.audiobible

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.xbible.R
import com.example.xbible.feature.tools.audiobible.components.LocalAudioModuleCard
import uniffi.xbible_engine.AudioModuleInfo

// ── Sort + filter options ───────────────────────────────────────────

enum class ModuleSort(val label: String) {
    Date("Date"),
    Contributor("Contributor"),
    Title("Title"),
}

sealed interface ModuleFilter {
    data object None : ModuleFilter
    data class Contributor(val value: String) : ModuleFilter
    data class Language(val value: String) : ModuleFilter

    /** Human-readable summary for chips / tooltips. */
    val label: String
        get() = when (this) {
            None -> "All"
            is Contributor -> value
            is Language -> value
        }
}

// ── The screen ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ModuleListScreen(
    title: String,
    modules: List<AudioModuleInfo>,
    onBack: (() -> Unit)? = null,
    onModuleClick: (AudioModuleInfo) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(ModuleSort.Title) }
    var filterKey by rememberSaveable { mutableStateOf<String?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    val contributors = remember(modules) {
        modules.mapNotNull { it.metadata?.contributor?.takeIf(String::isNotBlank) }
            .distinct()
            .sorted()
    }
    val languages = remember(modules) {
        modules.mapNotNull { it.metadata?.language?.takeIf(String::isNotBlank) }
            .distinct()
            .sorted()
    }

    val activeFilter: ModuleFilter = remember(filterKey) {
        when {
            filterKey == null -> ModuleFilter.None
            filterKey!!.startsWith("c|") -> ModuleFilter.Contributor(filterKey!!.removePrefix("c|"))
            filterKey!!.startsWith("l|") -> ModuleFilter.Language(filterKey!!.removePrefix("l|"))
            else -> ModuleFilter.None
        }
    }

    val visibleModules by remember(modules, query, sort, activeFilter) {
        derivedStateOf {
            modules
                .asSequence()
                .filter { m ->
                    when (val f = activeFilter) {
                        ModuleFilter.None -> true
                        is ModuleFilter.Contributor -> m.metadata?.contributor == f.value
                        is ModuleFilter.Language -> m.metadata?.language == f.value
                    }
                }
                .filter { m ->
                    val q = query.trim().lowercase()
                    q.isEmpty() ||
                            m.metadata?.displayTitle?.lowercase()?.contains(q) == true ||
                            m.metadata?.contributor?.lowercase()?.contains(q) == true ||
                            m.metadata?.language?.lowercase()?.contains(q) == true
                }
                .sortedWith(
                    when (sort) {
                        ModuleSort.Title -> compareBy {
                            it.metadata?.displayTitle?.lowercase().orEmpty()
                        }
                        ModuleSort.Contributor -> compareBy {
                            it.metadata?.contributor?.lowercase().orEmpty()
                        }
                        ModuleSort.Date -> compareByDescending {
                            it.metadata?.organization?.stampedAt ?: 0L
                        }
                    }
                )
                .toList()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                },
                actions = {
                    // ── Sort trigger + menu ──────────────────────────────
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Outlined.Sort, contentDescription = "Sort")
                        }
                        SortMenu(
                            expanded = showSortMenu,
                            onDismiss = { showSortMenu = false },
                            current = sort,
                            onSelect = { sort = it; showSortMenu = false },
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            SearchFilterRow(
                query = query,
                onQueryChange = { query = it },
                contributors = contributors,
                languages = languages,
                currentFilter = activeFilter,
                onFilterChange = { f ->
                    filterKey = when (f) {
                        ModuleFilter.None -> null
                        is ModuleFilter.Contributor -> "c|${f.value}"
                        is ModuleFilter.Language -> "l|${f.value}"
                    }
                },
            )

            if (visibleModules.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (query.isNotBlank()) {
                            stringResource(R.string.no_results_for, query)
                        } else {
                            stringResource(R.string.no_modules_found)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(150.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(
                        visibleModules,
                        key = { it.metadata?.uniqueId ?: it.fileName },
                    ) { module ->
                        LocalAudioModuleCard(
                            module = module,
                            onClick = { onModuleClick(module) },
                        )
                    }
                }
            }
        }
    }
}

// ── The search + filter row (below the top bar) ─────────────────────

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun SearchFilterRow(
    query: String,
    onQueryChange: (String) -> Unit,
    contributors: List<String>,
    languages: List<String>,
    currentFilter: ModuleFilter,
    onFilterChange: (ModuleFilter) -> Unit,
) {
    var showFilterMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Search modules") },
            leadingIcon = {
                Icon(Icons.Outlined.Search, contentDescription = null)
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = CircleShape,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
            ),
        )

        Box {
            FilledTonalIconButton(
                onClick = { showFilterMenu = true },
                shape = CircleShape,
                colors = if (currentFilter != ModuleFilter.None) {
                    IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    )
                } else {
                    IconButtonDefaults.filledTonalIconButtonColors()
                },
            ) {
                Icon(Icons.Outlined.FilterList, "Filter")
            }
            FilterMenu(
                expanded = showFilterMenu,
                onDismiss = { showFilterMenu = false },
                contributors = contributors,
                languages = languages,
                current = currentFilter,
                onSelect = { onFilterChange(it); showFilterMenu = false },
            )
        }
    }
}

// ── Filter menu — expressive shape + sections ──────────────────────

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FilterMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    contributors: List<String>,
    languages: List<String>,
    current: ModuleFilter,
    onSelect: (ModuleFilter) -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
    ) {
        DropdownMenuItem(
            text = { Text("All modules", fontWeight = FontWeight.SemiBold) },
            leadingIcon = { Icon(Icons.Outlined.FilterList, null) },
            trailingIcon = {
                if (current == ModuleFilter.None) CheckIcon()
            },
            onClick = { onSelect(ModuleFilter.None) },
        )

        if (contributors.isNotEmpty()) {
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            MenuHeader("Contributors")
            contributors.forEach { c ->
                val isActive = current == ModuleFilter.Contributor(c)
                DropdownMenuItem(
                    text = {
                        Text(
                            c,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    trailingIcon = { if (isActive) CheckIcon() },
                    onClick = { onSelect(ModuleFilter.Contributor(c)) },
                )
            }
        }

        if (languages.isNotEmpty()) {
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            MenuHeader("Languages")
            languages.forEach { l ->
                val isActive = current == ModuleFilter.Language(l)
                DropdownMenuItem(
                    text = {
                        Text(
                            l,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    trailingIcon = { if (isActive) CheckIcon() },
                    onClick = { onSelect(ModuleFilter.Language(l)) },
                )
            }
        }
    }
}

// ── Sort menu ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SortMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    current: ModuleSort,
    onSelect: (ModuleSort) -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
    ) {
        MenuHeader("Sort by")
        ModuleSort.entries.forEach { s ->
            val isActive = current == s
            DropdownMenuItem(
                text = {
                    Text(
                        s.label,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
                trailingIcon = { if (isActive) CheckIcon() },
                onClick = { onSelect(s) },
            )
        }
    }
}

// ── Small shared bits ───────────────────────────────────────────────

@Composable
private fun CheckIcon() {
    Icon(
        Icons.Default.Check,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun MenuHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}