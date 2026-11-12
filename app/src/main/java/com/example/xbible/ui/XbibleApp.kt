package com.example.xbible.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import com.example.xbible.R
import com.example.xbible.core.ui.components.MiniPlayer
import com.example.xbible.core.ui.layout.LayoutMode
import com.example.xbible.core.ui.layout.LocalLayoutInfo
import com.example.xbible.feature.library.LibraryScreen
import com.example.xbible.feature.store.FreeBibleStoreScreen
import com.example.xbible.feature.store.LanguageSectionScreen
import com.example.xbible.feature.store.StoreItemDetailScreen
import com.example.xbible.feature.store.StoreRoute
import com.example.xbible.feature.store.StoreSectionScreen
import com.example.xbible.feature.study.StudyScreen
import com.example.xbible.feature.tools.ToolsScreen
import com.example.xbible.feature.tools.audiobible.AudioBibleNavigation
import com.example.xbible.feature.tools.audiobible.AudioBiblePlayerScreen
import com.example.xbible.feature.tools.timeline.TimelineScreen
import com.example.xbible.ui.icons.XBibleIcons
import com.example.xbible.viewmodel.AudioBibleViewModel
import com.example.xbible.viewmodel.FreeBibleStoreViewModel
import com.example.xbible.viewmodel.ReaderViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun XbibleApp(
    readerViewModel: ReaderViewModel,
) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.STUDY) }

    // Store sub-navigation state. Null = the main store screen.
    var storeRoute by rememberSaveable(stateSaver = StoreRouteSaver) {
        mutableStateOf<StoreRoute?>(null)
    }

    // Hardware back pops the store sub-stack first.
    BackHandler(enabled = storeRoute != null) {
        storeRoute = null
    }

    val activity = checkNotNull(LocalActivity.current as? ComponentActivity) {
        "XbibleApp must be hosted inside a ComponentActivity"
    }

    val audioBibleViewModel: AudioBibleViewModel = hiltViewModel(viewModelStoreOwner = activity)
    val selectedModule by audioBibleViewModel.selectedModule.collectAsState()

    val freeBibleStoreViewModel: FreeBibleStoreViewModel =
        hiltViewModel(viewModelStoreOwner = activity)

    var showFullScreenPlayer by rememberSaveable { mutableStateOf(false) }

    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val customNavSuiteType = with(adaptiveInfo) {
        if (!windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)) {
            NavigationSuiteType.NavigationBar
        } else {
            NavigationSuiteType.WideNavigationRailCollapsed
        }
    }

    val layoutInfo = LocalLayoutInfo.current
    val isWideLayout = layoutInfo.mode == LayoutMode.TabletLandscape ||
            layoutInfo.mode == LayoutMode.TabletPortrait ||
            layoutInfo.mode == LayoutMode.PhoneLandscape

    Box(modifier = Modifier.fillMaxSize()) {

        NavigationSuiteScaffold(
            layoutType = customNavSuiteType,
            modifier = Modifier.fillMaxSize(),
            navigationSuiteColors = NavigationSuiteDefaults.colors(
                navigationBarContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                navigationRailContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
            navigationSuiteItems = {
                AppDestinations.entries.filter { it.showInNav }.forEach { destination ->
                    item(
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = stringResource(destination.labelRes),
                            )
                        },
                        label = { Text(stringResource(destination.labelRes)) },
                        selected = destination == currentDestination,
                        onClick = {
                            // Leaving Store resets its sub-stack.
                            if (destination != AppDestinations.STORE) {
                                storeRoute = null
                            }
                            currentDestination = destination
                        }
                    )
                }
            }
        ) {
            Row(modifier = Modifier.fillMaxSize()) {

                if (isWideLayout && selectedModule != null && !showFullScreenPlayer) {
                    MiniPlayer(
                        viewModel = audioBibleViewModel,
                        onClick = { showFullScreenPlayer = true },
                        isVertical = true,
                        modifier = Modifier
                            .width(96.dp)
                            .fillMaxHeight()
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                    )
                }

                MainContent(
                    modifier = Modifier.weight(1f),
                    destination = currentDestination,
                    storeRoute = storeRoute,
                    onStoreRouteChange = { storeRoute = it },
                    readerViewModel = readerViewModel,
                    audioBibleViewModel = audioBibleViewModel,
                    freeBibleStoreViewModel = freeBibleStoreViewModel,
                    onNavigate = { currentDestination = it }
                )
            }
        }

        if (selectedModule != null && !showFullScreenPlayer && !isWideLayout) {
            MiniPlayer(
                viewModel = audioBibleViewModel,
                onClick = { showFullScreenPlayer = true },
                isVertical = false,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(WindowInsets.navigationBars.asPaddingValues())
                    .padding(bottom = 80.dp)
            )
        }

        if (showFullScreenPlayer && selectedModule != null) {
            AudioBiblePlayerScreen(
                viewModel = audioBibleViewModel,
                onBack = { showFullScreenPlayer = false }
            )
        }
    }
}

