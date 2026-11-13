package com.example.xbible.feature.study

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedToggleButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.xbible.core.ui.components.HighlightColor
import com.example.xbible.core.ui.components.HighlightStyle
import com.example.xbible.ui.icons.XBibleIcons

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VerseActionsSheet(
    reference: String,
    currentHighlight: HighlightColor?,
    currentStyle: HighlightStyle,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onOpenEntities: () -> Unit,
    onHighlight: (HighlightColor, HighlightStyle) -> Unit,
    onClearHighlight: () -> Unit,
    // ── Annotate ────────────────────────────────────────────
    onAnnotateNote: () -> Unit = {},
    onAnnotateTag: () -> Unit = {},
    onAnnotateLink: () -> Unit = {},
    onAnnotateRelation: () -> Unit = {},
    onAnnotateBookmark: () -> Unit = {},
    // ── Study ───────────────────────────────────────────────
    onStudyCommentary: () -> Unit = {},
    onStudyLexicon: () -> Unit = {},
    onStudyDictionary: () -> Unit = {},
    onStudyReferences: () -> Unit = {},
    onStudyEntities: () -> Unit = { onOpenEntities() },
    // ── Share ───────────────────────────────────────────────
    onShare: () -> Unit = {},
    onExport: () -> Unit = {},
    onSelectAll: () -> Unit = {},
) {
    var selectedTab by remember { mutableStateOf(VerseActionTab.Annotate) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))

        // ── Highlight color shortcuts (always visible) ─────────
        HighlightColorRow(
            currentHighlight = currentHighlight,
            onHighlight = { color ->
                onHighlight(color, currentStyle)
                onDismiss()
            },
            onClearHighlight = {
                onClearHighlight()
                onDismiss()
            },
        )

        Spacer(Modifier.height(20.dp))

        // ── Per-tab options (horizontal scroll, centered) ──────
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            contentPadding = PaddingValues(horizontal = 24.dp),
        ) {
            when (selectedTab) {
                VerseActionTab.Annotate -> {
                    item { ActionChip("Note", Icons.Default.EditNote, onAnnotateNote) }
                    item { ActionChip("Tag", Icons.Outlined.Label, onAnnotateTag) }
                    item { ActionChip("Link", Icons.Default.Link, onAnnotateLink) }
                    item { ActionChip("Relation", Icons.Outlined.AccountTree, onAnnotateRelation) }
                    item { ActionChip("Bookmark", Icons.Default.BookmarkAdd, onAnnotateBookmark) }
                }
                VerseActionTab.Study -> {
                    item { ActionChip("Commentary", Icons.AutoMirrored.Filled.MenuBook, onStudyCommentary) }
                    item { ActionChip("Lexicon", Icons.Outlined.Translate, onStudyLexicon) }
                    item { ActionChip("Dictionary", Icons.AutoMirrored.Filled.MenuBook, onStudyDictionary) }
                    item { ActionChip("References", Icons.Outlined.History, onStudyReferences) }
                    item { ActionChip("Entities", XBibleIcons.Study, onStudyEntities) }
                }
                VerseActionTab.Share -> {
                    item { ActionChip("Copy", Icons.Default.ContentCopy, onCopy) }
                    item { ActionChip("Share", Icons.Default.Share, onShare) }
                    item { ActionChip("Export", Icons.Default.Share, onExport) }
                    item { ActionChip("Select all", Icons.Outlined.SelectAll, onSelectAll) }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Bottom tab bar (matches StudyToolPicker) ───────────
        VerseActionTabBar(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
        )
    }
}

// ── Components ─────────────────────────────────────────────────────

@Composable
private fun HighlightColorRow(
    currentHighlight: HighlightColor?,
    onHighlight: (HighlightColor) -> Unit,
    onClearHighlight: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Clear / delete — same circle footprint as the swatches
        Surface(
            onClick = onClearHighlight,
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Clear Highlight",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        HighlightColor.entries.forEach { color ->
            val isSelected = currentHighlight == color
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Selected: ring + full-size swatch. Unselected: smaller
                // swatch so the selected one reads as "popped".
                Surface(
                    onClick = { onHighlight(color) },
                    modifier = Modifier.size(if (isSelected) 40.dp else 32.dp),
                    shape = CircleShape,
                    color = color.color,
                    border = if (isSelected) {
                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    } else null,
                ) {}
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VerseActionTabBar(
    selectedTab: VerseActionTab,
    onTabSelected: (VerseActionTab) -> Unit,
) {
    val tabs = VerseActionTab.entries
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ButtonGroup(
            overflowIndicator = { menuState ->
                IconButton(onClick = { menuState.show() }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More")
                }
            },
            modifier = Modifier.wrapContentWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = selectedTab == tab
                customItem(
                    buttonGroupContent = {
                        val shapes = when {
                            index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            index == tabs.size - 1 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        }
                        CompositionLocalProvider(
                            LocalMinimumInteractiveComponentSize provides Dp.Unspecified,
                        ) {
                            OutlinedToggleButton(
                                checked = isSelected,
                                onCheckedChange = { if (it) onTabSelected(tab) },
                                shapes = shapes,
                                modifier = Modifier.defaultMinSize(
                                    minWidth = 1.dp,
                                    minHeight = 1.dp,
                                ),
                                contentPadding = PaddingValues(
                                    horizontal = 16.dp,
                                    vertical = 8.dp,
                                ),
                            ) {
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold
                                    else FontWeight.Medium,
                                    maxLines = 1,
                                )
                            }
                        }
                    },
                    menuContent = { menuState ->
                        DropdownMenuItem(
                            text = { Text(tab.label) },
                            onClick = {
                                onTabSelected(tab)
                                menuState.dismiss()
                            },
                        )
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(FilterChipDefaults.IconSize),
            )
        },
        shape = CircleShape,
    )
}

private enum class VerseActionTab(val label: String) {
    Annotate("Annotate"),
    Study("Study"),
    Share("Share"),
}