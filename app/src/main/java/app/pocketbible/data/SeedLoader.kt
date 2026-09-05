package app.pocketbible.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Hydrates the Room database from the bundled assets/content/ tree on first
 * launch, and again whenever content_version increases (e.g. an app update
 * ships more entries). User data -- saved verses, view history -- is never
 * touched here.
 *
 * Content is split into modules, indexed by content/manifest.json:
 *   - core.json           translations + the book canon (structural, rarely changes)
 *   - topics.json         feelings/aliases/entries/passages/entry_passages/daily_passages,
 *                         all in English -- the base/fallback content
 *   - topics/<language>.json   translated label/description/reflection/prayer for
 *                         one UI language; any topic or entry missing from it
 *                         just falls back to English, so a language can ship
 *                         partial and grow over time
 *   - scripture/<translation_id>/<book_id>.json   one file per book per translation
 *   - characters.json     Characters tab: name/intro/category per figure, plus verse_refs
 *                         (book/chapter/verse citations only -- the verse text itself is
 *                         looked up live from scripture_verse at render time, same as the
 *                         Read tab, never duplicated here), plus a character_of_day array
 *                         (month_day -> character_id, same shape as topics.json's
 *                         daily_passages -- several days can point at the same character)
 *   - character_translation entries, same fallback-to-English pattern as topics
 *   - lectionary/<year>.json  Daily Mass reading citations for one calendar year
 *                         (date -> first_reading/psalm/second_reading/gospel
 *                         citations, plus an optional reflection) -- see
 *                         tools/fetch_lectionary.py and tools/parse_lectionary.py
 *                         for where the citations come from. Citations only,
 *                         same live-resolved-from-scripture_verse model as
 *                         character verse_refs.
 *   - stories.json        Stories tab: title/testament/book_group/story_type/
 *                         summary/moral/reflection per story, plus verse_refs
 *                         (citations only, same live-resolved model as
 *                         character verse_refs -- chapter_start/chapter_end
 *                         since a story can span more than one chapter) and
 *                         an optional character_ids array (curated links to
 *                         the Characters tab, capped at 10 per character).
 *   - story_translation entries, same fallback-to-English pattern as
 *                         character_translation
 *
 * Adding a book or a new translation/language is meant to be a matter of
 * dropping a new scripture/<translation_id>/<book_id>.json file (see
 * tools/import_scripture.py) and adding one line to manifest.json's
 * "scripture" list; adding or extending topic translations is the same
 * pattern via topics/<language>.json and the "topic_translations" list.
 * No other module needs to change either way.
 */
class SeedLoader(private val context: Context, private val db: ContentDatabase) {

    private val prefs = context.getSharedPreferences("pocketbible_prefs", Context.MODE_PRIVATE)

