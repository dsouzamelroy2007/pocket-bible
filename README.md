# Pocket Bible — prototype

A Catholic pocket-Bible Android app: pick how you're feeling, get a verse,
a plain-language reflection, and a short prayer — fully offline. This is
the **prototype**: real, runnable source, built to validate the concept on
a phone before investing in production polish.

## What's here

```
app/src/main/java/app/pocketbible/
  MainActivity.kt        Nav host + bottom bar (Feelings / Saved)
  PocketBibleApp.kt       Application class — owns the DB + repository
  data/
    ContentModel.kt       Room entities, DAOs, database (passage-based schema)
    SeedLoader.kt          Reads assets/seed_content.json into Room on first launch
    ContentRepository.kt   Thin layer between Room and the ViewModel
  ui/
    MainViewModel.kt       Feelings, current entry cycle, saved verses, Bible reading state
    home/HomeScreen.kt      Feelings grid
    verse/VerseScreen.kt    Passage + reflection + prayer + save/another
    saved/SavedScreen.kt    Saved list
    bible/BibleScreen.kt    Book list → chapter list → chapter reader (Read tab)
    theme/Theme.kt          Material3 color scheme
app/src/main/assets/seed_content.json   Bundled content: 14 feelings, 28 entries, 2 sample chapters
tools/import_scripture.py               Converts a real WEB-CE download into scripture JSON
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
   `seed_content.json`; the feelings grid fills in within a second or two.

If Gradle sync fails, the most likely culprit is a version mismatch (AGP
8.5.0 / Kotlin 1.9.24 / Compose compiler 1.5.14) — Android Studio's
"Upgrade Assistant" will offer compatible versions if so.

## Try this

- **Search** on the Feelings tab matches free text against feeling aliases
  (try "burned out" or "cant forgive") and jumps straight to a matching
  feeling.
- **Verse of the day** on the Feelings tab pulls from `daily_passage`,
  cycling through the 28 curated passages across all 366 days — see the
  caveat about this in the seed file's `_note`.
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

## Scaling the Read tab to the full Bible

The Read tab only shows books and chapters that actually have text in the
`scripture_verse` table — that's deliberate. Importing more of the Bible
is a **data change**, not a code change: drop rows into that table (via
the seed JSON or a follow-up asset) and the book/chapter list updates
itself with no Kotlin to touch.

What *not* to do: don't hand-author or ask an LLM to generate the full
Bible text from memory. At the scale of ~31,000 verses, small wording
drift becomes a real accuracy problem in a Catholic Bible app, and it's
avoidable — the source text already exists and is free.

The real path:

1. Download the WEB-CE plain-text or USFM bundle from
   [ebible.org](https://ebible.org/find/details.php?id=engwebc) (search
   "eng-web-c").
2. Run `tools/import_scripture.py` against it (see the script's own
   docstring for the expected input format and a `book_map.json`
   example). It reshapes the source into this app's `scripture` JSON
   array — it contains no Bible text itself, just the conversion logic.
3. Merge the result into `seed_content.json`'s `"scripture"` array (or
   load it as a second asset — `SeedLoader.seedIfNeeded()` takes an asset
   name as a parameter), bump `content_version`, rebuild.

You can do this incrementally — Psalms and the Gospels first, the rest
later — since the book map controls exactly what gets imported.

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
- **Accessibility pass, dark-mode contrast check, tablet layout,
  localization scaffolding** — none of it started yet.

Say the word when you want to move on any of these, or straight into
production hardening once you've run the prototype on a device.
