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
import uniffi.xbible_engine.Section
import uniffi.xbible_engine.SwordModule
import uniffi.xbible_engine.ModuleBook

data class StudyTab(
    val id: String = java.util.UUID.randomUUID().toString(),
    val module: SwordModule? = null,
    val reference: String? = null,
    val content: List<Section> = emptyList(),
    val bookIndex: Int = -1,
    val chapterIndex: Int = -1,
    val books: List<ModuleBook> = emptyList()
)

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
                if (_currentModule.value == null && _installedBibles.value.isNotEmpty()) {
                    loadInitialContent()
                }
            }
        }
    }
}
