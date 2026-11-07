package com.example.xbible.feature.store.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uniffi.xbible_engine.StoreItem

sealed interface TranslationCardStatus {
    data object Idle : TranslationCardStatus
    data object Pending : TranslationCardStatus
    data class Installing(val fraction: Float) : TranslationCardStatus
    data object Installed : TranslationCardStatus
    data class Failed(val error: String) : TranslationCardStatus
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TranslationCard(
    item: StoreItem,
    status: TranslationCardStatus,
    onInstall: () -> Unit,
    onOpen: () -> Unit,
    onClick: () -> Unit,            // ← tap anywhere on the card
    modifier: Modifier = Modifier,
    cardWidth: Dp? = 160.dp,
) {
    Column(
        modifier = modifier
            .then(
                if (cardWidth != null) Modifier.width(cardWidth) else Modifier.fillMaxWidth()
            )
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Physical cover
        TranslationCover(item = item)

        // Bottom info bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.shortName.ifBlank { item.name },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.badge != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.badge ?: "",
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                }
            }

            // Single status action — Get / Open / progress / Retry
            ActionView(status = status, onInstall = onInstall, onOpen = onOpen)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ActionView(
    status: TranslationCardStatus,
    onInstall: () -> Unit,
    onOpen: () -> Unit,
) {
    when (status) {
        TranslationCardStatus.Idle -> {
            Button(
                onClick = onInstall,
                modifier = Modifier.height(28.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            ) {
                Text("Get", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        TranslationCardStatus.Installed -> {
            OutlinedButton(
                onClick = onOpen,
                modifier = Modifier.height(28.dp),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            ) {
                Text("Open", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        TranslationCardStatus.Pending -> {
            Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                CircularWavyProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                )
            }
        }

        is TranslationCardStatus.Installing -> {
            Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                CircularWavyProgressIndicator(
                    progress = { status.fraction },
                    modifier = Modifier.size(28.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "${(status.fraction * 100).toInt()}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        is TranslationCardStatus.Failed -> {
            OutlinedButton(
                onClick = onInstall,
                modifier = Modifier.height(28.dp),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            ) {
                Text("Retry", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}