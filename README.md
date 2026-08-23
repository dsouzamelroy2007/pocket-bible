# Pocket Bible — prototype

A Catholic pocket-Bible Android app: pick how you're feeling, get a verse,
a plain-language reflection, and a short prayer — fully offline. This is
the **prototype**: real, runnable source, built to validate the concept on
a phone before investing in production polish.

## What's here

```
app/src/main/java/app/pocketbible/
  MainActivity.kt        Nav host + bottom bar (Topics / Read / Saved) + language switcher wiring
  PocketBibleApp.kt       Application class — owns the DB + repository
  data/
    ContentModel.kt       Room entities, DAOs, database (passage-based schema)
    SeedLoader.kt          Reads assets/content/ (see below) into Room on first launch
    ContentRepository.kt   Thin layer between Room and the ViewModel
  ui/
    MainViewModel.kt       Topics, current entry cycle, saved verses, Bible reading state
    home/HomeScreen.kt      Topics grid, search, verse of the day, language switcher
    verse/VerseScreen.kt    Passage + reflection + prayer + save/another
    saved/SavedScreen.kt    Saved list
    bible/BibleScreen.kt    Book list → chapter list → chapter reader (Read tab)
    bible/BookNames.kt      Book-id → localized-name lookup (names only, not scripture text)
    theme/Theme.kt          Material3 color scheme + per-category accent colors
app/src/main/assets/content/            Bundled content, see "Content layout" below
tools/import_scripture.py               Converts a real Bible text dump into per-book scripture files
```

The three screens match the wireframe from our design pass. The data model
follows the schema we agreed on: passages (not single verses), an
`entry_passage` join table so an entry can eventually cite more than one
passage, and `intensity` ordering so an acute entry surfaces before a
reflective one.

## Running it

You'll need **Android Studio** (Koala or newer) with an SDK for API 34 and
an emulator or device on API 26+. I built this project's source directly —
I don't have network access or an Android SDK in this environment, so I
haven't compiled it myself. It's written carefully and the data model is
validated, but treat the first build as the real test.

1. Unzip and open the `PocketBibleApp` folder in Android Studio — **Open**,
   not **Import**.
