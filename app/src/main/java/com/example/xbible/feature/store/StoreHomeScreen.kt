package com.example.xbible.feature.store

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xbible.feature.store.components.CollectionTile
import com.example.xbible.feature.store.components.ContributorAvatarTile
import com.example.xbible.feature.store.components.ModuleCard
import com.example.xbible.viewmodel.AudioStoreViewModel
import uniffi.xbible_engine.AudioCollection
import uniffi.xbible_engine.AudioContributor
import uniffi.xbible_engine.AudioLanguage
import uniffi.xbible_engine.RemoteAudioModuleInfo

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StoreHomeScreen(
    viewModel: AudioStoreViewModel,
    onOpenLanguage: (AudioLanguage) -> Unit,
    onOpenContributor: (AudioContributor) -> Unit,
    onOpenCollection: (AudioCollection) -> Unit,
    onOpenAlbum: (uniffi.xbible_engine.AudioAlbum) -> Unit,
    onOpenModule: (RemoteAudioModuleInfo) -> Unit,
    onOpenAllRecents: () -> Unit,
    onOpenAllInstalled: () -> Unit,
    onOpenAllCollections: () -> Unit,
) {
    val modules by viewModel.availableModules.collectAsState()
    val languages by viewModel.languages.collectAsState()
    val collections by viewModel.collections.collectAsState()
    val narrators by viewModel.narrators.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchText by viewModel.searchText.collectAsState()

    val recentModules = remember(modules) {
        modules.filter { !it.isInstalled }.sortedByDescending { it.updatedAt }
    }
    val installedModules = remember(modules) { modules.filter { it.isInstalled } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Audio Store",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            HeroCard(
                moduleCount = modules.size,
                languageCount = languages.size,
            )
//            SearchField(
//                query = searchText,
//                onQueryChange = { viewModel.updateSearchText(it) },
//            )
            if (isLoading && modules.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) { LoadingIndicator() }
            }
            if (languages.isNotEmpty()) {
                SectionHeader(title = "Languages", onClick = null)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                ) {
                    items(languages, key = { it.id }) { language ->
                        AssistChip(
                            onClick = { onOpenLanguage(language) },
                            label = { Text(language.name, fontWeight = FontWeight.SemiBold) },
                            shape = CircleShape,
                        )
                    }
                }
            }
            if (narrators.isNotEmpty()) {
                SectionHeader(title = "Contributors", onClick = null)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                ) {
                    items(narrators, key = { it.id }) { contributor ->
                        ContributorAvatarTile(
                            contributor = contributor,
                            onClick = { onOpenContributor(contributor) },
                        )
                    }
                }
            }
            if (collections.isNotEmpty()) {
                SectionHeader(title = "Collections", onClick = onOpenAllCollections)
                Column(horizontalAlignment = Alignment.Start) {
                    collections.take(5).forEachIndexed { index, c ->
                        CollectionTile(
                            collection = c,
                            onClick = { onOpenCollection(c) },
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }
                }
            }
            if (recentModules.isNotEmpty()) {
                SectionHeader(title = "Recents", onClick = onOpenAllRecents)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                ) {
                    items(recentModules.take(10), key = { it.uniqueId }) { module ->
                        ModuleCard(
                            module = module,
                            viewModel = viewModel,
                            onClick = { onOpenModule(module) },
                            modifier = Modifier.width(160.dp),
                        )
                    }
                }
            }
            if (installedModules.isNotEmpty()) {
                SectionHeader(title = "Installed", onClick = onOpenAllInstalled)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                ) {
                    items(installedModules.take(10), key = { it.uniqueId }) { module ->
                        ModuleCard(
                            module = module,
                            viewModel = viewModel,
                            onClick = { onOpenModule(module) },
                            modifier = Modifier.width(160.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroCard(moduleCount: Int, languageCount: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Brush.linearGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    Color.Transparent,
                )
            ).let { gradient ->
                Box(
                    Modifier
                        .matchParentSize()
                        .background(gradient)
                )
            }
            androidx.compose.material3.Icon(
                imageVector = Icons.Default.Book,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(120.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
            )
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Book, null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "X-BIBLE AUDIO",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Listen to the Word",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "$moduleCount modules across $languageCount language${if (languageCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        placeholder = { Text("Search modules, narrators") },
        leadingIcon = {
            Icon(androidx.compose.material.icons.Icons.Outlined.Search, null)
        },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(androidx.compose.material.icons.Icons.Outlined.Close, "Clear")
                }
            }
        } else null,
        shape = CircleShape,
        singleLine = true,
    )
}

@Composable
private fun SectionHeader(title: String, onClick: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        if (onClick != null) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward, // Changed from KeyboardArrowRight to ArrowForward
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}