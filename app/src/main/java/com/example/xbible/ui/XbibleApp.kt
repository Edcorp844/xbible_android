package com.example.xbible.ui

import androidx.activity.ComponentActivity
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.window.core.layout.WindowSizeClass
import com.example.xbible.R
import com.example.xbible.core.ui.components.MiniPlayer
import com.example.xbible.core.ui.layout.LayoutMode
import com.example.xbible.core.ui.layout.LocalLayoutInfo
import com.example.xbible.feature.library.LibraryScreen
import com.example.xbible.feature.store.StoreScreen
import com.example.xbible.feature.study.StudyScreen
import com.example.xbible.feature.tools.ToolsScreen
import com.example.xbible.feature.tools.audiobible.AudioBibleNavigation
import com.example.xbible.feature.tools.audiobible.AudioBiblePlayerScreen
import com.example.xbible.feature.tools.timeline.TimelineScreen
import com.example.xbible.ui.icons.XBibleIcons
import com.example.xbible.viewmodel.AudioBibleViewModel
import com.example.xbible.viewmodel.EngineViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun XbibleApp(engineViewModel: EngineViewModel) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.STUDY) }

    val activity = checkNotNull(LocalActivity.current as? ComponentActivity) {
        "XbibleApp must be hosted inside a ComponentActivity"
    }

    val audioBibleViewModel: AudioBibleViewModel = hiltViewModel(viewModelStoreOwner = activity)
    val selectedModule by audioBibleViewModel.selectedModule.collectAsState()

    var showFullScreenPlayer by rememberSaveable { mutableStateOf(false) }

    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val customNavSuiteType = with(adaptiveInfo) {
        if (!windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)) {
            NavigationSuiteType.NavigationBar
        } else {
            NavigationSuiteType.WideNavigationRailCollapsed
        }
    }

    // Determine if we should use the side-by-side (landscape/tablet) layout
    val layoutInfo = LocalLayoutInfo.current
    val isWideLayout = layoutInfo.mode == LayoutMode.TabletLandscape ||
            layoutInfo.mode == LayoutMode.TabletPortrait ||
            layoutInfo.mode == LayoutMode.PhoneLandscape

    Box(modifier = Modifier.fillMaxSize()) {

        // 1. The Main App
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
                        onClick = { currentDestination = destination }
                    )
                }
            }
        ) {
            // Wrap content in a Row to allow the vertical MiniPlayer to sit
            // nicely between the NavigationRail and the MainContent
            Row(modifier = Modifier.fillMaxSize()) {

                // If we are in landscape/tablet AND a song is selected, show the vertical sticker
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
                    modifier = Modifier.weight(1f), // Take up remaining space
                    destination = currentDestination,
                    engineViewModel = engineViewModel,
                    audioBibleViewModel = audioBibleViewModel,
                    onNavigate = { currentDestination = it }
                )
            }
        }

        // 2. MiniPlayer Overlay (Portrait only)
        // Only show the bottom bar if we are NOT in the wide layout
        if (selectedModule != null && !showFullScreenPlayer && !isWideLayout) {
            MiniPlayer(
                viewModel = audioBibleViewModel,
                onClick = { showFullScreenPlayer = true },
                isVertical = false,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(WindowInsets.navigationBars.asPaddingValues())
                    .padding(bottom = 80.dp) // Position above the NavigationBar
            )
        }

        // 3. The Artwork Screen (Full Screen Overlay)
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
    engineViewModel: EngineViewModel,
    audioBibleViewModel: AudioBibleViewModel,
    onNavigate: (AppDestinations) -> Unit
) {
    Box(modifier = modifier) {
        when (destination) {
            AppDestinations.STUDY -> StudyScreen(
                engineViewModel = engineViewModel,
                onNavigateToStore = { onNavigate(AppDestinations.STORE) })

            AppDestinations.STORE -> StoreScreen()

            AppDestinations.TOOLS -> ToolsScreen(
                onNavigateToTimeline = { onNavigate(AppDestinations.TIMELINE) },
                onNavigateToAudioBible = { onNavigate(AppDestinations.AUDIO_BIBLE) }
            )

            AppDestinations.LIBRARY -> LibraryScreen(
                onOpenModule = { module ->
                    engineViewModel.selectModule(module)
                    onNavigate(AppDestinations.STUDY)
                }
            )

            AppDestinations.TIMELINE -> TimelineScreen(
                onBack = { onNavigate(AppDestinations.TOOLS) },
                onEventClick = { event -> }
            )

            AppDestinations.AUDIO_BIBLE -> AudioBibleNavigation(
                viewModel = audioBibleViewModel,
                onBackToTools = { onNavigate(AppDestinations.TOOLS) }
            )
        }
    }
}

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