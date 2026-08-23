"""
Converts a real World English Bible (Catholic Edition) text dump into the
`scripture` array this app's seed_content.json expects.

This script does NOT contain any Bible text itself and does not fetch
anything from the network. You provide the source file; this just reshapes
it. That split matters here — scripture text should come from a checked
source, not be hand-typed or generated from memory at bulk scale.

Where to get the source text
-----------------------------
ebible.org hosts the WEB-CE as plain-text and USFM downloads:
  https://ebible.org/find/details.php?id=engwebc
Grab the "Text (readable)" or "USFM" bundle for eng-web-c.

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
    python3 import_scripture.py path/to/web-ce.vpl.txt \
        --book-map book_map.json \
        --out scripture_import.json

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


def parse_vpl(path: str, book_map: dict) -> list[dict]:
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
                "id": f"web-c:{book_id}:{chapter}:{verse}",
                "translation_id": "web-c",
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
    parser.add_argument("source", help="Path to the verse-per-line WEB-CE text file")
    parser.add_argument("--book-map", required=True, help="JSON file mapping source book abbreviations to app book ids")
    parser.add_argument("--out", required=True, help="Where to write the resulting scripture JSON array")
    args = parser.parse_args()

    book_map = json.load(open(args.book_map, encoding="utf-8"))
    verses = parse_vpl(args.source, book_map)

    json.dump(verses, open(args.out, "w", encoding="utf-8"), indent=2, ensure_ascii=False)
    print(f"Wrote {len(verses)} verses to {args.out}")


if __name__ == "__main__":
    main()
