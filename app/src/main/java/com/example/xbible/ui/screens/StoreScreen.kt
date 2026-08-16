package com.example.xbible.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
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
import androidx.compose.material3.OutlinedToggleButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.xbible.ui.components.BookCardView
import com.example.xbible.ui.components.CategoryTabBar
import com.example.xbible.ui.components.LanguageHeader
import com.example.xbible.viewmodel.InstallationStatus
import com.example.xbible.viewmodel.StoreViewModel
import uniffi.xbible_engine.SwordModule
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StoreScreen(
    viewModel: StoreViewModel = viewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val organizedModules by viewModel.organizedModules.collectAsState()
    val installationStates by viewModel.installationStates.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val remoteSources by viewModel.remoteSources.collectAsState()
    val selectedSource by viewModel.selectedSource.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var selectedCategory by remember { mutableStateOf("") }
    var expandedLanguages by remember { mutableStateOf(setOf<String>()) }
    var showSourceDropdown by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Show search button in app bar if the primary search bar (item 0) is scrolled off
    val showAppBarSearch by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0
        }
    }

    // Auto-select first category when loaded or list changes
    androidx.compose.runtime.LaunchedEffect(categories) {
        if (categories.isNotEmpty() && (selectedCategory.isEmpty() || !categories.contains(
                selectedCategory
            ))
        ) {
            selectedCategory = categories.first()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Store", style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    if (showAppBarSearch) {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                listState.animateScrollToItem(0)
                            }
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "Scroll to Search")
                        }
                    }
                    Box {
                        SplitButtonLayout(
                            leadingButton = {
                                SplitButtonDefaults.ElevatedLeadingButton(
                                    onClick = { }
                                ) {
                                    Text(
                                        text = selectedSource ?: "Source",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            },
                            trailingButton = {
                                SplitButtonDefaults.ElevatedTrailingButton(
                                    checked = showSourceDropdown,
                                    onCheckedChange = {
                                        showSourceDropdown = it
                                    }
                                ) {
                                    Icon(
                                        Icons.Outlined.KeyboardArrowDown,
                                        contentDescription = "Source"
                                    )
                                }
                            }
                        )
                    }
                    DropdownMenu(
                        expanded = showSourceDropdown,
                        onDismissRequest = { showSourceDropdown = false },
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        remoteSources.forEach { source ->
                            val isSelected = (source == selectedSource)

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = source,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                },
                                onClick = {
                                    viewModel.selectSource(source)
                                    showSourceDropdown = false
                                    selectedCategory = "" // Reset category selection for new source
                                },
                                leadingIcon = {
                                    // M3 UX Expressive Guidelines: Use leading icons to visually confirm selected status
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = "Selected"
                                        )
                                    } else {
                                        // Provides consistent spacing/alignment for unselected items
                                        Spacer(modifier = Modifier.size(24.dp))
                                    }
                                },
                            )
                        }
                    }

                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        val languages = organizedModules[selectedCategory] ?: emptyMap()
        
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Item 0: Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search Bibles, commentaries...") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
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
                    singleLine = true
                )
            }

            // Item 1: Sticky Category Tab Bar
            stickyHeader {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CategoryTabBar(
                        categories = categories,
                        selectedCategory = selectedCategory,
                        onCategorySelected = {
                            selectedCategory = it
                            expandedLanguages = emptySet()
                            coroutineScope.launch {
                                // Scroll to just below search bar when category changes
                                if (listState.firstVisibleItemIndex > 1) {
                                    listState.scrollToItem(1)
                                }
                            }
                        }
                    )
                }
            }

            if (isLoading && organizedModules.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillParentMaxSize()
                            .padding(bottom = 100.dp), 
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingIndicator()
                    }
                }
            } else if (organizedModules[selectedCategory]?.isEmpty() != false && !isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillParentMaxSize()
                            .padding(bottom = 100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                "No modules found in this category",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            } else {
                languages.keys.sorted().forEach { langCode ->
                    val modules = languages[langCode] ?: emptyList()
                    val isExpanded = true

                    item(key = "header_$langCode") {
                        LanguageHeader(
                            langCode = langCode,
                            count = modules.size,
                            isExpanded = isExpanded,
                            onToggle = {
                                expandedLanguages = if (isExpanded) {
                                    expandedLanguages - langCode
                                } else {
                                    expandedLanguages + langCode
                                }
                            }
                        )
                    }

                    if (isExpanded) {
                        item(key = "row_$langCode") {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(modules) { module ->
                                    val status = installationStates[module.name]
                                        ?: InstallationStatus.Idle
                                    BookCardView(
                                        module = module,
                                        status = status,
                                        onAction = {
                                            handleModuleAction(module, status, viewModel)
                                        },
                                        categoryName = module.category
                                    )
                                }
                            }
                        }
                        item(key = "divider_$langCode") {
                            HorizontalDivider(
                                modifier = Modifier.padding(
                                    vertical = 8.dp,
                                    horizontal = 16.dp
                                ),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun handleModuleAction(
    module: SwordModule,
    status: InstallationStatus,
    viewModel: StoreViewModel
) {
    when (status) {
        is InstallationStatus.Idle, is InstallationStatus.Failed, is InstallationStatus.Cancelled -> {
            viewModel.install(module)
        }

        is InstallationStatus.Installed -> {
            // Open logic - could navigate to StudyScreen with this module
        }

        is InstallationStatus.Pending, is InstallationStatus.Installing -> {
            viewModel.cancelInstall(module.name)
        }
    }
}

