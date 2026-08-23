package app.pocketbible.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Hydrates the Room database from the bundled assets/seed_content.json on
 * first launch, and again whenever content_version increases (e.g. an app
 * update ships more entries). User data — saved verses, view history — is
 * never touched here.
 */
class SeedLoader(private val context: Context, private val db: ContentDatabase) {

    private val prefs = context.getSharedPreferences("pocketbible_prefs", Context.MODE_PRIVATE)

    suspend fun seedIfNeeded(assetName: String = "seed_content.json") {
        val json = JSONObject(
            context.assets.open(assetName).bufferedReader().use { it.readText() }
        )
        val version = json.optInt("content_version", 1)
        if (prefs.getInt("content_version", -1) == version) return

        val seedDao = db.seedDao()

        seedDao.insertTranslations(json.getJSONArray("translations").mapObjects { o ->
            Translation(
                id = o.getString("id"),
                name = o.getString("name"),
                abbreviation = o.getString("abbreviation"),
                license = o.getString("license"),
                versification = o.getString("versification"),
                includesDeuterocanon = o.getBoolean("includes_deuterocanon"),
                hasImprimatur = o.optBoolean("has_imprimatur", false),
                language = o.getString("language")
            )
        })

        seedDao.insertBooks(
            json.optJSONArray("books")?.mapObjects { o ->
                Book(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    displayName = o.getString("display_name"),
                    testament = o.getString("testament"),
                    isDeuterocanonical = o.optBoolean("is_deuterocanonical", false),
                    sortOrder = o.optInt("sort_order", 0)
                )
            } ?: emptyList()
        )

        seedDao.insertPassages(json.getJSONArray("passages").mapObjects { o ->
            Passage(
                id = o.getString("id"),
                translationId = o.getString("translation_id"),
                bookId = o.getString("book_id"),
                chapterStart = o.getInt("chapter_start"),
                chapterEnd = o.optInt("chapter_end", o.getInt("chapter_start")),
                verseStart = o.getInt("verse_start"),
                verseEnd = o.getInt("verse_end"),
                text = o.getString("text"),
                pullQuote = o.optStringOrNull("pull_quote"),
                referenceDisplay = o.getString("reference_display"),
                referenceAlt = o.optStringOrNull("reference_alt")
            )
        })

        val feelings = mutableListOf<Feeling>()
        val aliases = mutableListOf<FeelingAlias>()
        val feelingArray = json.getJSONArray("feelings")
        for (i in 0 until feelingArray.length()) {
            val o = feelingArray.getJSONObject(i)
            feelings += Feeling(
                id = o.getString("id"),
                label = o.getString("label"),
                icon = o.getString("icon"),
                category = o.getString("category"),
                description = o.getString("description"),
                sortOrder = o.optInt("sort_order", 0)
            )
            val aliasArray = o.optJSONArray("aliases") ?: JSONArray()
            for (j in 0 until aliasArray.length()) {
                val a = aliasArray.getJSONObject(j)
                aliases += FeelingAlias(
                    feelingId = o.getString("id"),
                    alias = a.getString("alias"),
                    weight = a.optDouble("weight", 1.0).toFloat()
                )
            }
        }
        seedDao.insertFeelings(feelings)
        seedDao.insertAliases(aliases)

        seedDao.insertEntries(json.getJSONArray("entries").mapObjects { o ->
            Entry(
                id = o.getString("id"),
                feelingId = o.getString("feeling_id"),
                reflection = o.getString("reflection"),
                prayer = o.getString("prayer"),
                intensity = o.getString("intensity"),
                depthOrder = o.optInt("depth_order", 0),
                cccReference = o.optStringOrNull("ccc_reference"),
                saintQuote = o.optStringOrNull("saint_quote"),
                saintAttribution = o.optStringOrNull("saint_attribution"),
                liturgicalSeason = o.optStringOrNull("liturgical_season")
            )
        })

        seedDao.insertEntryPassages(json.getJSONArray("entry_passages").mapObjects { o ->
            EntryPassage(
                entryId = o.getString("entry_id"),
                passageId = o.getString("passage_id"),
                position = o.optInt("position", 0),
                role = o.getString("role")
            )
        })

        seedDao.insertDailyPassages(
            json.optJSONArray("daily_passages")?.mapObjects { o ->
                DailyPassage(
                    monthDay = o.getString("month_day"),
                    passageId = o.getString("passage_id")
                )
            } ?: emptyList()
        )

        seedDao.insertScriptureVerses(
            json.optJSONArray("scripture")?.mapObjects { o ->
                ScriptureVerse(
                    id = o.getString("id"),
                    translationId = o.getString("translation_id"),
                    bookId = o.getString("book_id"),
                    chapter = o.getInt("chapter"),
                    verse = o.getInt("verse"),
                    text = o.getString("text")
                )
            } ?: emptyList()
        )

        prefs.edit().putInt("content_version", version).apply()
    }
}

private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
    (0 until length()).map { transform(getJSONObject(it)) }

private fun JSONObject.optStringOrNull(key: String): String? =
    if (has(key) && !isNull(key)) getString(key) else null
