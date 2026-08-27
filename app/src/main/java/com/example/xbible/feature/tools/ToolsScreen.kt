package com.example.xbible.feature.tools

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.xbible.R
import com.example.xbible.ui.icons.XBibleIcons

// Data class representing a tool item
data class ToolItem(
    val id: Int,
    val titleRes: Int,
    val descriptionRes: Int,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ToolsScreen(
    onNavigateToTimeline: () -> Unit
) {
    val studyAids = listOf(
        ToolItem(1, R.string.timeline, R.string.timeline_desc, XBibleIcons.TimeLine),
        ToolItem(5, R.string.biblical_maps, R.string.biblical_maps_desc, Icons.Default.Map)
    )
    
    val personalTools = listOf(
        ToolItem(4, R.string.reading_plans, R.string.reading_plans_desc, Icons.AutoMirrored.Filled.Assignment),
        ToolItem(3, R.string.notes, R.string.notes_desc, XBibleIcons.Notes)
    )
    
    val mediaTools = listOf(
        ToolItem(2, R.string.audio_bible, R.string.audio_bible_desc, Icons.Default.Headphones)
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.bible_tools),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            ToolSection(
                title = stringResource(R.string.study_aids),
                items = studyAids,
                onNavigateToTimeline = onNavigateToTimeline
            )

            Spacer(modifier = Modifier.height(12.dp))

            ToolSection(
                title = stringResource(R.string.personal),
                items = personalTools,
                onNavigateToTimeline = onNavigateToTimeline
            )

            Spacer(modifier = Modifier.height(12.dp))

            ToolSection(
                title = stringResource(R.string.media),
                items = mediaTools,
                onNavigateToTimeline = onNavigateToTimeline
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ToolSection(
    title: String,
    items: List<ToolItem>,
    onNavigateToTimeline: () -> Unit
) {
    SectionTitle(title)
    val count = items.size
    items.forEachIndexed { index, tool ->
        SegmentedListItem(
            onClick = { 
                if (tool.id == 1) onNavigateToTimeline()
            },
            modifier = Modifier.height(IntrinsicSize.Min),
            shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
            colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            leadingContent = { 
                Box(
                    modifier = Modifier.fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    SegmentIcon(tool.icon)
                }
            },
            trailingContent = {
                Box(
                    modifier = Modifier.fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            },
            supportingContent = { Text(stringResource(tool.descriptionRes)) }
        ) {
            Text(stringResource(tool.titleRes), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.secondary,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(start = 16.dp, bottom = 4.dp, top = 8.dp)
    )
}

@Composable
fun SegmentIcon(icon: ImageVector) {
    Surface(
        modifier = Modifier.size(40.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Extensions to ListItemDefaults to support the Segmented look.
 */
val ListItemDefaults.SegmentedGap: Dp get() = 4.dp
