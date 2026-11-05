package com.example.xbible.feature.store

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.xbible.viewmodel.FreeBibleStoreViewModel
import com.example.xbible.viewmodel.InstallState

@Composable
fun InstallProgressDialog(viewModel: FreeBibleStoreViewModel) {
    val install: InstallState = viewModel.activeInstall.collectAsStateWithLifecycle().value
        ?: return

    val title: String = when {
        install.failed != null -> "Install failed"
        install.done -> "${install.translationId} installed"
        install.fraction <= 0f -> "Preparing ${install.translationId}"
        else -> "Installing ${install.translationId}"
    }

    AlertDialog(
        onDismissRequest = {
            if (install.done) viewModel.dismissActiveInstall()
        },
        title = {
            Text(title, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (install.failed != null) {
                    Text(
                        text = install.failed,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { install.fraction.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (install.message.isNotBlank()) {
                        Text(
                            text = install.message,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    if (install.chaptersTotal > 0) {
                        Text(
                            text = "${install.chaptersDone}/${install.chaptersTotal} chapters • " +
                                    "${install.versesDone} verses",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (install.currentBook.isNotBlank()) {
                        Text(
                            text = "Now: ${install.currentBook} ${install.currentChapter}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (install.done) {
                TextButton(onClick = { viewModel.dismissActiveInstall() }) {
                    Text("Done")
                }
            } else {
                TextButton(onClick = { viewModel.cancelActiveInstall() }) {
                    Text("Cancel")
                }
            }
        },
        dismissButton = null,
    )
}