package com.example.xbible.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.xbible.data.BibleRepository
import com.example.xbible.core.ui.components.WordConfig
import com.example.xbible.core.ui.components.AddedWordStyle
import com.example.xbible.core.ui.components.VerseLayout
import com.example.xbible.core.ui.components.HighlightColor
import com.example.xbible.core.ui.components.HighlightStyle
import com.example.xbible.core.ui.components.VerseHighlight
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uniffi.xbible_engine.DictionaryQuery
import uniffi.xbible_engine.DictionaryResult
import uniffi.xbible_engine.EntityRef
import uniffi.xbible_engine.FreeBibleBook
import uniffi.xbible_engine.InstalledTranslation
import uniffi.xbible_engine.LexiconQuery
import uniffi.xbible_engine.LexiconResult
import uniffi.xbible_engine.ReaderBlock
import uniffi.xbible_engine.ReaderChapter
import uniffi.xbible_engine.ReaderWord
import uniffi.xbible_engine.SearchHit
import uniffi.xbible_engine.SearchResults
import uniffi.xbible_engine.SearchType
import uniffi.xbible_engine.SwordModule
import java.util.UUID
import javax.inject.Inject

// ── Tabs ────────────────────────────────────────────────────────────

enum class ReaderTabType { READ, SEARCH }

data class ReaderTab(
    val id: String = UUID.randomUUID().toString(),

    // Which translation is this tab reading?
    val translation: InstalledTranslation? = null,

    // Book + chapter
    val books: List<FreeBibleBook> = emptyList(),
    val bookId: String? = null,
    val bookName: String = "",
    val chapter: Int = 1,

    // Content from FreeBible
    val chapterData: ReaderChapter? = null,

    val type: TabType = TabType.READ,

    // Search state
    val searchQuery: String = "",
    val searchResults: List<SearchHit> = emptyList(),
    val searchHitsContent: Map<String, ReaderChapter> = emptyMap(),
    val isSearching: Boolean = false,
    val searchLoadProgress: Float = 0f,
    val searchType: SearchType = SearchType.MULTI_WORD,
) {
    val blocks: List<ReaderBlock> get() = chapterData?.blocks.orEmpty()
    val hasNextChapter: Boolean get() = chapterData?.nextChapter != null
    val hasPrevChapter: Boolean get() = chapterData?.prevChapter != null
}

enum class ReaderStudyTool {
    Dictionary, Lexicon, Commentary
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

    // ── Current chapter (mirrors the active tab) ────────────────────

    private val _currentChapterData = MutableStateFlow<ReaderChapter?>(null)
    val currentChapterData: StateFlow<ReaderChapter?> =
        _currentChapterData.asStateFlow()

    /** All entities mentioned anywhere in the currently loaded chapter. */
    private val _chapterEntities = MutableStateFlow<List<EntityRef>>(emptyList())
    val chapterEntities: StateFlow<List<EntityRef>> = _chapterEntities.asStateFlow()

    private val _parallelChapterData = MutableStateFlow<ReaderChapter?>(null)
    val parallelChapterData: StateFlow<ReaderChapter?> =
        _parallelChapterData.asStateFlow()

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

    // ── Study tools (unchanged — they still use SWORD modules) ──────

    private val _selectedTool = MutableStateFlow(StudyTool.Dictionary)
    val selectedTool: StateFlow<StudyTool> = _selectedTool.asStateFlow()

    // Dictionary
    private val _selectedWordForLookup = MutableStateFlow("")
    val selectedWordForLookup: StateFlow<String> = _selectedWordForLookup.asStateFlow()

    private val _dictionaryResults = MutableStateFlow<List<DictionaryResult>>(emptyList())
    val dictionaryResults: StateFlow<List<DictionaryResult>> = _dictionaryResults.asStateFlow()

    private val _isDictionaryLoading = MutableStateFlow(false)
    val isDictionaryLoading: StateFlow<Boolean> = _isDictionaryLoading.asStateFlow()

    // Lexicon
    private val _selectedStrongsForLookup = MutableStateFlow("")
    val selectedStrongsForLookup: StateFlow<String> = _selectedStrongsForLookup.asStateFlow()

