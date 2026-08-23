"""
Converts a real Bible text dump — any language, any translation — into the
`scripture` array this app's seed_content.json expects.

This script does NOT contain any Bible text itself and does not fetch
anything from the network. You provide the source file; this just reshapes
it. That split matters here — scripture text should come from a checked
source, not be hand-typed or generated from memory at bulk scale. That
holds for every language, not just English: don't paste in machine- or
LLM-translated scripture as a substitute for a real, checked translation.

Where to get source text
-------------------------
ebible.org hosts hundreds of public-domain translations, in many languages,
as plain-text and USFM downloads, e.g.:
  https://ebible.org/find/ (search by language)
For English WEB-CE specifically: https://ebible.org/find/details.php?id=engwebc
Other commonly-cited public-domain translations by language (verify license
and text before importing — this list is a starting point, not a guarantee):
  German:     Schlachter 1951, Elberfelder 1905
  French:     Louis Segond 1910
  Portuguese: João Ferreira de Almeida (edição revisada)
  Spanish:    Reina-Valera 1909
  Hindi:      Hindi O.V. (Old Version)
ebible.org's per-language listings are the place to confirm exact editions
and download links.

Expected input format
----------------------
This script expects a simple plain-text format, one verse per line:

    Ps.23.1	The LORD is my shepherd; I shall lack nothing.
    Ps.23.2	He makes me lie down in green pastures...

i.e. "<BookAbbrev>.<Chapter>.<Verse><TAB><text>" — this is the common
"VPL" (verse-per-line) export format several Bible sites offer, and is far
easier to parse reliably than USFM's markup. If your download is USFM
instead, convert it to VPL first (e.g. with the `usfm-grammar` Python
package) before running this script.

Usage
-----
    python3 import_scripture.py path/to/source.vpl.txt \
        --translation-id web-c \
        --book-map book_map.json \
        --out scripture_import.json

--translation-id sets the `translation_id` on every imported verse (and is
used as part of each verse's generated `id`, so different translations of
the same verse never collide). It must match a `translation` row you add
to seed_content.json's "translations" array (id, name, abbreviation,
license, versification, includes_deuterocanon, has_imprimatur, language) —
that's how the app knows the translation's display name and language.
Running this once per translation/language is exactly how a second (or
tenth) language gets added; nothing else in the schema needs to change.

book_map.json maps the source file's book abbreviations to this app's
book ids, e.g.:
    { "Ps": "ps", "Isa": "isa", "Matt": "mt", ... }

Only books present in book_map.json are imported — anything else is
skipped, so you can import incrementally (e.g. Psalms and the Gospels
first) rather than requiring all 73 books in one pass.

The output is a JSON array ready to paste into seed_content.json's
"scripture" key, or load separately — SeedLoader.seedIfNeeded() reads
whatever asset name you pass it, so a second file works fine too.
"""

import argparse
import json
import sys


def parse_vpl(path: str, book_map: dict, translation_id: str) -> list[dict]:
    verses = []
    skipped_books = set()
    with open(path, encoding="utf-8") as f:
        for line_no, line in enumerate(f, start=1):
            line = line.rstrip("\n")
            if not line or "\t" not in line:
                continue
            ref, text = line.split("\t", 1)
            text = text.strip()
            if not text:
                continue
            try:
                book_abbrev, chapter_str, verse_str = ref.split(".")
                chapter, verse = int(chapter_str), int(verse_str)
            except ValueError:
                print(f"line {line_no}: couldn't parse reference {ref!r}, skipping", file=sys.stderr)
                continue

            book_id = book_map.get(book_abbrev)
            if book_id is None:
                skipped_books.add(book_abbrev)
                continue

            verses.append({
                "id": f"{translation_id}:{book_id}:{chapter}:{verse}",
                "translation_id": translation_id,
                "book_id": book_id,
                "chapter": chapter,
                "verse": verse,
                "text": text,
            })

    if skipped_books:
        print(
            f"Skipped {len(skipped_books)} book(s) not in book_map.json: "
            f"{', '.join(sorted(skipped_books))}",
            file=sys.stderr,
        )
    return verses


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("source", help="Path to the verse-per-line source text file")
    parser.add_argument(
        "--translation-id", required=True,
        help="Id for this translation, e.g. web-c, de-schlachter, fr-segond1910 — "
             "must match a row you add to seed_content.json's translations array",
    )
    parser.add_argument("--book-map", required=True, help="JSON file mapping source book abbreviations to app book ids")
    parser.add_argument("--out", required=True, help="Where to write the resulting scripture JSON array")
    args = parser.parse_args()

    book_map = json.load(open(args.book_map, encoding="utf-8"))
    verses = parse_vpl(args.source, book_map, args.translation_id)

    json.dump(verses, open(args.out, "w", encoding="utf-8"), indent=2, ensure_ascii=False)
    print(f"Wrote {len(verses)} {args.translation_id} verses to {args.out}")


if __name__ == "__main__":
    main()
