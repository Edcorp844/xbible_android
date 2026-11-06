package com.example.xbible.feature.study

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.xbible.core.ui.components.HighlightColor
import com.example.xbible.core.ui.components.HighlightStyle
import com.example.xbible.ui.icons.XBibleIcons

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VerseActionsSheet(
    reference: String,
    verseText: String,
    currentHighlight: HighlightColor?,
    currentStyle: HighlightStyle,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onOpenEntities: () -> Unit,
    onHighlight: (HighlightColor, HighlightStyle) -> Unit,
    onClearHighlight: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        // 1. Header (Reference & Verse Preview)
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text(
                text = reference,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = verseText,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(8.dp))

        // 2. Actions (Updated for M3 1.3+ API)
        ListItem(
            modifier = Modifier.clickable {
                onCopy()
                onDismiss()
            },
            leadingContent = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        ) {
            Text("Copy Verse Text", style = MaterialTheme.typography.bodyLarge)
        }

        ListItem(
            modifier = Modifier.clickable {
                onOpenEntities()
                onDismiss()
            },
            leadingContent = { Icon(XBibleIcons.Study, contentDescription = null) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        ) {
            Text("Entities & Notes", style = MaterialTheme.typography.bodyLarge)
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        // 3. Highlight Colors
        Text(
            text = "Highlight Color",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Clear Highlight Option
            item {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable {
                            onClearHighlight()
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Box(
                            modifier = Modifier.size(44.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear Highlight",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Color Options
            items(HighlightColor.entries) { color ->
                val isSelected = currentHighlight == color
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 52.dp else 44.dp)
                        .clip(CircleShape)
                        .clickable {
                            onHighlight(color, currentStyle)
                            onDismiss()
                        }
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = CircleShape,
                        color = color.color,
                        border = if (isSelected) {
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        } else null
                    ) {
                        Box(modifier = Modifier.size(52.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Highlight Styles
        Text(
            text = "Highlight Style",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HighlightStyle.entries.forEach { style ->
                FilterChip(
                    selected = currentStyle == style,
                    onClick = {
                        // Apply the new style immediately
                        onHighlight(currentHighlight ?: HighlightColor.Yellow, style)
                    },
                    label = { Text(style.name) }
                )
            }
        }
    }
}