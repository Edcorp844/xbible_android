package com.example.xbible.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.xbible.ui.components.BookCardView
import com.example.xbible.ui.components.CategoryTabBar
import com.example.xbible.ui.components.LanguageHeader
import com.example.xbible.viewmodel.InstallationStatus
import com.example.xbible.viewmodel.LibraryViewModel
import uniffi.xbible_engine.SwordModule

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LibraryScreen(
    onOpenModule: (SwordModule) -> Unit,
    viewModel: LibraryViewModel = viewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val organizedModules by viewModel.organizedModules.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var selectedCategory by remember { mutableStateOf("") }
    var expandedLanguages by remember { mutableStateOf(setOf<String>()) }

    // Auto-select first category when loaded or list changes
    androidx.compose.runtime.LaunchedEffect(categories) {
        if (categories.isNotEmpty() && (selectedCategory.isEmpty() || !categories.contains(selectedCategory))) {
            selectedCategory = categories.first()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Library") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search installed Bibles...") },
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

            CategoryTabBar(
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = { 
                    selectedCategory = it
                    expandedLanguages = emptySet()
                }
            )

            if (isLoading && organizedModules.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
            } else if (organizedModules[selectedCategory]?.isEmpty() != false && !isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(48.dp))
                        Text("No installed modules found", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            } else {
                val languages = organizedModules[selectedCategory] ?: emptyMap()
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    languages.keys.sorted().forEach { langCode ->
                        val modules = languages[langCode] ?: emptyList()
                        val isExpanded = true
                        
                        item {
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
                            item {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(modules) { module ->
                                        BookCardView(
                                            module = module,
                                            status = InstallationStatus.Installed,
                                            onAction = {
                                                onOpenModule(module)
                                            },
                                            categoryName = module.category,
                                            onDelete = {
                                                viewModel.deleteModule(module.name)
                                            },
                                            menuAction = {
                                                // Update logic?
                                            }
                                        )
                                    }
                                }
                            }
                            item {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
