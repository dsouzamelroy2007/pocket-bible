"""
Fetches a year's daily Catholic Mass reading citations from cpbjr's
Catholic Readings API (https://github.com/cpbjr/catholic-readings-api,
data served from https://cpbjr.github.io/catholic-readings-api/) and
caches them into this repo, once, as content/lectionary/<year>-source.json.

This is citations only -- "Numbers 6:22-27", "Psalm 67:2-3, 5, 6, 8" --
never the reading text itself. The app resolves the actual verse text
live from its own already-bundled scripture_verse table (see
tools/parse_lectionary.py, which turns these citations into the book_id/
chapter/verse rows the app reads), exactly the same "citation now, text
resolved at render time" split character.verse_refs already uses. That
split is what keeps this feature clear of the USCCB-full-text licensing
question raised when this feature was scoped.

The app never calls this API directly -- this script runs once (or is
re-run by a maintainer / a scheduled job when a new year's readings are
published) and commits its cached output. If the source ever goes dark,
whatever year's file is already committed keeps working.

Usage:
    python3 tools/fetch_lectionary.py 2026

Writes app/src/main/assets/content/lectionary/2026-source.json --
one entry per calendar day of that year, each with the season and
whatever of firstReading/psalm/secondReading/gospel the API returned
(weekdays outside Sundays/solemnities have no secondReading).
"""
import datetime
import json
import sys
import time
import urllib.request
import urllib.error
from typing import Optional

BASE_URL = "https://cpbjr.github.io/catholic-readings-api/readings/{year}/{month_day}.json"


def fetch_day(year: int, month: int, day: int) -> Optional[dict]:
    month_day = f"{month:02d}-{day:02d}"
    url = BASE_URL.format(year=year, month_day=month_day)
    try:
        with urllib.request.urlopen(url, timeout=15) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        if e.code == 404:
            return None
        raise


def main(year: int, out_path: str):
    start = datetime.date(year, 1, 1)
    end = datetime.date(year, 12, 31)
    entries = []
    missing = []
    cur = start
    while cur <= end:
        data = fetch_day(cur.year, cur.month, cur.day)
        if data is None:
            missing.append(cur.isoformat())
        else:
            entries.append(data)
        cur += datetime.timedelta(days=1)
        time.sleep(0.05)  # be a polite, not a load-testing, client

    print(f"fetched {len(entries)} days, {len(missing)} missing", file=sys.stderr)
    if missing:
        print("missing dates:", missing, file=sys.stderr)

    entries.sort(key=lambda e: e["date"])
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump({"year": year, "source": "https://github.com/cpbjr/catholic-readings-api",
                    "fetched": datetime.date.today().isoformat(), "days": entries}, f, indent=2, ensure_ascii=False)
        f.write("\n")
    print(f"wrote {out_path}", file=sys.stderr)


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("usage: fetch_lectionary.py <year>", file=sys.stderr)
        sys.exit(1)
    year = int(sys.argv[1])
    out = f"app/src/main/assets/content/lectionary/{year}-source.json"
    main(year, out)
