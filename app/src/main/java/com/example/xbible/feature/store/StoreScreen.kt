package com.example.xbible.feature.store

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.xbible.R
import com.example.xbible.core.ui.components.BookCardView
import com.example.xbible.core.ui.components.CategoryTabBar
import com.example.xbible.core.ui.components.LanguageHeader
import com.example.xbible.viewmodel.InstallationStatus
import com.example.xbible.viewmodel.StoreViewModel
import kotlinx.coroutines.launch
import uniffi.xbible_engine.SwordModule

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StoreScreen(
    viewModel: StoreViewModel = hiltViewModel(),
    onOpenAudioStore: (() -> Unit)? = null,
) {
    var showAudioStore by remember { mutableStateOf(false) }

    if (showAudioStore) {
        AudioStoreScreen(onExitToStudy = { showAudioStore = false })
        return
    }

    // ── Observed VM state ─────────────────────────────────────────────
    val isLoading by viewModel.isLoading.collectAsState()
    val filteredLanguages by viewModel.filteredLanguages.collectAsState()
    val availableCategories by viewModel.availableCategories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val remoteSources by viewModel.remoteSources.collectAsState()
    val selectedSource by viewModel.selectedSource.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    /**
     * The single source of truth for install state the UI reads.
     * Collecting it here means *this* composable recomposes on every
     * progress tick — and because we read it inside the `items(...)`
     * lambda below, each row also recomposes.
     */
    val statuses by viewModel.visibleStatuses.collectAsState()

    var expandedLanguages by remember { mutableStateOf(setOf<String>()) }
    var showSourceDropdown by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val showAppBarSearch by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            StoreTopBar(
                showAppBarSearch = showAppBarSearch,
                onScrollToSearch = {
                    coroutineScope.launch { listState.animateScrollToItem(0) }
                },
                onOpenAudioStore = { onOpenAudioStore?.invoke() ?: run { showAudioStore = true } },
                selectedSource = selectedSource,
                remoteSources = remoteSources,
                showSourceDropdown = showSourceDropdown,
                onShowSourceDropdownChange = { showSourceDropdown = it },
                onSelectSource = { source ->
                    showSourceDropdown = false
                    viewModel.selectSource(source)
                },
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            // Item 0 — search bar
            item(key = "search") {
                SearchField(
                    query = searchQuery,
                    onQueryChange = viewModel::updateSearchQuery,
                )
            }

            // Item 1 — sticky category tab bar
            stickyHeader(key = "categories") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    CategoryTabBar(
                        categories = availableCategories,
                        selectedCategory = selectedCategory,
                        onCategorySelected = {
                            viewModel.setCategory(it)
                            expandedLanguages = emptySet()
                            coroutineScope.launch {
                                if (listState.firstVisibleItemIndex > 1) {
                                    listState.scrollToItem(1)
                                }
                            }
                        },
                    )
                }
            }

            when {
                isLoading && filteredLanguages.isEmpty() -> {
                    item(key = "loading") {
                        Box(
                            modifier = Modifier.fillParentMaxSize().padding(bottom = 100.dp),
                            contentAlignment = Alignment.Center,
                        ) { LoadingIndicator() }
                    }
                }
                filteredLanguages.isEmpty() -> {
                    item(key = "empty") {
                        EmptyState()
                    }
                }
                else -> {
                    filteredLanguages.keys.sorted().forEach { langCode ->
                        val modules = filteredLanguages[langCode].orEmpty()
                        val isExpanded = langCode !in expandedLanguages // default expanded

                        item(key = "header_$langCode") {
                            LanguageHeader(
                                langCode = langCode,
                                count = modules.size,
                                isExpanded = isExpanded,
                                onToggle = {
                                    expandedLanguages = if (isExpanded) {
                                        expandedLanguages + langCode
                                    } else {
                                        expandedLanguages - langCode
                                    }
                                },
                            )
                        }

                        if (isExpanded) {
                            item(key = "row_$langCode") {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    items(modules, key = { it.name }) { module ->
                                        /**
                                         * CRITICAL: read from the *collected*
                                         * `statuses` map, not from
                                         * `viewModel.statusFor(module)`. The
                                         * latter would capture the value at
                                         * first composition and never
                                         * update until the list changed.
                                         */
                                        val status = statuses[module.name]
                                            ?: InstallationStatus.Idle

                                        BookCardView(
                                            module = module,
                                            status = status,
                                            onAction = {
                                                handleModuleAction(module, status, viewModel)
                                            },
                                            categoryName = module.category,
                                        )
                                    }
                                }
                            }
                            item(key = "divider_$langCode") {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Extracted composables ────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StoreTopBar(
    showAppBarSearch: Boolean,
    onScrollToSearch: () -> Unit,
    onOpenAudioStore: () -> Unit,
    selectedSource: String?,
    remoteSources: List<String>,
    showSourceDropdown: Boolean,
    onShowSourceDropdownChange: (Boolean) -> Unit,
    onSelectSource: (String) -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                stringResource(R.string.store_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        },
        actions = {
            if (showAppBarSearch) {
                IconButton(onClick = onScrollToSearch) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = stringResource(R.string.scroll_to_search),
                    )
                }
            }

            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = { PlainTooltip { Text("Audio Store") } },
                state = rememberTooltipState(),
            ) {
                IconButton(onClick = onOpenAudioStore) {
                    Icon(Icons.Outlined.Headphones, contentDescription = "Audio Store")
                }
            }

            Box {
                SplitButtonLayout(
                    leadingButton = {
                        SplitButtonDefaults.ElevatedLeadingButton(onClick = {}) {
                            Text(
                                text = selectedSource ?: stringResource(R.string.source),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    trailingButton = {
                        SplitButtonDefaults.ElevatedTrailingButton(
                            checked = showSourceDropdown,
                            onCheckedChange = onShowSourceDropdownChange,
                        ) {
                            Icon(
                                Icons.Outlined.KeyboardArrowDown,
                                contentDescription = stringResource(R.string.source),
                            )
                        }
                    },
                    modifier = Modifier,
                    spacing = SplitButtonDefaults.Spacing,
                )
            }

            DropdownMenu(
                expanded = showSourceDropdown,
                onDismissRequest = { onShowSourceDropdownChange(false) },
                shape = RoundedCornerShape(24.dp),
            ) {
                remoteSources.forEach { source ->
                    val isSelected = source == selectedSource
                    DropdownMenuItem(
                        text = {
                            Text(source, style = MaterialTheme.typography.bodyLarge)
                        },
                        onClick = { onSelectSource(source) },
                        leadingIcon = {
                            if (isSelected) {
                                Icon(
                                    Icons.Outlined.Check,
                                    contentDescription = stringResource(R.string.selected_item),
                                )
                            } else {
                                Spacer(Modifier.size(24.dp))
                            }
                        },
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
    )
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text(stringResource(R.string.search_placeholder_store)) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.clear_search),
                    )
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
    )
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(bottom = 100.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(48.dp))
            Text(
                stringResource(R.string.no_modules_found),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

private fun handleModuleAction(
    module: SwordModule,
    status: InstallationStatus,
    viewModel: StoreViewModel,
) {
    when (status) {
        is InstallationStatus.Idle,
        is InstallationStatus.Failed,
        is InstallationStatus.Cancelled -> viewModel.install(module)

        is InstallationStatus.Installed -> {
            // Open logic — navigate to StudyScreen with this module
        }

        is InstallationStatus.Pending,
        is InstallationStatus.Installing -> viewModel.cancelInstall(module.name)
    }
}