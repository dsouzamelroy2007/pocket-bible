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

## License

See the repository for license information.

## Feedback

This is a learning project. Feedback on the architecture, UI, or scriptural accuracy is welcome.
