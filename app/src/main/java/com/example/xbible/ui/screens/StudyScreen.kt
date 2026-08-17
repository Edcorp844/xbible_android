package com.example.xbible.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.*
import androidx.compose.material3.adaptive.navigation.rememberSupportingPaneScaffoldNavigator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import com.example.xbible.ui.components.PageView
import com.example.xbible.ui.icons.tab_group
import com.example.xbible.ui.screens.study.ModuleSelectionContent
import com.example.xbible.ui.screens.study.ReferencePickerContent
import com.example.xbible.ui.screens.study.SplitDetailPane
import com.example.xbible.ui.screens.study.TabsPreview
import com.example.xbible.viewmodel.EngineViewModel
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
    
    val moduleSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val referenceSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val lexiconSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Outer navigator for Bible/Reference Drawer
    val drawerNavigator = rememberSupportingPaneScaffoldNavigator(
        scaffoldDirective = calculatePaneScaffoldDirective(adaptiveInfo).copy(
            maxHorizontalPartitions = 1,
            horizontalPartitionSpacerSize = 0.dp,
            verticalPartitionSpacerSize = 0.dp
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        SupportingPaneScaffold(
            modifier = Modifier.fillMaxSize(),
            directive = drawerNavigator.scaffoldDirective,
            value = drawerNavigator.scaffoldValue,
            mainPane = {
                AnimatedPane(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        topBar = sharedHeader,
                        floatingActionButtonPosition = FabPosition.Center,
                        floatingActionButton = {
                            AnimatedVisibility(
                                visible = showFab,
                                enter = slideInVertically(initialOffsetY = { it }),
                                exit = slideOutVertically(targetOffsetY = { it })
                            ) {
                                HorizontalFloatingToolbar(
                                modifier = Modifier.navigationBarsPadding(),
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
                        .width(400.dp)
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
    }

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
