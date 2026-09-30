# Pocket Bible

## About

**Pocket Bible** is an offline Catholic Android app for reading Scripture and finding guidance for the moment. Choose a feeling such as fear, doubt, anxiety, loss, or joy to find a related passage, reflection, and prayer. Version 2 adds a daily reading experience, a larger character library, and a browsable collection of Bible stories and parables.


**Features:**
- 📖 The full 73-book Catholic canon in English, plus Scripture translations in German, French, Hindi, Italian, Marathi, and Portuguese; book coverage varies by translation
- 🌍 Interface in English, German, French, Portuguese, Spanish, Hindi, Italian, and Marathi (Spanish currently has no bundled Bible-text translation)
- ❤️ Curated feelings-to-passages mapping with reflections and prayers
- 📅 Verse of the Day and daily Catholic readings with reflections
- 👥 280 biblical character profiles with introductions and Scripture references
- 📚 146 Bible stories and parables with Scripture references, summaries, and reflections
- 🔖 Bookmark stories and save favorite verses
- ✝️ Fully offline—no network required
- 🔍 Search topics, characters, and stories
- 🎨 Material3 design with category-specific accent colors

---

## What's here

```
app/src/main/java/app/pocketbible/
  MainActivity.kt        Nav host + bottom bar (Topics / Bible / Readings & Reflection / Characters / Stories)
  PocketBibleApp.kt       Application class — owns the DB + repository
  data/
    ContentModel.kt       Room entities, DAOs, database (passage-based schema)
    SeedLoader.kt          Reads assets/content/ (see below) into Room on first launch
    ContentRepository.kt   Thin layer between Room and the ViewModel
  ui/
    MainViewModel.kt       Topics, daily content, characters, stories, saved items, Bible reading
    home/HomeScreen.kt      Topics grid, search, and language switcher
    verse/VerseScreen.kt    Passage + reflection + prayer + save/another
    saved/SavedScreen.kt    Saved list
    bible/BibleScreen.kt    Book list → chapter list → chapter reader (Read tab)
    reading/DailyReadingScreen.kt  Verse of the Day and daily Catholic readings
    characters/             Searchable biblical character directory and profiles
    stories/                Searchable Bible stories, filters, and saved stories
    bible/BookNames.kt      Book-id → localized-name lookup (names only, not scripture text)
    theme/Theme.kt          Material3 color scheme + per-category accent colors
app/src/main/assets/content/            Bundled content, see "Content layout" below
tools/import_scripture.py               Converts a real Bible text dump into per-book scripture files
```

The five main tabs are Topics, Bible, Readings & Reflection, Characters,
and Stories. All content is bundled locally; Bible passages are resolved
from the selected translation when available.

## Running it

You'll need **JDK 17**, **Android SDK Platform 36**, and an emulator or
device running API 26 or newer. From the project root, build with
`./gradlew assembleDebug`, or open the project in Android Studio and run the
app. On first launch, the app seeds its local database from
`app/src/main/assets/content/`.

## Try this

- Search the **Topics** tab by feeling or phrase (try "burned out" or
  "cant forgive"), then open a topic for related passages, reflections,
  and prayers.
- Open **Readings & Reflection** for the Verse of the Day and date-based Catholic readings
  with reflections.
- Browse **Characters** to search 280 biblical profiles and open their
  introductions and Scripture references.
- Browse **Stories** to search 146 stories and parables; filter by
  testament or story type, then save stories to revisit them.
- In **Bible**, browse by book and chapter or use "Go to a verse" to jump
  directly to a reference. Use the language menu to switch among available
  Bible translations.
- Save favorite verses and bookmark stories for later.

## Content layout

Everything the app ships is bundled offline under `app/src/main/assets/content/`
and indexed by `content/manifest.json`, which `SeedLoader` reads first:

```
content/
  manifest.json                   Index: content_version + paths to every module below
  core.json                       Translation metadata + the 73-book Catholic canon
  topics.json                     Feelings/aliases/entries/passages/entry_passages/daily_passages,
                                   all English -- the base content and the fallback for any
                                   language/topic combination not yet translated
  topics/
    <language>.json               Translated topic labels, descriptions, reflections, and prayers
  characters.json                 Character profiles and Scripture references
  character_translations/         Translated character content
  stories.json                    Bible stories and parables
  stories/                        Translated story content
  lectionary/                     Daily Catholic reading citations and reflections by year
  scripture/
    <translation-id>/             One JSON file per available book and translation
```

Bundled Bible text is available in English (World English Bible Classic),
German (Schlachter 1951), French (Sainte Bible libre pour le monde), Hindi
(Indian Revised Version), Italian (Riveduta 1927), Marathi (Indian Revised
Version), and Portuguese (Almeida Atualizada). Coverage varies by
translation, and the Read tab shows the books actually present in the
selected translation. When a UI language has no bundled scripture
translation, the app uses English; Spanish currently applies to the
interface only.

