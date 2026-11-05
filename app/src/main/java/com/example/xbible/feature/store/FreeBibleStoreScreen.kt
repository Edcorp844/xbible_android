package com.example.xbible.feature.store

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.xbible.core.ui.components.SectionHeader
import com.example.xbible.feature.store.components.ActionView
import com.example.xbible.feature.store.components.CollectionTile
import com.example.xbible.feature.store.components.TranslationCard
import com.example.xbible.feature.store.components.TranslationCardStatus
import com.example.xbible.viewmodel.FreeBibleStoreViewModel
import com.example.xbible.viewmodel.InstallState
import kotlinx.coroutines.launch
import uniffi.xbible_engine.LanguageFacet
import uniffi.xbible_engine.StoreItem
import uniffi.xbible_engine.StoreShelf

private const val MAX_ITEMS_PER_ROW = 10

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeBibleStoreScreen(
    viewModel: FreeBibleStoreViewModel,
    onOpenTranslation: (translationId: String, name: String) -> Unit,
    onOpenEntitySet: (datasetId: String, kind: String) -> Unit,
    onOpenItem: (StoreItem) -> Unit,
    onSeeAll: (shelfTitle: String) -> Unit = {},
    onLanguageClick: (languageCode: String) -> Unit = {},
    onSeeAllLanguages: () -> Unit = {},
) {
    val shelves by viewModel.shelves.collectAsStateWithLifecycle()
    val languages by viewModel.languages.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val activeInstall by viewModel.activeInstall.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val pullToRefreshState = rememberPullToRefreshState()

    val showAppBarSearch by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

    // Pull-to-refresh is only relevant when there's content on screen
    val isRefreshing = isLoading && shelves.isNotEmpty()

    // Hoisted outside the LazyColumn so `remember` runs in composable scope
    val searchResults = remember(searchQuery, shelves) {
        if (searchQuery.isBlank()) emptyList()
        else searchAcrossShelves(shelves, searchQuery)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Bible Store",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    if (showAppBarSearch) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(0)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "Scroll to search",
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isLoading && shelves.isEmpty() -> {
                    ExpressiveLoadingIndicator()
                }

                errorMessage != null && shelves.isEmpty() -> {
                    ExpressiveErrorState(
                        errorMessage = errorMessage ?: "Unknown error",
                        onRetry = { viewModel.refreshCatalog() }
                    )
                }

                else -> {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refreshCatalog() },
                        state = pullToRefreshState,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 32.dp),
                            state = listState,
                        ) {
                            item(key = "search") {
                                SearchField(
                                    query = searchQuery,
                                    onQueryChange = viewModel::updateSearchQuery,
                                )
                            }

                            // ── Search Results Mode ──────────────────
                            if (searchQuery.isNotBlank()) {
                                if (searchResults.isEmpty()) {
                                    item(key = "search_empty") {
                                        EmptySearchState(query = searchQuery)
                                    }
                                } else {
                                    item(key = "search_header") {
                                        SearchResultsHeader(
                                            count = searchResults.size,
                                            query = searchQuery,
                                        )
                                    }
                                    items(
                                        searchResults,
                                        key = { "search:${it.kind}:${it.id}" }
                                    ) { item ->
                                        Box(
                                            modifier = Modifier.padding(
                                                horizontal = 20.dp,
                                                vertical = 6.dp,
                                            )
                                        ) {
                                            CollectionTile(
                                                item = item,
                                                onClick = {
                                                    when (item.kind) {
                                                        "translation" ->
                                                            onOpenTranslation(
                                                                item.id,
                                                                item.name,
                                                            )
                                                        "entity_set" -> {
                                                            val (dataset, kind) =
                                                                item.id.split(':', limit = 2)
                                                            onOpenEntitySet(dataset, kind)
                                                        }
                                                        else -> onOpenItem(item)
                                                    }
                                                },
                                            )
                                        }
                                    }
                                }
                            } else {
                                // ── Normal Browse Mode ──────────────

                                // Active Install Banner
                                if (activeInstall != null && activeInstall?.done == false) {
                                    item {
                                        ActiveInstallBanner(
                                            installState = activeInstall!!,
                                            onCancel = { viewModel.cancelActiveInstall() }
                                        )
                                    }
                                }

                                // Language Chips
                                if (languages.isNotEmpty()) {
                                    item {
                                        LanguageChipsSection(
                                            languages = languages,
                                            onLanguageClick = onLanguageClick,
                                            onSeeAll = onSeeAllLanguages,
                                        )
                                    }
                                }

                                // Shelf Sections
                                items(shelves, key = { it.title }) { shelf ->
                                    val isListSection = shelf.title in listOf(
                                        "Bible Reference", "Commentaries"
                                    )

                                    if (isListSection) {
                                        VerticalListShelfSection(
                                            shelf = shelf,
                                            viewModel = viewModel,
                                            onOpenItem = onOpenItem,
                                            onOpenEntitySet = onOpenEntitySet,
                                            onSeeAll = { onSeeAll(shelf.title) },
                                            activeInstall = activeInstall,
                                        )
                                    } else {
                                        HorizontalShelfSection(
                                            shelf = shelf,
                                            activeInstall = activeInstall,
                                            viewModel = viewModel,
                                            onOpenTranslation = onOpenTranslation,
                                            onOpenEntitySet = onOpenEntitySet,
                                            onOpenItem = onOpenItem,
                                            onSeeAll = { onSeeAll(shelf.title) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// ─────────────────────────────────────────────────────────────────────
// Search helpers
// ─────────────────────────────────────────────────────────────────────

private fun searchAcrossShelves(
    shelves: List<StoreShelf>,
    query: String,
): List<StoreItem> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return emptyList()

    val seen = mutableSetOf<String>()
    val results = mutableListOf<StoreItem>()

    for (shelf in shelves) {
        for (item in shelf.items) {
            val key = "${item.kind}:${item.id}"
            if (key in seen) continue

            val matches = item.name.lowercase().contains(q) ||
                    item.id.lowercase().contains(q)

            if (matches) {
                seen += key
                results += item
            }
        }
    }
    return results
}


// ─────────────────────────────────────────────────────────────────────
// Search Field
// ─────────────────────────────────────────────────────────────────────

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("Search translations, people, places…") },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear search",
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


// ─────────────────────────────────────────────────────────────────────
// Search Results Header & Empty State
// ─────────────────────────────────────────────────────────────────────

@Composable
private fun SearchResultsHeader(count: Int, query: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = if (count == 1) "1 result" else "$count results",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "for \"$query\"",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EmptySearchState(query: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "No results for \"$query\"",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Try a different translation name or language.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}


// ─────────────────────────────────────────────────────────────────────
// Language Chips
// ─────────────────────────────────────────────────────────────────────

@Composable
private fun LanguageChipsSection(
    languages: List<LanguageFacet>,
    onLanguageClick: (String) -> Unit,
    onSeeAll: () -> Unit,
) {
    val priorityCodes = listOf("eng", "spa", "fra", "kor", "cmn", "hin", "por")

    val displayLanguages = remember(languages) {
        val priority = priorityCodes.mapNotNull { code ->
            languages.firstOrNull { it.code == code }
        }

        val remaining = languages.filter { lang ->
            priority.none { it.code == lang.code }
        }

        (priority + remaining).take(10)
    }

    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        SectionHeader(
            title = "Languages",
            onClick = onSeeAll,
        )

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            displayLanguages.forEach { language ->
                AssistChip(
                    onClick = {},
                    label = { Text(language.name, fontWeight = FontWeight.SemiBold) },
                    shape = CircleShape,
                )
            }
        }
    }
}


// ─────────────────────────────────────────────────────────────────────
// Loading State
// ─────────────────────────────────────────────────────────────────────

@Composable
private fun ExpressiveLoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 36.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(52.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer,
                    strokeWidth = 5.dp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Loading Bible Catalog",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Fetching available translations...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


// ─────────────────────────────────────────────────────────────────────
// Error State
// ─────────────────────────────────────────────────────────────────────

@Composable
private fun ExpressiveErrorState(
    errorMessage: String,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Failed to load catalog",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                FilledTonalButton(
                    onClick = onRetry,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Retry")
                }
            }
        }
    }
}


