# About Pocket Bible

**Pocket Bible** is a contemplative Catholic Android app designed to meet you in the moment.

## Quick Description

Select how you're feeling—fear, doubt, anxiety, loss, joy—and instantly receive a relevant Bible passage, a plain-language reflection, and a thoughtful prayer. Everything works offline.

## Project Status

This is a **prototype**—real, runnable source code designed to validate the concept on an Android device before production polish. The architecture is production-ready and scalable.

## Key Capabilities

- **📖 Full Biblical Text**: 73-book Catholic canon in English, with German (Schlachter 1951 translation) for the 66-book Protestant canon
- **🌍 Multi-Language Support**: UI in English, German, French, Portuguese, Spanish, Hindi, Italian, and Marathi
- **❤️ Feelings-to-Scripture Mapping**: Curated passages, reflections, and prayers linked to emotional states
- **✝️ Fully Offline**: No internet connection required; all content is bundled
- **💾 Persistent Storage**: Save favorite passages via Room database
- **🔍 Search**: Find passages by feeling with alias matching (e.g., "burned out")
- **🎨 Beautiful UI**: Material3 design with category-specific accent colors and responsive layouts

## Tech Stack

- **Language**: Kotlin
- **Framework**: Jetpack Compose (UI)
- **Database**: Android Room (local persistence)
- **Architecture**: MVVM with Repository pattern
- **Minimum SDK**: API 26
- **Target SDK**: API 34

## Repository Structure

- `app/src/main/java/app/pocketbible/` — Kotlin source (UI, ViewModels, data access)
- `app/src/main/assets/content/` — Bundled offline content (scripture, topics, translations)
- `tools/` — Python import scripts for adding new Bible translations and languages
- `docs/` — Privacy policy and other documentation
- `store/` — Google Play Store listing assets

## Getting Started

