package app.pocketbible.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

// ---------- Shipped content ----------

@Entity(tableName = "translation")
data class Translation(
    @PrimaryKey val id: String,
    val name: String,
    val abbreviation: String,
    val license: String,
    val versification: String,
    @ColumnInfo(name = "includes_deuterocanon") val includesDeuterocanon: Boolean,
    @ColumnInfo(name = "has_imprimatur") val hasImprimatur: Boolean,
    val language: String
)

@Entity(tableName = "book")
data class Book(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    val testament: String,
    @ColumnInfo(name = "is_deuterocanonical") val isDeuterocanonical: Boolean,
    @ColumnInfo(name = "sort_order") val sortOrder: Int
)

/**
 * A passage is one or more consecutive verses in one translation. It is no
 * longer assumed to be a single verse, or to stay within one chapter.
 */
@Entity(
    tableName = "passage",
    indices = [Index("book_id"), Index("translation_id")]
)
data class Passage(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "translation_id") val translationId: String,
    @ColumnInfo(name = "book_id") val bookId: String,
    @ColumnInfo(name = "chapter_start") val chapterStart: Int,
    @ColumnInfo(name = "chapter_end") val chapterEnd: Int,
    @ColumnInfo(name = "verse_start") val verseStart: Int,
    @ColumnInfo(name = "verse_end") val verseEnd: Int,
    val text: String,
    @ColumnInfo(name = "pull_quote") val pullQuote: String?,
    @ColumnInfo(name = "reference_display") val referenceDisplay: String,
    @ColumnInfo(name = "reference_alt") val referenceAlt: String?
)

@Entity(tableName = "feeling")
data class Feeling(
    @PrimaryKey val id: String,
    val label: String,
    val icon: String,
    val category: String,
    val description: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int
)

@Entity(
    tableName = "feeling_alias",
    primaryKeys = ["feeling_id", "alias"]
)
data class FeelingAlias(
    @ColumnInfo(name = "feeling_id") val feelingId: String,
    val alias: String,
    val weight: Float
)

/**
 * A translated label/description for one feeling, in one UI language. The
 * `feeling` table's own label/description stay the English original and
 * double as the fallback when no row exists here for the app's current
 * language -- so a language ships as much or as little of this as is ready,
 * one language at a time, without ever leaving a topic blank.
 */
@Entity(
    tableName = "feeling_translation",
    primaryKeys = ["feeling_id", "language"]
)
data class FeelingTranslation(
    @ColumnInfo(name = "feeling_id") val feelingId: String,
    val language: String,
    val label: String,
    val description: String
)

@Entity(tableName = "entry")
data class Entry(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "feeling_id") val feelingId: String,
    val reflection: String,
    val prayer: String,
    val intensity: String,
    @ColumnInfo(name = "depth_order") val depthOrder: Int,
    @ColumnInfo(name = "ccc_reference") val cccReference: String?,
    @ColumnInfo(name = "saint_quote") val saintQuote: String?,
    @ColumnInfo(name = "saint_attribution") val saintAttribution: String?,
    @ColumnInfo(name = "liturgical_season") val liturgicalSeason: String?
)

/** Translated reflection/prayer for one entry, in one UI language. Same fallback-to-English pattern as [FeelingTranslation]. */
@Entity(
    tableName = "entry_translation",
    primaryKeys = ["entry_id", "language"]
)
data class EntryTranslation(
    @ColumnInfo(name = "entry_id") val entryId: String,
    val language: String,
    val reflection: String,
    val prayer: String
)

/**
 * Joins an entry to one or more passages. Exactly one row per entry should
 * carry role = "primary"; it is what the feeling screen shows. Additional
 * rows (role = "echo" or "context") support it without crowding the card.
 */
@Entity(
    tableName = "entry_passage",
    primaryKeys = ["entry_id", "position"],
    indices = [Index("passage_id")]
)
data class EntryPassage(
    @ColumnInfo(name = "entry_id") val entryId: String,
    @ColumnInfo(name = "passage_id") val passageId: String,
    val position: Int,
    val role: String
)

@Entity(tableName = "daily_passage")
data class DailyPassage(
    @PrimaryKey @ColumnInfo(name = "month_day") val monthDay: String,
    @ColumnInfo(name = "passage_id") val passageId: String
)

/**
 * Full chapter text for open-ended reading, kept separate from the curated
 * `passage`/`entry_passage` pair above. A book only appears in the reading
 * tab once at least one verse of it exists here — so importing more of the
 * Bible later is purely a data change, no UI or Kotlin code to touch.
 */
