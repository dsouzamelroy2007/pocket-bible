package app.pocketbible.data

import kotlinx.coroutines.flow.Flow

class ContentRepository(private val dao: ContentDao) {

    fun feelings(language: String): Flow<List<Feeling>> = dao.feelings(language)

    fun savedEntries(language: String): Flow<List<EntrySummary>> = dao.savedEntries(language)

    suspend fun entriesForFeeling(feelingId: String, language: String): List<EntrySummary> =
        dao.entriesForFeeling(feelingId, language)

    suspend fun passagesForEntry(entryId: String): List<PassageWithRole> =
        dao.passagesForEntry(entryId)

    suspend fun toggleSave(entryId: String, currentlySaved: Boolean) {
        if (currentlySaved) dao.unsave(entryId)
        else dao.save(SavedEntry(entryId = entryId, savedAt = System.currentTimeMillis()))
    }

    suspend fun recordView(entryId: String, feelingId: String) {
        dao.record(ViewHistory(entryId = entryId, feelingId = feelingId, viewedAt = System.currentTimeMillis()))
    }

    /** Feeling ids ranked by how well free text matches, for a future search box. */
    suspend fun feelingsMatching(query: String): List<String> =
        if (query.isBlank()) emptyList() else dao.feelingsMatching(query.trim().lowercase())

    // ---------- Open-ended reading ----------

    /** Translation to read in for [language], falling back to English if none is loaded yet. */
    suspend fun translationForLanguage(language: String): String =
        dao.translationForLanguage(language) ?: "web-c"

    fun readableBooks(translationId: String): Flow<List<Book>> = dao.readableBooks(translationId)

    suspend fun chaptersForBook(bookId: String, translationId: String): List<Int> =
        dao.chaptersForBook(bookId, translationId)

    suspend fun versesForChapter(bookId: String, chapter: Int, translationId: String): List<ScriptureVerse> =
        dao.versesForChapter(bookId, chapter, translationId)

    suspend fun verseOfDay(monthDay: String): Passage? =
        dao.passageOfDay(monthDay)?.let { dao.passage(it) }

    // ---------- Characters ----------

    fun characters(language: String, includeDeuterocanon: Boolean): Flow<List<CharacterSummary>> =
        dao.characters(language, includeDeuterocanon)

    suspend fun verseRefsForCharacter(characterId: String, language: String): List<CharacterVerseRef> =
        dao.verseRefsForCharacter(characterId, language)

    /** Whether the translation currently shown for [language] includes the deuterocanonical books. */
    suspend fun translationIncludesDeuterocanon(language: String): Boolean =
        dao.translationIncludesDeuterocanon(translationForLanguage(language))

    // ---------- Bible bookmarks ----------

    fun bookmarks(): Flow<List<BibleBookmark>> = dao.bookmarks()

    suspend fun addBookmark(translationId: String, bookId: String, chapter: Int, verse: Int?) {
        dao.insertBookmark(
            BibleBookmark(
                translationId = translationId,
                bookId = bookId,
                chapter = chapter,
                verse = verse,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun removeBookmark(id: Long) = dao.deleteBookmark(id)
}