The point of splitting it this way: adding a book, or a whole new
translation/language, is dropping one new file under `scripture/` and
adding one line to `manifest.json` — `core.json`, `topics.json`, and every
other scripture file are untouched. Each scripture file carries its own
`source`, `license`, `language`, and `verified` fields, so provenance is
visible right next to the text instead of buried in a commit message.

Bible text does not change once it's imported (it's a fixed historical
text, not a value that goes stale), so once a book is in here it's good
indefinitely — there's no "update" story to build, just an "add more"
one.

### Scaling the Read tab to the full Bible (or a new language)

The Read tab only shows books that actually have a file under
`content/scripture/<translation-id>/` — that's deliberate. Importing more
is a **data change**, not a code change: drop in more files and the
book/chapter list updates itself with no Kotlin to touch.

What *not* to do: don't hand-author or ask an LLM to generate Bible text
from memory, in English or any other language. At Bible scale, small
wording drift becomes a real accuracy problem, and it's avoidable — real,
checked source text exists and is free for many languages.

The real path — two import scripts, same idea, pick whichever matches the
format your download comes in:

1. Get a bundle for the translation you want from
   [ebible.org](https://ebible.org/find/) (search by language; check the
   license on the translation's own page) or another source you can vouch
   for.
2. If it's an **epub** (eBible.org offers these for most of its
   translations): run `tools/import_epub.py` against it directly — see its
   docstring for the `--book-map`/`--translation-id`/`--language`/`--source`
   flags. It parses the book-per-chapter/verse markup epub readers already
   rely on, so there's no intermediate conversion step. This is how the
   current English text (World English Bible Classic, all 73 books) was
   imported.
   If it's **plain-text or USFM** instead: convert USFM to VPL first (e.g.
   `usfm-grammar`), then run `tools/import_scripture.py` (see its own
   docstring for the VPL format and a `book_map.json` example).
   If the source renders the divine name as "Yahweh"/"Yah" and you want
   "the LORD" instead, run `tools/apply_lord_rendering.py` against the
   output directory afterward — a separate, explicit pass so the change
   is visible on its own, not folded silently into the import step.
3. Either script writes one `content/scripture/<translation-id>/<book-id>.json`
   file per book — no Bible text lives in the scripts themselves, just the
   conversion logic — and prints the `manifest.json` lines to add.
4. Add those lines to `content/manifest.json`'s `"scripture"` array, bump
   `content_version`, rebuild.

You can do this incrementally — Psalms and the Gospels first, the rest
later, one language at a time — since the book map controls exactly what
gets imported per run.

### Adding a new UI language

Three layers respond to the in-app language switcher (the button on the
Topics tab, backed by `AppCompatDelegate.setApplicationLocales` — MainActivity
is an `AppCompatActivity` specifically so `recreate()` actually reloads
resources in the new locale, not just persists the choice):

1. **App chrome** — nav labels, buttons, prompts. Fully resource-driven:
    `values-de/`, `values-es/`, `values-fr/`, `values-hi/`, `values-it/`,
    `values-mr/`, and `values-pt/` under `app/src/main/res/`. A new language
    is a new `values-<lang>/strings.xml` with the same keys, no code changes.
2. **Book names** — just names ("Psalms", "Luke"), not scripture text.
   `BookNames.kt` maps each book id to a translated `R.string` per
   language, falling back to the bundled English name for any book id
   without one.
3. **Topics content** — feeling label/description, entry reflection/prayer.
   Bundled data (Room, seeded from `content/topics/<language>.json`), not
   string resources, translated per the file layout above with the same
   fallback-to-English pattern via `feeling_translation`/`entry_translation`
   tables (`ContentModel.kt`) that `MainViewModel.ensureFreshForCurrentLanguage()`
   re-queries whenever the language changes.
4. **Read tab scripture** — which `translation_id` the Read tab shows is
   resolved from the current language via `ContentDao.translationForLanguage()`
   (`SELECT id FROM translation WHERE language = :language`), falling back
   to `web-c` (English) if that language has no scripture file loaded yet.
   `readableBooks`/`chaptersForBook`/`versesForChapter` all filter by that
   translation_id — this matters once more than one translation's verses
   share the `scripture_verse` table, so a chapter view never mixes verses
   from two languages together. Importing a translation via the scripts
   above is what actually makes a language's Read tab show real text; the
    fallback keeps the Read tab usable when no scripture translation exists
    for the selected UI language. English, German, French, Hindi, Italian,
    Marathi, and Portuguese translations are bundled with differing book
    coverage; Spanish UI users currently read from the English translation.

Layers 1 and 2 are safe to translate freely — UI vocabulary and proper
nouns, not scripture. Layer 3 is *my own* devotional prose (not scripture),
so it's translatable the same way — that's what `content/topics/de.json`
is. What still doesn't get machine-translated, deliberately, is **scripture
text itself** (including the Psalms already loaded): different languages
have specific trusted translations (Luther, Segond, Almeida, Reina-Valera),
and an ad-hoc translation wouldn't be any of those — see the scripture
workflow above for the real path.
