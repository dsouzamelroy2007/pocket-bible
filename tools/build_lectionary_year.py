"""
Turns one year's raw fetched lectionary source
(content/lectionary/<year>-source.json, from fetch_lectionary.py) into the
app-ready structured file (content/lectionary/<year>.json) that SeedLoader
reads: every reading's citation parsed into book_id/chapter/verse ranges via
parse_lectionary.parse_citation, `reflection` left null for every day
(authored separately, in monthly batches, English first -- see
.github/ABOUT.md's "Adding a new lectionary year" section for that process).

Usage: python3 tools/build_lectionary_year.py <year>
  reads  content/lectionary/<year>-source.json
  writes content/lectionary/<year>.json

Re-running this after a re-fetch (e.g. the source repo publishes a
correction) is always safe -- it fully regenerates the file, including
wiping back to reflection: null, so re-run it *before* re-authoring
reflections for the year, not after.
"""
import json
import sys

from parse_lectionary import parse_citation

ROLE_ORDER = ["first_reading", "psalm", "second_reading", "gospel"]


def build(year: int, source_path: str, out_path: str) -> None:
    source = json.load(open(source_path, encoding="utf-8"))
    days_out = []
    failed = []

    for day in source["days"]:
        readings_out = []
        for role in ROLE_ORDER:
            citation = day["readings"].get(role)
            if citation is None:
                continue
            citation_display = citation
            book_id, ranges = parse_citation(citation)
            if (book_id is None or not ranges) and role == "psalm" and not citation.lower().startswith("psalm"):
                # Source occasionally drops the "Psalm" prefix, leaving a bare
                # "137:1-2, 3, 4-5, 6" -- safe to assume here since this is
                # specifically the psalm role.
                citation_display = f"Psalm {citation}"
                book_id, ranges = parse_citation(citation_display)
            if book_id is None or not ranges:
                failed.append((day["date"], role, citation))
                continue
            refs = [
                {
                    "book_id": book_id,
                    "chapter_start": cs,
                    "verse_start": vs,
                    "chapter_end": ce,
                    "verse_end": ve,
                }
                for (cs, vs, ce, ve) in ranges
            ]
            readings_out.append({
                "role": role,
                "citation_display": citation_display,
                "refs": refs,
            })
        days_out.append({
            "date": day["date"],
            "season": day["season"],
            "usccb_link": day["usccbLink"],
            "readings": readings_out,
            "reflection": None,
        })

    out = {
        "year": year,
        "source": "https://github.com/cpbjr/catholic-readings-api",
        "_note": (
            f"Daily Mass reading citations for {year}, fetched from cpbjr's "
            "Catholic Readings API (see tools/fetch_lectionary.py) and parsed "
            "into book_id/chapter/verse ranges (see tools/parse_lectionary.py "
            "and tools/build_lectionary_year.py). Citations only -- no reading "
            "text is stored here; it resolves live from scripture_verse for "
            "whichever translation is current, the same citation-now/"
            "text-at-render-time split character.verse_refs already uses. "
            "'reflection' is null for every day until authored separately "
            "(original devotional prose, English first, monthly batches -- "
            "see .github/ABOUT.md). Versification note: the lectionary's "
            "citations follow NAB/Hebrew-continuous numbering, which differs "
            "from this app's bundled WEB text for a few books -- graceful "
            "truncation handles most of it (this app's verse lookup only "
            "returns verses that exist, never errors); genuine content gaps "
            "(e.g. Daniel's deuterocanonical additions, if this year's "
            "calendar assigns them) get flagged during the reflection-writing "
            "pass, the same way 2026's were."
        ),
        "days": days_out,
    }

    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(out, f, indent=2, ensure_ascii=False)
        f.write("\n")

    total = sum(len(d["readings"]) for d in days_out) + len(failed)
    print(f"{year}: {len(days_out)} days, {total} citations, {len(failed)} failed to parse", file=sys.stderr)
    for date, role, citation in failed:
        print(f"  {date} {role}: {citation}", file=sys.stderr)


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("usage: build_lectionary_year.py <year>", file=sys.stderr)
        sys.exit(1)
    year = int(sys.argv[1])
    build(
        year,
        f"app/src/main/assets/content/lectionary/{year}-source.json",
        f"app/src/main/assets/content/lectionary/{year}.json",
    )
