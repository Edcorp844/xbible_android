package com.example.xbible.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
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
            NavigationSuiteType.ShortNavigationBarCompact
        } else {
            NavigationSuiteType.WideNavigationRailCollapsed
        }
    }

    NavigationSuiteScaffold(
        layoutType = customNavSuiteType,
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
            destination = currentDestination,
            engineViewModel = engineViewModel,
            onNavigate = { currentDestination = it }
        )
    }
}

@Composable
fun MainContent(
    destination: AppDestinations,
    engineViewModel: EngineViewModel,
    onNavigate: (AppDestinations) -> Unit
) {
    when (destination) {
        AppDestinations.STUDY -> StudyScreen(engineViewModel = engineViewModel, onNavigateToStore = { onNavigate(AppDestinations.STORE) })
        AppDestinations.STORE -> StoreScreen()
        AppDestinations.TOOLS -> ToolsScreen()
        AppDestinations.LIBRARY -> LibraryScreen()
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector
) {
    STUDY("Study", Icons.AutoMirrored.Outlined.MenuBook),
    STORE("Store", Icons.Outlined.Storefront),
    TOOLS("Tools", Icons.Outlined.Build),
    LIBRARY("Library", Icons.AutoMirrored.Outlined.LibraryBooks),
}
