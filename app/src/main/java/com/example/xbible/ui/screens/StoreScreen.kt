package com.example.xbible.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.xbible.ui.components.BookCardView
import com.example.xbible.viewmodel.InstallationStatus
import com.example.xbible.viewmodel.StoreViewModel
import uniffi.xbible_engine.SwordModule

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
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

    var selectedCategory by remember { mutableStateOf("") }
    var expandedLanguages by remember { mutableStateOf(setOf<String>()) }
    var showSourceDropdown by remember { mutableStateOf(false) }

    // Auto-select first category when loaded
    androidx.compose.runtime.LaunchedEffect(categories) {
        if (selectedCategory.isEmpty() && categories.isNotEmpty()) {
            selectedCategory = categories.first()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            MediumTopAppBar(
                title = { 
                    Column {
                        Text("Store")
                        selectedSource?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showSourceDropdown = true }) {
                            Icon(Icons.Default.Storefront, contentDescription = "Switch Store")
                        }
                        DropdownMenu(
                            expanded = showSourceDropdown,
                            onDismissRequest = { showSourceDropdown = false }
                        ) {
                            remoteSources.forEach { source ->
                                DropdownMenuItem(
                                    text = { Text(source) },
                                    onClick = {
                                        viewModel.selectSource(source)
                                        showSourceDropdown = false
                                        selectedCategory = "" // Reset category selection for new source
                                    },
                                    trailingIcon = {
                                        if (source == selectedSource) {
                                            Icon(Icons.Default.Info, contentDescription = "Selected")
                                        }
                                    }
                                )
                            }
                        }
                    }
                },
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
                        Text("No modules found in this category", style = MaterialTheme.typography.bodyLarge)
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
                        val isExpanded = expandedLanguages.contains(langCode)
                        
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
                                        val status = installationStates[module.name] ?: InstallationStatus.Idle
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

private fun handleModuleAction(module: SwordModule, status: InstallationStatus, viewModel: StoreViewModel) {
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

@Composable
fun LanguageHeader(
    langCode: String,
    count: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = langCode.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Surface(
                modifier = Modifier.padding(start = 8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = CircleShape
            ) {
                Text(
                    text = count.toString(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        Icon(
            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null
        )
    }
}

@Composable
fun CategoryTabBar(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            CategoryTabItem(
                title = category,
                isSelected = category == selectedCategory,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

@Composable
fun CategoryTabItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
