package app.pocketbible.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.pocketbible.data.BibleBookmark
import app.pocketbible.data.Book
import app.pocketbible.data.CharacterSummary
import app.pocketbible.data.ContentRepository
import app.pocketbible.data.DailyReading
import app.pocketbible.data.EntrySummary
import app.pocketbible.data.Feeling
import app.pocketbible.data.Passage
import app.pocketbible.data.PassageWithRole
import app.pocketbible.data.ScriptureVerse
import app.pocketbible.data.Translation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * A character's citation paired with the real verse text, resolved for the
 * current translation. [verses] is empty if that translation doesn't have
 * this chapter loaded yet -- the book id/chapter/verse range are kept
 * (rather than a pre-formatted reference string) so the UI can render the
 * book name localized, the same way the Read tab does.
 */
data class CharacterVerseDisplay(
    val caption: String,
    val bookId: String,
    val chapter: Int,
    val verseStart: Int,
    val verseEnd: Int,
    val verses: List<ScriptureVerse>
)

/** One reading role ("first_reading"/"psalm"/"second_reading"/"gospel") for today, its citation, and the real resolved text for the current translation -- empty if that translation doesn't have the cited book/chapter(s) yet. */
data class ResolvedReading(
    val role: String,
    val citationDisplay: String,
    val text: String
)