@Entity(
    tableName = "scripture_verse",
    indices = [Index("book_id", "chapter")]
)
data class ScriptureVerse(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "translation_id") val translationId: String,
    @ColumnInfo(name = "book_id") val bookId: String,
    val chapter: Int,
    val verse: Int,
    val text: String
)

// ---------- Local user state (not shipped, created empty) ----------

@Entity(tableName = "saved_entry")
data class SavedEntry(
    @PrimaryKey @ColumnInfo(name = "entry_id") val entryId: String,
    @ColumnInfo(name = "saved_at") val savedAt: Long,
    val note: String? = null
)

@Entity(tableName = "view_history", indices = [Index("entry_id")])
data class ViewHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "entry_id") val entryId: String,
    @ColumnInfo(name = "feeling_id") val feelingId: String,
    @ColumnInfo(name = "viewed_at") val viewedAt: Long
)

// ---------- Read models used by the UI ----------

data class EntrySummary(
    @Embedded val entry: Entry,
    @ColumnInfo(name = "feeling_label") val feelingLabel: String,
    @ColumnInfo(name = "passage_text") val passageText: String,
    @ColumnInfo(name = "pull_quote") val pullQuote: String?,
    @ColumnInfo(name = "reference_display") val referenceDisplay: String,
    @ColumnInfo(name = "reference_alt") val referenceAlt: String?,
    @ColumnInfo(name = "is_saved") val isSaved: Boolean,
    @ColumnInfo(name = "last_viewed") val lastViewed: Long?
)

data class PassageWithRole(
    @Embedded val passage: Passage,
    val role: String,
    val position: Int
)

// ---------- DAOs ----------

@Dao
interface ContentDao {

    /**
     * [language] selects a UI language (e.g. "de") to prefer via
     * feeling_translation; falls back to the feeling's own English
     * label/description wherever no translated row exists yet.
     */
    @Query(
        """
        SELECT f.id, COALESCE(ft.label, f.label) AS label, f.icon, f.category,
               COALESCE(ft.description, f.description) AS description, f.sort_order
        FROM feeling f
        LEFT JOIN feeling_translation ft ON ft.feeling_id = f.id AND ft.language = :language
        ORDER BY f.sort_order
        """
    )
    fun feelings(language: String): Flow<List<Feeling>>

    /**
     * Entries for a feeling with their primary passage attached. Entries
     * never shown before sort first; within that, acute (crisis-pitched)
     * entries come before steady, then settled, then by depth_order.
     * [language] selects translated reflection/prayer/feeling_label the same
     * way [feelings] does, falling back to English per-field.
     */
    @Query(
        """
        SELECT e.id, e.feeling_id,
               COALESCE(et.reflection, e.reflection) AS reflection,
               COALESCE(et.prayer, e.prayer) AS prayer,
               e.intensity, e.depth_order, e.ccc_reference, e.saint_quote,
               e.saint_attribution, e.liturgical_season,
               COALESCE(ft.label, f.label) AS feeling_label,
               p.text AS passage_text,
               p.pull_quote AS pull_quote,
               p.reference_display AS reference_display,
               p.reference_alt AS reference_alt,
               (s.entry_id IS NOT NULL) AS is_saved,
               (SELECT MAX(viewed_at) FROM view_history h WHERE h.entry_id = e.id) AS last_viewed
        FROM entry e
        JOIN feeling f ON f.id = e.feeling_id
        LEFT JOIN feeling_translation ft ON ft.feeling_id = f.id AND ft.language = :language
        LEFT JOIN entry_translation et ON et.entry_id = e.id AND et.language = :language
        JOIN entry_passage ep ON ep.entry_id = e.id AND ep.role = 'primary'
        JOIN passage p ON p.id = ep.passage_id
        LEFT JOIN saved_entry s ON s.entry_id = e.id
        WHERE e.feeling_id = :feelingId
        ORDER BY
            last_viewed IS NOT NULL,
            CASE e.intensity WHEN 'acute' THEN 0 WHEN 'steady' THEN 1 ELSE 2 END,
            e.depth_order
        """
    )
    suspend fun entriesForFeeling(feelingId: String, language: String): List<EntrySummary>

    @Query(
        """
        SELECT p.*, ep.role AS role, ep.position AS position
        FROM entry_passage ep
        JOIN passage p ON p.id = ep.passage_id
        WHERE ep.entry_id = :entryId
        ORDER BY ep.position
        """
    )
    suspend fun passagesForEntry(entryId: String): List<PassageWithRole>

    /** Plain LIKE search for the prototype. Swap for FTS4 before production scale. */
    @Query(
        """
        SELECT DISTINCT feeling_id FROM feeling_alias
        WHERE alias LIKE '%' || :query || '%'
        ORDER BY weight DESC
        LIMIT 5
        """
    )
    suspend fun feelingsMatching(query: String): List<String>

