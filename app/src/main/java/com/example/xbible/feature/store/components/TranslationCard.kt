package com.example.xbible.feature.store.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    onDelete: (() -> Unit)? = null,
    onDetails: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .width(160.dp)
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

            // Menu
            if (status is TranslationCardStatus.Installed) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Options",
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuGroup(
                            shapes = MenuDefaults.groupShape(index = 0, count = 2)
                        ) {
                            if (onDetails != null) {
                                DropdownMenuItem(
                                    text = { Text("Details") },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Info, contentDescription = null)
                                    },
                                    onClick = {
                                        showMenu = false
                                        onDetails()
                                    },
                                )
                            }
                            if (onDelete != null) {
                                DropdownMenuItem(
                                    text = { Text("Uninstall") },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Delete, contentDescription = null)
                                    },
                                    colors = MenuDefaults.itemColors(
                                        textColor = MaterialTheme.colorScheme.error,
                                        leadingIconColor = MaterialTheme.colorScheme.error,
                                    ),
                                    onClick = {
                                        showMenu = false
                                        onDelete()
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // Action area
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