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

    fun readableBooks(): Flow<List<Book>> = dao.readableBooks()

    fun allBooks(): Flow<List<Book>> = dao.allBooks()

    suspend fun chaptersForBook(bookId: String): List<Int> = dao.chaptersForBook(bookId)

    suspend fun versesForChapter(bookId: String, chapter: Int): List<ScriptureVerse> =
        dao.versesForChapter(bookId, chapter)

    suspend fun verseOfDay(monthDay: String): Passage? =
        dao.passageOfDay(monthDay)?.let { dao.passage(it) }
}