    suspend fun seedIfNeeded(manifestPath: String = "content/manifest.json") {
        val manifest = readJson(manifestPath)
        val version = manifest.optInt("content_version", 1)
        if (prefs.getInt("content_version", -1) == version) return

        val seedDao = db.seedDao()

        val core = readJson(manifest.getString("core"))
        seedDao.insertTranslations(core.getJSONArray("translations").mapObjects { o ->
            Translation(
                id = o.getString("id"),
                name = o.getString("name"),
                abbreviation = o.getString("abbreviation"),
                license = o.getString("license"),
                versification = o.getString("versification"),
                includesDeuterocanon = o.getBoolean("includes_deuterocanon"),
                hasImprimatur = o.optBoolean("has_imprimatur", false),
                language = o.getString("language"),
                sourceName = o.optStringOrNull("source_name"),
                sourceUrl = o.optStringOrNull("source_url"),
                licenseUrl = o.optStringOrNull("license_url")
            )
        })
        seedDao.insertBooks(
            core.optJSONArray("books")?.mapObjects { o ->
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

        val topics = readJson(manifest.getString("topics"))
        seedDao.insertPassages(topics.getJSONArray("passages").mapObjects { o ->
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
        val feelingArray = topics.getJSONArray("feelings")
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

        val feelingTranslations = mutableListOf<FeelingTranslation>()
        val entryTranslations = mutableListOf<EntryTranslation>()
        val translationFiles = manifest.optJSONArray("topic_translations") ?: JSONArray()
        for (i in 0 until translationFiles.length()) {
            val ref = translationFiles.getJSONObject(i)
            val file = readJson(ref.getString("path"))
            val language = file.getString("language")
            file.optJSONArray("feeling_translations")?.mapObjects { o ->
                FeelingTranslation(
                    feelingId = o.getString("feeling_id"),
                    language = language,
                    label = o.getString("label"),
                    description = o.getString("description")
                )
            }?.let { feelingTranslations += it }
            file.optJSONArray("entry_translations")?.mapObjects { o ->
                EntryTranslation(
                    entryId = o.getString("entry_id"),
                    language = language,
                    reflection = o.getString("reflection"),
                    prayer = o.getString("prayer")
                )
            }?.let { entryTranslations += it }
        }
        seedDao.insertFeelingTranslations(feelingTranslations)
        seedDao.insertEntryTranslations(entryTranslations)

        seedDao.insertEntries(topics.getJSONArray("entries").mapObjects { o ->
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

        seedDao.insertEntryPassages(topics.getJSONArray("entry_passages").mapObjects { o ->
            EntryPassage(
                entryId = o.getString("entry_id"),
                passageId = o.getString("passage_id"),
                position = o.optInt("position", 0),
                role = o.getString("role")
            )
        })

        seedDao.insertDailyPassages(
            topics.optJSONArray("daily_passages")?.mapObjects { o ->
                DailyPassage(
                    monthDay = o.getString("month_day"),
                    passageId = o.getString("passage_id")
                )
            } ?: emptyList()
        )

        val scriptureVerses = mutableListOf<ScriptureVerse>()
        val scriptureFiles = manifest.optJSONArray("scripture") ?: JSONArray()
        for (i in 0 until scriptureFiles.length()) {
            val ref = scriptureFiles.getJSONObject(i)
            val book = readJson(ref.getString("path"))
            val translationId = book.getString("translation_id")
            val bookId = book.getString("book_id")
            val verses = book.optJSONArray("verses") ?: JSONArray()
            for (j in 0 until verses.length()) {
                val v = verses.getJSONObject(j)
                val chapter = v.getInt("chapter")
                val verse = v.getInt("verse")
                scriptureVerses += ScriptureVerse(
                    id = "$translationId:$bookId:$chapter:$verse",
                    translationId = translationId,
                    bookId = bookId,
                    chapter = chapter,
                    verse = verse,
                    text = v.getString("text")
                )
            }
        }
        seedDao.insertScriptureVerses(scriptureVerses)

        manifest.optString("characters", "").takeIf { it.isNotEmpty() }?.let { path ->
            val characters = readJson(path)
            seedDao.insertCharacters(characters.getJSONArray("characters").mapObjects { o ->
                BibleCharacter(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    intro = o.getString("intro"),
                    category = o.getString("category"),
                    sortOrder = o.optInt("sort_order", 0),
                    requiresDeuterocanon = o.optBoolean("requires_deuterocanon", false),
                    reflection = if (o.has("reflection")) o.getString("reflection") else null,
                    prayer = if (o.has("prayer")) o.getString("prayer") else null
                )
            })

            val verseRefs = mutableListOf<CharacterVerseRef>()
            val characterArray = characters.getJSONArray("characters")
            for (i in 0 until characterArray.length()) {
                val o = characterArray.getJSONObject(i)
                val characterId = o.getString("id")
                val refs = o.optJSONArray("verse_refs") ?: JSONArray()
                for (j in 0 until refs.length()) {
                    val r = refs.getJSONObject(j)
                    verseRefs += CharacterVerseRef(
                        characterId = characterId,
                        bookId = r.getString("book_id"),
                        chapter = r.getInt("chapter"),
                        verseStart = r.getInt("verse_start"),
                        verseEnd = r.optInt("verse_end", r.getInt("verse_start")),
                        caption = r.getString("caption"),
                        position = j
                    )
                }
            }
            seedDao.clearCharacterVerseRefs()
            seedDao.insertCharacterVerseRefs(verseRefs)

            val characterTranslations = mutableListOf<CharacterTranslation>()
            val translationFiles = manifest.optJSONArray("character_translations") ?: JSONArray()
            for (i in 0 until translationFiles.length()) {
                val ref = translationFiles.getJSONObject(i)
                val file = readJson(ref.getString("path"))
                val language = file.getString("language")
                file.optJSONArray("character_translations")?.mapObjects { o ->
                    CharacterTranslation(
                        characterId = o.getString("character_id"),
                        language = language,
                        name = o.getString("name"),
                        intro = o.getString("intro")
                    )
                }?.let { characterTranslations += it }
            }
            seedDao.insertCharacterTranslations(characterTranslations)

            val captionTranslations = mutableListOf<CharacterVerseRefTranslation>()
            val captionFiles = manifest.optJSONArray("character_verse_ref_translations") ?: JSONArray()
            for (i in 0 until captionFiles.length()) {
                val ref = captionFiles.getJSONObject(i)
                val file = readJson(ref.getString("path"))
                val language = file.getString("language")
                file.optJSONArray("character_verse_ref_translations")?.mapObjects { o ->
                    CharacterVerseRefTranslation(
                        characterId = o.getString("character_id"),
                        position = o.getInt("position"),
                        language = language,
                        caption = o.getString("caption")
                    )
                }?.let { captionTranslations += it }
            }
            seedDao.insertCharacterVerseRefTranslations(captionTranslations)

            seedDao.insertCharacterOfDay(
                characters.optJSONArray("character_of_day")?.mapObjects { o ->
                    CharacterOfDay(
                        monthDay = o.getString("month_day"),
                        characterId = o.getString("character_id")
                    )
                } ?: emptyList()
            )
        }

        val dailyReadings = mutableListOf<DailyReading>()
        val readingCitations = mutableListOf<ReadingCitation>()
        val lectionaryFiles = manifest.optJSONArray("lectionary") ?: JSONArray()
        for (i in 0 until lectionaryFiles.length()) {
            val ref = lectionaryFiles.getJSONObject(i)
            val year = readJson(ref.getString("path"))
            val days = year.optJSONArray("days") ?: JSONArray()
            for (j in 0 until days.length()) {
                val day = days.getJSONObject(j)
                val date = day.getString("date")
                dailyReadings += DailyReading(
                    date = date,
                    season = day.getString("season"),
                    usccbLink = day.getString("usccb_link"),
                    reflection = if (day.has("reflection")) day.getString("reflection") else null
                )
                val readings = day.optJSONArray("readings") ?: JSONArray()
                for (k in 0 until readings.length()) {
                    val reading = readings.getJSONObject(k)
                    val role = reading.getString("role")
                    val citationDisplay = reading.getString("citation_display")
                    val refs = reading.optJSONArray("refs") ?: JSONArray()
                    for (p in 0 until refs.length()) {
                        val r = refs.getJSONObject(p)
                        readingCitations += ReadingCitation(
                            date = date,
                            role = role,
                            citationDisplay = citationDisplay,
                            bookId = r.getString("book_id"),
                            chapterStart = r.getInt("chapter_start"),
                            verseStart = r.getInt("verse_start"),
                            chapterEnd = r.getInt("chapter_end"),
                            verseEnd = r.getInt("verse_end"),
                            position = p
                        )
                    }
                }
            }
        }
        seedDao.insertDailyReadings(dailyReadings)
        seedDao.clearReadingCitations()
        seedDao.insertReadingCitations(readingCitations)

        manifest.optString("stories", "").takeIf { it.isNotEmpty() }?.let { path ->
            val storiesFile = readJson(path)
            val storyArray = storiesFile.getJSONArray("stories")
            seedDao.insertStories(storyArray.mapObjects { o ->
                Story(
                    id = o.getString("id"),
                    title = o.getString("title"),
                    testament = o.getString("testament"),
                    bookGroup = o.getString("book_group"),
                    storyType = o.getString("story_type"),
                    summary = o.getString("summary"),
                    moral = o.getString("moral"),
                    reflection = o.getString("reflection"),
                    sortOrder = o.optInt("sort_order", 0)
                )
            })

            val storyVerseRefs = mutableListOf<StoryVerseRef>()
            val storyCharacterLinks = mutableListOf<StoryCharacterLink>()
            for (i in 0 until storyArray.length()) {
                val o = storyArray.getJSONObject(i)
                val storyId = o.getString("id")
                val refs = o.optJSONArray("verse_refs") ?: JSONArray()
                for (j in 0 until refs.length()) {
                    val r = refs.getJSONObject(j)
                    storyVerseRefs += StoryVerseRef(
                        storyId = storyId,
                        bookId = r.getString("book_id"),
                        chapterStart = r.getInt("chapter_start"),
                        verseStart = r.getInt("verse_start"),
                        chapterEnd = r.optInt("chapter_end", r.getInt("chapter_start")),
                        verseEnd = r.getInt("verse_end"),
                        position = j
                    )
                }
                val characterIds = o.optJSONArray("character_ids") ?: JSONArray()
                for (j in 0 until characterIds.length()) {
                    storyCharacterLinks += StoryCharacterLink(
                        storyId = storyId,
                        characterId = characterIds.getString(j)
                    )
                }
            }
            seedDao.clearStoryVerseRefs()
            seedDao.insertStoryVerseRefs(storyVerseRefs)
            seedDao.clearStoryCharacterLinks()
            seedDao.insertStoryCharacterLinks(storyCharacterLinks)

            val storyTranslations = mutableListOf<StoryTranslation>()
            val storyTranslationFiles = manifest.optJSONArray("story_translations") ?: JSONArray()
            for (i in 0 until storyTranslationFiles.length()) {
                val ref = storyTranslationFiles.getJSONObject(i)
                val file = readJson(ref.getString("path"))
                val language = file.getString("language")
                file.optJSONArray("story_translations")?.mapObjects { o ->
                    StoryTranslation(
                        storyId = o.getString("story_id"),
                        language = language,
                        title = o.getString("title"),
                        summary = o.getString("summary"),
                        moral = o.getString("moral"),
                        reflection = o.getString("reflection")
                    )
                }?.let { storyTranslations += it }
            }
            seedDao.insertStoryTranslations(storyTranslations)
        }

        prefs.edit().putInt("content_version", version).apply()
    }

    private fun readJson(assetPath: String): JSONObject =
        JSONObject(context.assets.open(assetPath).bufferedReader().use { it.readText() })
}

private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
    (0 until length()).map { transform(getJSONObject(it)) }

private fun JSONObject.optStringOrNull(key: String): String? =
    if (has(key) && !isNull(key)) getString(key) else null