// ─────────────────────────────────────────────────────────────────────
// Active Installation Banner
// ─────────────────────────────────────────────────────────────────────

@Composable
private fun ActiveInstallBanner(
    installState: InstallState,
    onCancel: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Installing ${installState.translationId.uppercase()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = installState.message.ifEmpty { "Downloading translation..." },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onCancel,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel Install"
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { installState.fraction.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
            )
        }
    }
}


// ─────────────────────────────────────────────────────────────────────
// Horizontal Row Shelf
// ─────────────────────────────────────────────────────────────────────

@Composable
private fun HorizontalShelfSection(
    shelf: StoreShelf,
    activeInstall: InstallState?,
    viewModel: FreeBibleStoreViewModel,
    onOpenTranslation: (String, String) -> Unit,
    onOpenEntitySet: (String, String) -> Unit,
    onOpenItem: (StoreItem) -> Unit,
    onSeeAll: () -> Unit,
) {
    if (shelf.items.isEmpty()) return

    val visible = shelf.items.take(MAX_ITEMS_PER_ROW)
    val hasMore = shelf.items.size > MAX_ITEMS_PER_ROW

    Column(modifier = Modifier.padding(bottom = 20.dp)) {
        SectionHeader(
            title = shelf.title,
            onClick = onSeeAll.takeIf { hasMore },
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(visible, key = { "${it.kind}:${it.id}" }) { item ->
                val status = statusFor(
                    item = item,
                    activeInstall = activeInstall,
                    isInstalled = viewModel.isInstalled(item.id),
                )
                TranslationCard(
                    item = item,
                    status = status,
                    onInstall = { viewModel.installTranslation(item.id) },
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
                    onDelete = if (status is TranslationCardStatus.Installed &&
                        item.kind == "translation"
                    ) {
                        { viewModel.uninstallTranslation(item.id) }
                    } else null,
                    onDetails = { onOpenItem(item) },
                )
            }
        }
    }
}


