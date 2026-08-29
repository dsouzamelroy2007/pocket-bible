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

## License

See the repository for license information.

## Feedback

This is a learning project. Feedback on the architecture, UI, or scriptural accuracy is welcome.
