package com.example.xbible.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xbible.viewmodel.InstallationStatus
import uniffi.xbible_engine.SwordModule

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BookCardView(
    module: SwordModule,
    status: InstallationStatus,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    categoryName: String? = null,
    menuAction: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    isLibraryMode: Boolean = false
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .width(160.dp)
            .padding(4.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Physical Book Cover
        BookView(module = module)

        // Bottom Info Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = module.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = module.description,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Menu button logic
            if (isLibraryMode || (status is InstallationStatus.Installed && (menuAction != null || onDelete != null))) {
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color.Gray.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Options",
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        val groupCount = if (onDelete != null) 2 else 1
                        
                        DropdownMenuGroup(
                            shapes = MenuDefaults.groupShape(index = 0, count = groupCount)
                        ) {
                            if (isLibraryMode) {
                                DropdownMenuItem(
                                    text = { Text("Open in Study") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Outlined.ArrowOutward,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        onAction()
                                    }
                                )
                            } else {
                                menuAction?.let {
                                    DropdownMenuItem(
                                        text = { Text("Update") },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.CloudDownload,
                                                contentDescription = null
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            it()
                                        }
                                    )
                                }
                            }
                        }

                        if (onDelete != null) {
                            Spacer(modifier = Modifier.height(MenuDefaults.GroupSpacing))
                            
                            DropdownMenuGroup(
                                shapes = MenuDefaults.groupShape(index = 1, count = groupCount)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Outlined.Delete,
                                            contentDescription = null
                                        )
                                    },
                                    colors = MenuDefaults.itemColors(
                                        textColor = MaterialTheme.colorScheme.error,
                                        leadingIconColor = MaterialTheme.colorScheme.error
                                    ),
                                    onClick = {
                                        showMenu = false
                                        onDelete()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Action UI based on status (Hidden in Library Mode)
            if (!isLibraryMode) {
                ActionView(status = status, onAction = onAction)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ActionView(
    status: InstallationStatus,
    onAction: () -> Unit
) {
    when (status) {
        is InstallationStatus.Idle -> {
            Button(
                onClick = { onAction() },
                modifier = Modifier.height(28.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            ) {
                Text("Get", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        is InstallationStatus.Installed -> {
            OutlinedButton(
                onClick = { onAction() },
                modifier = Modifier.height(28.dp),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Open", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        is InstallationStatus.Pending -> {
            Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                CircularWavyProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                )
            }
        }
        is InstallationStatus.Installing -> {
            Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                CircularWavyProgressIndicator(
                    progress = { status.progress },
                    modifier = Modifier.size(28.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${(status.progress * 100).toInt()}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        is InstallationStatus.Failed, is InstallationStatus.Cancelled -> {
            OutlinedButton(
                onClick = { onAction() },
                modifier = Modifier.height(28.dp),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Text("Retry", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
