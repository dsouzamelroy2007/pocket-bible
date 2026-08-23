package app.pocketbible.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.pocketbible.data.Book
import app.pocketbible.data.ContentRepository
import app.pocketbible.data.EntrySummary
import app.pocketbible.data.Feeling
import app.pocketbible.data.Passage
import app.pocketbible.data.PassageWithRole
import app.pocketbible.data.ScriptureVerse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainViewModel(private val repo: ContentRepository) : ViewModel() {

    private val _feelings = MutableStateFlow<List<Feeling>>(emptyList())
    val feelings: StateFlow<List<Feeling>> = _feelings.asStateFlow()

    private val _saved = MutableStateFlow<List<EntrySummary>>(emptyList())
    val saved: StateFlow<List<EntrySummary>> = _saved.asStateFlow()

    private val _feelingEntries = MutableStateFlow<List<EntrySummary>>(emptyList())
    val feelingEntries: StateFlow<List<EntrySummary>> = _feelingEntries.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _selectedFeeling = MutableStateFlow<Feeling?>(null)
    val selectedFeeling: StateFlow<Feeling?> = _selectedFeeling.asStateFlow()

    private val _extraPassages = MutableStateFlow<List<PassageWithRole>>(emptyList())
    val extraPassages: StateFlow<List<PassageWithRole>> = _extraPassages.asStateFlow()

    // ---------- Open-ended reading ----------

    private val _readableBooks = MutableStateFlow<List<Book>>(emptyList())
    val readableBooks: StateFlow<List<Book>> = _readableBooks.asStateFlow()

    private val _selectedBook = MutableStateFlow<Book?>(null)
    val selectedBook: StateFlow<Book?> = _selectedBook.asStateFlow()

    private val _chapters = MutableStateFlow<List<Int>>(emptyList())
    val chapters: StateFlow<List<Int>> = _chapters.asStateFlow()

    private val _currentChapter = MutableStateFlow<Int?>(null)
    val currentChapter: StateFlow<Int?> = _currentChapter.asStateFlow()

    private val _chapterVerses = MutableStateFlow<List<ScriptureVerse>>(emptyList())
    val chapterVerses: StateFlow<List<ScriptureVerse>> = _chapterVerses.asStateFlow()

    private val _allBooks = MutableStateFlow<List<Book>>(emptyList())
    val allBooks: StateFlow<List<Book>> = _allBooks.asStateFlow()

    private val _scrollToVerse = MutableStateFlow<Int?>(null)
    val scrollToVerse: StateFlow<Int?> = _scrollToVerse.asStateFlow()

    // ---------- Search ----------

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Feeling>>(emptyList())
    val searchResults: StateFlow<List<Feeling>> = _searchResults.asStateFlow()

    // ---------- Verse of the day ----------

    private val _verseOfDay = MutableStateFlow<Passage?>(null)
    val verseOfDay: StateFlow<Passage?> = _verseOfDay.asStateFlow()

    // ---------- Language ----------

    private var loadedLanguage: String? = null

    /** BCP-47 language of the in-app switcher, or the system's if "System default" is selected. */
    private fun currentLanguage(): String =
        (AppCompatDelegate.getApplicationLocales().get(0) ?: Locale.getDefault()).language

    /** Scripture translation to read in for the current language, falling back to English. */
    private suspend fun currentTranslationId(): String = repo.translationForLanguage(currentLanguage())

    /**
     * Re-issues the language-dependent queries (topics list, saved list, the
     * currently open topic's entries, and the Read tab's book list) if the
     * app's language has changed since the last call. Safe to call on every
     * composition: it's a no-op once the language is already current.
     * Needed because a language switch causes MainActivity to recreate()
     * itself, and whether this ViewModel instance survives that (keeping
     * its already-collected Flows pinned to the old language) or not isn't
     * something to rely on either way -- this covers both cases.
     */
    fun ensureFreshForCurrentLanguage() {
        val language = currentLanguage()
        if (loadedLanguage == language) return
        loadedLanguage = language
        viewModelScope.launch { repo.feelings(language).collect { _feelings.value = it } }
        viewModelScope.launch { repo.savedEntries(language).collect { _saved.value = it } }
        viewModelScope.launch {
            repo.readableBooks(currentTranslationId()).collect { _readableBooks.value = it }
        }
        _selectedFeeling.value?.let { feeling ->
            viewModelScope.launch { _feelingEntries.value = repo.entriesForFeeling(feeling.id, language) }
        }
    }

    init {
        ensureFreshForCurrentLanguage()
        viewModelScope.launch { repo.allBooks().collect { _allBooks.value = it } }
        viewModelScope.launch {
            val monthDay = LocalDate.now().format(DateTimeFormatter.ofPattern("MM-dd"))
            _verseOfDay.value = repo.verseOfDay(monthDay)
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            val ids = repo.feelingsMatching(query)
            _searchResults.value = ids.mapNotNull { id -> _feelings.value.find { it.id == id } }
        }
    }

    fun selectBook(book: Book) {
        _selectedBook.value = book
        viewModelScope.launch { _chapters.value = repo.chaptersForBook(book.id, currentTranslationId()) }
    }

    /** Chapters actually loaded for [book] in the current reading language — drives the "go to reference" picker's chapter list. */
    suspend fun chaptersAvailable(book: Book): List<Int> = repo.chaptersForBook(book.id, currentTranslationId())

    /** Verse numbers actually loaded for [book]/[chapter] — drives the picker's verse list. */
    suspend fun versesAvailable(book: Book, chapter: Int): List<Int> =
        repo.versesForChapter(book.id, chapter, currentTranslationId()).map { it.verse }

    fun openChapter(chapter: Int) {
        val book = _selectedBook.value ?: return
        _currentChapter.value = chapter
        _scrollToVerse.value = null
        viewModelScope.launch { _chapterVerses.value = repo.versesForChapter(book.id, chapter, currentTranslationId()) }
    }

    /**
     * Jumps straight to book/chapter/verse. Returns null on success, or a
     * user-facing message if that chapter or verse isn't loaded yet — this
     * app only has a handful of sample chapters so far, and the picker
     * intentionally lists every book, not just the ones with text.
     */
    suspend fun goToReference(book: Book, chapter: Int, verse: Int?): String? {
        val translationId = currentTranslationId()
        val chapters = repo.chaptersForBook(book.id, translationId)
        if (chapter !in chapters) {
            return "${book.displayName} $chapter isn't loaded in this prototype yet."
        }
        val verses = repo.versesForChapter(book.id, chapter, translationId)
        if (verse != null && verses.none { it.verse == verse }) {
            return "${book.displayName} $chapter has ${verses.size} verse(s) loaded here — verse $verse isn't among them."
        }
        _selectedBook.value = book
        _chapters.value = chapters
        _currentChapter.value = chapter
        _chapterVerses.value = verses
        _scrollToVerse.value = verse
        return null
    }

    fun nextChapter() {
        val chapters = _chapters.value
        val current = _currentChapter.value ?: return
        val index = chapters.indexOf(current)
        if (index in 0 until chapters.lastIndex) openChapter(chapters[index + 1])
    }

    fun previousChapter() {
        val chapters = _chapters.value
        val current = _currentChapter.value ?: return
        val index = chapters.indexOf(current)
        if (index > 0) openChapter(chapters[index - 1])
    }

    private val currentEntry: EntrySummary?
        get() = _feelingEntries.value.getOrNull(_currentIndex.value)

    fun selectFeeling(feeling: Feeling) {
        _selectedFeeling.value = feeling
        _currentIndex.value = 0
        viewModelScope.launch {
            _feelingEntries.value = repo.entriesForFeeling(feeling.id, currentLanguage())
            loadExtraPassages()
            currentEntry?.let { repo.recordView(it.entry.id, feeling.id) }
        }
    }

    fun another() {
        val entries = _feelingEntries.value
        if (entries.isEmpty()) return
        _currentIndex.value = (_currentIndex.value + 1) % entries.size
        viewModelScope.launch {
            loadExtraPassages()
            currentEntry?.let { repo.recordView(it.entry.id, it.entry.feelingId) }
        }
    }

    fun toggleSaveCurrent() {
        val entry = currentEntry ?: return
        viewModelScope.launch {
            repo.toggleSave(entry.entry.id, entry.isSaved)
            _feelingEntries.value = repo.entriesForFeeling(entry.entry.feelingId, currentLanguage())
        }
    }

    private suspend fun loadExtraPassages() {
        val entry = currentEntry
        _extraPassages.value = if (entry == null) {
            emptyList()
        } else {
            repo.passagesForEntry(entry.entry.id).filter { it.role != "primary" }
        }
    }

    class Factory(private val repo: ContentRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MainViewModel(repo) as T
    }
}
