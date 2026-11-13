package com.example.xbible.feature.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.xbible.R
import com.example.xbible.core.ui.components.CategoryTabBar
import com.example.xbible.core.ui.components.LanguageHeader
import com.example.xbible.feature.store.components.TranslationCard
import com.example.xbible.feature.store.components.TranslationCardStatus
import com.example.xbible.viewmodel.FreeBibleStoreViewModel
import com.example.xbible.viewmodel.LibraryViewModel
import kotlinx.coroutines.launch
import uniffi.xbible_engine.InstalledTranslation
import uniffi.xbible_engine.StoreItem

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class,
)
@Composable
fun LibraryScreen(
    onOpenTranslation: (InstalledTranslation) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
    storeViewModel: FreeBibleStoreViewModel = hiltViewModel(),
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val installedTranslations by viewModel.installedTranslations.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    // Define the categories we want to show in the tab bar
    val categories = listOf("Translations", "Study Tools", "Commentaries")
    var selectedCategory by remember { mutableStateOf("Translations") }

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
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.library_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    if (showAppBarSearch) {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                listState.animateScrollToItem(0)
                            }
                        }) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = stringResource(R.string.scroll_to_search),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        // Filter translations by search query
        val filteredTranslations = if (searchQuery.isBlank()) {
            installedTranslations
        } else {
            installedTranslations.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.englishName.contains(searchQuery, ignoreCase = true) ||
                        it.shortName.contains(searchQuery, ignoreCase = true)
            }
        }

        val groupedByLanguage = remember(filteredTranslations) {
            filteredTranslations.groupBy { it.languageName ?: it.language.uppercase() }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text(stringResource(R.string.search_placeholder_library)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
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

            stickyHeader {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    CategoryTabBar(
                        categories = categories,
                        selectedCategory = selectedCategory,
                        onCategorySelected = {
                            selectedCategory = it
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
                isLoading && installedTranslations.isEmpty() -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillParentMaxSize()
                                .padding(bottom = 100.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            LoadingIndicator()
                        }
                    }
                }

                !isLoading && filteredTranslations.isEmpty() -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillParentMaxSize()
                                .padding(bottom = 100.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                )
                                Text(
                                    stringResource(R.string.no_installed_modules_found),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                    }
                }

                else -> {
                    groupedByLanguage.keys.sorted().forEach { langName ->
                        val translations = groupedByLanguage[langName] ?: emptyList()

                        item(key = "header_$langName") {
                            LanguageHeader(
                                langCode = langName,
                                count = translations.size,
                                isExpanded = true,
                                onToggle = { /* Expand/collapse logic if needed */ },
                            )
                        }

                        item(key = "row_$langName") {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                items(translations, key = { it.id }) { translation ->
                                    // Map InstalledTranslation to StoreItem for the card
                                    val storeItem = StoreItem(
                                        id = translation.id,
                                        kind = "translation",
                                        name = translation.name,
                                        englishName = translation.englishName,
                                        shortName = translation.shortName,
                                        language = translation.language,
                                        languageName = translation.languageName,
                                        description = "",
                                        badge = null,
                                        installed = true,
                                        hasStrongs = false,
                                        bookCount = translation.bookCount,
                                        chapterCount = translation.chapterCount,
                                        verseCount = translation.verseCount
                                    )

                                    TranslationCard(
                                        item = storeItem,
                                        status = TranslationCardStatus.Installed,
                                        onInstall = { /* Already installed */ },
                                        onOpen = { onOpenTranslation(translation) },
                                        onClick = { onOpenTranslation(translation) },
                                        onDelete = {
                                            viewModel.deleteTranslation(translation.id)
                                            // Optionally refresh store VM state as well
                                            // storeViewModel.uninstallTranslation(translation.id)
                                        }
                                    )
                                }
                            }
                        }
                        item(key = "divider_$langName") {
                            HorizontalDivider(
                                modifier = Modifier.padding(
                                    vertical = 8.dp,
                                    horizontal = 16.dp
                                ),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            )
                        }
                    }
                }
            }
        }
    }
}