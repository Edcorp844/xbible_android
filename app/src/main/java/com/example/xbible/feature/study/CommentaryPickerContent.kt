package com.example.xbible.feature.study

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.xbible.viewmodel.ReaderViewModel

@Composable
fun CommentaryPickerContent(
    readerViewModel: ReaderViewModel,
    currentReference: String,
    onDismiss: () -> Unit,
) {
    // TODO: replace with a real catalogue lookup once the store exposes
    // installed commentaries. Hardcoded single commentary for now.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = "Open commentary at $currentReference",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = {
                readerViewModel.openCommentaryForCurrentChapter(
                    commentaryId = "adam-clarke",
                    commentaryName = "Adam Clarke",
                )
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Open Adam Clarke")
        }
    }
}