package com.example.xbible.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonDefaults.filledIconButtonColors
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedToggleButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TonalToggleButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xbible.ui.components.PageView
import com.example.xbible.viewmodel.EngineViewModel
import com.example.xbible.viewmodel.StudyTab
import uniffi.xbible_engine.ModuleBook
import uniffi.xbible_engine.SwordModule

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StudyScreen(
    engineViewModel: EngineViewModel,
    onNavigateToStore: () -> Unit
) {
    val sections by engineViewModel.currentChapterContent.collectAsState()
    val installedBibles by engineViewModel.installedBibles.collectAsState()
    val isLoading by engineViewModel.isLoadingContent.collectAsState()
    val currentReference by engineViewModel.currentReference.collectAsState()
    val currentModule by engineViewModel.currentModule.collectAsState()
    
    val tabs by engineViewModel.tabs.collectAsState()
    val activeTabIndex by engineViewModel.activeTabIndex.collectAsState()
    var showTabsPreview by remember { mutableStateOf(false) }
    var showModuleSelection by remember { mutableStateOf(false) }
    var showReferencePicker by remember { mutableStateOf(false) }
    val moduleSheetState = rememberModalBottomSheetState()
    val referenceSheetState = rememberModalBottomSheetState()
    
    val currentBooks by engineViewModel.currentBooksFlow.collectAsState()
    val currentBookIndex by engineViewModel.currentBookIndexFlow.collectAsState()
    val currentChapterIndex by engineViewModel.currentChapterIndexFlow.collectAsState()
    
    val hasNext by engineViewModel.hasNext.collectAsState()
    val hasPrevious by engineViewModel.hasPrevious.collectAsState()

    val listState = rememberLazyListState()
    
    var lastScrollIndex by remember { mutableIntStateOf(0) }
    var lastScrollOffset by remember { mutableIntStateOf(0) }
    var isScrollingUp by remember { mutableStateOf(true) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                val isUp = when {
                    index < lastScrollIndex -> true
                    index > lastScrollIndex -> false
                    else -> offset <= lastScrollOffset
                }
                
                if (isScrollingUp != isUp) {
                    isScrollingUp = isUp
                }
                
                lastScrollIndex = index
                lastScrollOffset = offset
            }
    }

    val showFab by remember {
        derivedStateOf {
            isScrollingUp && (hasNext || hasPrevious || tabs.size > 1)
        }
    }

    LaunchedEffect(sections) {
        if (sections.isNotEmpty()) {
            listState.scrollToItem(0)
            isScrollingUp = true
        }
    }

    LaunchedEffect(Unit) {
        engineViewModel.refreshEngine()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    title = {

                            SplitButtonLayout(
                                leadingButton = {
                                    SplitButtonDefaults.ElevatedLeadingButton(
                                        onClick = { showModuleSelection = true }
                                    ) {
                                        Text(
                                            text = currentModule?.name ?: "Select Bible",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                },
                                trailingButton = {
                                    SplitButtonDefaults.ElevatedTrailingButton(
                                        checked = showReferencePicker,
                                        onCheckedChange = {
                                            showReferencePicker = it
                                        }
                                    ) {
                                        Text(text = currentReference ?: "Study")
                                    }
                                }
                            )

                    },
                    actions = {
                        IconToggleButton(checked = false, onCheckedChange = { }) {
                            Icon(
                                Icons.Outlined.Search,
                                contentDescription = "Search"
                            )

                        }
                        IconToggleButton(checked = false, onCheckedChange = { }) {
                            Icon(
                                Icons.Outlined.MoreVert,
                                contentDescription = "More"
                            )

                        }
                    }
                )
            },
            floatingActionButtonPosition = FabPosition.Center,
            floatingActionButton = {
                AnimatedVisibility(
                    visible = showFab,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    HorizontalFloatingToolbar(
                        expanded = true,
                        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
                        content = {
                            IconButton(
                                onClick = { engineViewModel.previousChapter() },
                                enabled = hasPrevious
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Chapter")
                            }

                            Box(contentAlignment = Alignment.Center) {
                                FilledTonalButton(
                                    onClick = { showTabsPreview = true },

                                    shape = IconButtonDefaults.filledShape
                                ) {
                                    Icon(Icons.Default.Layers, contentDescription = "Tabs")
                                }
                                if (tabs.size > 1) {
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(top = 2.dp, end = 2.dp),
                                        color = MaterialTheme.colorScheme.secondary,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = tabs.size.toString(),
                                            modifier = Modifier.padding(horizontal = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondary
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = { engineViewModel.nextChapter() },
                                enabled = hasNext
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Chapter")
                            }
                        }
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when {
                    installedBibles.isEmpty() -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "No Bibles installed.")
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onNavigateToStore) {
                                Text("Go to Store")
                            }
                        }
                    }
                    isLoading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    sections.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "No content loaded. Select a chapter to study.")
                        }
                    }
                    else -> {
                        PageView(
                            sections = sections,
                            state = listState
                        )
                    }
                }
            }
        }

        // Module Selection Bottom Sheet
        if (showModuleSelection) {
            ModalBottomSheet(
                onDismissRequest = { showModuleSelection = false },
                sheetState = moduleSheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                ModuleSelectionContent(
                    installedBibles = installedBibles,
                    currentModule = currentModule,
                    onModuleSelected = {
                        engineViewModel.selectModule(it)
                        showModuleSelection = false
                    }
                )
            }
        }

        // Reference Picker Bottom Sheet
        if (showReferencePicker) {
            ModalBottomSheet(
                onDismissRequest = { showReferencePicker = false },
                sheetState = referenceSheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                ReferencePickerContent(
                    books = currentBooks,
                    selectedBookIndex = currentBookIndex,
                    selectedChapterIndex = currentChapterIndex,
                    onChapterSelected = { chapterNumber ->
                        engineViewModel.selectChapter(chapterNumber)
                        showReferencePicker = false
                    },
                    onBookSelected = { index ->
                        engineViewModel.selectBook(index)
                    }
                )
            }
        }

        // Tabs Preview Overlay
        AnimatedVisibility(
            visible = showTabsPreview,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            TabsPreview(
                tabs = tabs,
                activeTabIndex = activeTabIndex,
                onTabSelected = { index ->
                    engineViewModel.switchTab(index)
                    showTabsPreview = false
                },
                onAddTab = {
                    engineViewModel.addNewTab()
                    showTabsPreview = false
                },
                onRemoveTab = { index -> engineViewModel.removeTab(index) },
                onDismiss = { showTabsPreview = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ReferencePickerContent(
    books: List<ModuleBook>,
    selectedBookIndex: Int,
    selectedChapterIndex: Int,
    onBookSelected: (Int) -> Unit,
    onChapterSelected: (Int) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.6f)
            .padding(bottom = 32.dp)
    ) {
        ButtonGroup(
            overflowIndicator = { menuState ->
                IconButton(onClick = { menuState.show() }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
        ) {
            customItem(
                buttonGroupContent = {
                    OutlinedToggleButton(
                        checked = selectedTab == 0,
                        onCheckedChange = { if (it) selectedTab = 0 },
                        shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
                    ) {
                        Text("Books")
                    }
                },
                menuContent = { menuState ->
                    DropdownMenuItem(
                        text = { Text("Books") },
                        onClick = {
                            selectedTab = 0
                            menuState.dismiss()
                        }
                    )
                }
            )
            customItem(
                buttonGroupContent = {
                    OutlinedToggleButton(
                        checked = selectedTab == 1,
                        onCheckedChange = { if (it) selectedTab = 1 },
                        shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
                    ) {
                        Text("Chapters")
                    }
                },
                menuContent = { menuState ->
                    DropdownMenuItem(
                        text = { Text("Chapters") },
                        onClick = {
                            selectedTab = 1
                            menuState.dismiss()
                        }
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.weight(1f)) {
            if (selectedTab == 0) {
                // Books Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    gridItemsIndexed(books) { index, book ->
                        val isSelected = index == selectedBookIndex
                        Surface(
                            modifier = Modifier
                                .aspectRatio(2f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onBookSelected(index)
                                    selectedTab = 1
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = book.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSecondary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // Chapters Grid
                if (selectedBookIndex in books.indices) {
                    val book = books[selectedBookIndex]
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        gridItemsIndexed(book.chapters) { index, chapter ->
                            val isSelected = index == selectedChapterIndex
                            Surface(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onChapterSelected(chapter.number) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = chapter.number.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSecondary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Select a book first")
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
            )
        }
        Surface(
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(content = content)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ModuleSelectionContent(
    installedBibles: List<SwordModule>,
    currentModule: SwordModule?,
    onModuleSelected: (SwordModule) -> Unit
) {
    val groupedByLanguage = remember(installedBibles) {
        installedBibles.groupBy { it.language }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Select Bible Version",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp),
            fontWeight = FontWeight.Bold
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            groupedByLanguage.keys.sorted().forEach { langCode ->
                val modules = groupedByLanguage[langCode] ?: emptyList()
                item {
                    SettingsGroup(title = langCode.uppercase()) {
                        modules.forEachIndexed { index, module ->
                            val isSelected = currentModule?.name == module.name
                            
                            ListItem(
                                modifier = Modifier.clickable { onModuleSelected(module) },
                                colors = ListItemDefaults.colors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    else Color.Transparent
                                ),
                                headlineContent = {
                                    Text(
                                        text = module.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                                    )
                                },
                                supportingContent = if (module.description.isNotEmpty()) {
                                    {
                                        Text(
                                            text = module.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                } else null,
                                trailingContent = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Layers,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                } else null
                            )

                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun TabsPreview(
    tabs: List<StudyTab>,
    activeTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    onAddTab: () -> Unit,
    onRemoveTab: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(enabled = false) {}
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.systemBars.asPaddingValues())
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Study Tabs",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onBackground)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                gridItemsIndexed(tabs) { index, tab ->
                    TabThumbnail(
                        tab = tab,
                        isActive = index == activeTabIndex,
                        onClick = { onTabSelected(index) },
                        onRemove = { onRemoveTab(index) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onAddTab,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Bible Tab")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun TabThumbnail(
    tab: StudyTab,
    isActive: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.7f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.surface
        ),
        border = if (isActive) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tab.module?.name ?: "Empty",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Tab", modifier = Modifier.size(16.dp))
                    }
                }
                
                Text(
                    text = tab.reference ?: "No Reference",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp)
                ) {
                    Column {
                        tab.content.take(5).forEach { section ->
                            val previewText = section.verses.firstOrNull()?.words?.joinToString(" ") { it.text } ?: ""
                            Text(
                                text = previewText,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 8.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
