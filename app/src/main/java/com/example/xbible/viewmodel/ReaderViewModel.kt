package com.example.xbible.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.xbible.core.ui.components.AddedWordStyle
import com.example.xbible.core.ui.components.HighlightColor
import com.example.xbible.core.ui.components.HighlightStyle
import com.example.xbible.core.ui.components.VerseHighlight
import com.example.xbible.core.ui.components.VerseLayout
import com.example.xbible.core.ui.components.WordConfig
import com.example.xbible.data.BibleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uniffi.xbible_engine.CommentaryBlock
import uniffi.xbible_engine.CommentaryChapter
import uniffi.xbible_engine.EntityRef
import uniffi.xbible_engine.FreeBibleBook
import uniffi.xbible_engine.InstalledTranslation
import uniffi.xbible_engine.ReaderBlock
import uniffi.xbible_engine.ReaderChapter
import uniffi.xbible_engine.ReaderWord
import uniffi.xbible_engine.StoreItem
import java.util.UUID
import javax.inject.Inject

// ── Tabs ────────────────────────────────────────────────────────────

enum class ReaderTabType { READ, SEARCH }

/**
 * Local mirror of the Rust `SearchType` enum.
 *
 * The FFI no longer exports `SearchType` (it went away with the SWORD
 * search surface). This enum keeps the search-settings UI + `ReaderTab`
 * compiling. When the Rust search API is re-exported, delete this file
 * and switch the imports in `ReaderViewModel.kt` / `StudySheets.kt`
 * back to `uniffi.xbible_engine.SearchType`.
 */
enum class SearchType {
    REGULAR_EXPRESSION,
    PHRASE,
    MULTI_WORD,
}

enum class StudyTool {
    Dictionary, Lexicon, Commentary
}

/**
 * One reader tab.
 *
 * A tab in [ReaderTabType.READ] mode is *either* a translation read
 * ([translation] set, [chapterData] populated) *or* a commentary read
 * ([commentaryId] set, [commentaryData] populated) — never both.
 * [isCommentary] is the single source of truth for that distinction.
 */
data class ReaderTab(
    val id: String = UUID.randomUUID().toString(),

    // ── Source selection (mutually exclusive in READ mode) ─────
    val translation: InstalledTranslation? = null,
    val commentaryId: String? = null,
    val commentaryName: String = "",

    // ── Book + chapter (shared by translations and commentaries) ─
    val books: List<FreeBibleBook> = emptyList(),
    val bookId: String? = null,
    val bookName: String = "",
    val chapter: Int = 1,

    // ── Content from FreeBible ─────────────────────────────────
    val chapterData: ReaderChapter? = null,
    val commentaryData: CommentaryChapter? = null,

    val type: ReaderTabType = ReaderTabType.READ,

    // ── Search state ───────────────────────────────────────────
    val searchQuery: String = "",
    val searchType: SearchType = SearchType.MULTI_WORD,
    val searchHits: List<String> = emptyList(),
    val searchHitsContent: Map<String, ReaderChapter> = emptyMap(),
    val isSearching: Boolean = false,
    val searchLoadProgress: Float = 0f,
) {
    val isCommentary: Boolean get() = commentaryId != null && translation == null

    val blocks: List<ReaderBlock> get() = chapterData?.blocks.orEmpty()
    val commentaryBlocks: List<CommentaryBlock> get() = commentaryData?.blocks.orEmpty()

    /**
     * Forward navigation hint. For translations this is the Rust
     * `ReaderChapter.nextChapter`; for commentaries the ViewModel
     * falls back to book-index arithmetic when `CommentaryChapter`
     * doesn't expose `nextChapter`.
     */
    val hasNextChapter: Boolean
        get() = if (isCommentary) commentaryData?.nextChapter != null
        else chapterData?.nextChapter != null

    val hasPrevChapter: Boolean
        get() = if (isCommentary) commentaryData?.prevChapter != null
        else chapterData?.prevChapter != null
}

private const val THEOGRAPHIC_DATASET = "theographic"

// ── ViewModel ───────────────────────────────────────────────────────

