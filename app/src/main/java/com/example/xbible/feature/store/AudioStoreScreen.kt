package com.example.xbible.feature.store

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.xbible.viewmodel.AudioStoreViewModel

@Composable
fun AudioStoreScreen(
    onExitToStudy: () -> Unit = {},
    viewModel: AudioStoreViewModel = hiltViewModel(),
) {
    AudioStoreRoot(
        onExitToStudy = onExitToStudy,
        viewModel = viewModel,
    )
}