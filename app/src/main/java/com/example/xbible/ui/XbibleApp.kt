package com.example.xbible.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
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

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            imageVector = it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                MainContent(destination = currentDestination, engineViewModel = engineViewModel)
            }
        }
    }
}

@Composable
fun MainContent(destination: AppDestinations, engineViewModel: EngineViewModel) {
    when (destination) {
        AppDestinations.STUDY -> StudyScreen(engineViewModel = engineViewModel)
        AppDestinations.STORE -> StoreScreen()
        AppDestinations.TOOLS -> ToolsScreen()
        AppDestinations.LIBRARY -> LibraryScreen()
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    STUDY("Study", Icons.AutoMirrored.Filled.MenuBook),
    STORE("Store", Icons.Default.Storefront),
    TOOLS("Tools", Icons.Default.Build),
    LIBRARY("Library", Icons.AutoMirrored.Filled.LibraryBooks),
}
