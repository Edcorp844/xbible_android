package com.example.xbible

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.xbible.ui.XbibleApp
import com.example.xbible.ui.screens.EngineInitializationScreen
import com.example.xbible.ui.theme.XbibleTheme
import com.example.xbible.viewmodel.EngineViewModel

class MainActivity : ComponentActivity() {
    private val engineViewModel: EngineViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        setContent {
            XbibleTheme {
                val isReady by engineViewModel.isReady.collectAsState()
                val errorMessage by engineViewModel.errorMessage.collectAsState()

                when {
                    isReady -> XbibleApp(engineViewModel = engineViewModel)
                    errorMessage != null -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = errorMessage ?: "", color = Color.Red)
                        }
                    }
                    else -> EngineInitializationScreen()
                }
            }
        }
    }
}
