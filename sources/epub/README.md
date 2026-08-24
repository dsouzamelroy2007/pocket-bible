# Raw scripture source files

Raw eBible.org/Haiola epubs, kept here as-supplied so their provenance is
traceable, before `tools/import_epub.py` turns them into
`app/src/main/assets/content/scripture/<translation_id>/<book>.json`.
Each entry below was verified directly from the epub's own `copyright.xhtml`
front-matter page (not just its OPF `dc:rights`, which is sometimes just a
bare "Copyright © ..." line with no license terms).

Already imported, not stored here (found in earlier sessions, not re-added
retroactively): `engweb.epub` (World English Bible Classic, English, Public
Domain, 73 books) and `deu1951.epub` (Schlachter 1951, German, CC BY 4.0, 66
books, no deuterocanon).

## Pending import (added this session, not yet processed)

| File | Title | Language | Books | License |
|---|---|---|---|---|
| `fr_sainte_bible_libre.epub` | Sainte Bible libre pour le monde | French (fr) | 81 -- full Catholic 73-book canon plus extra apocrypha (1/2/3/4 Maccabees, 1/2 Esdras, Prayer of Manasseh, Additions to Daniel/Esther, Psalm 151) | Public Domain / CC0. **Marked "draft translation, being proofread and edited"** by eBible.org as of 2026-08-21 -- flag this in the imported translation's notes. |
| `hi_irv_2019.epub` | Indian Revised Version (IRV) Hindi 2019 | Hindi (hi) | 66 -- Protestant canon, no deuterocanon | CC BY-SA 4.0 (Bridge Connectivity Solutions). Share-alike: redistributed/adapted text must stay under the same license. |
| `it_riveduta_1927.epub` | Riveduta Bibbia 1927 | Italian (it) | 66 -- Protestant canon, no deuterocanon | Public Domain |
| `mr_irv.epub` | Indian Revised Version (IRV) Marathi | Marathi (mr) | 66 -- Protestant canon, no deuterocanon | CC BY-SA 4.0 (Bridge Connectivity Solutions). Share-alike, same terms as the Hindi IRV above. |

Marathi is a new language for this app (not yet in `core.json`'s language
list) -- importing it means adding a `mr` translation entry plus deciding
whether Marathi gets its own UI-chrome localization (strings.xml) or falls
back to English/system default for chrome while scripture text is real.
