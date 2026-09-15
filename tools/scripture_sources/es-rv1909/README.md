# Spanish scripture text — staged for v3

Deferred to v3: Spanish ("es") was added to the language picker with full
UI-chrome translation (`res/values-es/strings.xml`) and partial topics
content, but the app has **never** had Spanish Bible text — there was no
`content/scripture/<translation-id>/` for Spanish at all. Rather than ship
v2 with a selectable language whose Bible tab silently falls back to
English scripture, Spanish was removed from `LanguageMenuButton.kt`'s
`APP_LANGUAGES` list for the v2 release. This directory stages everything
needed to bring it back in v3.

## What's here

- `spaRV1909_vpl.txt` — the raw, unmodified verse-per-line export of the
  **Reina-Valera 1909** Spanish Bible, downloaded from eBible.org
  (`https://ebible.org/Scriptures/spaRV1909_vpl.zip`, extracted 2026-09-15).
  Public domain ("Dominio Público"), per `spaRV1909_about.htm`. 66 books
  (Protestant canon — **no deuterocanonical books**: Tobit, Judith,
  Wisdom, Sirach, Baruch, 1–2 Maccabees are absent from this translation).
  31,102 verse lines, one book abbreviation per canonical book.
- `spaRV1909_about.htm` — eBible.org's own attribution/license page for
  this file, saved alongside it for reference.
- `book_map.json` — already built and ready to use: maps every one of
  this file's 66 book abbreviations (`GEN`, `EXO`, ... `REV`) to this
  app's book ids (`gen`, `ex`, ... `rev`). Nothing needs to change here.

## Why this isn't already imported

`tools/import_scripture.py` expects each VPL line as
`<BookAbbrev>.<Chapter>.<Verse><TAB>text` (dot-separated reference, then a
tab, then the verse text) — that's the `eng-web-c` convention. This
particular eBible.org export instead uses
`<BookAbbrev> <Chapter>:<Verse> text` (space before the ref, colon
between chapter and verse, another space before the text, **no tab at
all**). Confirmed by inspecting the raw bytes — e.g. line 1 is literally:

```
GEN 1:1 En el principio crió Dios los cielos y la tierra.
```

So `import_scripture.py` can't consume this file as-is; it needs one
small reformatting pass first. The regex below was tested against all
31,102 lines of `spaRV1909_vpl.txt` with zero non-matches, so this is a
safe, mechanical step — no text is altered, only the delimiters.

## Steps to finish the import (v3)

Run from the repo root. All paths below are relative to
`tools/scripture_sources/es-rv1909/`.

**1. Reformat the source into the format `import_scripture.py` expects:**

```python
import re

with open("tools/scripture_sources/es-rv1909/spaRV1909_vpl.txt", encoding="utf-8") as fin, \
     open("tools/scripture_sources/es-rv1909/spaRV1909_vpl.tabbed.txt", "w", encoding="utf-8") as fout:
    for line in fin:
        line = line.rstrip("\n")
        if not line:
            continue
        m = re.match(r"^(\S+)\s+(\d+):(\d+)\s+(.*)$", line)
        if not m:
            print("no match:", line)  # should never print — verified 0 failures on 2026-09-15
            continue
        book, chap, verse, text = m.groups()
        fout.write(f"{book}.{chap}.{verse}\t{text}\n")
```

**2. Add the translation's row to `content/core.json`'s `"translations"`
array** (same schema every other translation row already uses — copy the
pattern from `riveduta-1927` or `aa-pt`, both also public-domain,
non-deuterocanon):

```json
{
  "id": "rv1909",
  "name": "Reina-Valera 1909",
  "abbreviation": "RV1909",
  "license": "public-domain",
  "versification": "masoretic",
  "includes_deuterocanon": false,
  "has_imprimatur": false,
  "language": "es",
  "source_name": "eBible.org — Santa Biblia, Reina Valera 1909",
  "source_url": "https://ebible.org/find/details.php?id=spaRV1909",
  "license_url": null
}
```

**3. Run the import:**

```bash
python3 tools/import_scripture.py \
    tools/scripture_sources/es-rv1909/spaRV1909_vpl.tabbed.txt \
    --translation-id rv1909 \
    --language es \
    --license public-domain \
    --source "eBible.org spaRV1909" \
    --book-map tools/scripture_sources/es-rv1909/book_map.json \
    --out-dir app/src/main/assets/content/scripture/rv1909
```

This writes one `<book-id>.json` per book found (66 files — every book
this app defines *except* the 7 deuterocanonical ones: `tob`, `jdt`,
`1macc`, `2macc`, `wis`, `sir`, `bar`, since RV1909 doesn't include them
and there's no book_map entry for them).

**4. Add one manifest entry per imported book file** to
`content/manifest.json`'s `"scripture"` array, same pattern as every
existing `web-c`/`schlachter-1951`/etc. entry:

```json
{ "translation_id": "rv1909", "book_id": "gen", "path": "content/scripture/rv1909/gen.json" }
```

(66 entries total — the import script's own output tells you exactly
which book ids it wrote, so this can be scripted rather than typed by
hand.)

**5. Re-add `"es" to "Español"`** to `APP_LANGUAGES` in
`app/src/main/java/app/pocketbible/ui/LanguageMenuButton.kt` (it was
removed for the v2 release specifically because this scripture gap
existed — see git history around 2026-09-15 for the removal commit).

**6. Then close the rest of the Spanish content gap** — this scripture
import only covers the Bible tab. Topics, characters, stories, and daily
reflections still need Spanish translation; see the
`project_v2_spanish_content` memory note (or `.github/ABOUT.md`) for
that status, which is tracked separately from this scripture work.

**7. Bump `content_version` in `content/manifest.json`, run
`./gradlew assembleDebug`, spot-check a few passages on-device (Genesis
1, a Psalm, a Gospel reading) for correct rendering of Spanish
diacritics/punctuation, then commit.**

## A note on completeness

RV1909 lacks the deuterocanonical books entirely, so even after this
import, Spanish will be a 66-book Bible while English/German/French/
Portuguese ship 73 (or German's 66, matching its own Schlachter source).
If full Catholic-canon Spanish text is wanted later, a different public-
domain source would be needed for just those 7 books (e.g. check
eBible.org for a Spanish Catholic translation with deuterocanon, or pull
those specific books from a different Spanish edition) — that's a
separate, smaller follow-up, not a blocker for shipping the 66-book
version.
