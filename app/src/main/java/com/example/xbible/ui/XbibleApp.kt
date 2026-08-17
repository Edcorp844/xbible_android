package com.example.xbible.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.ShoppingBag
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
import com.example.xbible.ui.icons.handyman
import com.example.xbible.ui.screens.LibraryScreen
import com.example.xbible.ui.screens.StoreScreen
import com.example.xbible.ui.screens.StudyScreen
import com.example.xbible.ui.screens.ToolsScreen
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
                            contentDescription = destination.label,
                        )
                    },
                    label = { Text(destination.label) },
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
    val label: String,
    val icon: ImageVector
) {
    STUDY("Study", Icons.AutoMirrored.Outlined.MenuBook),
    STORE("Store", Icons.Outlined.ShoppingBag),
    TOOLS("Tools", handyman),
    LIBRARY("Library", Icons.AutoMirrored.Outlined.LibraryBooks),
}
