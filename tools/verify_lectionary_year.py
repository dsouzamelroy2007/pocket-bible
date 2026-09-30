"""
Flags citations in content/lectionary/<year>.json whose chapter/verse range
doesn't fully exist in the bundled WEB scripture (content/scripture/web-c).
Doesn't fix anything -- this app's own verse lookup already only returns
verses that exist (graceful truncation, never an error), so a flagged
mismatch is just something worth knowing about before writing that day's
reflection: is it a harmless one-verse psalm-heading offset (NAB counts a
sung heading as verse 1, WEB doesn't), or a genuine content gap (e.g. a
deuterocanonical passage this app's bundled text doesn't include at all)?

Usage: python3 tools/verify_lectionary_year.py <year>
"""
from __future__ import annotations

import json
import sys

BASE = "app/src/main/assets/content/scripture/web-c"
_book_cache: dict[str, dict] = {}


def load_book(book_id: str) -> dict | None:
    if book_id not in _book_cache:
        try:
            _book_cache[book_id] = json.load(open(f"{BASE}/{book_id}.json", encoding="utf-8"))
        except FileNotFoundError:
            _book_cache[book_id] = None
    return _book_cache[book_id]


def existing_verses(book_id: str, chapter: int) -> set[int]:
    data = load_book(book_id)
    if data is None:
        return set()
    verses = data.get("verses", data if isinstance(data, list) else [])
    return {v["verse"] for v in verses if v.get("chapter") == chapter}


def check_ref(ref: dict) -> list[str]:
    problems = []
    book_id = ref["book_id"]
    if load_book(book_id) is None:
        return [f"no bundled text for book '{book_id}'"]
    start_verses = existing_verses(book_id, ref["chapter_start"])
    if ref["verse_start"] not in start_verses:
        problems.append(
            f"{book_id} {ref['chapter_start']}:{ref['verse_start']} does not exist in WEB"
        )
    end_verses = existing_verses(book_id, ref["chapter_end"])
    if ref["verse_end"] not in end_verses:
        problems.append(
            f"{book_id} {ref['chapter_end']}:{ref['verse_end']} does not exist in WEB"
        )
    return problems


def main(year: int) -> None:
    d = json.load(open(f"app/src/main/assets/content/lectionary/{year}.json", encoding="utf-8"))
    flagged = 0
    for day in d["days"]:
        for reading in day["readings"]:
            for ref in reading["refs"]:
                problems = check_ref(ref)
                if problems:
                    flagged += 1
                    print(f"{day['date']} {reading['role']} ({reading['citation_display']}): {'; '.join(problems)}")
    print(f"\n{flagged} ref(s) flagged out of {sum(len(r['refs']) for day in d['days'] for r in day['readings'])} total", file=sys.stderr)


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("usage: verify_lectionary_year.py <year>", file=sys.stderr)
        sys.exit(1)
    main(int(sys.argv[1]))
