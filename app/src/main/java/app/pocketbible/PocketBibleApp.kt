package app.pocketbible

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import app.pocketbible.data.ContentDatabase
import app.pocketbible.data.ContentRepository
import app.pocketbible.data.SeedLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Adds the scripture_verse table (Read tab) without touching existing data. */
private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `scripture_verse` (
                `id` TEXT NOT NULL,
                `translation_id` TEXT NOT NULL,
                `book_id` TEXT NOT NULL,
                `chapter` INTEGER NOT NULL,
                `verse` INTEGER NOT NULL,
                `text` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_scripture_verse_book_id_chapter` " +
                "ON `scripture_verse` (`book_id`, `chapter`)"
        )
    }
}

/** Adds feeling_translation/entry_translation tables for the in-app language switcher. */
private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `feeling_translation` (
                `feeling_id` TEXT NOT NULL,
                `language` TEXT NOT NULL,
                `label` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                PRIMARY KEY(`feeling_id`, `language`)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `entry_translation` (
                `entry_id` TEXT NOT NULL,
                `language` TEXT NOT NULL,
                `reflection` TEXT NOT NULL,
                `prayer` TEXT NOT NULL,
                PRIMARY KEY(`entry_id`, `language`)
            )
            """.trimIndent()
        )
    }
}

/** Adds the character/character_translation/character_verse_ref tables for the Characters tab. */
private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `character` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `intro` TEXT NOT NULL,
                `category` TEXT NOT NULL,
                `sort_order` INTEGER NOT NULL,
                `requires_deuterocanon` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `character_translation` (
                `character_id` TEXT NOT NULL,
                `language` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `intro` TEXT NOT NULL,
                PRIMARY KEY(`character_id`, `language`)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `character_verse_ref` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `character_id` TEXT NOT NULL,
                `book_id` TEXT NOT NULL,
                `chapter` INTEGER NOT NULL,
                `verse_start` INTEGER NOT NULL,
                `verse_end` INTEGER NOT NULL,
                `caption` TEXT NOT NULL,
                `position` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_character_verse_ref_character_id` " +
                "ON `character_verse_ref` (`character_id`)"
        )
    }
}

/** Adds the bible_bookmark table for the Bible tab's "continue reading" bookmarks. */
private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `bible_bookmark` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `translation_id` TEXT NOT NULL,
                `book_id` TEXT NOT NULL,
                `chapter` INTEGER NOT NULL,
                `verse` INTEGER,
                `created_at` INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}

/** Adds the character_verse_ref_translation table for translated verse-citation captions. */
private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `character_verse_ref_translation` (
                `character_id` TEXT NOT NULL,
                `position` INTEGER NOT NULL,
                `language` TEXT NOT NULL,
                `caption` TEXT NOT NULL,
                PRIMARY KEY(`character_id`, `position`, `language`)
            )
            """.trimIndent()
        )
    }
}

private val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE translation ADD COLUMN source_name TEXT")
        db.execSQL("ALTER TABLE translation ADD COLUMN source_url TEXT")
        db.execSQL("ALTER TABLE translation ADD COLUMN license_url TEXT")
    }
}

class PocketBibleApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: ContentDatabase by lazy {
        Room.databaseBuilder(this, ContentDatabase::class.java, "pocketbible.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
            // Covers any *future* schema change that doesn't get a real
            // Migration written for it — still prototype-stage safety net,
            // not a substitute for writing migrations as the schema grows.
            .fallbackToDestructiveMigration()
            .build()
    }

    val repository: ContentRepository by lazy { ContentRepository(database.contentDao()) }

    override fun onCreate() {
        super.onCreate()
        // Room's Flow queries re-emit automatically once this finishes, so the
        // UI doesn't need to wait on it explicitly — the feelings grid just
        // fills in a moment after first launch.
        appScope.launch {
            SeedLoader(this@PocketBibleApp, database).seedIfNeeded()
        }
    }
}