    private val _selectedLexiconModule = MutableStateFlow<SwordModule?>(null)
    val selectedLexiconModule: StateFlow<SwordModule?> = _selectedLexiconModule.asStateFlow()

    private val _availableLexicons = MutableStateFlow<List<SwordModule>>(emptyList())
    val availableLexicons: StateFlow<List<SwordModule>> = _availableLexicons.asStateFlow()

    private val _lexiconResults = MutableStateFlow<List<LexiconResult>>(emptyList())
    val lexiconResults: StateFlow<List<LexiconResult>> = _lexiconResults.asStateFlow()

    private val _isLexiconLoading = MutableStateFlow(false)
    val isLexiconLoading: StateFlow<Boolean> = _isLexiconLoading.asStateFlow()

    // Commentary
    private val _selectedCommentaryModule = MutableStateFlow<SwordModule?>(null)
    val selectedCommentaryModule: StateFlow<SwordModule?> =
        _selectedCommentaryModule.asStateFlow()

    private val _availableCommentaries = MutableStateFlow<List<SwordModule>>(emptyList())
    val availableCommentaries: StateFlow<List<SwordModule>> =
        _availableCommentaries.asStateFlow()

    private val _commentaryResults = MutableStateFlow<List<uniffi.xbible_engine.Section>>(emptyList())
    val commentaryResults: StateFlow<List<uniffi.xbible_engine.Section>> =
        _commentaryResults.asStateFlow()

    private val _isCommentaryLoading = MutableStateFlow(false)
    val isCommentaryLoading: StateFlow<Boolean> = _isCommentaryLoading.asStateFlow()

    private val _currentCommentaryReference = MutableStateFlow("")
    val currentCommentaryReference: StateFlow<String> =
        _currentCommentaryReference.asStateFlow()

    // ── Internal state ──────────────────────────────────────────────

    private var searchContentLoadingJob: Job? = null
    private var entitiesJob: Job? = null
    private var chapterEntitiesJob: Job? = null

    private val prefs = application.getSharedPreferences("word_config", Context.MODE_PRIVATE)
    private val tabPrefs = application.getSharedPreferences("reader_tabs_state", Context.MODE_PRIVATE)

    init {
        loadConfig()
        setupEngine()
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

    private fun setupEngine() {
        viewModelScope.launch {
            repository.initialize()
                .onSuccess {
                    _isReady.value = true
                    loadInstalledTranslations()
                }
                .onFailure { e ->
                    _errorMessage.value = "Failed to initialize Bible engine: ${e.message}"
                }
        }
    }

    // ── Installed translations ──────────────────────────────────────

    private fun loadInstalledTranslations() {
        viewModelScope.launch {
            val translations = withContext(Dispatchers.IO) {
                repository.getFreeBibleInstalledTranslations()
            }
            _installedTranslations.value = translations

            // Restore tabs from disk, or seed a default one.
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

    private fun tryRestoreTabs(
        translations: List<InstalledTranslation>,
    ): List<ReaderTab>? {
        val savedCount = tabPrefs.getInt("tab_count", 0)
        if (savedCount == 0) return null

        val restored = mutableListOf<ReaderTab>()
        for (i in 0 until savedCount) {
            val typeStr = tabPrefs.getString("tab_${i}_type", TabType.READ.name)
                ?: TabType.READ.name
            val type = runCatching { TabType.valueOf(typeStr) }.getOrElse { TabType.READ }

            val translationId = tabPrefs.getString("tab_${i}_translation", null)
            val translation = translations.find { it.id == translationId }
                ?: translations.firstOrNull()

            val bookId = tabPrefs.getString("tab_${i}_book_id", null)
            val chapter = tabPrefs.getInt("tab_${i}_chapter", 1)

            val searchQuery = tabPrefs.getString("tab_${i}_search_query", "") ?: ""
            val searchTypeStr = tabPrefs.getString("tab_${i}_search_type", SearchType.MULTI_WORD.name)
                ?: SearchType.MULTI_WORD.name
            val searchType = runCatching { SearchType.valueOf(searchTypeStr) }
                .getOrElse { SearchType.MULTI_WORD }

            restored += ReaderTab(
                type = type,
                translation = translation,
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
                putString("tab_${i}_book_id", t.bookId)
                putInt("tab_${i}_chapter", t.chapter)
                putString("tab_${i}_search_query", t.searchQuery)
                putString("tab_${i}_search_type", t.searchType.name)
            }
            apply()
        }
    }

    // ── Chapter loading ─────────────────────────────────────────────

    fun openChapter(bookId: String, chapter: Int) {
        val tab = _tabs.value.getOrNull(_activeTabIndex.value) ?: return
        val translation = tab.translation ?: return

        // Any in-flight entity lookup belongs to a different chapter now.
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

                    // Kick off entity fetch for the freshly loaded chapter.
                    loadChapterEntities(data)

                    // If a parallel translation is active, load the same chapter there too.
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
                            bookId = bookId,
                            bookName = data.bookName,
                            chapter = chapter,
                            chapterData = data,
                        )
                    }
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
     *
     * The Rust FFI only exposes per-verse lookups, so we fan out over the
     * chapter's verse numbers and de-dupe by (kind, id). The whole loop runs
     * inside one `repository` call so it stays on the engine's single thread.
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
                    books = books,
                    bookId = null,
                    chapterData = null,
                )
            }

