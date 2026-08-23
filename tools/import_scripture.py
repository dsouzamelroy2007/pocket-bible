"""
Converts a real Bible text dump — any language, any translation — into the
per-book scripture files this app's content/manifest.json indexes.

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

Output layout
--------------
This app's content lives under app/src/main/assets/content/, indexed by
manifest.json. Scripture is one file per book per translation:
    content/scripture/<translation-id>/<book-id>.json

This script writes exactly that: one JSON file per book found in the
source, containing translation_id/book_id/language/license/source/verified
plus that book's verses (chapter, verse, text — no per-verse id or
translation_id/book_id, since those live once at the file level and
SeedLoader derives each verse's id from them).

Usage
-----
    python3 import_scripture.py path/to/source.vpl.txt \
        --translation-id web-c \
        --language en \
        --license public-domain \
        --source "ebible.org eng-web-c" \
        --book-map book_map.json \
        --out-dir ../app/src/main/assets/content/scripture/web-c

--translation-id sets the `translation_id` for every imported verse. It
must match a `translation` row in content/core.json's "translations" array
(id, name, abbreviation, license, versification, includes_deuterocanon,
has_imprimatur, language) — that's how the app knows the translation's
display name and language. Running this once per translation/language,
into its own content/scripture/<translation-id>/ folder, is exactly how a
second (or tenth) language gets added; nothing else in the schema needs to
change.

book_map.json maps the source file's book abbreviations to this app's
book ids, e.g.:
    { "Ps": "ps", "Isa": "isa", "Matt": "mt", ... }

Only books present in book_map.json are imported — anything else is
skipped, so you can import incrementally (e.g. Psalms and the Gospels
first) rather than requiring all 73 books in one pass.

After running this, add one entry per new book file to content/manifest.json's
"scripture" array (translation_id, book_id, path) — this script prints the
exact lines to add — and bump manifest.json's content_version so
SeedLoader re-seeds on next launch.
"""

import argparse
import json
import os
import sys
from collections import defaultdict


def parse_vpl(path: str, book_map: dict) -> dict[str, list[dict]]:
    """Returns {book_id: [ {chapter, verse, text}, ... ]}."""
    verses_by_book: dict[str, list[dict]] = defaultdict(list)
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

            verses_by_book[book_id].append({"chapter": chapter, "verse": verse, "text": text})

    if skipped_books:
        print(
            f"Skipped {len(skipped_books)} book(s) not in book_map.json: "
            f"{', '.join(sorted(skipped_books))}",
            file=sys.stderr,
        )
    return verses_by_book


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("source", help="Path to the verse-per-line source text file")
    parser.add_argument(
        "--translation-id", required=True,
        help="Id for this translation, e.g. web-c, de-schlachter, fr-segond1910 — "
             "must match a row in content/core.json's translations array",
    )
    parser.add_argument("--language", required=True, help="BCP-47-ish language code, e.g. en, de, fr")
    parser.add_argument("--license", default="public-domain", help="License string to record in each file (default: public-domain)")
    parser.add_argument("--source", dest="source_name", required=True, help="Human-readable source description, e.g. 'ebible.org eng-web-c'")
    parser.add_argument("--book-map", required=True, help="JSON file mapping source book abbreviations to app book ids")
    parser.add_argument("--out-dir", required=True, help="Directory to write <book-id>.json files into, e.g. content/scripture/web-c")
    parser.add_argument("--note", default=None, help="Optional free-text note stored in every output file (e.g. which chapters are included)")
    args = parser.parse_args()

    book_map = json.load(open(args.book_map, encoding="utf-8"))
    verses_by_book = parse_vpl(args.source, book_map)

    os.makedirs(args.out_dir, exist_ok=True)
    manifest_lines = []
    for book_id, verses in sorted(verses_by_book.items()):
        verses.sort(key=lambda v: (v["chapter"], v["verse"]))
        file_obj = {
            "translation_id": args.translation_id,
            "book_id": book_id,
            "language": args.language,
            "license": args.license,
            "source": args.source_name,
            "verified": True,
            "note": args.note,
            "verses": verses,
        }
        out_path = os.path.join(args.out_dir, f"{book_id}.json")
        with open(out_path, "w", encoding="utf-8") as f:
            json.dump(file_obj, f, indent=2, ensure_ascii=False)
            f.write("\n")
        print(f"Wrote {len(verses)} verses to {out_path}")
        manifest_lines.append(
            f'    {{"translation_id": "{args.translation_id}", "book_id": "{book_id}", '
            f'"path": "content/scripture/{args.translation_id}/{book_id}.json"}},'
        )

    print("\nAdd these entries to content/manifest.json's \"scripture\" array, "
          "then bump content_version:")
    print("\n".join(manifest_lines))


if __name__ == "__main__":
    main()