@Composable
fun MainContent(
    modifier: Modifier = Modifier,
    destination: AppDestinations,
    storeRoute: StoreRoute?,
    onStoreRouteChange: (StoreRoute?) -> Unit,
    readerViewModel: ReaderViewModel,
    audioBibleViewModel: AudioBibleViewModel,
    freeBibleStoreViewModel: FreeBibleStoreViewModel,
    onNavigate: (AppDestinations) -> Unit,
) {
    Box(modifier = modifier) {
        when (destination) {
            AppDestinations.STUDY -> StudyScreen(
                readerViewModel = readerViewModel,
                onNavigateToStore = { onNavigate(AppDestinations.STORE) },
            )

            AppDestinations.STORE -> StoreHost(
                viewModel = freeBibleStoreViewModel,
                route = storeRoute,
                onRouteChange = onStoreRouteChange,
            )

            AppDestinations.TOOLS -> ToolsScreen(
                onNavigateToTimeline = { onNavigate(AppDestinations.TIMELINE) },
                onNavigateToAudioBible = { onNavigate(AppDestinations.AUDIO_BIBLE) },
            )

            AppDestinations.LIBRARY -> LibraryScreen(
                onOpenTranslation = { translation ->
                    // Feed the translation into the reader tab and switch to
                    // the study destination. `selectTranslation` also loads
                    // the book list and opens the first book's chapter.
                    readerViewModel.selectTranslation(translation)
                    onNavigate(AppDestinations.STUDY)
                }
            )

            AppDestinations.TIMELINE -> TimelineScreen(
                onBack = { onNavigate(AppDestinations.TOOLS) },
                onEventClick = { _ -> },
            )

            AppDestinations.AUDIO_BIBLE -> AudioBibleNavigation(
                viewModel = audioBibleViewModel,
                onBackToTools = { onNavigate(AppDestinations.TOOLS) },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// Store sub-stack
// ─────────────────────────────────────────────────────────────────────

@Composable
private fun StoreHost(
    viewModel: FreeBibleStoreViewModel,
    route: StoreRoute?,
    onRouteChange: (StoreRoute?) -> Unit,
) {
    when (route) {
        null -> FreeBibleStoreScreen(
            viewModel = viewModel,
            onOpenTranslation = { _, _ -> /* TODO: open reader */ },
            onOpenEntitySet = { _, _ -> /* TODO */ },
            onOpenItem = { item ->
                onRouteChange(StoreRoute.ItemDetail(item.id, item.kind))
            },
            onSeeAll = { shelfTitle ->
                onRouteChange(StoreRoute.Section(shelfTitle))
            },
            onLanguageClick = { code ->
                onRouteChange(StoreRoute.Language(code))
            },
            onSeeAllLanguages = { onRouteChange(StoreRoute.Languages) },
            onOpenAudioStore = {},
        )

        is StoreRoute.Section -> {
            val shelves by viewModel.shelves.collectAsStateWithLifecycle()
            val items = remember(shelves, route.title) {
                shelves.firstOrNull { it.title == route.title }?.items ?: emptyList()
            }
            StoreSectionScreen(
                title = route.title,
                items = items,
                viewModel = viewModel,
                onBack = { onRouteChange(null) },
                onOpenTranslation = { _, _ -> /* TODO */ },
                onOpenEntitySet = { _, _ -> /* TODO */ },
                onOpenItem = { item ->
                    onRouteChange(StoreRoute.ItemDetail(item.id, item.kind))
                },
            )
        }

        is StoreRoute.ItemDetail -> { /* unchanged */ }

        is StoreRoute.Languages -> {
            val languages by viewModel.languages.collectAsStateWithLifecycle()
            LanguageSectionScreen(
                languages = languages,
                onLanguageSelected = { code ->
                    onRouteChange(StoreRoute.Language(code))
                },
                onBack = { onRouteChange(null) },
            )
        }

        is StoreRoute.Language -> {
            val shelves by viewModel.shelves.collectAsStateWithLifecycle()
            val languages by viewModel.languages.collectAsStateWithLifecycle()
            val langName = languages.firstOrNull { it.code == route.code }?.name ?: route.code

            // Show every item in this language across every shelf, flattened.
            val items = remember(shelves, route.code) {
                shelves.flatMap { it.items }
                    .filter { it.language == route.code }
                    .distinctBy { "${it.kind}:${it.id}" }
            }

            StoreSectionScreen(
                title = langName,
                items = items,
                viewModel = viewModel,
                onBack = { onRouteChange(StoreRoute.Languages) },
                onOpenTranslation = { _, _ -> /* TODO */ },
                onOpenEntitySet = { _, _ -> /* TODO */ },
                onOpenItem = { item ->
                    onRouteChange(StoreRoute.ItemDetail(item.id, item.kind))
                },
                languageFilter = route.code,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// Save/restore the store route across config changes
// ─────────────────────────────────────────────────────────────────────

private val StoreRouteSaver = Saver<StoreRoute?, ArrayList<Any>>(
    save = { route ->
        when (route) {
            null -> arrayListOf()
            is StoreRoute.Section -> arrayListOf("section", route.title)
            is StoreRoute.ItemDetail -> arrayListOf("item", route.id, route.kind)
            StoreRoute.Languages -> arrayListOf("languages")
            is StoreRoute.Language -> arrayListOf("language", route.code)
        }
    },
    restore = { saved ->
        when {
            saved.isEmpty() -> null
            saved[0] == "section" -> StoreRoute.Section(saved[1] as String)
            saved[0] == "item" -> StoreRoute.ItemDetail(
                saved[1] as String,
                saved[2] as String,
            )
            saved[0] == "languages" -> StoreRoute.Languages
            saved[0] == "language" -> StoreRoute.Language(saved[1] as String)
            else -> null
        }
    },
)

enum class AppDestinations(
    val labelRes: Int,
    val icon: ImageVector,
    val showInNav: Boolean = true,
) {
    STUDY(R.string.study, XBibleIcons.Study),
    STORE(R.string.store, XBibleIcons.Store),
    TOOLS(R.string.tools, XBibleIcons.Tools),
    LIBRARY(R.string.library, XBibleIcons.Library),
    TIMELINE(R.string.timeline, XBibleIcons.TimeLine, showInNav = false),
    AUDIO_BIBLE(R.string.audio_bible, Icons.Default.Headphones, showInNav = false),
}