            val firstBook = books.firstOrNull() ?: return@launch
            openChapter(firstBook.id, 1)
        }
    }

    fun selectBook(bookId: String) {
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

    // ── Navigation ──────────────────────────────────────────────────

    fun nextChapter() {
        val tab = _tabs.value.getOrNull(_activeTabIndex.value) ?: return
        val data = tab.chapterData ?: return
        val next = data.nextChapter ?: run {
            // Roll over to next book
            val idx = tab.books.indexOfFirst { it.id == tab.bookId }
            val nextBook = tab.books.getOrNull(idx + 1) ?: return
            openChapter(nextBook.id, 1)
            return
        }
        openChapter(data.bookId, next)
    }

    fun previousChapter() {
        val tab = _tabs.value.getOrNull(_activeTabIndex.value) ?: return
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

        // Persist the current tab's live state before switching
        updateCurrentTabInList { it.copy(
            chapterData = _currentChapterData.value,
            translation = _currentTranslation.value,
            books = _currentBooks.value,
        ) }

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
            type = TabType.SEARCH,
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
        _currentBooks.value = tab.books
        _currentChapterData.value = tab.chapterData

        if (tab.type == TabType.READ
            && tab.chapterData == null
            && tab.translation != null
        ) {
            viewModelScope.launch {
                val books = withContext(Dispatchers.IO) {
                    repository.getFreeBibleBooks(tab.translation.id)
                }
                _currentBooks.value = books
                updateCurrentTabInList { it.copy(books = books) }

                val bookId = tab.bookId ?: books.firstOrNull()?.id
                if (bookId != null) openChapter(bookId, tab.chapter)
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

    fun performSearch(query: String) {
        val tabIdx = _activeTabIndex.value
        val tab = _tabs.value.getOrNull(tabIdx) ?: return
        if (tab.type != TabType.SEARCH) return
        val translation = tab.translation ?: return
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _tabs.update { tabs ->
                tabs.mapIndexed { i, t ->
                    if (i == tabIdx) t.copy(searchResults = emptyList(), searchQuery = "")
                    else t
                }
            }
            return
        }

        searchContentLoadingJob?.cancel()
        viewModelScope.launch {
            _tabs.update { tabs ->
                tabs.mapIndexed { i, t ->
                    if (i == tabIdx) t.copy(
                        isSearching = true,
                        searchQuery = query,
                        searchResults = emptyList(),
                        searchHitsContent = emptyMap(),
                        searchLoadProgress = 0f,
                    ) else t
                }
            }

            try {
                // FreeBible search returns SearchResults across the whole
                // installed translation — same shape as the SWORD path.
                val results: SearchResults = withContext(Dispatchers.IO) {
                    repository.search(
                        moduleName = translation.id,
                        query = trimmed,
                        searchType = tab.searchType,
                    )
                }

                _tabs.update { tabs ->
                    tabs.mapIndexed { i, t ->
                        if (i == tabIdx) t.copy(
                            searchResults = results.hits,
                            isSearching = results.hits.isNotEmpty(),
                        ) else t
                    }
                }

                if (results.hits.isNotEmpty()) {
                    loadSearchHitsSequentially(tabIdx, translation, results.hits)
                } else {
                    _tabs.update { tabs ->
                        tabs.mapIndexed { i, t ->
                            if (i == tabIdx) t.copy(isSearching = false) else t
                        }
                    }
                }
            } catch (e: Throwable) {
                _errorMessage.value = "Search error: ${e.message}"
                _tabs.update { tabs ->
                    tabs.mapIndexed { i, t ->
                        if (i == tabIdx) t.copy(isSearching = false) else t
                    }
                }
            }
        }
    }

    private fun loadSearchHitsSequentially(
        tabIdx: Int,
        translation: InstalledTranslation,
        hits: List<SearchHit>,
    ) {
        searchContentLoadingJob = viewModelScope.launch {
            for ((index, hit) in hits.withIndex()) {
                // Hit key format from the backend: "BOOK_ID CHAP:VERSE" or
                // "BOOK_ID CHAP" — we split it back out and load the chapter.
                val (bookId, chapterNum) = parseHitKey(hit.key) ?: continue

                try {
                    val chapter = withContext(Dispatchers.IO) {
                        repository.readFreeBibleChapter(
                            translation.id, bookId, chapterNum,
                        )
                    }
                    if (chapter != null) {
                        _tabs.update { tabs ->
                            if (tabIdx !in tabs.indices) return@update tabs
                            tabs.mapIndexed { i, t ->
                                if (i == tabIdx) t.copy(
                                    searchHitsContent = t.searchHitsContent + (hit.key to chapter),
                                    searchLoadProgress = (index + 1f) / hits.size,
                                    isSearching = index < hits.size - 1,
                                ) else t
                            }
                        }
                    }
                } catch (_: Throwable) {
                    // Skip failed hit
                }
            }
        }
    }

    /** "GEN 1:1" or "GEN 1" → ("GEN", 1) */
    private fun parseHitKey(key: String): Pair<String, Int>? {
        val parts = key.trim().split(Regex("\\s+"))
        if (parts.size < 2) return null
        val bookId = parts[0]
        val chap = parts[1].substringBefore(':').toIntOrNull() ?: return null
        return bookId to chap
    }

    fun openSearchHit(hit: SearchHit) {
        val idx = _activeTabIndex.value
        val tab = _tabs.value.getOrNull(idx) ?: return
        if (tab.type != TabType.SEARCH || tab.translation == null) return

        val (bookId, chapter) = parseHitKey(hit.key) ?: return

        val updated = tab.copy(
            type = TabType.READ,
            bookId = bookId,
            chapter = chapter,
            chapterData = null,
        )
        _tabs.value = _tabs.value.toMutableList().also { it[idx] = updated }
        updateActiveTab(updated)
        openChapter(bookId, chapter)
    }

    fun openSearchHitInNewTab(hit: SearchHit) {
        val tab = _tabs.value.getOrNull(_activeTabIndex.value) ?: return
        if (tab.type != TabType.SEARCH || tab.translation == null) return
        val (bookId, chapter) = parseHitKey(hit.key) ?: return

        val newTab = ReaderTab(
            type = TabType.READ,
            translation = tab.translation,
            bookId = bookId,
            chapter = chapter,
        )
        _tabs.value = _tabs.value + newTab
        _activeTabIndex.value = _tabs.value.size - 1
        updateActiveTab(newTab)
        openChapter(bookId, chapter)
    }

    fun updateSearchType(searchType: SearchType) {
        val idx = _activeTabIndex.value
        val tab = _tabs.value.getOrNull(idx) ?: return
        if (tab.type != TabType.SEARCH) return

        _tabs.update { tabs ->
            tabs.mapIndexed { i, t -> if (i == idx) t.copy(searchType = searchType) else t }
        }
        saveTabsState()
        if (tab.searchQuery.isNotBlank()) performSearch(tab.searchQuery)
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

    // ── Highlights ──────────────────────────────────────────────────
    private val _highlights = MutableStateFlow<Map<String, VerseHighlight>>(emptyMap())
    val highlights: StateFlow<Map<String, VerseHighlight>> = _highlights.asStateFlow()

    fun setHighlight(bookId: String, chapter: Int, verse: Int, color: HighlightColor, style: HighlightStyle) {
        val key = "$bookId:$chapter:$verse"
        _highlights.update { current ->
            current + (key to VerseHighlight(color, style))
        }
    }

    fun removeHighlight(bookId: String, chapter: Int, verse: Int) {
        val key = "$bookId:$chapter:$verse"
        _highlights.update { it - key }
    }

    // ── Study tools (SWORD-backed, unchanged) ──────────────────────

    fun selectTool(tool: StudyTool) {
        _selectedTool.value = tool
        if (tool == StudyTool.Commentary) loadCommentaryContent()
    }

    fun lookupWord(word: ReaderWord) {
        val clean = word.text.filter { it.isLetterOrDigit() }
        if (clean.isEmpty()) return

        _selectedWordForLookup.value = clean
        _selectedTool.value = StudyTool.Dictionary
        _isDictionaryLoading.value = true

        viewModelScope.launch {
            val query = DictionaryQuery(
                word = clean,
                strongs = listOfNotNull(word.strongs),
                language = "en",
            )
            val response = withContext(Dispatchers.IO) { repository.lookupDictionary(query) }
            _dictionaryResults.value = response.results
            _isDictionaryLoading.value = false
        }
    }

    fun lookupStrongs(strongsCode: String) {
        if (strongsCode.isEmpty()) return
        _selectedStrongsForLookup.value = strongsCode
        _selectedTool.value = StudyTool.Lexicon

        if (_availableLexicons.value.isEmpty()) {
            viewModelScope.launch {
                _availableLexicons.value = withContext(Dispatchers.IO) {
                    repository.getLexiconModules()
                }
                if (_selectedLexiconModule.value == null) {
                    _selectedLexiconModule.value = _availableLexicons.value.firstOrNull()
                }
                loadLexiconContent()
            }
        } else {
            loadLexiconContent()
        }
    }

    fun selectLexiconModule(module: SwordModule) {
        _selectedLexiconModule.value = module
        loadLexiconContent()
    }

    fun loadLexiconContent() {
        val strongs = _selectedStrongsForLookup.value
        if (strongs.isEmpty()) {
            _lexiconResults.value = emptyList()
            return
        }
        val module = _selectedLexiconModule.value
        _isLexiconLoading.value = true
        val lang = module?.language ?: "en"

        viewModelScope.launch {
            val query = LexiconQuery(strongsNumber = strongs, language = lang)
            val response = withContext(Dispatchers.IO) { repository.lookupStrongsNumber(query) }
            _lexiconResults.value = response.results
            _isLexiconLoading.value = false
        }
    }

    fun selectCommentaryModule(module: SwordModule) {
        _selectedCommentaryModule.value = module
        loadCommentaryContent()
    }

    fun loadCommentaryContent() {
        val module = _selectedCommentaryModule.value ?: return
        val data = _currentChapterData.value ?: return
        val reference = "${data.bookName} ${data.chapterNumber}"

        _isCommentaryLoading.value = true
        _currentCommentaryReference.value = reference

        viewModelScope.launch {
            _commentaryResults.value = withContext(Dispatchers.IO) {
                repository.getChapterContent(module.name, reference)
            }
            _isCommentaryLoading.value = false
        }
    }

    // ── Refresh ─────────────────────────────────────────────────────

    fun refreshEngine() {
        if (!repository.isInitialized()) return
        viewModelScope.launch {
            _installedTranslations.value = withContext(Dispatchers.IO) {
                repository.getFreeBibleInstalledTranslations()
            }
            _availableLexicons.value = withContext(Dispatchers.IO) {
                repository.getLexiconModules()
            }
            _availableCommentaries.value = withContext(Dispatchers.IO) {
                repository.getCommentaryModules()
            }
            if (_selectedLexiconModule.value == null) {
                _selectedLexiconModule.value = _availableLexicons.value.firstOrNull()
            }
            if (_selectedCommentaryModule.value == null) {
                _selectedCommentaryModule.value = _availableCommentaries.value.firstOrNull()
            }
        }
    }

    // ── Books helpers (exposed for the book picker sheet) ──────────

    fun loadBooksForCurrentTranslation() {
        val translation = _currentTranslation.value ?: return
        viewModelScope.launch {
            _currentBooks.value = withContext(Dispatchers.IO) {
                repository.getFreeBibleBooks(translation.id)
            }
        }
    }
}