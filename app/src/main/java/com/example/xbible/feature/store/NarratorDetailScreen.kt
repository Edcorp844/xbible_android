package com.example.xbible.feature.store

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.xbible.feature.store.components.ModuleCard
import com.example.xbible.feature.store.components.displayName
import com.example.xbible.viewmodel.AudioStoreViewModel
import uniffi.xbible_engine.AudioContributor
import uniffi.xbible_engine.RemoteAudioModuleInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NarratorDetailScreen(
    viewModel: AudioStoreViewModel,
    narrator: AudioContributor,
    onBack: (() -> Unit)? = null,
    onOpenModule: (RemoteAudioModuleInfo) -> Unit,
) {
    val catalog by viewModel.availableModules.collectAsState()
    val narrated = remember(catalog, narrator.name) {
        catalog.filter {
            it.contributor?.equals(narrator.name, ignoreCase = true) == true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(narrator.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            // 1. Header
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(110.dp).clip(CircleShape)) {
                    val url = narrator.photo?.url
                    if (!url.isNullOrBlank()) {
                        AsyncImage(
                            model = url,
                            contentDescription = narrator.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(
                                    MaterialTheme.colorScheme.secondaryContainer
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                initials(narrator.name),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }
                Spacer(Modifier.width(20.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        narrator.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                narrator.role.displayName(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        },
                    )
                }
            }

            // 2. Bio
            val bio = narrator.bio?.text
            if (!bio.isNullOrBlank()) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        "Biography",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        cleanBio(bio),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // 3. Narrated modules
            if (narrated.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            "Modules",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "${narrated.size} ${if (narrated.size == 1) "title" else "titles"} " +
                                    "narrated by ${narrator.name.split(" ").firstOrNull() ?: narrator.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                    ) {
                        items(narrated, key = { it.uniqueId }) { module ->
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
}

private fun initials(name: String): String =
    name.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")

private fun cleanBio(text: String): String =
    text.replace("\\n", " ").replace("\n", " ").trim()