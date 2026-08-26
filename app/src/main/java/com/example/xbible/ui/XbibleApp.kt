package com.example.xbible.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.window.core.layout.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.example.xbible.R
import com.example.xbible.feature.library.LibraryScreen
import com.example.xbible.feature.store.StoreScreen
import com.example.xbible.feature.study.StudyScreen
import com.example.xbible.feature.tools.ToolsScreen
import com.example.xbible.ui.icons.XBibleIcons
import com.example.xbible.viewmodel.EngineViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun XbibleApp(engineViewModel: EngineViewModel) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.STUDY) }
    
    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val customNavSuiteType = with(adaptiveInfo) {
        if (!windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)) {
            NavigationSuiteType.NavigationBar
        } else {
            NavigationSuiteType.WideNavigationRailCollapsed
        }
    }

    NavigationSuiteScaffold(
        layoutType = customNavSuiteType,
        modifier = Modifier.fillMaxSize(),
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            navigationRailContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        navigationSuiteItems = {
            AppDestinations.entries.forEach { destination ->
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
        MainContent(
            modifier = Modifier.fillMaxSize(),
            destination = currentDestination,
            engineViewModel = engineViewModel,
            onNavigate = { currentDestination = it }
        )
    }
}

@Composable
fun MainContent(
    modifier: Modifier = Modifier,
    destination: AppDestinations,
    engineViewModel: EngineViewModel,
    onNavigate: (AppDestinations) -> Unit
) {
    Box(modifier = modifier) {
        when (destination) {
            AppDestinations.STUDY -> StudyScreen(engineViewModel = engineViewModel, onNavigateToStore = { onNavigate(AppDestinations.STORE) })
            AppDestinations.STORE -> StoreScreen()
            AppDestinations.TOOLS -> ToolsScreen()
            AppDestinations.LIBRARY -> LibraryScreen(
                onOpenModule = { module ->
                    engineViewModel.selectModule(module)
                    onNavigate(AppDestinations.STUDY)
                }
            )
        }
    }
}

enum class AppDestinations(
    val labelRes: Int,
    val icon: ImageVector
) {
    STUDY(R.string.study, XBibleIcons.Study),
    STORE(R.string.store, XBibleIcons.Store),
    TOOLS(R.string.tools, XBibleIcons.Tools),
    LIBRARY(R.string.library, XBibleIcons.Library),
}