@HiltViewModel
class ReaderViewModel @Inject constructor(
    application: Application,
    private val repository: BibleRepository,
) : AndroidViewModel(application) {

    // ── Readiness ────────────────────────────────────────────────────

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ── Installed translations ──────────────────────────────────────

    private val _installedTranslations = MutableStateFlow<List<InstalledTranslation>>(emptyList())
    val installedTranslations: StateFlow<List<InstalledTranslation>> =
        _installedTranslations.asStateFlow()

    private val _currentTranslation = MutableStateFlow<InstalledTranslation?>(null)
    val currentTranslation: StateFlow<InstalledTranslation?> =
        _currentTranslation.asStateFlow()

    private val _parallelTranslation = MutableStateFlow<InstalledTranslation?>(null)
    val parallelTranslation: StateFlow<InstalledTranslation?> =
        _parallelTranslation.asStateFlow()

    // ── Installed commentaries (catalogue for the study panel) ─────

    private val _commentaries = MutableStateFlow<List<StoreItem>>(emptyList())
    val commentaries: StateFlow<List<StoreItem>> = _commentaries.asStateFlow()

    // ── Current chapter (translation reads) ────────────────────────

    private val _currentChapterData = MutableStateFlow<ReaderChapter?>(null)
    val currentChapterData: StateFlow<ReaderChapter?> =
        _currentChapterData.asStateFlow()

    /** All entities mentioned anywhere in the currently loaded chapter. */
    private val _chapterEntities = MutableStateFlow<List<EntityRef>>(emptyList())
    val chapterEntities: StateFlow<List<EntityRef>> = _chapterEntities.asStateFlow()

    private val _parallelChapterData = MutableStateFlow<ReaderChapter?>(null)
    val parallelChapterData: StateFlow<ReaderChapter?> =
        _parallelChapterData.asStateFlow()

    // ── Current commentary (commentary reads in the active tab) ────

    private val _currentCommentaryId = MutableStateFlow<String?>(null)
    val currentCommentaryId: StateFlow<String?> = _currentCommentaryId.asStateFlow()

    private val _currentCommentaryName = MutableStateFlow<String>("")
    val currentCommentaryName: StateFlow<String> = _currentCommentaryName.asStateFlow()

    private val _currentCommentaryData = MutableStateFlow<CommentaryChapter?>(null)
    val currentCommentaryData: StateFlow<CommentaryChapter?> =
        _currentCommentaryData.asStateFlow()

    // ── Study panel commentary (independent of the active tab) ─────
    //
    // The study-tools Commentary tab reads a commentary at the reader's
    // current chapter without switching tabs. `studyCommentaryId` is
    // persisted; `loadStudyCommentary` sets it; `openStudyCommentaryInNewTab`
    // promotes it to a dedicated reader tab.

    private val _studyCommentaryId = MutableStateFlow<String?>(null)
    val studyCommentaryId: StateFlow<String?> = _studyCommentaryId.asStateFlow()

    private val _studyCommentaryName = MutableStateFlow<String>("")
    val studyCommentaryName: StateFlow<String> = _studyCommentaryName.asStateFlow()

    private val _studyCommentaryData = MutableStateFlow<CommentaryChapter?>(null)
    val studyCommentaryData: StateFlow<CommentaryChapter?> =
        _studyCommentaryData.asStateFlow()

    private val _studyCommentaryLoading = MutableStateFlow(false)
    val studyCommentaryLoading: StateFlow<Boolean> =
        _studyCommentaryLoading.asStateFlow()

    // ── Books / loading ────────────────────────────────────────────

    private val _currentBooks = MutableStateFlow<List<FreeBibleBook>>(emptyList())
    val currentBooksFlow: StateFlow<List<FreeBibleBook>> = _currentBooks.asStateFlow()

    private val _isLoadingContent = MutableStateFlow(false)
    val isLoadingContent: StateFlow<Boolean> = _isLoadingContent.asStateFlow()

    // ── Word display config ─────────────────────────────────────────

    private val _wordConfig = MutableStateFlow(WordConfig())
    val wordConfig: StateFlow<WordConfig> = _wordConfig.asStateFlow()

    // ── Tabs ────────────────────────────────────────────────────────

    private val _tabs = MutableStateFlow<List<ReaderTab>>(listOf(ReaderTab()))
    val tabs: StateFlow<List<ReaderTab>> = _tabs.asStateFlow()

    private val _activeTabIndex = MutableStateFlow(0)
    val activeTabIndex: StateFlow<Int> = _activeTabIndex.asStateFlow()

    // ── Verse entities (for the "who's here" sheet) ─────────────────

    private val _verseEntities = MutableStateFlow<List<EntityRef>>(emptyList())
    val verseEntities: StateFlow<List<EntityRef>> = _verseEntities.asStateFlow()

    // ── Study tools ─────────────────────────────────────────────────

    private val _selectedTool = MutableStateFlow(StudyTool.Dictionary)
    val selectedTool: StateFlow<StudyTool> = _selectedTool.asStateFlow()

    private val _selectedWordForLookup = MutableStateFlow("")
    val selectedWordForLookup: StateFlow<String> = _selectedWordForLookup.asStateFlow()

    private val _selectedStrongsForLookup = MutableStateFlow("")
    val selectedStrongsForLookup: StateFlow<String> = _selectedStrongsForLookup.asStateFlow()

    private val _currentCommentaryReference = MutableStateFlow("")
    val currentCommentaryReference: StateFlow<String> =
        _currentCommentaryReference.asStateFlow()

    // ── Internal state ──────────────────────────────────────────────

    private var searchContentLoadingJob: Job? = null
    private var entitiesJob: Job? = null
    private var chapterEntitiesJob: Job? = null
    private var studyCommentaryJob: Job? = null

    private val prefs = application.getSharedPreferences("word_config", Context.MODE_PRIVATE)
    private val tabPrefs = application.getSharedPreferences("reader_tabs_state", Context.MODE_PRIVATE)
    private val highlightsPrefs = application.getSharedPreferences("bible_highlights", Context.MODE_PRIVATE)

    // ── Highlights ──────────────────────────────────────────────────
    private val _highlights = MutableStateFlow<Map<String, VerseHighlight>>(emptyMap())
    val highlights: StateFlow<Map<String, VerseHighlight>> = _highlights.asStateFlow()

    init {
        loadConfig()
        loadHighlights()
        restoreStudyCommentaryChoice()
        setupEngine()
    }

    private fun loadHighlights() {
        val map = mutableMapOf<String, VerseHighlight>()
        highlightsPrefs.all.forEach { (key, value) ->
            if (value is String) {
                val parts = value.split(":")
                if (parts.size == 2) {
                    try {
                        val color = HighlightColor.valueOf(parts[0])
                        val style = HighlightStyle.valueOf(parts[1])
                        map[key] = VerseHighlight(color, style)
                    } catch (_: Exception) {}
                }
            }
        }
        _highlights.value = map
    }

    // ── Bootstrap ───────────────────────────────────────────────────

    private fun loadConfig() {
        val fontSize = prefs.getFloat("fontSize", 18f)
        val fontFamilyName = prefs.getString("fontFamily", "Serif") ?: "Serif"
        val fontFamily = when (fontFamilyName) {
            "Serif" -> FontFamily.Serif
            "SansSerif" -> FontFamily.SansSerif
            "Monospace" -> FontFamily.Monospace
            "Cursive" -> FontFamily.Cursive
            else -> FontFamily.Default
        }
        val addedWordsStyleName = prefs.getString("addedWordsStyle", "Brackets") ?: "Brackets"
        val addedWordsStyle = try {
            AddedWordStyle.valueOf(addedWordsStyleName)
        } catch (_: Exception) {
            AddedWordStyle.Brackets
        }
        val verseLayoutName = prefs.getString("verseLayout", VerseLayout.Paragraph.name) ?: VerseLayout.Paragraph.name
        val verseLayout = try {
            VerseLayout.valueOf(verseLayoutName)
        } catch (_: Exception) {
            VerseLayout.Paragraph
        }
        _wordConfig.value = WordConfig(
            fontSize = fontSize,
            fontFamily = fontFamily,
            lineHeight = prefs.getFloat("lineHeight", 1.5f),
            wordSpacing = prefs.getFloat("wordSpacing", 0f),
            justify = prefs.getBoolean("justify", true),
            showRedWords = prefs.getBoolean("showRedWords", true),
            showStrongs = prefs.getBoolean("showStrongs", true),
            showLemma = prefs.getBoolean("showLemma", true),
            showMorph = prefs.getBoolean("showMorph", true),
            showVerseNotes = prefs.getBoolean("showVerseNotes", true),
            showWordNotes = prefs.getBoolean("showWordNotes", true),
            addedWordsStyle = addedWordsStyle,
            verseLayout = verseLayout,
        )
    }

    private fun saveConfig(config: WordConfig) {
        val fontFamilyName = when (config.fontFamily) {
            FontFamily.Serif -> "Serif"
            FontFamily.SansSerif -> "SansSerif"
            FontFamily.Monospace -> "Monospace"
            FontFamily.Cursive -> "Cursive"
            else -> "Default"
        }
        prefs.edit().apply {
            putFloat("fontSize", config.fontSize)
            putString("fontFamily", fontFamilyName)
            putFloat("lineHeight", config.lineHeight)
            putFloat("wordSpacing", config.wordSpacing)
            putBoolean("justify", config.justify)
            putBoolean("showRedWords", config.showRedWords)
            putBoolean("showStrongs", config.showStrongs)
            putBoolean("showLemma", config.showLemma)
            putBoolean("showMorph", config.showMorph)
            putBoolean("showVerseNotes", config.showVerseNotes)
            putBoolean("showWordNotes", config.showWordNotes)
            putString("addedWordsStyle", config.addedWordsStyle.name)
            putString("verseLayout", config.verseLayout.name)
            apply()
        }
    }

    private fun restoreStudyCommentaryChoice() {
        val savedId = prefs.getString("study_commentary_id", null) ?: return
        val savedName = prefs.getString("study_commentary_name", "") ?: ""
        _studyCommentaryId.value = savedId
        _studyCommentaryName.value = savedName
    }

    private fun setupEngine() {
        viewModelScope.launch {
            repository.initialize()
                .onSuccess {
                    _isReady.value = true
                    loadInstalledTranslations()
                    loadCommentaries()
                }
                .onFailure { e ->
                    _errorMessage.value = "Failed to initialize Bible engine: ${e.message}"
                }
        }
    }

    // ── Installed translations / commentaries ───────────────────────

    private fun loadInstalledTranslations() {
        viewModelScope.launch {
            val translations = withContext(Dispatchers.IO) {
                repository.getFreeBibleInstalledTranslations()
            }
            _installedTranslations.value = translations

            val restored = tryRestoreTabs(translations)
            if (restored != null) {
                _tabs.value = restored
                val idx = tabPrefs.getInt("active_tab_index", 0)
                    .coerceIn(0, restored.size - 1)
                _activeTabIndex.value = idx
                updateActiveTab(restored[idx])
                return@launch
            }

            val seed = translations.firstOrNull() ?: return@launch
            val newTab = ReaderTab(translation = seed)
            _tabs.value = listOf(newTab)
            updateActiveTab(newTab)
        }
    }

    private fun loadCommentaries() {
        viewModelScope.launch {
            _commentaries.value = withContext(Dispatchers.IO) {
                repository.getInstalledCommentaries()
            }
        }
    }

    private fun tryRestoreTabs(
        translations: List<InstalledTranslation>,
    ): List<ReaderTab>? {
        val savedCount = tabPrefs.getInt("tab_count", 0)
        if (savedCount == 0) return null

        val restored = mutableListOf<ReaderTab>()
        for (i in 0 until savedCount) {
            val typeStr = tabPrefs.getString("tab_${i}_type", ReaderTabType.READ.name)
                ?: ReaderTabType.READ.name
            val type = runCatching { ReaderTabType.valueOf(typeStr) }
                .getOrElse { ReaderTabType.READ }

            val translationId = tabPrefs.getString("tab_${i}_translation", null)
            val translation = translations.find { it.id == translationId }
                ?: translations.firstOrNull()

            val commentaryId = tabPrefs.getString("tab_${i}_commentary_id", null)
            val commentaryName = tabPrefs.getString("tab_${i}_commentary_name", "") ?: ""

            val bookId = tabPrefs.getString("tab_${i}_book_id", null)
            val chapter = tabPrefs.getInt("tab_${i}_chapter", 1)

            val searchQuery = tabPrefs.getString("tab_${i}_search_query", "") ?: ""
            val searchTypeStr = tabPrefs.getString(
                "tab_${i}_search_type",
                SearchType.MULTI_WORD.name,
            ) ?: SearchType.MULTI_WORD.name
            val searchType = runCatching { SearchType.valueOf(searchTypeStr) }
                .getOrElse { SearchType.MULTI_WORD }

            restored += ReaderTab(
                type = type,
                translation = if (commentaryId == null) translation else null,
                commentaryId = commentaryId,
                commentaryName = commentaryName,
                bookId = bookId,
                chapter = chapter,
                searchQuery = searchQuery,
                searchType = searchType,
            )
        }
        return restored.ifEmpty { null }
    }

    private fun saveTabsState() {
        val tabs = _tabs.value
        tabPrefs.edit().apply {
            putInt("active_tab_index", _activeTabIndex.value)
            putInt("tab_count", tabs.size)
            tabs.forEachIndexed { i, t ->
                putString("tab_${i}_type", t.type.name)
                putString("tab_${i}_translation", t.translation?.id)
                putString("tab_${i}_commentary_id", t.commentaryId)
                putString("tab_${i}_commentary_name", t.commentaryName)
                putString("tab_${i}_book_id", t.bookId)
                putInt("tab_${i}_chapter", t.chapter)
                putString("tab_${i}_search_query", t.searchQuery)
                putString("tab_${i}_search_type", t.searchType.name)
            }
            apply()
        }
    }

    // ── Chapter loading (translation) ──────────────────────────────

    fun openChapter(bookId: String, chapter: Int) {
        val tab = _tabs.value.getOrNull(_activeTabIndex.value) ?: return
        val translation = tab.translation ?: return

        chapterEntitiesJob?.cancel()
        _chapterEntities.value = emptyList()

        viewModelScope.launch {
            _isLoadingContent.value = true
            try {
                val data = withContext(Dispatchers.IO) {
                    repository.readFreeBibleChapter(translation.id, bookId, chapter)
                }
                if (data == null) {
                    _errorMessage.value = "Chapter not found"
                } else {
                    _currentChapterData.value = data
                    _currentTranslation.value = translation

                    // Translation mode: clear commentary-side state so the
                    // UI doesn't render a stale commentary underneath.
                    _currentCommentaryId.value = null
                    _currentCommentaryName.value = ""
                    _currentCommentaryData.value = null

                    loadChapterEntities(data)

                    _parallelTranslation.value?.let { parallel ->
                        if (parallel.id != translation.id) {
                            _parallelChapterData.value = withContext(Dispatchers.IO) {
                                repository.readFreeBibleChapter(
                                    parallel.id, bookId, chapter,
                                )
                            }
                        }
                    }

                    updateCurrentTabInList {
                        it.copy(
                            translation = translation,
                            commentaryId = null,
                            commentaryName = "",
                            bookId = bookId,
                            bookName = data.bookName,
                            chapter = chapter,
                            chapterData = data,
                            commentaryData = null,
                        )
                    }

                    // Keep the study panel synced with the new chapter.
                    refreshStudyCommentary()
                }
            } catch (e: Throwable) {
                _errorMessage.value = "Error loading chapter: ${e.message}"
            } finally {
                _isLoadingContent.value = false
            }
        }
    }

    /**
     * Fetch every entity mentioned in [chapter] and publish it on
     * [chapterEntities] for the "In This Chapter" footer.
     */
    private fun loadChapterEntities(chapter: ReaderChapter) {
        val verseNumbers = chapter.blocks
            .flatMap { it.verses }
            .map { it.number }
            .distinct()

        if (verseNumbers.isEmpty()) {
            _chapterEntities.value = emptyList()
            return
        }

        chapterEntitiesJob = viewModelScope.launch {
            try {
                _chapterEntities.value = withContext(Dispatchers.IO) {
                    repository.getFreeBibleEntitiesForChapter(
                        datasetId = THEOGRAPHIC_DATASET,
                        book = chapter.bookId,
                        chapter = chapter.chapterNumber,
                        verseNumbers = verseNumbers,
                    )
                }
            } catch (e: Throwable) {
                _chapterEntities.value = emptyList()
                _errorMessage.value = "Entities for chapter failed: ${e.message}"
            }
        }
    }

    fun selectTranslation(translation: InstalledTranslation) {
        viewModelScope.launch {
            val books = withContext(Dispatchers.IO) {
                repository.getFreeBibleBooks(translation.id)
            }
            _currentTranslation.value = translation
            _currentBooks.value = books

            updateCurrentTabInList {
                it.copy(
                    translation = translation,
                    commentaryId = null,
                    commentaryName = "",
                    books = books,
                    bookId = null,
                    chapterData = null,
                    commentaryData = null,
                )
            }

            val firstBook = books.firstOrNull() ?: return@launch
            openChapter(firstBook.id, 1)
        }
    }

    fun selectBook(bookId: String) {
        val tab = _tabs.value.getOrNull(_activeTabIndex.value) ?: return
        if (tab.isCommentary) {
            openCommentaryChapter(
                commentaryId = tab.commentaryId ?: return,
                commentaryName = tab.commentaryName,
                bookId = bookId,
                chapter = 1,
            )
            return
        }
        val book = _currentBooks.value.firstOrNull { it.id == bookId } ?: return
        openChapter(book.id, 1)
    }

    fun selectParallelTranslation(translation: InstalledTranslation?) {
        _parallelTranslation.value = translation
        if (translation == null) {
            _parallelChapterData.value = null
            return
        }
        val current = _currentChapterData.value ?: return
        viewModelScope.launch {
            _parallelChapterData.value = withContext(Dispatchers.IO) {
                repository.readFreeBibleChapter(
                    translation.id, current.bookId, current.chapterNumber,
                )
            }
        }
    }

    // ── Commentary reading (active tab) ────────────────────────────

    /**
     * Open a chapter of an installed commentary in the active tab.
     *
     * Commentaries reuse the same book-id space as translations
     * (`GEN`, `EXO`, …) but read via `readCommentaryChapter`, which
     * returns a `CommentaryChapter` (homiletical blocks), not a
     * `ReaderChapter`.
     */
    fun openCommentaryChapter(
        commentaryId: String,
        commentaryName: String,
        bookId: String,
        chapter: Int,
    ) {
        chapterEntitiesJob?.cancel()
        _chapterEntities.value = emptyList()

        viewModelScope.launch {
            _isLoadingContent.value = true
            try {
                val data = withContext(Dispatchers.IO) {
                    repository.readCommentaryChapter(commentaryId, bookId, chapter)
                }
                if (data == null) {
                    _errorMessage.value = "No commentary for $bookId $chapter"
                } else {
                    _currentCommentaryId.value = commentaryId
                    _currentCommentaryName.value = commentaryName
                    _currentCommentaryData.value = data

                    // Commentary mode: clear translation-side state.
                    _currentChapterData.value = null
                    _currentTranslation.value = null
                    _parallelChapterData.value = null

                    if (_currentBooks.value.isEmpty()) {
                        loadBooksForCommentaryPicker()
                    }

                    updateCurrentTabInList {
                        it.copy(
                            translation = null,
                            commentaryId = commentaryId,
                            commentaryName = commentaryName,
                            bookId = bookId,
                            bookName = data.bookName,
                            chapter = chapter,
                            chapterData = null,
                            commentaryData = data,
                        )
                    }
                }
            } catch (e: Throwable) {
                _errorMessage.value = "Error loading commentary: ${e.message}"
            } finally {
                _isLoadingContent.value = false
            }
        }
    }

    /**
     * Open a commentary in the active tab, seeding it with a book list
     * and jumping to the first book (or [startBookId] / [startChapter]
     * if given).
     */
    fun openCommentary(
        commentaryId: String,
        commentaryName: String,
        startBookId: String? = null,
        startChapter: Int = 1,
    ) {
        viewModelScope.launch {
            val books = withContext(Dispatchers.IO) {
                val sourceId = _currentTranslation.value?.id
                    ?: _installedTranslations.value.firstOrNull()?.id
                if (sourceId != null) {
                    repository.getFreeBibleBooks(sourceId)
                } else {
                    emptyList()
                }
            }
            _currentBooks.value = books

            val bookId = startBookId ?: books.firstOrNull()?.id
            if (bookId != null) {
                openCommentaryChapter(
                    commentaryId = commentaryId,
                    commentaryName = commentaryName,
                    bookId = bookId,
                    chapter = startChapter,
                )
            } else {
                _errorMessage.value = "No books available to index the commentary"
            }
        }
    }

    /**
     * Called by the study-tools Commentary tab. Opens the current
     * translation chapter's reference in the given commentary, in a
     * *new* tab so the translation the user was reading stays put.
     */
    fun openCommentaryForCurrentChapter(
        commentaryId: String,
        commentaryName: String,
    ) {
        val data = _currentChapterData.value ?: run {
            _errorMessage.value = "No chapter loaded to cross-reference"
            return
        }
        val bookId = data.bookId
        val chapter = data.chapterNumber
        val books = _currentBooks.value

        val newTab = ReaderTab(
            translation = null,
            commentaryId = commentaryId,
            commentaryName = commentaryName,
            books = books,
            bookId = bookId,
            chapter = chapter,
        )
        _tabs.value = _tabs.value + newTab
        _activeTabIndex.value = _tabs.value.size - 1
        updateActiveTab(newTab)
        openCommentaryChapter(commentaryId, commentaryName, bookId, chapter)
    }

    /**
     * Replaces the active tab's translation with the given commentary at
     * the same reference. Use when the user expects an in-place swap.
     */
    fun replaceCurrentTabWithCommentary(
        commentaryId: String,
        commentaryName: String,
    ) {
        val data = _currentChapterData.value ?: run {
            _errorMessage.value = "No chapter loaded to cross-reference"
            return
        }
        openCommentaryChapter(
            commentaryId = commentaryId,
            commentaryName = commentaryName,
            bookId = data.bookId,
            chapter = data.chapterNumber,
        )
    }

    private suspend fun loadBooksForCommentaryPicker() {
        val sourceId = _installedTranslations.value.firstOrNull()?.id ?: return
        val books = withContext(Dispatchers.IO) {
            repository.getFreeBibleBooks(sourceId)
        }
        _currentBooks.value = books
        updateCurrentTabInList { it.copy(books = books) }
    }

    // ── Study-panel commentary (independent of active tab) ─────────

    /**
     * Called by the study panel to load a specific commentary at the
     * currently-loaded translation chapter. Does NOT change the reader tab.
     */
    fun loadStudyCommentary(commentaryId: String, commentaryName: String) {
        _studyCommentaryId.value = commentaryId
        _studyCommentaryName.value = commentaryName
        prefs.edit()
            .putString("study_commentary_id", commentaryId)
            .putString("study_commentary_name", commentaryName)
            .apply()
        refreshStudyCommentary()
    }

    /**
     * Clears the remembered study commentary. Panel goes back to the
     * "choose a commentary" empty state.
     */
    fun clearStudyCommentary() {
        _studyCommentaryId.value = null
        _studyCommentaryName.value = ""
        _studyCommentaryData.value = null
        prefs.edit()
            .remove("study_commentary_id")
            .remove("study_commentary_name")
            .apply()
    }

    /**
     * Promotes the study commentary to a new reader tab at the current
     * chapter. Leaves the translation tab the user was reading in place.
     */
    fun openStudyCommentaryInNewTab() {
        val id = _studyCommentaryId.value ?: return
        val name = _studyCommentaryName.value
        openCommentaryForCurrentChapter(id, name)
    }

    /**
     * Re-fetches the study commentary at the reader's current chapter.
     * Fired automatically by `openChapter` so the panel tracks the
     * reader without any user action.
     */
    private fun refreshStudyCommentary() {
        val id = _studyCommentaryId.value ?: return
        val name = _studyCommentaryName.value
        val data = _currentChapterData.value
        if (data == null) {
            _studyCommentaryData.value = null
            return
        }

        studyCommentaryJob?.cancel()
        studyCommentaryJob = viewModelScope.launch {
            _studyCommentaryLoading.value = true
            try {
                _studyCommentaryData.value = withContext(Dispatchers.IO) {
                    repository.readCommentaryChapter(
                        commentaryId = id,
                        bookId = data.bookId,
                        chapter = data.chapterNumber,
                    )
                }
            } catch (e: Throwable) {
                _studyCommentaryData.value = null
                _errorMessage.value = "Commentary load failed: ${e.message}"
            } finally {
                _studyCommentaryLoading.value = false
            }
        }
    }

    // ── Navigation ──────────────────────────────────────────────────

    fun nextChapter() {
        val tab = _tabs.value.getOrNull(_activeTabIndex.value) ?: return

        if (tab.isCommentary) {
            val commentaryId = tab.commentaryId ?: return
            val name = tab.commentaryName
            val data = tab.commentaryData ?: return

            data.nextChapter?.let { next ->
                openCommentaryChapter(commentaryId, name, data.bookId, next)
                return
            }
            val book = tab.books.firstOrNull { it.id == data.bookId }
            if (book != null && data.chapterNumber < book.numberOfChapters) {
                openCommentaryChapter(commentaryId, name, data.bookId, data.chapterNumber + 1)
                return
            }
            val idx = tab.books.indexOfFirst { it.id == data.bookId }
            val nextBook = tab.books.getOrNull(idx + 1) ?: return
            openCommentaryChapter(commentaryId, name, nextBook.id, 1)
            return
        }

        // ── Translation path ───────────────────────────────────────
        val data = tab.chapterData ?: return
        val next = data.nextChapter ?: run {
            val idx = tab.books.indexOfFirst { it.id == tab.bookId }
            val nextBook = tab.books.getOrNull(idx + 1) ?: return
            openChapter(nextBook.id, 1)
            return
        }
        openChapter(data.bookId, next)
    }

    fun previousChapter() {
        val tab = _tabs.value.getOrNull(_activeTabIndex.value) ?: return

        if (tab.isCommentary) {
            val commentaryId = tab.commentaryId ?: return
            val name = tab.commentaryName
            val data = tab.commentaryData ?: return

            data.prevChapter?.let { prev ->
                openCommentaryChapter(commentaryId, name, data.bookId, prev)
                return
            }
            if (data.chapterNumber > 1) {
                openCommentaryChapter(commentaryId, name, data.bookId, data.chapterNumber - 1)
                return
            }
            val idx = tab.books.indexOfFirst { it.id == data.bookId }
            if (idx <= 0) return
            val prevBook = tab.books[idx - 1]
            openCommentaryChapter(commentaryId, name, prevBook.id, prevBook.numberOfChapters)
            return
        }

        // ── Translation path ───────────────────────────────────────
        val data = tab.chapterData ?: return
        val prev = data.prevChapter ?: run {
            val idx = tab.books.indexOfFirst { it.id == tab.bookId }
            if (idx <= 0) return
            val prevBook = tab.books[idx - 1]
            openChapter(prevBook.id, prevBook.numberOfChapters)
            return
        }
        openChapter(data.bookId, prev)
    }

    // ── Tabs ────────────────────────────────────────────────────────

    fun switchTab(index: Int) {
        if (index !in _tabs.value.indices) return

        updateCurrentTabInList {
            it.copy(
                chapterData = _currentChapterData.value,
                commentaryData = _currentCommentaryData.value,
                commentaryId = _currentCommentaryId.value,
                commentaryName = _currentCommentaryName.value,
                translation = _currentTranslation.value,
                books = _currentBooks.value,
            )
        }

        _activeTabIndex.value = index
        updateActiveTab(_tabs.value[index])
        saveTabsState()
    }

    fun addNewTab() {
        val translation = _currentTranslation.value
            ?: _installedTranslations.value.firstOrNull()
            ?: return
        val newTab = ReaderTab(translation = translation)
        _tabs.value = _tabs.value + newTab
        _activeTabIndex.value = _tabs.value.size - 1
        updateActiveTab(newTab)
        saveTabsState()

        viewModelScope.launch {
            val books = withContext(Dispatchers.IO) {
                repository.getFreeBibleBooks(translation.id)
            }
            _currentBooks.value = books
            updateCurrentTabInList { it.copy(books = books) }
            books.firstOrNull()?.let { openChapter(it.id, 1) }
        }
    }

    fun addSearchTab() {
        val tab = ReaderTab(
            type = ReaderTabType.SEARCH,
            translation = _currentTranslation.value,
        )
        _tabs.value = _tabs.value + tab
        _activeTabIndex.value = _tabs.value.size - 1
        updateActiveTab(tab)
        saveTabsState()
    }

    fun removeTab(index: Int) {
        if (_tabs.value.size <= 1 || index !in _tabs.value.indices) return
        val newTabs = _tabs.value.toMutableList().apply { removeAt(index) }
        _tabs.value = newTabs
        val nextIdx = _activeTabIndex.value
            .let { if (it >= newTabs.size) newTabs.size - 1 else it }
        _activeTabIndex.value = nextIdx
        updateActiveTab(newTabs[nextIdx])
        saveTabsState()
    }

    private fun updateActiveTab(tab: ReaderTab) {
        _currentTranslation.value = tab.translation
        _currentCommentaryId.value = tab.commentaryId
        _currentCommentaryName.value = tab.commentaryName
        _currentBooks.value = tab.books
        _currentChapterData.value = tab.chapterData
        _currentCommentaryData.value = tab.commentaryData

        if (tab.type != ReaderTabType.READ) return
        if (tab.chapterData != null || tab.commentaryData != null) return

        val bookId = tab.bookId ?: tab.books.firstOrNull()?.id
        when {
            tab.isCommentary && bookId != null -> {
                openCommentaryChapter(
                    commentaryId = tab.commentaryId!!,
                    commentaryName = tab.commentaryName,
                    bookId = bookId,
                    chapter = tab.chapter,
                )
            }
            tab.translation != null -> {
                viewModelScope.launch {
                    val books = withContext(Dispatchers.IO) {
                        repository.getFreeBibleBooks(tab.translation.id)
                    }
                    _currentBooks.value = books
                    updateCurrentTabInList { it.copy(books = books) }
                    val bid = bookId ?: books.firstOrNull()?.id
                    if (bid != null) openChapter(bid, tab.chapter)
                }
            }
        }
    }

    private fun updateCurrentTabInList(update: (ReaderTab) -> ReaderTab) {
        val idx = _activeTabIndex.value
        val tabs = _tabs.value.toMutableList()
        if (idx in tabs.indices) {
            tabs[idx] = update(tabs[idx])
            _tabs.value = tabs
            saveTabsState()
        }
    }

    // ── Search ──────────────────────────────────────────────────────
    //
    // NOTE: The current FreeBible FFI does not expose a text-search API.
    // The SWORD `search(moduleName, query, searchType)` call was on
    // `XBibleEngine` and is no longer exported. To restore search, add a
    // `search(...)`-shaped method on `FreeBibleReader` (or a new
    // `search` entry point on `FreeBibleEngine`) in Rust, regenerate
    // bindings, and wire it up here.

    fun performSearch(query: String) {
        val tabIdx = _activeTabIndex.value
        val tab = _tabs.value.getOrNull(tabIdx) ?: return
        if (tab.type != ReaderTabType.SEARCH || tab.translation == null) return
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _tabs.update { tabs ->
                tabs.mapIndexed { i, t ->
                    if (i == tabIdx) t.copy(searchHits = emptyList(), searchQuery = "", searchHitsContent = emptyMap())
                    else t
                }
            }
            return
        }

        val translationId = tab.translation.id

        _tabs.update { tabs ->
            tabs.mapIndexed { i, t ->
                if (i == tabIdx) t.copy(
                    isSearching = true,
                    searchQuery = query,
                    searchHits = emptyList(),
                    searchHitsContent = emptyMap(),
                    searchLoadProgress = 0f,
                ) else t
            }
        }

        viewModelScope.launch {
            try {
                val results = withContext(Dispatchers.IO) {
                    repository.searchBible(translationId, trimmed) { progress ->
                        _tabs.update { tabs ->
                            tabs.mapIndexed { i, t ->
                                if (i == tabIdx && t.searchQuery == query) {
                                    t.copy(searchLoadProgress = progress)
                                } else t
                            }
                        }
                    }
                }
                val hits = results.map { it.first }
                val content = results.toMap()

                _tabs.update { tabs ->
                    tabs.mapIndexed { i, t ->
                        if (i == tabIdx && t.searchQuery == query) {
                            t.copy(
                                isSearching = false,
                                searchHits = hits,
                                searchHitsContent = content,
                                searchLoadProgress = 1f,
                            )
                        } else t
                    }
                }
            } catch (e: Throwable) {
                Log.e("ReaderSearch", "performSearch failed", e)
                _tabs.update { tabs ->
                    tabs.mapIndexed { i, t ->
                        if (i == tabIdx) t.copy(isSearching = false) else t
                    }
                }
                _errorMessage.value = e.message ?: "Search failed"
            }
        }
    }

    fun updateSearchType(searchType: SearchType) {
        val idx = _activeTabIndex.value
        val tab = _tabs.value.getOrNull(idx) ?: return
        if (tab.type != ReaderTabType.SEARCH) return

        _tabs.update { tabs ->
            tabs.mapIndexed { i, t ->
                if (i == idx) t.copy(searchType = searchType) else t
            }
        }
        saveTabsState()
        if (tab.searchQuery.isNotBlank()) performSearch(tab.searchQuery)
    }

    /** "GEN 1:1" or "GEN 1" → ("GEN", 1) */
    private fun parseHitKey(key: String): Pair<String, Int>? {
        val parts = key.trim().split(Regex("\\s+"))
        if (parts.size < 2) return null
        val bookId = parts[0]
        val chap = parts[1].substringBefore(':').toIntOrNull() ?: return null
        return bookId to chap
    }

    fun openSearchHit(key: String) {
        val idx = _activeTabIndex.value
        val tab = _tabs.value.getOrNull(idx) ?: return
        if (tab.type != ReaderTabType.SEARCH || tab.translation == null) return

        val (bookId, chapter) = parseHitKey(key) ?: return

        val updated = tab.copy(
            type = ReaderTabType.READ,
            bookId = bookId,
            chapter = chapter,
            chapterData = null,
            commentaryId = null,
            commentaryName = "",
            commentaryData = null,
        )
        _tabs.value = _tabs.value.toMutableList().also { it[idx] = updated }
        updateActiveTab(updated)
        openChapter(bookId, chapter)
    }

    fun openSearchHitInNewTab(key: String) {
        val tab = _tabs.value.getOrNull(_activeTabIndex.value) ?: return
        if (tab.type != ReaderTabType.SEARCH || tab.translation == null) return
        val (bookId, chapter) = parseHitKey(key) ?: return

        val newTab = ReaderTab(
            type = ReaderTabType.READ,
            translation = tab.translation,
            bookId = bookId,
            chapter = chapter,
        )
        _tabs.value = _tabs.value + newTab
        _activeTabIndex.value = _tabs.value.size - 1
        updateActiveTab(newTab)
        openChapter(bookId, chapter)
    }

    // ── Verse entities ──────────────────────────────────────────────

    fun showEntitiesForVerse(verse: Int) {
        val data = _currentChapterData.value ?: return
        entitiesJob?.cancel()
        entitiesJob = viewModelScope.launch {
            _verseEntities.value = emptyList()
            try {
                _verseEntities.value = withContext(Dispatchers.IO) {
                    repository.freeBibleEntitiesForVerse(
                        datasetId = THEOGRAPHIC_DATASET,
                        book = data.bookId,
                        chapter = data.chapterNumber,
                        verse = verse,
                    )
                }
            } catch (e: Throwable) {
                _errorMessage.value = "Entities lookup failed: ${e.message}"
            }
        }
    }

    fun clearVerseEntities() {
        entitiesJob?.cancel()
        _verseEntities.value = emptyList()
    }

    // ── Word config ─────────────────────────────────────────────────

    fun updateWordConfig(update: (WordConfig) -> WordConfig) {
        val new = update(_wordConfig.value)
        _wordConfig.value = new
        saveConfig(new)
    }

    fun resetWordConfig() {
        val def = WordConfig()
        _wordConfig.value = def
        saveConfig(def)
    }



    fun setHighlight(
        bookId: String,
        chapter: Int,
        verse: Int,
        color: HighlightColor,
        style: HighlightStyle,
    ) {
        val key = "$bookId:$chapter:$verse"
        _highlights.update { current ->
            current + (key to VerseHighlight(color, style))
        }
        highlightsPrefs.edit().putString(key, "${color.name}:${style.name}").apply()
    }

    fun removeHighlight(bookId: String, chapter: Int, verse: Int) {
        val key = "$bookId:$chapter:$verse"
        _highlights.update { it - key }
        highlightsPrefs.edit().remove(key).apply()
    }

    // ── Study tools ─────────────────────────────────────────────────

    fun selectTool(tool: StudyTool) {
        _selectedTool.value = tool
        if (tool == StudyTool.Commentary) {
            val data = _currentChapterData.value
            _currentCommentaryReference.value =
                if (data != null) "${data.bookName} ${data.chapterNumber}"
                else ""
        }
    }

    fun lookupWord(word: ReaderWord) {
        val clean = word.text.filter { it.isLetterOrDigit() }
        if (clean.isEmpty()) return

        _selectedWordForLookup.value = clean
        _selectedTool.value = StudyTool.Dictionary
        // TODO: call repository.lookupDictionary(...) once re-exported.
    }

    fun lookupStrongs(strongsCode: String) {
        if (strongsCode.isEmpty()) return
        _selectedStrongsForLookup.value = strongsCode
        _selectedTool.value = StudyTool.Lexicon
        // TODO: call repository.lookupStrongsNumber(...) once re-exported.
    }

    // ── Refresh ─────────────────────────────────────────────────────

    /**
     * Re-reads the installed-translations list and the commentary
     * catalogue. Called by the study screen on entry, and after any
     * install/uninstall triggered from the store screen.
     */
    fun refreshEngine() {
        if (!repository.isInitialized()) return
        viewModelScope.launch {
            _installedTranslations.value = withContext(Dispatchers.IO) {
                repository.getFreeBibleInstalledTranslations()
            }
            _commentaries.value = withContext(Dispatchers.IO) {
                repository.getInstalledCommentaries()
            }
        }
    }

    /**
     * Re-reads only the commentary catalogue. Lighter than
     * [refreshEngine]; use when only commentary install/uninstall
     * state may have changed.
     */
    fun refreshCommentaries() {
        if (!repository.isInitialized()) return
        viewModelScope.launch {
            _commentaries.value = withContext(Dispatchers.IO) {
                repository.getInstalledCommentaries()
            }
        }
    }

    // ── Books helpers (exposed for the book picker sheet) ──────────

    fun loadBooksForCurrentTranslation() {
        val tab = _tabs.value.getOrNull(_activeTabIndex.value)
        if (tab?.isCommentary == true) {
            viewModelScope.launch { loadBooksForCommentaryPicker() }
            return
        }
        val translation = _currentTranslation.value ?: return
        viewModelScope.launch {
            _currentBooks.value = withContext(Dispatchers.IO) {
                repository.getFreeBibleBooks(translation.id)
            }
        }
    }
}