package com.example.xbible.feature.study

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.AdaptStrategy
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.LevitatedPaneScrim
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffold
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldDefaults
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.rememberSupportingPaneScaffoldNavigator
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.xbible.R
import com.example.xbible.core.ui.components.HighlightStyle
import com.example.xbible.core.ui.components.PageView
import com.example.xbible.core.ui.layout.LocalLayoutInfo
import com.example.xbible.ui.icons.XBibleIcons
import com.example.xbible.viewmodel.ReaderTabType
import com.example.xbible.viewmodel.ReaderViewModel
import com.example.xbible.viewmodel.StudyTool
import uniffi.xbible_engine.ReaderWord

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3AdaptiveApi::class,
)
@Composable
fun StudyScreen(
    readerViewModel: ReaderViewModel,
    onNavigateToStore: () -> Unit,
) {
    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val layoutInfo = LocalLayoutInfo.current
    val isCompact = layoutInfo.isCompactWidth
    val isExpanded = layoutInfo.isExpandedWidth

    val clipboardManager = LocalClipboardManager.current

    // ── State from ReaderViewModel ───────────────────────────────
    val chapterData by readerViewModel.currentChapterData.collectAsState()
    val commentaryData by readerViewModel.currentCommentaryData.collectAsState()
    val commentaryName by readerViewModel.currentCommentaryName.collectAsState()
    val commentaryReference by readerViewModel.currentCommentaryReference.collectAsState()

    val parallelChapterData by readerViewModel.parallelChapterData.collectAsState()
    val wordConfig by readerViewModel.wordConfig.collectAsState()
    val installedTranslations by readerViewModel.installedTranslations.collectAsState()
    val isLoading by readerViewModel.isLoadingContent.collectAsState()
    val currentTranslation by readerViewModel.currentTranslation.collectAsState()
    val parallelTranslation by readerViewModel.parallelTranslation.collectAsState()
    val tabs by readerViewModel.tabs.collectAsState()
    val activeTabIndex by readerViewModel.activeTabIndex.collectAsState()
    val currentBooks by readerViewModel.currentBooksFlow.collectAsState()
    val highlights by readerViewModel.highlights.collectAsState()

    // Chapter-wide entities (In This Chapter footer) — populated on chapter load.
    val chapterEntities by readerViewModel.chapterEntities.collectAsState()

    // ── Local UI state ──────────────────────────────────────────
    var selectedWord by remember { mutableStateOf<ReaderWord?>(null) }
    var showLexicon by remember { mutableStateOf(false) }
    var showTabsPreview by remember { mutableStateOf(false) }
    var showModuleSelection by remember { mutableStateOf(false) }
    var showReferencePicker by remember { mutableStateOf(false) }
    var showSearchSettings by remember { mutableStateOf(false) }
    var showFontSettings by remember { mutableStateOf(false) }
    var showParallelMode by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showCommentaryPicker by remember { mutableStateOf(false) }

    // Verse Actions State
    var selectedVerseNumber by remember { mutableStateOf<Int?>(null) }
    var selectedVerseText by remember { mutableStateOf("") }
    var showVerseActions by remember { mutableStateOf(false) }
    val selectedHighlightStyle = remember { HighlightStyle.Solid }

    val moduleSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val referenceSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val lexiconSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val searchSettingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val fontSettingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val parallelModeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val verseActionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val commentaryPickerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // ── Supporting-pane navigator for the drawer ────────────────
    val drawerNavigator = rememberSupportingPaneScaffoldNavigator(
        scaffoldDirective = calculatePaneScaffoldDirective(adaptiveInfo).copy(
            maxHorizontalPartitions = 1,
            horizontalPartitionSpacerSize = 0.dp,
            verticalPartitionSpacerSize = 0.dp,
        ),
        adaptStrategies = SupportingPaneScaffoldDefaults.adaptStrategies(
            supportingPaneAdaptStrategy = AdaptStrategy.Levitate(
                alignment = Alignment.TopEnd,
                scrim = {
                    LevitatedPaneScrim(
                        onClick = {
                            showModuleSelection = false
                            showReferencePicker = false
                            showSearchSettings = false
                            showFontSettings = false
                            showParallelMode = false
                        }
                    )
                },
            ),
        ),
    )

    val isBibleDrawerVisible =
        (showModuleSelection || showReferencePicker || showSearchSettings ||
                showFontSettings || showParallelMode) && !isCompact

    LaunchedEffect(isBibleDrawerVisible) {
        if (isBibleDrawerVisible) {
            drawerNavigator.navigateTo(SupportingPaneScaffoldRole.Supporting)
        } else {
            drawerNavigator.navigateBack()
        }
    }

    LaunchedEffect(selectedWord, isExpanded) {
        if (selectedWord != null && isExpanded) showLexicon = true
    }

    val currentTab = tabs.getOrNull(activeTabIndex)
    val isCommentaryTab = currentTab?.isCommentary == true
    val currentBlocks = chapterData?.blocks.orEmpty()
    val currentCommentaryBlocks = commentaryData?.blocks.orEmpty()
    val hasNext = currentTab?.hasNextChapter == true
    val hasPrev = currentTab?.hasPrevChapter == true

    // ── Scroll state + FAB visibility ───────────────────────────
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
                if (isScrollingUp != isUp) isScrollingUp = isUp
                lastScrollIndex = index
                lastScrollOffset = offset
            }
    }

    val showFab by remember {
        derivedStateOf { isScrollingUp && (hasNext || hasPrev || tabs.size > 1) }
    }

    LaunchedEffect(chapterData, commentaryData) {
        if (chapterData != null || commentaryData != null) {
            listState.scrollToItem(0)
            isScrollingUp = true
        }
    }

    LaunchedEffect(Unit) { readerViewModel.refreshEngine() }

    // Resizable lexicon pane
    var lexiconPaneWidth by remember { mutableStateOf(400.dp) }
    val density = LocalDensity.current

    // ── Helpers ────────────────────────────────────────────────
    val closeVerseActions = {
        showVerseActions = false
        selectedVerseNumber = null
        selectedVerseText = ""
    }

    val openCommentaryStudyTool = {
        showLexicon = true
        readerViewModel.selectTool(StudyTool.Commentary)
        val rememberedId = readerViewModel.studyCommentaryId.value
        val rememberedName = readerViewModel.studyCommentaryName.value
        val fallback = readerViewModel.commentaries.value.firstOrNull()

        when {
            rememberedId != null -> {
                readerViewModel.loadStudyCommentary(rememberedId, rememberedName)
            }
            fallback != null -> {
                readerViewModel.loadStudyCommentary(fallback.id, fallback.name)
            }
            // else: nothing installed → panel will show the install empty state
        }
    }

    // ── Shared header ──────────────────────────────────────────
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
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            } else ButtonDefaults.elevatedButtonColors()
                        ) {
                            Text(
                                text = when {
                                    isCommentaryTab -> commentaryName.ifEmpty {
                                        stringResource(R.string.commentary)
                                    }
                                    else -> currentTranslation?.shortName
                                        ?: stringResource(R.string.select_bible)
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    trailingButton = {
                        val isSearchTab = currentTab?.type == ReaderTabType.SEARCH
                        val isSelected =
                            (if (isSearchTab) showSearchSettings else showReferencePicker) && !isCompact
                        SplitButtonDefaults.ElevatedTrailingButton(
                            checked = isSelected,
                            onCheckedChange = {
                                if (isCompact) {
                                    if (isSearchTab) showSearchSettings = it else showReferencePicker = it
                                    showLexicon = false
                                    showModuleSelection = false
                                } else {
                                    if (isSearchTab) {
                                        showSearchSettings = it
                                        showReferencePicker = false
                                    } else {
                                        showReferencePicker = it
                                        showSearchSettings = false
                                    }
                                    showModuleSelection = false
                                }
                            },
                        ) {
                            val reference = when {
                                isCommentaryTab -> commentaryData?.let {
                                    "${it.bookName} ${it.chapterNumber}"
                                }
                                else -> chapterData?.let {
                                    "${it.bookName} ${it.chapterNumber}"
                                }
                            }
                            Text(
                                text = if (isSearchTab) {
                                    stringResource(R.string.settings)
                                } else {
                                    reference ?: stringResource(R.string.study)
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                )
            },
            actions = {
                if (!isCompact) {
                    IconToggleButton(
                        checked = showLexicon,
                        onCheckedChange = { showLexicon = it },
                    ) {
                        Icon(
                            XBibleIcons.Sidebar,
                            contentDescription = stringResource(R.string.study_tools),
                        )
                    }
                }
                IconButton(onClick = { readerViewModel.addSearchTab() }) {
                    Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
                }
                Box {
                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more))
                    }
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false },
                        shape = RoundedCornerShape(24.dp),
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.font_and_settings)) },
                            onClick = {
                                if (isCompact) {
                                    showFontSettings = true
                                } else {
                                    showFontSettings = !showFontSettings
                                    showParallelMode = false
                                    showModuleSelection = false
                                    showReferencePicker = false
                                    showSearchSettings = false
                                }
                                showMoreMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    XBibleIcons.FontSize,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.parallel_display)) },
                            onClick = {
                                if (isCompact) {
                                    showParallelMode = true
                                } else {
                                    showParallelMode = !showParallelMode
                                    showFontSettings = false
                                    showModuleSelection = false
                                    showReferencePicker = false
                                    showSearchSettings = false
                                }
                                showMoreMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    XBibleIcons.ParallelMode,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.commentary)) },
                            onClick = {
                                showMoreMenu = false
                                showCommentaryPicker = true
                            },
                            leadingIcon = {
                                Icon(
                                    XBibleIcons.Sidebar,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                        )
                    }
                }
            },
        )
    }

    // ── Scaffold + drawer ──────────────────────────────────────
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
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
                                exit = slideOutVertically(targetOffsetY = { it }),
                            ) {
                                HorizontalFloatingToolbar(
                                    modifier = Modifier.navigationBarsPadding(),
                                    expanded = true,
                                    colors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
                                    content = {
                                        IconButton(
                                            onClick = { readerViewModel.previousChapter() },
                                            enabled = hasPrev,
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = stringResource(R.string.previous_chapter),
                                            )
                                        }
                                        Box(contentAlignment = Alignment.Center) {
                                            FilledTonalButton(
                                                onClick = { showTabsPreview = true },
                                                shape = IconButtonDefaults.filledShape,
                                            ) {
                                                Icon(
                                                    XBibleIcons.Tabs,
                                                    contentDescription = stringResource(R.string.tabs),
                                                )
                                            }
                                            if (tabs.size > 1) {
                                                Surface(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(top = 2.dp, end = 2.dp),
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    shape = RoundedCornerShape(8.dp),
                                                ) {
                                                    Text(
                                                        text = tabs.size.toString(),
                                                        modifier = Modifier.padding(horizontal = 4.dp),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSecondary,
                                                    )
                                                }
                                            }
                                        }
                                        IconButton(
                                            onClick = { readerViewModel.nextChapter() },
                                            enabled = hasNext,
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = stringResource(R.string.next_chapter),
                                            )
                                        }
                                    },
                                )
                            }
                        },
                    ) { paddingValues ->
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                        ) {
                            val innerMaxWidth = maxWidth
                            val minPaneWidth = 300.dp

                            Row(modifier = Modifier.fillMaxSize()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                ) {
                                    when {
                                        currentTab?.type == ReaderTabType.SEARCH -> {
                                            SearchView(
                                                tab = currentTab,
                                                onSearch = readerViewModel::performSearch,
                                                onReferenceClick = { hitKey ->
                                                    readerViewModel.openSearchHit(hitKey)
                                                },
                                                onCopyVerse = { ref, chapter ->
                                                    val text = chapter.blocks
                                                        .flatMap { b -> b.verses }
                                                        .joinToString(" ") { v -> v.text }
                                                    clipboardManager.setText(AnnotatedString("$ref: $text"))
                                                },
                                                onOpenInNewTab = { hitKey ->
                                                    readerViewModel.openSearchHitInNewTab(hitKey)
                                                },
                                                onPreviewChapter = { ref ->
                                                    readerViewModel.openSearchHit(ref)
                                                },
                                                onLexiconLookup = { ref ->
                                                    readerViewModel.openSearchHit(ref)
                                                    showLexicon = true
                                                },
                                            )
                                        }
                                        isCommentaryTab -> {
                                            when {
                                                isLoading && commentaryData == null -> {
                                                    Box(
                                                        Modifier.fillMaxSize(),
                                                        contentAlignment = Alignment.Center,
                                                    ) { CircularProgressIndicator() }
                                                }
                                                commentaryData == null -> {
                                                    Box(
                                                        Modifier.fillMaxSize(),
                                                        contentAlignment = Alignment.Center,
                                                    ) {
                                                        Text(stringResource(R.string.no_content_loaded))
                                                    }
                                                }
                                                else -> {
                                                    CommentaryReaderView(
                                                        chapter = commentaryData!!,
                                                        commentaryName = commentaryName,
                                                        config = wordConfig,
                                                        state = listState,
                                                        selectedVerseNumber = selectedVerseNumber,
                                                        onVerseLongPress = { verse ->
                                                            val verseObj = commentaryData?.blocks
                                                                ?.find { it.verseNumber == verse }
                                                            selectedVerseNumber = verse
                                                            selectedVerseText = verseObj?.text ?: ""
                                                            showVerseActions = true
                                                        },
                                                    )
                                                }
                                            }
                                        }
                                        else -> {
                                            when {
                                                installedTranslations.isEmpty() -> {
                                                    Column(
                                                        modifier = Modifier.fillMaxSize(),
                                                        verticalArrangement = Arrangement.Center,
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                    ) {
                                                        Text(stringResource(R.string.no_bibles_installed))
                                                        Spacer(Modifier.height(16.dp))
                                                        Button(onClick = onNavigateToStore) {
                                                            Text(stringResource(R.string.go_to_store))
                                                        }
                                                    }
                                                }
                                                isLoading -> {
                                                    Box(
                                                        Modifier.fillMaxSize(),
                                                        contentAlignment = Alignment.Center,
                                                    ) { CircularProgressIndicator() }
                                                }
                                                chapterData == null || currentBlocks.isEmpty() -> {
                                                    Box(
                                                        Modifier.fillMaxSize(),
                                                        contentAlignment = Alignment.Center,
                                                    ) {
                                                        Text(stringResource(R.string.no_content_loaded))
                                                    }
                                                }
                                                else -> {
                                                    PageView(
                                                        chapter = chapterData!!,
                                                        parallelChapter = parallelChapterData,
                                                        config = wordConfig,
                                                        state = listState,
                                                        onWordClick = { word -> },
                                                        onStrongsClick = { strongs -> },
                                                        onVerseTap = { /* nothing, or brief flash */ },
                                                        onVerseLongPress = { verse ->
                                                            val verseObj = chapterData?.blocks
                                                                ?.flatMap { it.verses }
                                                                ?.find { it.number == verse }
                                                            selectedVerseNumber = verse
                                                            selectedVerseText = verseObj?.text ?: ""
                                                            showVerseActions = true
                                                            readerViewModel.showEntitiesForVerse(verse)
                                                        },
                                                        chapterEntities = chapterEntities,
                                                        onEntityTap = { },
                                                        selectedVerseNumber = selectedVerseNumber,
                                                        highlights = highlights,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (showLexicon && !isCompact) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(12.dp)
                                            .pointerHoverIcon(PointerIcon.Hand)
                                            .draggable(
                                                state = rememberDraggableState { delta ->
                                                    val deltaDp = with(density) { delta.toDp() }
                                                    lexiconPaneWidth = (lexiconPaneWidth - deltaDp)
                                                        .coerceIn(minPaneWidth, innerMaxWidth * 0.7f)
                                                },
                                                orientation = Orientation.Horizontal,
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .height(48.dp)
                                                .width(4.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(
                                                    MaterialTheme.colorScheme.outlineVariant
                                                        .copy(alpha = 0.5f),
                                                ),
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .width(lexiconPaneWidth)
                                            .fillMaxHeight(),
                                    ) {
                                        SplitDetailPane(readerViewModel = readerViewModel)
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
                        modifier = Modifier.preferredHeight(1f)
                            .width(
                                when {
                                    layoutInfo.isExpandedWidth -> 480.dp
                                    layoutInfo.isMediumWidth -> 400.dp
                                    else -> 360.dp
                                }
                            ),
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .padding(top = 16.dp),
                            shape = RectangleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            tonalElevation = 3.dp,
                            shadowElevation = 16.dp,
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    IconButton(onClick = {
                                        showModuleSelection = false
                                        showReferencePicker = false
                                        showSearchSettings = false
                                        showFontSettings = false
                                        showParallelMode = false
                                    }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = stringResource(R.string.close_drawer_desc),
                                        )
                                    }
                                }

                                Spacer(Modifier.height(8.dp))

                                Box(modifier = Modifier.weight(1f)) {
                                    when {
                                        showModuleSelection -> {
                                            ModuleSelectionContent(
                                                translations = installedTranslations,
                                                current = currentTranslation,
                                            ) { translation ->
                                                readerViewModel.selectTranslation(translation)
                                                showModuleSelection = false
                                            }
                                        }
                                        showSearchSettings -> {
                                            currentTab?.let { tab ->
                                                SearchSettingsContent(tab.searchType) { type ->
                                                    readerViewModel.updateSearchType(type)
                                                    showSearchSettings = false
                                                }
                                            }
                                        }
                                        showReferencePicker -> {
                                            val bookId = if (isCommentaryTab) {
                                                commentaryData?.bookId
                                            } else {
                                                chapterData?.bookId
                                            }
                                            val chapterNumber = if (isCommentaryTab) {
                                                commentaryData?.chapterNumber
                                            } else {
                                                chapterData?.chapterNumber
                                            }
                                            ReferencePickerContent(
                                                books = currentBooks,
                                                currentBookId = bookId,
                                                currentChapter = chapterNumber ?: 1,
                                                onSelectBook = { bookId ->
                                                    readerViewModel.selectBook(bookId)
                                                },
                                                onSelectChapter = { ch ->
                                                    if (isCommentaryTab) {
                                                        commentaryData?.let {
                                                            readerViewModel.openCommentaryChapter(
                                                                commentaryId = currentTab?.commentaryId ?: return@ReferencePickerContent,
                                                                commentaryName = currentTab.commentaryName,
                                                                bookId = it.bookId,
                                                                chapter = ch,
                                                            )
                                                        }
                                                    } else {
                                                        chapterData?.let {
                                                            readerViewModel.openChapter(it.bookId, ch)
                                                        }
                                                    }
                                                    showReferencePicker = false
                                                },
                                            )
                                        }
                                        showFontSettings -> {
                                            FontSettingsContent(
                                                config = wordConfig,
                                                onConfigChange = { newConfig ->
                                                    readerViewModel.updateWordConfig { newConfig }
                                                },
                                                onResetConfig = { readerViewModel.resetWordConfig() },
                                            )
                                        }
                                        showParallelMode -> {
                                            ParallelModeContent(
                                                translations = installedTranslations,
                                                current = currentTranslation,
                                                parallel = parallelTranslation,
                                                onParallelSelected = { readerViewModel.selectParallelTranslation(it) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
        )
    }

    // ── Compact bottom sheets ──────────────────────────────────
    if (isCompact) {
        if (showModuleSelection) {
            ModalBottomSheet(
                onDismissRequest = { showModuleSelection = false },
                sheetState = moduleSheetState,
            ) {
                ModuleSelectionContent(installedTranslations, currentTranslation) { translation ->
                    readerViewModel.selectTranslation(translation)
                    showModuleSelection = false
                }
            }
        }
        if (showReferencePicker) {
            ModalBottomSheet(
                onDismissRequest = { showReferencePicker = false },
                sheetState = referenceSheetState,
            ) {
                val bookId = if (isCommentaryTab) commentaryData?.bookId else chapterData?.bookId
                val chapterNumber = if (isCommentaryTab) commentaryData?.chapterNumber else chapterData?.chapterNumber
                ReferencePickerContent(
                    books = currentBooks,
                    currentBookId = bookId,
                    currentChapter = chapterNumber ?: 1,
                    onSelectBook = { readerViewModel.selectBook(it) },
                    onSelectChapter = { ch ->
                        if (isCommentaryTab) {
                            commentaryData?.let {
                                readerViewModel.openCommentaryChapter(
                                    commentaryId = currentTab?.commentaryId ?: return@ReferencePickerContent,
                                    commentaryName = currentTab.commentaryName,
                                    bookId = it.bookId,
                                    chapter = ch,
                                )
                            }
                        } else {
                            chapterData?.let { readerViewModel.openChapter(it.bookId, ch) }
                        }
                        showReferencePicker = false
                    },
                )
            }
        }
        if (showSearchSettings) {
            ModalBottomSheet(
                onDismissRequest = { showSearchSettings = false },
                sheetState = searchSettingsSheetState,
            ) {
                currentTab?.let { tab ->
                    SearchSettingsContent(tab.searchType) { type ->
                        readerViewModel.updateSearchType(type)
                        showSearchSettings = false
                    }
                }
            }
        }
        if (showLexicon) {
            ModalBottomSheet(
                onDismissRequest = { showLexicon = false },
                sheetState = lexiconSheetState,
            ) {
                SplitDetailPane(readerViewModel)
            }
        }
        if (showFontSettings) {
            ModalBottomSheet(
                onDismissRequest = { showFontSettings = false },
                sheetState = fontSettingsSheetState,
            ) {
                FontSettingsContent(
                    config = wordConfig,
                    onConfigChange = { newConfig ->
                        readerViewModel.updateWordConfig { newConfig }
                    },
                    onResetConfig = { readerViewModel.resetWordConfig() },
                )
            }
        }
        if (showParallelMode) {
            ModalBottomSheet(
                onDismissRequest = { showParallelMode = false },
                sheetState = parallelModeSheetState,
            ) {
                ParallelModeContent(
                    translations = installedTranslations,
                    current = currentTranslation,
                    parallel = parallelTranslation,
                    onParallelSelected = { readerViewModel.selectParallelTranslation(it) },
                )
            }
        }

        // ── VERSE ACTIONS SHEET ─────────────────────────────────
        if (showVerseActions) {
            val targetBookName = chapterData?.bookName ?: commentaryData?.bookName ?: currentTab?.bookName ?: ""
            val targetChapterNum = chapterData?.chapterNumber ?: commentaryData?.chapterNumber ?: currentTab?.chapter ?: 1
            val targetBookId = chapterData?.bookId ?: commentaryData?.bookId ?: currentTab?.bookId ?: ""
            val activeHighlight = selectedVerseNumber?.let { highlights["$targetBookId:$targetChapterNum:$it"] }

            ModalBottomSheet(
                onDismissRequest = { closeVerseActions() },
                sheetState = verseActionsSheetState,
            ) {
                VerseActionsSheet(
                    reference = "$targetBookName $targetChapterNum:$selectedVerseNumber",
                    currentHighlight = activeHighlight?.color,
                    currentStyle = activeHighlight?.style ?: selectedHighlightStyle,
                    onDismiss = { closeVerseActions() },
                    onCopy = {
                        clipboardManager.setText(
                            AnnotatedString(
                                "$targetBookName $targetChapterNum:$selectedVerseNumber $selectedVerseText"
                            )
                        )
                        closeVerseActions()
                    },
                    onOpenEntities = {
                        closeVerseActions()
                        showLexicon = true
                        readerViewModel.selectTool(StudyTool.Dictionary)
                    },
                    onHighlight = { color, style ->
                        if (targetBookId.isNotBlank()) {
                            selectedVerseNumber?.let { vNum ->
                                readerViewModel.setHighlight(targetBookId, targetChapterNum, vNum, color, style)
                            }
                        }
                        closeVerseActions()
                    },
                    onClearHighlight = {
                        if (targetBookId.isNotBlank()) {
                            selectedVerseNumber?.let { vNum ->
                                readerViewModel.removeHighlight(targetBookId, targetChapterNum, vNum)
                            }
                        }
                        closeVerseActions()
                    },

                    // ── Annotate (stubs for now) ───────────────────
                    onAnnotateNote = { closeVerseActions() },
                    onAnnotateTag = { closeVerseActions() },
                    onAnnotateLink = { closeVerseActions() },
                    onAnnotateRelation = { closeVerseActions() },
                    onAnnotateBookmark = { closeVerseActions() },

                    // ── Study ──────────────────────────────────────
                    onStudyCommentary = {
                        closeVerseActions()
                        openCommentaryStudyTool()
                    },
                    onStudyLexicon = {
                        closeVerseActions()
                        showLexicon = true
                        readerViewModel.selectTool(StudyTool.Lexicon)
                    },
                    onStudyDictionary = {
                        closeVerseActions()
                        showLexicon = true
                        readerViewModel.selectTool(StudyTool.Dictionary)
                    },
                    onStudyReferences = {
                        closeVerseActions()
                        openCommentaryStudyTool()
                    },
                    onStudyEntities = {
                        closeVerseActions()
                        showLexicon = true
                        readerViewModel.selectTool(StudyTool.Dictionary)
                    },

                    // ── Share ──────────────────────────────────────
                    onShare = {
                        clipboardManager.setText(
                            AnnotatedString(
                                "$targetBookName $targetChapterNum:$selectedVerseNumber — $selectedVerseText"
                            )
                        )
                        closeVerseActions()
                    },
                    onExport = { closeVerseActions() },
                    onSelectAll = { closeVerseActions() },
                )
            }
        }
    }

    // ── Commentary picker sheet (both compact + expanded) ──────
    if (showCommentaryPicker) {
        ModalBottomSheet(
            onDismissRequest = { showCommentaryPicker = false },
            sheetState = commentaryPickerSheetState,
        ) {
            CommentaryPickerContent(
                readerViewModel = readerViewModel,
                currentReference = commentaryReference,
                onDismiss = { showCommentaryPicker = false },
            )
        }
    }

    AnimatedVisibility(
        visible = showTabsPreview,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300)),
    ) {
        TabsPreview(
            tabs = tabs,
            activeTabIndex = activeTabIndex,
            onTabSelected = {
                readerViewModel.switchTab(it)
                showTabsPreview = false
            },
            onAddTab = {
                readerViewModel.addNewTab()
                showTabsPreview = false
            },
            onAddSearchTab = {
                readerViewModel.addSearchTab()
                showTabsPreview = false
            },
            onRemoveTab = readerViewModel::removeTab,
            onDismiss = { showTabsPreview = false },
        )
    }
}