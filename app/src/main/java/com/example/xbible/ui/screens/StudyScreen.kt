package com.example.xbible.ui.screens

import android.text.Spanned
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AdaptStrategy
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.LevitatedPaneScrim
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffold
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldDefaults
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.rememberSupportingPaneScaffoldNavigator
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import com.example.xbible.ui.components.PageView
import com.example.xbible.ui.components.SectionView
import com.example.xbible.ui.icons.tab_group
import com.example.xbible.viewmodel.EngineViewModel
import com.example.xbible.viewmodel.StudyTab
import com.example.xbible.viewmodel.StudyTool
import uniffi.xbible_engine.DictionaryResult
import uniffi.xbible_engine.LexiconResult
import uniffi.xbible_engine.ModuleBook
import uniffi.xbible_engine.Section
import uniffi.xbible_engine.SwordModule
import uniffi.xbible_engine.Word

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun StudyScreen(
    engineViewModel: EngineViewModel,
    onNavigateToStore: () -> Unit
) {
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val isCompact = !adaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    val isExpanded = adaptiveInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED
    
    val sections by engineViewModel.currentChapterContent.collectAsState()
    val installedBibles by engineViewModel.installedBibles.collectAsState()
    val isLoading by engineViewModel.isLoadingContent.collectAsState()
    val currentReference by engineViewModel.currentReference.collectAsState()
    val currentModule by engineViewModel.currentModule.collectAsState()
    
    // Selection State
    var selectedWord by remember { mutableStateOf<Word?>(null) }
    var showLexicon by remember { mutableStateOf(false) }
    
    val tabs by engineViewModel.tabs.collectAsState()
    val activeTabIndex by engineViewModel.activeTabIndex.collectAsState()
    var showTabsPreview by remember { mutableStateOf(false) }
    
    var showModuleSelection by remember { mutableStateOf(false) }
    var showReferencePicker by remember { mutableStateOf(false) }
    
    val moduleSheetState = rememberModalBottomSheetState()
    val referenceSheetState = rememberModalBottomSheetState()
    val lexiconSheetState = rememberModalBottomSheetState()

    // Outer navigator for Bible/Reference Drawer
    val drawerNavigator = rememberSupportingPaneScaffoldNavigator(
        scaffoldDirective = calculatePaneScaffoldDirective(adaptiveInfo).copy(
            maxHorizontalPartitions = 1
        ),
        adaptStrategies = SupportingPaneScaffoldDefaults.adaptStrategies(
            supportingPaneAdaptStrategy = AdaptStrategy.Levitate(
                alignment = Alignment.TopEnd,
                scrim = {
                    LevitatedPaneScrim(
                        onClick = {
                            showModuleSelection = false
                            showReferencePicker = false
                        }
                    )
                }
            )
        )
    )

    // Handle Drawer Visibility
    val isBibleDrawerVisible = (showModuleSelection || showReferencePicker) && !isCompact
    LaunchedEffect(isBibleDrawerVisible) {
        if (isBibleDrawerVisible) {
            drawerNavigator.navigateTo(SupportingPaneScaffoldRole.Supporting)
        } else {
            drawerNavigator.navigateBack()
        }
    }

    // Auto-show lexicon on expanded screens
    LaunchedEffect(selectedWord, isExpanded) {
        if (selectedWord != null && isExpanded) {
            showLexicon = true
        }
    }

    val currentBooks by engineViewModel.currentBooksFlow.collectAsState()
    val currentBookIndex by engineViewModel.currentBookIndexFlow.collectAsState()
    val currentChapterIndex by engineViewModel.currentChapterIndexFlow.collectAsState()
    
    val hasNext by engineViewModel.hasNext.collectAsState()
    val hasPrevious by engineViewModel.hasPrevious.collectAsState()

    val listState = rememberLazyListState()
    
    // Resizable split state
    var lexiconPaneWidth by remember { mutableStateOf(400.dp) }
    val density = LocalDensity.current

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
                if (isScrollingUp != isUp) isScrollingUp = isUp
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

    // Shared Header UI
    val sharedHeader = @Composable {
        TopAppBar(
            title = {
                SplitButtonLayout(
                    leadingButton = {
                        val isSelected = showModuleSelection && !isCompact
                        SplitButtonDefaults.ElevatedLeadingButton(
                            onClick = { 
                                if (isCompact) {
                                    showModuleSelection = true
                                    showLexicon = false
                                    showReferencePicker = false
                                } else {
                                    showModuleSelection = !showModuleSelection
                                    showReferencePicker = false
                                }
                            },
                            colors = if (isSelected) {
                                ButtonDefaults.elevatedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            } else ButtonDefaults.elevatedButtonColors()
                        ) {
                            Text(
                                text = currentModule?.name ?: "Select Bible",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    trailingButton = {
                        val isSelected = showReferencePicker && !isCompact
                        SplitButtonDefaults.ElevatedTrailingButton(
                            checked = isSelected,
                            onCheckedChange = {
                                if (isCompact) {
                                    showReferencePicker = it
                                    showLexicon = false
                                    showModuleSelection = false
                                } else {
                                    showReferencePicker = it
                                    showModuleSelection = false
                                }
                            }
                        ) {
                            Text(text = currentReference ?: "Study")
                        }
                    }
                )
            },
            actions = {
                if (!isCompact) {
                    IconToggleButton(
                        checked = showLexicon,
                        onCheckedChange = { showLexicon = it }
                    ) {
                        Icon(
                            Icons.Outlined.AutoAwesome,
                            contentDescription = "Study Tools"
                        )
                    }
                }
                IconButton(onClick = { }) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
                IconButton(onClick = { }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
            }
        )
    }

    // --- ROOT SCAFFOLD ---
    SupportingPaneScaffold(
        modifier = Modifier.fillMaxSize(),
        directive = drawerNavigator.scaffoldDirective,
        value = drawerNavigator.scaffoldValue,
        mainPane = {
            AnimatedPane(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    topBar = sharedHeader,
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
                                            Icon(tab_group, contentDescription = "Tabs")
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
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        val innerMaxWidth = maxWidth
                        val minPaneWidth = 300.dp
                        
                        Row(modifier = Modifier.fillMaxSize()) {
                            // 1. Bible Text Area
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
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
                                            state = listState,
                                            onWordClick = { word ->
                                                selectedWord = word
                                                engineViewModel.lookupWord(word)
                                                showLexicon = true
                                            },
                                            onStrongsClick = { strongs ->
                                                engineViewModel.lookupStrongs(strongs)
                                                showLexicon = true
                                            }
                                        )
                                    }
                                }
                            }

                            // 2. Study Tools Side Pane (Resizable)
                            if (showLexicon && !isCompact) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(12.dp)
                                        .pointerHoverIcon(PointerIcon.Hand)
                                        .draggable(
                                            state = rememberDraggableState { delta ->
                                                val deltaDp = with(density) { delta.toDp() }
                                                lexiconPaneWidth = (lexiconPaneWidth - deltaDp).coerceIn(minPaneWidth, innerMaxWidth * 0.7f)
                                            },
                                            orientation = Orientation.Horizontal
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .height(48.dp)
                                            .width(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .width(lexiconPaneWidth)
                                        .fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                ) {
                                    SplitDetailPane(
                                        engineViewModel = engineViewModel,
                                        onDismiss = { showLexicon = false }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        supportingPane = {
            if (isBibleDrawerVisible) {
                AnimatedPane(
                    modifier = Modifier
                        .preferredHeight(1f)
                        .preferredWidth(400.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = RectangleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        tonalElevation = 3.dp,
                        shadowElevation = 16.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .padding(top = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (showModuleSelection) "Bible Selection" else "Reference Picker",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(onClick = {
                                    showModuleSelection = false
                                    showReferencePicker = false
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close Drawer")
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))

                            if (showModuleSelection) {
                                ModuleSelectionContent(installedBibles, currentModule) { module ->
                                    engineViewModel.selectModule(module)
                                    showModuleSelection = false
                                }
                            } else if (showReferencePicker) {
                                ReferencePickerContent(currentBooks, currentBookIndex, currentChapterIndex, { index -> engineViewModel.selectBook(index) }) { chapter ->
                                    engineViewModel.selectChapter(chapter)
                                    showReferencePicker = false
                                }
                            }
                        }
                    }
                }
            }
        }
    )

    // --- MOBILE OVERLAYS ---
    if (isCompact) {
        if (showModuleSelection) {
            ModalBottomSheet(onDismissRequest = { showModuleSelection = false }, sheetState = moduleSheetState) {
                ModuleSelectionContent(installedBibles, currentModule) { module ->
                    engineViewModel.selectModule(module)
                    showModuleSelection = false
                }
            }
        }
        if (showReferencePicker) {
            ModalBottomSheet(onDismissRequest = { showReferencePicker = false }, sheetState = referenceSheetState) {
                ReferencePickerContent(currentBooks, currentBookIndex, currentChapterIndex, { index -> engineViewModel.selectBook(index) }) { chapter ->
                    engineViewModel.selectChapter(chapter)
                    showReferencePicker = false
                }
            }
        }
        if (showLexicon) {
            ModalBottomSheet(onDismissRequest = { showLexicon = false }, sheetState = lexiconSheetState) {
                SplitDetailPane(engineViewModel) { showLexicon = false }
            }
        }
    }

    // Tabs Preview remain full screen
    AnimatedVisibility(
        visible = showTabsPreview,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        TabsPreview(
            tabs = tabs,
            activeTabIndex = activeTabIndex,
            onTabSelected = { index -> engineViewModel.switchTab(index); showTabsPreview = false },
            onAddTab = { engineViewModel.addNewTab(); showTabsPreview = false },
            onRemoveTab = { index -> engineViewModel.removeTab(index) },
            onDismiss = { showTabsPreview = false }
        )
    }
}

@Composable
fun SplitDetailPane(
    engineViewModel: EngineViewModel,
    onDismiss: () -> Unit
) {
    val selectedTab by engineViewModel.selectedTool.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        Surface(modifier = Modifier.fillMaxWidth(), color = Color.Transparent) {
            Column {
                StudyToolPicker(selectedTab = selectedTab) { engineViewModel.selectTool(it) }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
        
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                StudyTool.Dictionary -> DictionaryTabContent(engineViewModel)
                StudyTool.Lexicon -> LexiconTabContent(engineViewModel)
                StudyTool.Commentary -> CommentaryTabContent(engineViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StudyToolPicker(
    selectedTab: StudyTool,
    onTabSelected: (StudyTool) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        ButtonGroup(
            overflowIndicator = { menuState ->
                IconButton(onClick = { menuState.show() }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
            },
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
        ) {
            val toolList = StudyTool.entries
            toolList.forEachIndexed { index, tool ->
                val isSelected = selectedTab == tool
                
                customItem(
                    buttonGroupContent = {
                        val shapes = when {
                            index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            index == toolList.size - 1 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        }
                        OutlinedToggleButton(
                            checked = isSelected,
                            onCheckedChange = { if (it) onTabSelected(tool) },
                            shapes = shapes,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = tool.name,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    },
                    menuContent = { menuState ->
                        DropdownMenuItem(
                            text = { Text(tool.name) },
                            onClick = {
                                onTabSelected(tool)
                                menuState.dismiss()
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun DictionaryTabContent(engineViewModel: EngineViewModel) {
    val selectedWord by engineViewModel.selectedWordForLookup.collectAsState()
    val results by engineViewModel.dictionaryResults.collectAsState()
    val isLoading by engineViewModel.isDictionaryLoading.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectedWord.isNotEmpty()) {
                Column {
                    Text(text = selectedWord, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.width(24.dp).height(3.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                }
            } else {
                Text(text = "Select a word to lookup", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (selectedWord.isEmpty()) {
            DictionaryEmptyState()
        } else if (results.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "No definitions found for \"$selectedWord\".", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(results) { result -> DictionaryResultRow(result) }
            }
        }
    }
}

@Composable
fun DictionaryResultRow(result: DictionaryResult) {
    val clipboard = LocalClipboardManager.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                    Text(text = result.moduleName, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { clipboard.setText(AnnotatedString("${result.key}\n\n${result.definition}")) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = result.key, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            HtmlText(html = result.definition, key = result.key)
        }
    }
}

@Composable
fun DictionaryEmptyState() {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Dictionary Lookup", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "Click any word in the scripture view to automatically look up its definition across all matching installed dictionary modules.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun LexiconTabContent(engineViewModel: EngineViewModel) {
    val selectedStrongs by engineViewModel.selectedStrongsForLookup.collectAsState()
    val selectedModule by engineViewModel.selectedLexiconModule.collectAsState()
    val availableLexicons by engineViewModel.availableLexicons.collectAsState()
    val results by engineViewModel.lexiconResults.collectAsState()
    val isLoading by engineViewModel.isLexiconLoading.collectAsState()
    var showModuleMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Lexicon", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (availableLexicons.isEmpty()) {
                        Text(text = "No lexicons installed", style = MaterialTheme.typography.labelSmall)
                    } else {
                        Box {
                            Surface(onClick = { showModuleMenu = true }, shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)) {
                                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(text = selectedModule?.description?.takeIf { it.isNotEmpty() } ?: selectedModule?.name ?: "Select Lexicon", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall)
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(12.dp))
                                }
                            }
                            DropdownMenu(expanded = showModuleMenu, onDismissRequest = { showModuleMenu = false }) {
                                availableLexicons.forEach { module ->
                                    DropdownMenuItem(text = { Text(module.description.ifEmpty { module.name }) }, onClick = { engineViewModel.selectLexiconModule(module); showModuleMenu = false }, trailingIcon = if (selectedModule?.name == module.name) { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null)
                                }
                            }
                        }
                    }
                }
                if (selectedStrongs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Strong's Code: ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Surface(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                            Text(text = selectedStrongs, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (selectedStrongs.isEmpty()) {
            LexiconEmptyState()
        } else if (results.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(text = "No lexicon entries found for \"$selectedStrongs\" in ${selectedModule?.name}.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                items(results) { result ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = result.moduleName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "(${result.key})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        HtmlText(html = result.definition, key = result.key, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
fun LexiconEmptyState() {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Outlined.Translate, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Lexicon Study", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "Click on a Strong's number below any Greek/Hebrew word to see its lexicon entry, morphological information, and detailed translation notes.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun CommentaryTabContent(engineViewModel: EngineViewModel) {
    val selectedModule by engineViewModel.selectedCommentaryModule.collectAsState()
    val availableCommentaries by engineViewModel.availableCommentaries.collectAsState()
    val results by engineViewModel.commentaryResults.collectAsState()
    val isLoading by engineViewModel.isCommentaryLoading.collectAsState()
    val currentReference by engineViewModel.currentCommentaryReference.collectAsState()
    var showModuleMenu by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Commentary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (availableCommentaries.isEmpty()) {
                        Text(text = "No commentaries installed", style = MaterialTheme.typography.labelSmall)
                    } else {
                        Box {
                            Surface(onClick = { showModuleMenu = true }, shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)) {
                                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(text = selectedModule?.description?.takeIf { it.isNotEmpty() } ?: selectedModule?.name ?: "Select Commentary", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall)
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(12.dp))
                                }
                            }
                            DropdownMenu(expanded = showModuleMenu, onDismissRequest = { showModuleMenu = false }) {
                                availableCommentaries.forEach { module ->
                                    DropdownMenuItem(text = { Text(module.description.ifEmpty { module.name }) }, onClick = { engineViewModel.selectCommentaryModule(module); showModuleMenu = false }, trailingIcon = if (selectedModule?.name == module.name) { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Reference: ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(text = currentReference, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    if (results.isNotEmpty()) {
                        Button(onClick = { 
                            val text = results.joinToString("\n\n") { section ->
                                val title = section.title.joinToString(" ") { it.text }
                                val content = section.verses.joinToString("\n") { verse -> "[${verse.number}] ${verse.words.joinToString(" ") { it.text }}" }
                                "$title\n$content"
                            }
                            clipboard.setText(AnnotatedString(text))
                        }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp), modifier = Modifier.height(24.dp), colors = ButtonDefaults.textButtonColors()) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (selectedModule == null) {
            CommentaryEmptyState()
        } else if (results.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(text = "No commentary found for \"$currentReference\" in ${selectedModule?.name}.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(results) { section -> SectionView(section = section) }
            }
        }
    }
}

@Composable
fun CommentaryEmptyState() {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Commentaries", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "Select or install a commentary module from the Store to read study notes, theological essays, and explanations for the active scripture chapter.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun HtmlText(
    html: String,
    key: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium
) {
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val secondaryColor = MaterialTheme.colorScheme.secondary.toArgb()
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    
    val spanned = remember(html, key, textColor) {
        var text = html.trim()
        val keyLower = key.lowercase()
        // Clean prefix/suffix search key
        if (text.lowercase().startsWith(keyLower)) text = text.drop(keyLower.length).trim()
        if (text.lowercase().endsWith(keyLower)) text = text.dropLast(keyLower.length).trim()
        
        // Map common TEI/Dictionary classes to Android-supported HTML
        text = text.replace("class=\"orth\"", "color='#${Integer.toHexString(primaryColor).substring(2)}'")
        text = text.replace("class=\"pos\"", "color='#${Integer.toHexString(secondaryColor).substring(2)}'")
        text = text.replace("class=\"cit\"", "style=\"background-color:#00000010; padding:4px\"") // Blockquote-like

        HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, style.fontSize.value)
                setTextColor(textColor)
                setLineSpacing(0f, 1.2f)
                if (style.fontFamily == FontFamily.Serif) {
                    setTypeface(android.graphics.Typeface.SERIF)
                }
            }
        },
        update = { it.text = spanned }
    )
}

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
            .fillMaxHeight()
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
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
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween, Alignment.CenterHorizontally)
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
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            groupedByLanguage.keys.sorted().forEach { langCode ->
                val modules = groupedByLanguage[langCode] ?: emptyList()
                item {
                    SettingsGroup(title = langCode.uppercase()) {
                        modules.forEach { module ->
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
                item { Spacer(modifier = Modifier.height(8.dp)) }
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