2. Let Gradle sync. If it asks to create the Gradle wrapper, accept —
   `gradle-wrapper.properties` is included but the wrapper jar itself isn't
   (I can't fetch binaries), so Android Studio will generate it on first
   sync.
3. Run on an emulator or device. First launch seeds the database from
   `assets/content/` (see "Content layout" below); the topics grid fills in
   within a second or two.

If Gradle sync fails, the most likely culprit is a version mismatch (AGP
8.5.0 / Kotlin 1.9.24 / Compose compiler 1.5.14) — Android Studio's
"Upgrade Assistant" will offer compatible versions if so.

## Try this

- **Search** on the Topics tab matches free text against topic aliases
  (try "burned out" or "cant forgive") and jumps straight to a matching
  topic.
- **Verse of the day** on the Topics tab pulls from `daily_passage`,
  cycling through the curated passages across all 366 days — see the
  caveat about this in `content/topics.json`'s `_note`.
- Tap a feeling → passage, reflection, prayer. **Another** cycles entries;
  **Save** persists to Room.
- The **fear → Isaiah 41:10** entry now also shows an echo passage
  (Psalm 27:1) underneath — the multi-passage capability from the data
  model is live, not just schema.
- **Read tab → "Go to a verse"** — pick any book, type a chapter (and
  optionally a verse), tap Go. If that chapter is loaded, it opens and
  scrolls straight to the verse, which is highlighted. If it isn't loaded,
  you get a plain-language message instead of a blank screen or a crash.
  The book picker lists every book in the Catholic canon already seeded
  into the `book` table — not just the ones with text — so it's ready for
  more content without UI changes.
- Read tab also still supports plain browsing: book → chapter grid →
  reader, with Previous/Next.

## Content layout

Everything the app ships is bundled offline under `app/src/main/assets/content/`
and indexed by `content/manifest.json`, which `SeedLoader` reads first:

```
content/
  manifest.json                   Index: content_version + paths to every module below
  core.json                       Translations + the 73-book Catholic canon (names only)
  topics.json                     Feelings/aliases/entries/passages/entry_passages/daily_passages,
                                   all English -- the base content and the fallback for any
                                   language/topic combination not yet translated
  topics/
    de.json                       Translated label/description/reflection/prayer for German;
                                   any topic or entry missing from it falls back to English
    <other-language>.json         Same shape, one file per additional UI language
  scripture/
    web-c/
      ps.json                     One file per book per translation
      ... (add more books here)
    <other-translation-id>/       A future scripture translation/language gets its own folder
```

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

The real path:

1. Download a plain-text or USFM bundle for the translation you want from
   [ebible.org](https://ebible.org/find/) (English WEB-CE:
   [eng-web-c](https://ebible.org/find/details.php?id=engwebc)) or another
   source you can vouch for.
2. Run `tools/import_scripture.py` against it (see the script's own
   docstring for the expected input format, a `book_map.json` example, and
   the `--translation-id`/`--language`/`--source` flags). It writes one
   `content/scripture/<translation-id>/<book-id>.json` file per book — it
   contains no Bible text itself, just the conversion logic — and prints
   the `manifest.json` lines to add.
3. Add those lines to `content/manifest.json`'s `"scripture"` array, bump
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
   `values-de/`, `values-fr/`, `values-pt/`, `values-es/`, `values-hi/`
   under `app/src/main/res/`. A new language is a new `values-<lang>/strings.xml`
   with the same keys, no code changes.
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

Layers 1 and 2 are safe to translate freely — UI vocabulary and proper
nouns, not scripture. Layer 3 is *my own* devotional prose (not scripture),
so it's translatable the same way — that's what `content/topics/de.json`
is. What still doesn't get machine-translated, deliberately, is **scripture
text itself** (including the Psalms already loaded): different languages
have specific trusted translations (Luther, Segond, Almeida, Reina-Valera),
and an ad-hoc translation wouldn't be any of those — see the scripture
workflow above for the real path.

## What's intentionally left for production

This is a prototype, so several things are simplified on purpose:

- **Content scale.** 14 feelings × 2 entries so far. Launch needs roughly
  6–8 entries per feeling — writing work, not engineering work. The Read
  tab is at 4 sample chapters out of a full Bible; see the section above.
- **Verse of the day is a placeholder rotation**, not curated per-day
  picks — flagged in the seed file's `_note`, worth deliberate curation
  later if it matters to you.
- **Search only matches feelings**, not free-text search across passage
  or scripture content. The DAO pattern for it already exists
  (`feelingsMatching` uses a `LIKE` query) — adding a parallel
  `passagesMatching` query and merging results into the same search box
  is a natural next step, not a new pattern to invent.
- **Room migrations.** Version 1→2 (this build's `scripture_verse` table)
  now has a real `Migration(1, 2)` in `PocketBibleApp.kt`, so saved verses
  survive this update. `fallbackToDestructiveMigration()` is still there
  as a safety net for schema changes that don't get a matching migration
  written — keep writing real migrations as the schema grows, don't lean
  on the fallback.
- **Search.** The DAO has `feelingsMatching()` using a `LIKE` query, but
  it's not wired into the UI yet. Production should move to FTS4 for
  ranked, typo-tolerant search.
- **App identity.** Package name, app name, icon, and color palette are
  all placeholders — easy to swap, worth deciding deliberately.
- **Scripture licensing.** WEB-CE text in the seed file should be checked
  verse-by-verse against the official `eng-web-c` source at ebible.org
  before shipping.
- **No tests, no ProGuard/R8 rules, no crash reporting, no analytics.**
- **No verse-of-the-day wiring**, though `daily_passage` and the DAO query
  for it already exist.
- **No dedicated crisis-content handling.** Feelings like despair or
  self-harm need a visible, unambiguous link to real help alongside any
  reflection — that's a policy and content decision as much as a
  technical one, and belongs in the next phase before those feelings are
  added to the taxonomy.
- **Accessibility pass, dark-mode contrast check, tablet layout** — none
  of it started yet. UI localization scaffolding (in-app language
  switcher, 5 languages of chrome + book names) is in place; see "Adding
  a new UI language" above for what it does and doesn't cover.

Say the word when you want to move on any of these, or straight into
production hardening once you've run the prototype on a device.