// ─────────────────────────────────────────────────────────────────────
// Vertical List Shelf
// ─────────────────────────────────────────────────────────────────────

@Composable
private fun VerticalListShelfSection(
    shelf: StoreShelf,
    activeInstall : InstallState?,
    viewModel: FreeBibleStoreViewModel,
    onOpenItem: (StoreItem) -> Unit,
    onOpenEntitySet: (String, String) -> Unit,
    onSeeAll: () -> Unit,
) {
    if (shelf.items.isEmpty()) return

    val visible = shelf.items.take(3)
    val hasMore = shelf.items.size > 3

    Column(modifier = Modifier.padding(bottom = 20.dp)) {
        SectionHeader(
            title = shelf.title,
            onClick = onSeeAll.takeIf { hasMore },
        )

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            visible.forEach { item ->
                val status = statusFor(
                    item = item,
                    activeInstall = activeInstall,
                    isInstalled = viewModel.isInstalled(item.id),
                )
                CollectionTile(item = item, onClick = {}, trailing = {ActionView(status = status, onInstall = {viewModel.installTranslation(item.id)}, onOpen = {
                    onOpenItem(item)
                })})
            }
        }
    }
}


// ─────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────

private fun getPastelColor(id: String): Color {
    val palette = listOf(
        Color(0xFFFF8A65),
        Color(0xFF4DB6AC),
        Color(0xFFBA68C8),
        Color(0xFF7986CB),
        Color(0xFFFFB74D),
        Color(0xFF81C784)
    )
    var h = 0
    for (c in id) h = h * 31 + c.code
    return palette[(h and 0x7FFFFFFF) % palette.size]
}

private fun statusFor(
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