    @Query("SELECT passage_id FROM daily_passage WHERE month_day = :monthDay")
    suspend fun passageOfDay(monthDay: String): String?

    @Query("SELECT * FROM passage WHERE id = :id")
    suspend fun passage(id: String): Passage?

    @Insert
    suspend fun save(saved: SavedEntry)

    @Query("DELETE FROM saved_entry WHERE entry_id = :entryId")
    suspend fun unsave(entryId: String)

    @Insert
    suspend fun record(view: ViewHistory)

    @Query(
        """
        SELECT e.id, e.feeling_id,
               COALESCE(et.reflection, e.reflection) AS reflection,
               COALESCE(et.prayer, e.prayer) AS prayer,
               e.intensity, e.depth_order, e.ccc_reference, e.saint_quote,
               e.saint_attribution, e.liturgical_season,
               COALESCE(ft.label, f.label) AS feeling_label,
               p.text AS passage_text,
               p.pull_quote AS pull_quote,
               p.reference_display AS reference_display,
               p.reference_alt AS reference_alt,
               1 AS is_saved,
               (SELECT MAX(viewed_at) FROM view_history h WHERE h.entry_id = e.id) AS last_viewed
        FROM saved_entry s
        JOIN entry e ON e.id = s.entry_id
        JOIN feeling f ON f.id = e.feeling_id
        LEFT JOIN feeling_translation ft ON ft.feeling_id = f.id AND ft.language = :language
        LEFT JOIN entry_translation et ON et.entry_id = e.id AND et.language = :language
        JOIN entry_passage ep ON ep.entry_id = e.id AND ep.role = 'primary'
        JOIN passage p ON p.id = ep.passage_id
        ORDER BY s.saved_at DESC
        """
    )
    fun savedEntries(language: String): Flow<List<EntrySummary>>

    // ---------- Open-ended reading ----------

    /**
     * The translation to read in for a given UI language, e.g. "de" ->
     * "de-schlachter" once a German scripture file is loaded. Null means no
     * scripture has been imported for that language yet; callers fall back
     * to the English translation_id ("web-c") in that case, so the Read tab
     * shows English rather than going blank.
     */
    @Query("SELECT id FROM translation WHERE language = :language LIMIT 1")
    suspend fun translationForLanguage(language: String): String?

    /** Only books with text loaded *for this translation* show up in the reading tab. */
    @Query(
        """
        SELECT b.* FROM book b
        WHERE b.id IN (SELECT DISTINCT book_id FROM scripture_verse WHERE translation_id = :translationId)
        ORDER BY b.sort_order
        """
    )
    fun readableBooks(translationId: String): Flow<List<Book>>

    /** Every known book, for the "go to reference" picker — independent of what's loaded. */
    @Query("SELECT * FROM book ORDER BY sort_order")
    fun allBooks(): Flow<List<Book>>

    @Query(
        "SELECT DISTINCT chapter FROM scripture_verse WHERE book_id = :bookId AND translation_id = :translationId ORDER BY chapter"
    )
    suspend fun chaptersForBook(bookId: String, translationId: String): List<Int>

    @Query(
        "SELECT * FROM scripture_verse WHERE book_id = :bookId AND chapter = :chapter AND translation_id = :translationId ORDER BY verse"
    )
    suspend fun versesForChapter(bookId: String, chapter: Int, translationId: String): List<ScriptureVerse>
}

/** Bulk inserts used once, on first launch, to hydrate the bundled content. */
@Dao
interface SeedDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranslations(items: List<Translation>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(items: List<Book>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPassages(items: List<Passage>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeelings(items: List<Feeling>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAliases(items: List<FeelingAlias>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeelingTranslations(items: List<FeelingTranslation>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(items: List<Entry>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntryTranslations(items: List<EntryTranslation>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntryPassages(items: List<EntryPassage>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyPassages(items: List<DailyPassage>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScriptureVerses(items: List<ScriptureVerse>)
}

@Database(
    entities = [
        Translation::class, Book::class, Passage::class, Feeling::class,
        FeelingAlias::class, FeelingTranslation::class, Entry::class, EntryTranslation::class,
        EntryPassage::class, DailyPassage::class,
        SavedEntry::class, ViewHistory::class, ScriptureVerse::class
    ],
    version = 3,
    exportSchema = false
)
abstract class ContentDatabase : RoomDatabase() {
    abstract fun contentDao(): ContentDao
    abstract fun seedDao(): SeedDao
}
