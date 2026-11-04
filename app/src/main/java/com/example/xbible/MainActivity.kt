package com.example.xbible

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.example.xbible.core.ui.layout.ProvideLayoutInfo
import com.example.xbible.feature.onboarding.EngineInitializationScreen
import com.example.xbible.ui.XbibleApp
import com.example.xbible.ui.theme.XbibleTheme
import com.example.xbible.viewmodel.EngineViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val engineViewModel: EngineViewModel by viewModels()

    // Modern activity result launcher (replaces deprecated onRequestPermissionsResult).
    // Result is ignored — playback still works, only the notification is affected.
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op */ }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        // Ask for POST_NOTIFICATIONS before any media session can try to show a notification.
        requestNotificationPermissionIfNeeded()

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)

            XbibleTheme {
                // Provide layout info to *everything* below — including the
                // initialization screen, so it can adapt to landscape too.
                ProvideLayoutInfo(windowSizeClass = windowSizeClass) {
                    val isReady by engineViewModel.isReady.collectAsState()
                    val errorMessage by engineViewModel.errorMessage.collectAsState()

                    when {
                        isReady -> XbibleApp(engineViewModel = engineViewModel)

                        errorMessage != null -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(text = errorMessage ?: "", color = Color.Red)
                            }
                        }

                        else -> EngineInitializationScreen()
                    }
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}