private val READING_ROLE_ORDER = listOf("first_reading", "psalm", "second_reading", "gospel")

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

    private val _scrollToVerse = MutableStateFlow<Int?>(null)
    val scrollToVerse: StateFlow<Int?> = _scrollToVerse.asStateFlow()

    private val _bookmarks = MutableStateFlow<List<BibleBookmark>>(emptyList())
    val bookmarks: StateFlow<List<BibleBookmark>> = _bookmarks.asStateFlow()

    private val _translations = MutableStateFlow<List<Translation>>(emptyList())
    val translations: StateFlow<List<Translation>> = _translations.asStateFlow()

    // ---------- Search ----------

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Feeling>>(emptyList())
    val searchResults: StateFlow<List<Feeling>> = _searchResults.asStateFlow()

    // ---------- Characters ----------

    private val _characters = MutableStateFlow<List<CharacterSummary>>(emptyList())
    val characters: StateFlow<List<CharacterSummary>> = _characters.asStateFlow()

    private val _selectedCharacter = MutableStateFlow<CharacterSummary?>(null)
    val selectedCharacter: StateFlow<CharacterSummary?> = _selectedCharacter.asStateFlow()

    private val _characterVerses = MutableStateFlow<List<CharacterVerseDisplay>>(emptyList())
    val characterVerses: StateFlow<List<CharacterVerseDisplay>> = _characterVerses.asStateFlow()

    // ---------- Verse of the day ----------

    private val _verseOfDay = MutableStateFlow<Passage?>(null)
    val verseOfDay: StateFlow<Passage?> = _verseOfDay.asStateFlow()

    // ---------- Daily reading ----------

    private val _selectedReadingDate = MutableStateFlow(LocalDate.now())
    val selectedReadingDate: StateFlow<LocalDate> = _selectedReadingDate.asStateFlow()

    private val _dailyReading = MutableStateFlow<DailyReading?>(null)
    val dailyReading: StateFlow<DailyReading?> = _dailyReading.asStateFlow()

    private val _resolvedReadings = MutableStateFlow<List<ResolvedReading>>(emptyList())
    val resolvedReadings: StateFlow<List<ResolvedReading>> = _resolvedReadings.asStateFlow()

    fun previousReadingDay() {
        _selectedReadingDate.value = _selectedReadingDate.value.minusDays(1)
        viewModelScope.launch { loadDailyReading() }
    }

    fun nextReadingDay() {
        _selectedReadingDate.value = _selectedReadingDate.value.plusDays(1)
        viewModelScope.launch { loadDailyReading() }
    }

    fun goToTodayReading() {
        _selectedReadingDate.value = LocalDate.now()
        viewModelScope.launch { loadDailyReading() }
    }

    fun goToReadingDate(date: LocalDate) {
        _selectedReadingDate.value = date
        viewModelScope.launch { loadDailyReading() }
    }

    // ---------- Language ----------

    private var loadedLanguage: String? = null

    /** BCP-47 language of the in-app switcher, or the system's if "System default" is selected. */
    private fun currentLanguage(): String =
        (AppCompatDelegate.getApplicationLocales().get(0) ?: Locale.getDefault()).language

    /** Scripture translation to read in for the current language, falling back to English. */
    private suspend fun currentTranslationId(): String = repo.translationForLanguage(currentLanguage())

    /**
     * Replaces each entry's baked passage text with the real scripture text
     * for whatever translation the current UI language reads in, when that
     * book/chapter is loaded there. entry_passage always points at the same
     * curated Passage row regardless of language, so without this, a
     * topic's verse stayed in that row's original language (English) under
     * every UI language -- switching to German changed the reflection and
     * prayer text correctly, but not the verse next to them. Falls back to
     * the passage's own baked text when the current translation doesn't
     * have that book yet, so nothing goes blank.
     */
    private suspend fun withResolvedPassageText(entries: List<EntrySummary>): List<EntrySummary> {
        val translationId = currentTranslationId()
        return entries.map { entry ->
            val verses = repo.versesForChapter(entry.bookId, entry.chapter, translationId)
                .filter { it.verse in entry.verseStart..entry.verseEnd }
            if (verses.isEmpty()) entry
            else entry.copy(passageText = verses.sortedBy { it.verse }.joinToString(" ") { it.text })
        }
    }

    /** Same live-resolution as [withResolvedPassageText], for a single [Passage] (verse-of-day, echo/context passages). */
    private suspend fun resolvedPassage(passage: Passage): Passage {
        val verses = repo.versesForChapter(passage.bookId, passage.chapterStart, currentTranslationId())
            .filter { it.verse in passage.verseStart..passage.verseEnd }
        if (verses.isEmpty()) return passage
        return passage.copy(text = verses.sortedBy { it.verse }.joinToString(" ") { it.text }, pullQuote = null)
    }

    /** Loads the selected date's Mass readings (if this app ships that date's lectionary year) and resolves each role's citation(s) to real text for the current translation. */
    private suspend fun loadDailyReading() {
        val date = _selectedReadingDate.value.format(DateTimeFormatter.ISO_LOCAL_DATE)
        _dailyReading.value = repo.dailyReading(date)
        val translationId = currentTranslationId()
        _resolvedReadings.value = repo.readingCitations(date)
            .groupBy { it.role }
            .map { (role, refs) ->
                val sorted = refs.sortedBy { it.position }
                val perRange = sorted.map { ref ->
                    repo.versesForRange(ref.bookId, ref.chapterStart, ref.verseStart, ref.chapterEnd, ref.verseEnd, translationId)
                        .joinToString(" ") { it.text }
                }
                ResolvedReading(role = role, citationDisplay = sorted.first().citationDisplay, text = perRange.joinToString(" "))
            }
            .sortedBy { READING_ROLE_ORDER.indexOf(it.role) }
    }

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
        viewModelScope.launch {
            repo.savedEntries(language).collect { _saved.value = withResolvedPassageText(it) }
        }
        viewModelScope.launch {
            repo.readableBooks(currentTranslationId()).collect { _readableBooks.value = it }
        }
        viewModelScope.launch {
            val includeDeuterocanon = repo.translationIncludesDeuterocanon(language)
            repo.characters(language, includeDeuterocanon).collect { _characters.value = it }
        }
        _selectedFeeling.value?.let { feeling ->
            viewModelScope.launch {
                _feelingEntries.value = withResolvedPassageText(repo.entriesForFeeling(feeling.id, language))
            }
        }
        viewModelScope.launch {
            val monthDay = LocalDate.now().format(DateTimeFormatter.ofPattern("MM-dd"))
            _verseOfDay.value = repo.verseOfDay(monthDay)?.let { resolvedPassage(it) }
        }
        viewModelScope.launch { loadDailyReading() }
    }

    init {
        ensureFreshForCurrentLanguage()
        viewModelScope.launch { repo.bookmarks().collect { _bookmarks.value = it } }
        viewModelScope.launch { repo.translations().collect { _translations.value = it } }
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

    /** Selects [book] and opens its first available chapter in one step, for tapping a book in the browse list. */
    suspend fun openBook(book: Book) {
        val translationId = currentTranslationId()
        val chapters = repo.chaptersForBook(book.id, translationId)
        _selectedBook.value = book
        _chapters.value = chapters
        val chapter = chapters.firstOrNull() ?: return
        _currentChapter.value = chapter
        _scrollToVerse.value = null
        _chapterVerses.value = repo.versesForChapter(book.id, chapter, translationId)
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

    /**
     * Adds or removes a bookmark at the currently open book/chapter (and
     * verse, if the reader jumped to one via "go to a verse") -- lets
     * someone mark their place while reading and jump straight back to it
     * later from the book list, instead of hunting for it again.
     */
    fun toggleBookmarkCurrent() {
        val book = _selectedBook.value ?: return
        val chapter = _currentChapter.value ?: return
        val verse = _scrollToVerse.value
        val existing = _bookmarks.value.firstOrNull { it.bookId == book.id && it.chapter == chapter && it.verse == verse }
        viewModelScope.launch {
            if (existing != null) repo.removeBookmark(existing.id)
            else repo.addBookmark(currentTranslationId(), book.id, chapter, verse)
        }
    }

    /**
     * Opens a bookmarked reading position. Returns false if that book or
     * chapter isn't loaded for the current translation anymore (e.g. the
     * language changed since it was bookmarked), so the caller can leave
     * the bookmark in place rather than navigating to an empty reader.
     */
    suspend fun openBookmark(bookmark: BibleBookmark): Boolean {
        val translationId = currentTranslationId()
        val chapters = repo.chaptersForBook(bookmark.bookId, translationId)
        val book = _readableBooks.value.firstOrNull { it.id == bookmark.bookId }
        if (book == null || bookmark.chapter !in chapters) return false
        _selectedBook.value = book
        _chapters.value = chapters
        _currentChapter.value = bookmark.chapter
        _scrollToVerse.value = bookmark.verse
        _chapterVerses.value = repo.versesForChapter(bookmark.bookId, bookmark.chapter, translationId)
        return true
    }

    fun deleteBookmark(id: Long) {
        viewModelScope.launch { repo.removeBookmark(id) }
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
            _feelingEntries.value = withResolvedPassageText(repo.entriesForFeeling(feeling.id, currentLanguage()))
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
            _feelingEntries.value =
                withResolvedPassageText(repo.entriesForFeeling(entry.entry.feelingId, currentLanguage()))
        }
    }

    /** Loads [character]'s verse citations and resolves the real text for each from the current translation. */
    fun selectCharacter(character: CharacterSummary) {
        _selectedCharacter.value = character
        _characterVerses.value = emptyList()
        viewModelScope.launch {
            val translationId = currentTranslationId()
            val refs = repo.verseRefsForCharacter(character.id, currentLanguage())
            _characterVerses.value = refs.map { ref ->
                val chapterVerses = repo.versesForChapter(ref.bookId, ref.chapter, translationId)
                val verses = chapterVerses.filter { it.verse in ref.verseStart..ref.verseEnd }
                CharacterVerseDisplay(
                    caption = ref.caption,
                    bookId = ref.bookId,
                    chapter = ref.chapter,
                    verseStart = ref.verseStart,
                    verseEnd = ref.verseEnd,
                    verses = verses
                )
            }
        }
    }

    private suspend fun loadExtraPassages() {
        val entry = currentEntry
        _extraPassages.value = if (entry == null) {
            emptyList()
        } else {
            repo.passagesForEntry(entry.entry.id)
                .filter { it.role != "primary" }
                .map { it.copy(passage = resolvedPassage(it.passage)) }
        }
    }

    class Factory(private val repo: ContentRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MainViewModel(repo) as T
    }
}
