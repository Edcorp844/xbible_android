package com.example.xbible.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.xbible.data.BibleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uniffi.xbible_engine.DictionaryQuery
import uniffi.xbible_engine.DictionaryResult
import uniffi.xbible_engine.LexiconQuery
import uniffi.xbible_engine.LexiconResult
import uniffi.xbible_engine.ModuleBook
import uniffi.xbible_engine.Section
import uniffi.xbible_engine.SwordModule
import uniffi.xbible_engine.Word

data class StudyTab(
    val id: String = java.util.UUID.randomUUID().toString(),
    val module: SwordModule? = null,
    val reference: String? = null,
    val content: List<Section> = emptyList(),
    val bookIndex: Int = -1,
    val chapterIndex: Int = -1,
    val books: List<ModuleBook> = emptyList()
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

    private val _currentReference = MutableStateFlow<String?>(null)
    val currentReference: StateFlow<String?> = _currentReference.asStateFlow()

    private val _currentChapterContent = MutableStateFlow<List<Section>>(emptyList())
    val currentChapterContent: StateFlow<List<Section>> = _currentChapterContent.asStateFlow()

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

    init {
        setupEngine()
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
            
            if (modules.isNotEmpty()) {
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
        
        if (tab.content.isEmpty() && tab.module != null && tab.reference != null) {
            loadChapter(tab.module.name, tab.reference)
        }
        
        updateNavigationStatus()
    }

    fun switchTab(index: Int) {
        if (index in _tabs.value.indices) {
            // Save current state to the list before switching
            val currentTabs = _tabs.value.toMutableList()
            currentTabs[_activeTabIndex.value] = currentTabs[_activeTabIndex.value].copy(
                module = _currentModule.value,
                reference = _currentReference.value,
                content = _currentChapterContent.value,
                bookIndex = currentBookIndex,
                chapterIndex = currentChapterIndex,
                books = currentBooks
            )
            _tabs.value = currentTabs
            
            _activeTabIndex.value = index
            updateActiveTab(currentTabs[index])
        }
    }

    fun addNewTab() {
        val newTab = StudyTab()
        _tabs.value = _tabs.value + newTab
        switchTab(_tabs.value.size - 1)
        
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
                }
            }
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
        }
    }

    fun selectModule(module: SwordModule) {
        viewModelScope.launch {
            _currentModule.value = module
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
        }
    }

    fun selectBook(index: Int) {
        if (index in currentBooks.indices) {
            currentBookIndex = index
            _currentBookIndex.value = index
            // We don't load yet, user needs to pick a chapter
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
