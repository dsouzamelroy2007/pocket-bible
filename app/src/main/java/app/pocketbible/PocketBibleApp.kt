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

class PocketBibleApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: ContentDatabase by lazy {
        Room.databaseBuilder(this, ContentDatabase::class.java, "pocketbible.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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
