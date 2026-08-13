package com.example.xbible.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.xbible.ui.components.PageView
import com.example.xbible.viewmodel.EngineViewModel

@Composable
fun StudyScreen(engineViewModel: EngineViewModel) {
    val sections by engineViewModel.currentChapterContent.collectAsState()
    if (sections.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No content loaded. Select a chapter to study.")
        }
    } else {
        PageView(sections = sections)
    }
}