See the main [README.md](../README.md#running-it) for build and run instructions.

## Roadmap (Post-Prototype)

- Scale content: 6–8 entries per feeling (currently 2)
- Curate daily passage rotations
- Full-text search across passages
- Room migration tooling for schema evolution
- Crisis content handling (with links to professional resources)
- Accessibility audit and dark-mode contrast verification
- Production app signing and Play Store release

## Version 2 Scope

In development on the `v2` branch, kept separate from `main` and not merged
back until v1 finishes closed testing and goes live in production. The
`v2` branch also has its own `applicationId` (`app.pocketbible.v2`), app
label ("Pocket Bible V2"), and CI-built APK filename
(`pocket-bible-v2-debug.apk`), so a v2 debug build can be installed
side-by-side with the live v1 app on the same device.

- **Biblical characters, 114 → 366**: one character per calendar day
  (366 to cover a leap year), each day mapped to a character chosen for
  that day's liturgical or scriptural significance — e.g. Jesus Christ on
  December 25, Joseph (father of Jesus) on May 1, Mother Mary on
  September 8.
- **Verse of the Day, all languages**: 365 distinct motivational verses
  (366 for leap years) rotated one-per-day, translated into every
  supported UI language rather than English-only.
- **Daily Catholic readings**: daily Mass readings, responsorial psalm,
  and reflections, driven by lectionary citations (not full USCCB text)
  fetched from [cpbjr/catholic-readings-api](https://github.com/cpbjr/catholic-readings-api)
  (free, open-source, GitHub Pages, verified against US Catholic
  liturgical norms and the General Roman Calendar for the United
  States). Citations only, fetched once and cached permanently into this
  repo by a scheduled job — never re-fetched live on page load — with a
  small manually-maintained fallback table for major solemnities in case
  the source ever goes dark. Since lectionary citations are published a
  year at a time, each build version covers through the end of a given
  year: v2 covers through December 2026, v3 will extend through the end
  of 2027, and so on.
- **Daily notifications (email + WhatsApp)**: sent at 6:00 AM GMT to
  users who opt in, containing the verse of the day, the daily
  reading/reflection, and that day's mapped biblical character.

## Version 2 Phased Plan

Phases 1–3 are pure content + in-app work, no new infrastructure, and can
each ship independently. Phase 4 is the one architectural pivot — it's the
only part of v2 that needs the app (or an external surface) to talk to a
network at all. Phase 5 is the pre-release cleanup every other phase
leaves behind.

**Cross-cutting strategy**: for every content phase below, ship
English-complete first and let the existing fallback-to-English mechanism
(`feeling_translation`/`entry_translation`/`character_translation`
pattern in `ContentModel.kt`) cover the other 7 languages until each is
translated — this is exactly how the current 27 topics and 114 characters
already degrade gracefully for a language with partial coverage, so v2's
much larger content volume doesn't have to block on translating
everything before anything ships.

### Phase 1 — Biblical characters: 114 → 366 — **DONE**

- Final shape: a `character_of_day` table (`month_day -> character_id`,
  same shape as `daily_passages`, not a field on the character itself —
  this is what lets a character repeat on more than one day) fills all
  366 days. 114 original characters + 166 newly authored + 86 repeat days
  (a fixed pool of ~29 central figures cycled 2-3x each, since the pool of
  genuinely significant, non-padding figures runs out around 280) = 366
  filled, no gaps.
- Real Catholic feast days used wherever one applies and isn't already
  claimed by someone sharing that official day; Old Testament figures
  (no Catholic feast day exists for them at all) and any figure who lost
  a shared-feast tiebreak are placed by a specific thematic connection
  where one could be found (e.g. Moses on the Transfiguration, Job's
  friends the days right after his), falling back to canonical/narrative
  order only when no real connection exists.
- ~20 characters whose defining role is a sin a reader might recognize in
  themselves carry `reflection`/`prayer` fields (nullable, null for
  everyone else) — not scripture text, original devotional content naming
  the specific sin and a short prayer of turning away from it.
- English-complete; translation into the other 7 languages not done yet
  (falls back to English per the cross-cutting strategy above).
- **Deferred to a later phase**: an in-app "Character of the Day" screen.
  The data and the `characterOfDay(monthDay)` query both exist and work
  (mirroring `verseOfDay`/`passageOfDay`) — nothing in `MainViewModel` or
  the UI calls it yet. Wire it up whenever that phase comes around.

### Phase 2 — Verse of the Day: true 365/366, all languages

- Current state: `daily_passages` in `topics.json` already has 366
  `month_day` entries, but only 108 distinct passages cycling by
  rotation (per the file's own `_note`), English only.
- Curate 365/366 independently-picked, verified verses (checked against
  the WEB-CE source the same way the original 14 topics were, per the
  existing verification caveat already on record in `topics.json`) — not
  a rotation of a smaller set.
- Translate the daily verse pool into all 8 UI languages (same
  incremental/fallback strategy as Phase 1). Note this is verse
  *selection*, not re-translating scripture text — the actual verse text
  still resolves from each language's existing `scripture/<translation-id>/`
  files, same mechanism the Read tab and Characters tab already use.

### Phase 3 — Daily Catholic lectionary readings — **2026 & 2027 content-complete**

- One-time+annual pipeline: fetch a year's daily citations (readings,
  responsorial psalm) from
  [cpbjr/catholic-readings-api](https://github.com/cpbjr/catholic-readings-api),
  parse them into `content/lectionary/<year>.json`. The app never calls the
  API directly — it only ever reads the committed, parsed file.
- Because these are citations only (book/chapter/verse, like
  `character.verse_refs` already are), the actual reading text resolves
  the same way character verse references already do: live lookup
  against the bundled `scripture_verse` table for whichever translation
  is current. No new copyright exposure, no new bundled text.
- Reflections are original devotional prose (like topic reflections),
  authored per day, English first.
- `DailyReading`/`ReadingCitation` Room entities, keyed by real ISO date
  (not `month_day` — the specific reading assigned to a given calendar
  date shifts year to year, so each year needs its own full set of rows).
  `manifest.json`'s `"lectionary"` array lists every year currently
  loaded; `SeedLoader` already loops over the whole array generically, so
  adding a year is a data change only, never a code change.
- A Daily Readings & Reflection tab with day-by-day navigation (previous/
  next, a "Today" shortcut, and a date picker) whose selectable range is
  read live from the earliest/latest date actually seeded — it widens
  automatically the day a new year's file is added, no code touched.
- **Recurs yearly, and turned out to need a full re-author each time, not
  just an update**: comparing 2026 and 2027 citation-by-citation showed
  that only truly fixed-date solemnities (Christmas, Assumption, etc.,
  maybe ~15-20 days) keep identical readings year to year. Every other
  day's specific reading shifts, because Easter's date moves the whole
  Lent/Easter block, which moves when Ordinary Time starts, which shifts
  which specific weekday-in-cycle lands on any given calendar date — on
  top of the Sunday A/B/C and weekday Year I/II rotations. So a new
  year's ~365 reflections are essentially a fresh writing pass, not a
  port of the previous year's — see "Adding a new lectionary year" below.

#### Adding a new lectionary year

Repeatable runbook, same one used for 2027 — nothing here should need to
change for 2028, 2029, etc.:

1. `python3 tools/fetch_lectionary.py <year>` — fetches all 365/366 days
   from the live API into `content/lectionary/<year>-source.json`.
2. `python3 tools/build_lectionary_year.py <year>` — parses every
   citation into book/chapter/verse ranges and writes
   `content/lectionary/<year>.json`, `reflection: null` for every day.
   Reports any citations it couldn't parse (rare — a handful of source
   typos have shown up before; fix them either in
   `tools/parse_lectionary.py`'s book-name table or as a targeted
   fallback in `build_lectionary_year.py`, whichever the failure looks
   like) and re-run until 0 fail.
3. `python3 tools/verify_lectionary_year.py <year>` — flags citations
   whose verse range doesn't fully exist in the bundled WEB text. Most
   flags are harmless (a psalm's sung-heading offset — the app's verse
   lookup already truncates gracefully, never errors); a genuine content
   gap (most commonly Daniel 3's deuterocanonical canticle, since this
   app's bundled Daniel is Hebrew-canon only) just needs noting in the
   year's `_note` field and in that day's reflection (ground it in the
   other readings / the well-known story rather than quoting text this
   app doesn't have).
4. Author reflections in monthly batches, same process each time: pull
   that month's real resolved text first (join the day's refs against
   `content/scripture/web-c/*.json`), read it, write each reflection
   grounded in the actual Gospel and readings — never from memory of
   what a passage "probably" says. Load the month's dict of
   `reflection` values into the year's JSON, `json.dump(..., indent=2,
   ensure_ascii=False)` plus a trailing newline.
5. Add `{"year": <year>, "path": "content/lectionary/<year>.json"}` to
   `manifest.json`'s `"lectionary"` array — **append, don't replace**;
   every prior year stays loaded too, so nothing breaks for a device
   that hasn't updated in a while.
6. Bump `content_version`, `./gradlew assembleDebug`, spot-check the
   Daily Readings tab on a device before committing.
7. Start early: content authoring is the actual bottleneck (~365 days of
   real writing), so begin the next year's batches a couple of months
   before the calendar rolls over — the date picker will start offering
   the new year's dates the moment its data is seeded, so a late start
   means a live gap.

### Phase 4 — Daily notifications (email + WhatsApp)

The only phase that needs something outside the Android app itself —
today the app ships with no `INTERNET` permission and the privacy policy
states no data is collected, so this phase changes that story and needs
its own privacy-policy update, not just a feature flag.

**Open decisions, with a recommended default** (easy to revisit, this is
just where the plan currently leans):

- *Where does opt-in happen?* Recommended: a small external landing
  page + lightweight backend (not an in-app screen), so the Android app
  itself stays offline/no-network as documented, and this feature can
  ship without changing the app's permission model or Play Store
  data-safety declarations at all. In-app opt-in is a reasonable
  alternative if discoverability matters more than keeping the app
  fully offline — worth an explicit call before building.
- *Email provider*: any transactional provider (SES, SendGrid, Mailgun,
  Postmark) — low complexity, no special approval process.
- *WhatsApp provider*: needs a WhatsApp Business Platform account
  (direct via Meta Cloud API, or a BSP like Twilio/360dialog), business
  verification, and pre-approved message templates for proactive daily
  sends (WhatsApp doesn't allow free-form outbound messages outside a
  24-hour user-initiated session window) — meaningfully more lead time
  than email. Recommend shipping email first, WhatsApp as a follow-on
  sub-phase once verification is in hand.
- *Scheduler*: a dedicated cloud scheduler (e.g. Cloudflare Workers Cron
  Triggers, GCP Cloud Scheduler, AWS EventBridge) over a GitHub Actions
  `schedule` trigger — GH Actions cron is free but has documented firing
  delays, worse right at common times like 06:00 UTC.

**Tasks once those are settled**: opt-in capture (email + WhatsApp
number + consent), a small store of subscriber preferences, the 06:00
GMT job that assembles the day's payload (verse of the day + reading/
reflection + mapped character, from Phases 1–3) and sends it, unsubscribe
handling, and the privacy-policy rewrite.

### Phase 5 — Compliance, translation catch-up, release prep

- Finish translating Phases 1–3 content into all 8 languages (whatever's
  still English-only via fallback).
- Update `docs/privacy-policy.html` for whatever Phase 4 actually
  collects.
- **Before this branch is ever released**: revert `applicationId`
  (`app.pocketbible.v2` → `app.pocketbible`), the app label ("Pocket
  Bible V2" → "Pocket Bible"), and the CI APK filename back to match v1
  (in `app/build.gradle.kts` and `.github/workflows/build-apk.yml`) —
  those were only set up so a v2 debug build could sit side-by-side with
  v1 during development. Shipping v2 under a different `applicationId`
  would make Play Store treat it as a brand-new app/listing instead of
  an update to the existing one.
- Full regression pass across languages, offline behavior (everything
  except Phase 4's opt-in/send path must still work with no network),
  and the closed-testing checklist v1 already went through.

## License

See the repository for license information.

## Feedback

This is a learning project. Feedback on the architecture, UI, or scriptural accuracy is welcome.
