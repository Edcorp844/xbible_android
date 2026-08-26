package com.example.xbible.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.xbible.data.BibleRepository
import com.example.xbible.ui.components.WordConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uniffi.xbible_engine.DictionaryQuery
import uniffi.xbible_engine.DictionaryResult
import uniffi.xbible_engine.LexiconQuery
import uniffi.xbible_engine.LexiconResult
import uniffi.xbible_engine.ModuleBook
import uniffi.xbible_engine.SearchHit
import uniffi.xbible_engine.SearchType
import uniffi.xbible_engine.Section
import uniffi.xbible_engine.SwordModule
import uniffi.xbible_engine.Word

enum class TabType { READ, SEARCH }

data class StudyTab(
    val id: String = java.util.UUID.randomUUID().toString(),
    val module: SwordModule? = null,
    val reference: String? = null,
    val content: List<Section> = emptyList(),
    val bookIndex: Int = -1,
    val chapterIndex: Int = -1,
    val books: List<ModuleBook> = emptyList(),
    val type: TabType = TabType.READ,
    val searchQuery: String = "",
    val searchResults: List<SearchHit> = emptyList(),
    val searchHitsContent: Map<String, List<Section>> = emptyMap(),
    val isSearching: Boolean = false,
    val searchLoadProgress: Float = 0f,
    val searchType: SearchType = SearchType.MULTI_WORD
)

enum class StudyTool {
    Dictionary, Lexicon, Commentary
}

class EngineViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: BibleRepository = BibleRepository(application)
) : AndroidViewModel(application) {

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _installedBibles = MutableStateFlow<List<SwordModule>>(emptyList())
    val installedBibles: StateFlow<List<SwordModule>> = _installedBibles.asStateFlow()

    private val _currentModule = MutableStateFlow<SwordModule?>(null)
    val currentModule: StateFlow<SwordModule?> = _currentModule.asStateFlow()

    private val _parallelModule = MutableStateFlow<SwordModule?>(null)
    val parallelModule: StateFlow<SwordModule?> = _parallelModule.asStateFlow()

    private val _currentReference = MutableStateFlow<String?>(null)
    val currentReference: StateFlow<String?> = _currentReference.asStateFlow()

    private val _currentChapterContent = MutableStateFlow<List<Section>>(emptyList())
    val currentChapterContent: StateFlow<List<Section>> = _currentChapterContent.asStateFlow()

    private val _parallelChapterContent = MutableStateFlow<List<Section>>(emptyList())
    val parallelChapterContent: StateFlow<List<Section>> = _parallelChapterContent.asStateFlow()

    private val _wordConfig = MutableStateFlow(WordConfig())
    val wordConfig: StateFlow<WordConfig> = _wordConfig.asStateFlow()

    private val _isLoadingContent = MutableStateFlow(false)
    val isLoadingContent: StateFlow<Boolean> = _isLoadingContent.asStateFlow()

    private val _tabs = MutableStateFlow<List<StudyTab>>(listOf(StudyTab()))
    val tabs: StateFlow<List<StudyTab>> = _tabs.asStateFlow()

    private val _activeTabIndex = MutableStateFlow(0)
    val activeTabIndex: StateFlow<Int> = _activeTabIndex.asStateFlow()

    private val _hasNext = MutableStateFlow(false)
    val hasNext: StateFlow<Boolean> = _hasNext.asStateFlow()

    private val _hasPrevious = MutableStateFlow(false)
    val hasPrevious: StateFlow<Boolean> = _hasPrevious.asStateFlow()

    private val _currentBooks = MutableStateFlow<List<ModuleBook>>(emptyList())
    val currentBooksFlow: StateFlow<List<ModuleBook>> = _currentBooks.asStateFlow()

    private val _currentBookIndex = MutableStateFlow(-1)
    val currentBookIndexFlow: StateFlow<Int> = _currentBookIndex.asStateFlow()

    private val _currentChapterIndex = MutableStateFlow(-1)
    val currentChapterIndexFlow: StateFlow<Int> = _currentChapterIndex.asStateFlow()

    // Study Tools State
    private val _selectedTool = MutableStateFlow(StudyTool.Dictionary)
    val selectedTool: StateFlow<StudyTool> = _selectedTool.asStateFlow()

    // Dictionary State
    private val _selectedWordForLookup = MutableStateFlow("")
    val selectedWordForLookup: StateFlow<String> = _selectedWordForLookup.asStateFlow()

    private val _dictionaryResults = MutableStateFlow<List<DictionaryResult>>(emptyList())
    val dictionaryResults: StateFlow<List<DictionaryResult>> = _dictionaryResults.asStateFlow()

    private val _isDictionaryLoading = MutableStateFlow(false)
    val isDictionaryLoading: StateFlow<Boolean> = _isDictionaryLoading.asStateFlow()

    // Lexicon State
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

    // Commentary State
    private val _selectedCommentaryModule = MutableStateFlow<SwordModule?>(null)
    val selectedCommentaryModule: StateFlow<SwordModule?> = _selectedCommentaryModule.asStateFlow()

    private val _availableCommentaries = MutableStateFlow<List<SwordModule>>(emptyList())
    val availableCommentaries: StateFlow<List<SwordModule>> = _availableCommentaries.asStateFlow()

    private val _commentaryResults = MutableStateFlow<List<Section>>(emptyList())
    val commentaryResults: StateFlow<List<Section>> = _commentaryResults.asStateFlow()

    private val _isCommentaryLoading = MutableStateFlow(false)
    val isCommentaryLoading: StateFlow<Boolean> = _isCommentaryLoading.asStateFlow()

    private val _currentCommentaryReference = MutableStateFlow("")
    val currentCommentaryReference: StateFlow<String> = _currentCommentaryReference.asStateFlow()

    private var currentBooks: List<ModuleBook> = emptyList()
    private var currentBookIndex: Int = -1
    private var currentChapterIndex: Int = -1

    private val prefs = application.getSharedPreferences("word_config", Context.MODE_PRIVATE)
    private val tabPrefs = application.getSharedPreferences("tabs_state", Context.MODE_PRIVATE)

    init {
        loadConfig()
        setupEngine()
    }

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
        val lineHeight = prefs.getFloat("lineHeight", 1.5f)
        val wordSpacing = prefs.getFloat("wordSpacing", 0f)
        val justify = prefs.getBoolean("justify", false)
        val showRedWords = prefs.getBoolean("showRedWords", true)
        val showStrongs = prefs.getBoolean("showStrongs", true)
        val showLemma = prefs.getBoolean("showLemma", true)
        val showMorph = prefs.getBoolean("showMorph", true)
        val showVerseNotes = prefs.getBoolean("showVerseNotes", true)
        val showWordNotes = prefs.getBoolean("showWordNotes", true)

        _wordConfig.value = WordConfig(
            fontSize = fontSize,
            fontFamily = fontFamily,
            lineHeight = lineHeight,
            wordSpacing = wordSpacing,
            justify = justify,
            showRedWords = showRedWords,
            showStrongs = showStrongs,
            showLemma = showLemma,
            showMorph = showMorph,
            showVerseNotes = showVerseNotes,
            showWordNotes = showWordNotes
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
            apply()
        }
    }

    private fun setupEngine() {
        viewModelScope.launch {
            val result = repository.initialize()
            result.onSuccess {
                _isReady.value = true
                loadInitialContent()
            }.onFailure { e ->
                _errorMessage.value = "Failed to initialize Bible engine: ${e.message}"
            }
        }
    }

    private fun loadInitialContent() {
        viewModelScope.launch {
            val modules = withContext(Dispatchers.IO) { repository.getBibleModules() }
            _installedBibles.value = modules

            if (modules.isEmpty()) return@launch

            val savedTabCount = tabPrefs.getInt("tab_count", 0)
            if (savedTabCount > 0) {
                val restoredTabs = mutableListOf<StudyTab>()
                for (i in 0 until savedTabCount) {
                    val typeStr = tabPrefs.getString("tab_${i}_type", TabType.READ.name) ?: TabType.READ.name
                    val type = try { TabType.valueOf(typeStr) } catch(e: Exception) { TabType.READ }
                    val moduleName = tabPrefs.getString("tab_${i}_module", null)
                    val reference = tabPrefs.getString("tab_${i}_reference", null)
                    val searchQuery = tabPrefs.getString("tab_${i}_search_query", "") ?: ""
                    val searchTypeStr = tabPrefs.getString("tab_${i}_search_type", SearchType.MULTI_WORD.name) ?: SearchType.MULTI_WORD.name
                    val searchType = try { SearchType.valueOf(searchTypeStr) } catch(e: Exception) { SearchType.MULTI_WORD }

                    val module = modules.find { it.name == moduleName } ?: modules.firstOrNull()
                    
                    if (type == TabType.READ && module != null) {
                        val books = withContext(Dispatchers.IO) { repository.getBooks(module.name) }
                        val bookName = reference?.substringBeforeLast(" ")
                        val chapterStr = reference?.substringAfterLast(" ")
                        val chapterNumber = chapterStr?.toIntOrNull() ?: 1
                        
                        val bookIndex = if (bookName != null) books.indexOfFirst { it.name.contains(bookName, ignoreCase = true) || bookName.contains(it.name, ignoreCase = true) } else -1
                        val chapterIndex = if (bookIndex != -1) books[bookIndex].chapters.indexOfFirst { it.number == chapterNumber } else -1

                        restoredTabs.add(StudyTab(
                            type = type,
                            module = module,
                            reference = reference,
                            books = books,
                            bookIndex = bookIndex,
                            chapterIndex = chapterIndex
                        ))
                    } else {
                        restoredTabs.add(StudyTab(
                            type = type,
                            module = module,
                            searchQuery = searchQuery,
                            searchType = searchType
                        ))
                    }
                }
                
                if (restoredTabs.isNotEmpty()) {
                    _tabs.value = restoredTabs
                    val activeIndex = tabPrefs.getInt("active_tab_index", 0).coerceIn(0, restoredTabs.size - 1)
                    _activeTabIndex.value = activeIndex
                    updateActiveTab(restoredTabs[activeIndex])
                    return@launch
                }
            }

            // Fallback to default first module
            val firstModule = modules.first()
            val books = withContext(Dispatchers.IO) { repository.getBooks(firstModule.name) }

            if (books.isNotEmpty()) {
                val firstBook = books.first()
                val chapters = firstBook.chapters
                if (chapters.isNotEmpty()) {
                    val chapter = chapters[0]
                    val reference = "${firstBook.name} ${chapter.number}"

                    val initialTab = StudyTab(
                        module = firstModule,
                        reference = reference,
                        bookIndex = 0,
                        chapterIndex = 0,
                        books = books
                    )
                    _tabs.value = listOf(initialTab)
                    updateActiveTab(initialTab)
                }
            }
        }
    }

    private fun saveTabsState() {
        val tabs = _tabs.value
        val activeIndex = _activeTabIndex.value
        tabPrefs.edit().apply {
            putInt("active_tab_index", activeIndex)
            putInt("tab_count", tabs.size)
            tabs.forEachIndexed { i, tab ->
                putString("tab_${i}_type", tab.type.name)
                putString("tab_${i}_module", tab.module?.name)
                putString("tab_${i}_reference", tab.reference)
                putString("tab_${i}_search_query", tab.searchQuery)
                putString("tab_${i}_search_type", tab.searchType.name)
            }
            apply()
        }
    }

    private fun updateActiveTab(tab: StudyTab) {
        _currentModule.value = tab.module
        _currentReference.value = tab.reference
        _currentChapterContent.value = tab.content

        currentBooks = tab.books
        _currentBooks.value = tab.books
        currentBookIndex = tab.bookIndex
        _currentBookIndex.value = tab.bookIndex
        currentChapterIndex = tab.chapterIndex
        _currentChapterIndex.value = tab.chapterIndex

        if (tab.type == TabType.READ && tab.content.isEmpty() && tab.module != null && tab.reference != null) {
            loadChapter(tab.module.name, tab.reference)
        }

        updateNavigationStatus()
    }

    fun switchTab(index: Int) {
        if (index in _tabs.value.indices) {
            // Save current state to the list before switching
            val currentTabs = _tabs.value.toMutableList()
            val oldActiveIndex = _activeTabIndex.value
            if (oldActiveIndex in currentTabs.indices) {
                currentTabs[oldActiveIndex] = currentTabs[oldActiveIndex].copy(
                    module = _currentModule.value,
                    reference = _currentReference.value,
                    content = _currentChapterContent.value,
                    bookIndex = currentBookIndex,
                    chapterIndex = currentChapterIndex,
                    books = currentBooks
                )
            }
            _tabs.value = currentTabs

            _activeTabIndex.value = index
            updateActiveTab(currentTabs[index])
            saveTabsState()
        }
    }

    fun addNewTab() {
        val newTab = StudyTab()
        _tabs.value = _tabs.value + newTab
        switchTab(_tabs.value.size - 1)
        saveTabsState()

        // If we have installed bibles, initialize the new tab with the first one
        if (_installedBibles.value.isNotEmpty()) {
            val firstModule = _installedBibles.value.first()
            viewModelScope.launch {
                val books = withContext(Dispatchers.IO) { repository.getBooks(firstModule.name) }
                if (books.isNotEmpty()) {
                    val firstBook = books.first()
                    val firstChapter = firstBook.chapters.firstOrNull()?.number ?: 1
                    val reference = "${firstBook.name} $firstChapter"

                    val updatedTab = newTab.copy(
                        module = firstModule,
                        reference = reference,
                        bookIndex = 0,
                        chapterIndex = 0,
                        books = books
                    )
                    val currentTabs = _tabs.value.toMutableList()
                    currentTabs[_activeTabIndex.value] = updatedTab
                    _tabs.value = currentTabs
                    updateActiveTab(updatedTab)
                    saveTabsState()
                }
            }
        }
    }

    fun addSearchTab() {
        val currentModule = _currentModule.value
        val newTab = StudyTab(
            type = TabType.SEARCH,
            module = currentModule
        )
        _tabs.value = _tabs.value + newTab
        switchTab(_tabs.value.size - 1)
        saveTabsState()
    }

    private var searchContentLoadingJob: kotlinx.coroutines.Job? = null

    fun performSearch(query: String) {
        val tabIndex = _activeTabIndex.value
        val tab = _tabs.value.getOrNull(tabIndex) ?: return
        if (tab.type != TabType.SEARCH) return

        val module = tab.module ?: _currentModule.value ?: return

        searchContentLoadingJob?.cancel()
        
        viewModelScope.launch {
            _tabs.update { tabs ->
                tabs.mapIndexed { i, t ->
                    if (i == tabIndex) t.copy(isSearching = true, searchQuery = query, searchResults = emptyList(), searchHitsContent = emptyMap(), searchLoadProgress = 0f) else t
                }
            }
            
            try {
                val results = withContext(Dispatchers.IO) {
                    repository.search(module.name, query, tab.searchType)
                }

                _tabs.update { tabs ->
                    tabs.mapIndexed { i, t ->
                        if (i == tabIndex) t.copy(searchResults = results.hits, module = module, isSearching = results.hits.isNotEmpty()) else t
                    }
                }

                if (results.hits.isNotEmpty()) {
                    loadSearchResultsSequentially(tabIndex, results.hits)
                } else {
                     _tabs.update { tabs ->
                        tabs.mapIndexed { i, t -> if (i == tabIndex) t.copy(isSearching = false) else t }
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Search error: ${e.message}"
                _tabs.update { tabs ->
                    tabs.mapIndexed { i, t -> if (i == tabIndex) t.copy(isSearching = false) else t }
                }
            }
        }
    }

    private fun loadSearchResultsSequentially(tabIndex: Int, hits: List<SearchHit>) {
        searchContentLoadingJob = viewModelScope.launch {
            val module = _tabs.value[tabIndex].module ?: return@launch
            
            for ((index, hit) in hits.withIndex()) {
                try {
                    val content = withContext(Dispatchers.IO) {
                        repository.getSingleEntry(module, hit.key)
                    }
                    
                    _tabs.update { tabs ->
                        if (tabIndex !in tabs.indices) return@update tabs
                        tabs.mapIndexed { i, studyTab ->
                            if (i == tabIndex) {
                                studyTab.copy(
                                    searchHitsContent = studyTab.searchHitsContent + (hit.key to content),
                                    searchLoadProgress = (index + 1).toFloat() / hits.size,
                                    isSearching = index < hits.size - 1
                                )
                            } else {
                                studyTab
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Skip if one fails
                }
            }
        }
    }

    fun openSearchHit(hit: SearchHit) {
        val index = _activeTabIndex.value
        val currentTabs = _tabs.value.toMutableList()
        val tab = currentTabs.getOrNull(index) ?: return
        if (tab.type != TabType.SEARCH || tab.module == null) return

        viewModelScope.launch {
            _isLoadingContent.value = true
            try {
                val module = tab.module
                val reference = hit.key
                val books = withContext(Dispatchers.IO) { repository.getBooks(module.name) }
                
                // Try to find book index
                val bookName = reference.substringBeforeLast(" ")
                val chapterStr = reference.substringAfterLast(" ")
                val chapterNumber = chapterStr.toIntOrNull() ?: 1
                
                val bookIndex = books.indexOfFirst { it.name.contains(bookName, ignoreCase = true) || bookName.contains(it.name, ignoreCase = true) }
                val chapterIndex = if (bookIndex != -1) {
                    books[bookIndex].chapters.indexOfFirst { it.number == chapterNumber }
                } else -1

                val updatedTab = tab.copy(
                    type = TabType.READ,
                    reference = reference,
                    content = emptyList(),
                    books = books,
                    bookIndex = bookIndex,
                    chapterIndex = chapterIndex
                )
                currentTabs[index] = updatedTab
                _tabs.value = currentTabs
                updateActiveTab(updatedTab)
                saveTabsState()
            } catch (e: Exception) {
                _errorMessage.value = "Error opening search result: ${e.message}"
            } finally {
                _isLoadingContent.value = false
            }
        }
    }

    fun openSearchHitInNewTab(hit: SearchHit) {
        val tabIndex = _activeTabIndex.value
        val tab = _tabs.value.getOrNull(tabIndex) ?: return
        if (tab.type != TabType.SEARCH || tab.module == null) return

        viewModelScope.launch {
            _isLoadingContent.value = true
            try {
                val module = tab.module
                val reference = hit.key
                val books = withContext(Dispatchers.IO) { repository.getBooks(module.name) }
                
                val bookName = reference.substringBeforeLast(" ")
                val chapterStr = reference.substringAfterLast(" ")
                val chapterNumber = chapterStr.toIntOrNull() ?: 1
                
                val bookIndex = books.indexOfFirst { it.name.contains(bookName, ignoreCase = true) || bookName.contains(it.name, ignoreCase = true) }
                val chapterIndex = if (bookIndex != -1) {
                    books[bookIndex].chapters.indexOfFirst { it.number == chapterNumber }
                } else -1

                val newTab = StudyTab(
                    type = TabType.READ,
                    module = module,
                    reference = reference,
                    books = books,
                    bookIndex = bookIndex,
                    chapterIndex = chapterIndex
                )
                
                _tabs.update { it + newTab }
                _activeTabIndex.value = _tabs.value.size - 1
                updateActiveTab(newTab)
                saveTabsState()
            } catch (e: Exception) {
                _errorMessage.value = "Error opening search result in new tab: ${e.message}"
            } finally {
                _isLoadingContent.value = false
            }
        }
    }

    fun updateSearchType(searchType: SearchType) {
        val tabIndex = _activeTabIndex.value
        val tab = _tabs.value.getOrNull(tabIndex) ?: return
        if (tab.type != TabType.SEARCH) return

        _tabs.update { tabs ->
            tabs.mapIndexed { i, t ->
                if (i == tabIndex) {
                    t.copy(searchType = searchType)
                } else t
            }
        }
        saveTabsState()
        
        // Re-run search if there's a query
        if (tab.searchQuery.isNotEmpty()) {
            performSearch(tab.searchQuery)
        }
    }

    fun removeTab(index: Int) {
        val currentTabs = _tabs.value.toMutableList()
        if (currentTabs.size > 1 && index in currentTabs.indices) {
            currentTabs.removeAt(index)
            _tabs.value = currentTabs
            val nextIndex = if (_activeTabIndex.value >= currentTabs.size) currentTabs.size - 1 else _activeTabIndex.value
            _activeTabIndex.value = nextIndex
            updateActiveTab(currentTabs[nextIndex])
            saveTabsState()
        }
    }

    private fun updateNavigationStatus() {
        if (currentBooks.isEmpty() || currentBookIndex < 0) {
            _hasNext.value = false
            _hasPrevious.value = false
            return
        }

        val currentBook = currentBooks[currentBookIndex]

        // Has Next?
        _hasNext.value = when {
            currentChapterIndex < currentBook.chapters.size - 1 -> true
            currentBookIndex < currentBooks.size - 1 -> true
            else -> false
        }

        // Has Previous?
        _hasPrevious.value = when {
            currentChapterIndex > 0 -> true
            currentBookIndex > 0 -> true
            else -> false
        }
    }

    fun nextChapter() {
        val module = _currentModule.value ?: return
        if (currentBooks.isEmpty() || currentBookIndex < 0) return

        val currentBook = currentBooks[currentBookIndex]

        if (currentChapterIndex < currentBook.chapters.size - 1) {
            currentChapterIndex++
        } else if (currentBookIndex < currentBooks.size - 1) {
            currentBookIndex++
            currentChapterIndex = 0
        } else {
            return
        }

        _currentBookIndex.value = currentBookIndex
        _currentChapterIndex.value = currentChapterIndex

        val nextBook = currentBooks[currentBookIndex]
        val nextChapter = nextBook.chapters[currentChapterIndex]
        val reference = "${nextBook.name} ${nextChapter.number}"
        _currentReference.value = reference
        loadChapter(module.name, reference)
        updateNavigationStatus()
        updateCurrentTabInList()
        saveTabsState()
    }

    fun previousChapter() {
        val module = _currentModule.value ?: return
        if (currentBooks.isEmpty() || currentBookIndex < 0) return

        if (currentChapterIndex > 0) {
            currentChapterIndex--
        } else if (currentBookIndex > 0) {
            currentBookIndex--
            val prevBook = currentBooks[currentBookIndex]
            currentChapterIndex = prevBook.chapters.size - 1
        } else {
            return
        }

        _currentBookIndex.value = currentBookIndex
        _currentChapterIndex.value = currentChapterIndex

        val prevBook = currentBooks[currentBookIndex]
        val prevChapter = prevBook.chapters[currentChapterIndex]
        val reference = "${prevBook.name} ${prevChapter.number}"
        _currentReference.value = reference
        loadChapter(module.name, reference)
        updateNavigationStatus()
        updateCurrentTabInList()
        saveTabsState()
    }

    private fun updateCurrentTabInList() {
        val index = _activeTabIndex.value
        val currentTabs = _tabs.value.toMutableList()
        if (index in currentTabs.indices) {
            currentTabs[index] = currentTabs[index].copy(
                module = _currentModule.value,
                reference = _currentReference.value,
                content = _currentChapterContent.value,
                bookIndex = currentBookIndex,
                chapterIndex = currentChapterIndex,
                books = currentBooks
            )
            _tabs.value = currentTabs
            saveTabsState()
        }
    }

    fun selectModule(module: SwordModule) {
        viewModelScope.launch {
            _currentModule.value = module
            if (_parallelModule.value?.name == module.name) {
                _parallelModule.value = null
            }
            val books = withContext(Dispatchers.IO) { repository.getBooks(module.name) }
            currentBooks = books
            _currentBooks.value = books
            if (books.isNotEmpty()) {
                currentBookIndex = 0
                _currentBookIndex.value = 0
                val firstBook = books.first()
                val chapters = firstBook.chapters
                if (chapters.isNotEmpty()) {
                    currentChapterIndex = 0
                    _currentChapterIndex.value = 0
                    val chapter = chapters[0]
                    val reference = "${firstBook.name} ${chapter.number}"
                    _currentReference.value = reference
                    loadChapter(module.name, reference)
                }
            }
            updateNavigationStatus()
            updateCurrentTabInList()
            saveTabsState()
        }
    }

    fun selectBook(index: Int) {
        if (index in currentBooks.indices) {
            currentBookIndex = index
            _currentBookIndex.value = index
            // We don't load yet, user needs to pick a chapter
            saveTabsState()
        }
    }

    fun selectChapter(chapterNumber: Int) {
        val module = _currentModule.value ?: return
        if (currentBookIndex in currentBooks.indices) {
            val book = currentBooks[currentBookIndex]
            val chapterIndex = book.chapters.indexOfFirst { it.number == chapterNumber }
            if (chapterIndex != -1) {
                currentChapterIndex = chapterIndex
                _currentChapterIndex.value = chapterIndex
                val reference = "${book.name} $chapterNumber"
                _currentReference.value = reference
                loadChapter(module.name, reference)
                updateNavigationStatus()
                updateCurrentTabInList()
                saveTabsState()
            }
        }
    }

    fun selectParallelModule(module: SwordModule?) {
        _parallelModule.value = module
        if (module != null) {
            val reference = _currentReference.value
            if (reference != null) {
                loadParallelChapter(module.name, reference)
            }
        } else {
            _parallelChapterContent.value = emptyList()
        }
    }

    fun updateWordConfig(update: (WordConfig) -> WordConfig) {
        val newConfig = update(_wordConfig.value)
        _wordConfig.value = newConfig
        saveConfig(newConfig)
    }

    fun resetWordConfig() {
        val defaultConfig = WordConfig()
        _wordConfig.value = defaultConfig
        saveConfig(defaultConfig)
    }

    private fun loadParallelChapter(moduleName: String, reference: String) {
        viewModelScope.launch {
            try {
                val content = withContext(Dispatchers.IO) {
                    repository.getChapterContent(moduleName, reference)
                }
                _parallelChapterContent.value = content
            } catch (e: Exception) {
                // Silently fail or log
            }
        }
    }

    fun loadChapter(moduleName: String, reference: String) {
        viewModelScope.launch {
            _isLoadingContent.value = true
            try {
                val content = withContext(Dispatchers.IO) {
                    repository.getChapterContent(moduleName, reference)
                }
                _currentChapterContent.value = content
                
                // Also load parallel if exists
                val parallel = _parallelModule.value
                if (parallel != null) {
                    loadParallelChapter(parallel.name, reference)
                }
                
                updateCurrentTabInList()
            } catch (e: Exception) {
                _errorMessage.value = "Error loading chapter: ${e.message}"
            } finally {
                _isLoadingContent.value = false
            }
        }
    }

    fun refreshEngine() {
        if (repository.isInitialized()) {
            viewModelScope.launch {
                val modules = withContext(Dispatchers.IO) { repository.refreshModules() }
                _installedBibles.value = repository.getBibleModules()

                // Load tool metadata
                _availableLexicons.value = repository.getLexiconModules()
                _availableCommentaries.value = repository.getCommentaryModules()

                if (_selectedLexiconModule.value == null && _availableLexicons.value.isNotEmpty()) {
                    _selectedLexiconModule.value = _availableLexicons.value.first()
                }
                if (_selectedCommentaryModule.value == null && _availableCommentaries.value.isNotEmpty()) {
                    _selectedCommentaryModule.value = _availableCommentaries.value.first()
                }

                if (_currentModule.value == null && _installedBibles.value.isNotEmpty()) {
                    loadInitialContent()
                }
            }
        }
    }

    // --- Study Tools Actions ---

    fun selectTool(tool: StudyTool) {
        _selectedTool.value = tool
        if (tool == StudyTool.Commentary) {
            loadCommentaryContent()
        }
    }

    fun lookupWord(word: Word) {
        val cleanWord = word.text.filter { it.isLetterOrDigit() }
        if (cleanWord.isEmpty()) return

        _selectedWordForLookup.value = cleanWord
        _selectedTool.value = StudyTool.Dictionary
        _isDictionaryLoading.value = true

        viewModelScope.launch {
            val query = DictionaryQuery(word = cleanWord, strongs = emptyList(), language = word.language)
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
                _availableLexicons.value = withContext(Dispatchers.IO) { repository.getLexiconModules() }
                if (_selectedLexiconModule.value == null && _availableLexicons.value.isNotEmpty()) {
                    _selectedLexiconModule.value = _availableLexicons.value.first()
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
        val targetLanguage = module?.language ?: "en"

        viewModelScope.launch {
            val query = LexiconQuery(strongsNumber = strongs, language = targetLanguage)
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
        val reference = _currentReference.value ?: return

        _isCommentaryLoading.value = true
        _currentCommentaryReference.value = reference

        viewModelScope.launch {
            val results = withContext(Dispatchers.IO) {
                repository.getChapterContent(module.name, reference)
            }
            _commentaryResults.value = results
            _isCommentaryLoading.value = false
        }
    }
}