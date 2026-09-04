"""
Parses the human-readable reading citations cpbjr's Catholic Readings API
gives ("Numbers 6:22-27", "Psalm 67:2-3, 5, 6, 8", "Isaiah 52:13-53:12")
into the book_id/chapter/verse ranges this app resolves scripture text
from -- the same citation-only model character.verse_refs already uses,
just extended to support the range shapes real lectionary citations need
that a single character citation never did:

  - comma/"and" lists of verses or verse-ranges within one chapter
    ("67:2-3, 5, 6, 8" -> four ranges, all in chapter 67)
  - semicolon-separated independent segments, each with its own chapter
    ("9:1-4, 17-19; 10:1" -> chapter 9 ranges, plus a separate chapter 10 range)
  - a genuine cross-chapter span using an em/en dash before a second
    chapter:verse ("1 John 1:5-2:2" -> one range from ch.1 v.5 through
    ch.2 v.2)
  - "or" alternates (two forms of the same reading, long and short) --
    this always takes the first (longer) alternative
  - trailing sub-verse letters ("15bc", "8a") -- stripped; this app
    resolves whole verses, not half-verses
  - Esther's Greek-addition lettered chapters ("Esther C:12") -- not
    resolvable to a real chapter number here, so left as citation text
    only, no verse rows (same graceful-miss precedent as everything
    else in this app that can't resolve to real bundled verses)

Not run automatically -- import and call parse_citation() from whatever
builds content/lectionary/<year>.json, or run this file directly against
a *-source.json to see every citation it can/can't parse.
"""
import re

BOOK_NAME_TO_ID = {
    "1 chronicles": "1chr", "2 chronicles": "2chr",
    "1 corinthians": "1cor", "2 corinthians": "2cor",
    "1 john": "1jo", "2 john": "2jo", "3 john": "3jo",
    "1 kings": "1kgs", "2 kings": "2kgs",
    "1 maccabees": "1macc", "2 maccabees": "2macc",
    "1 peter": "1pet", "2 peter": "2pet",
    "1 samuel": "1sam", "2 samuel": "2sam",
    "1 thessalonians": "1the", "2 thessalonians": "2the",
    "1 timothy": "1tim", "2 timothy": "2tim",
    "acts": "acts", "amos": "amos", "baruch": "bar",
    "colossians": "col", "daniel": "dan", "deuteronomy": "deut",
    "ecclesiastes": "eccl", "ephesians": "eph", "esther": "esth",
    "exodus": "ex", "ezekiel": "ezek", "ezra": "ezra",
    "galatians": "gal", "genesis": "gen", "habakkuk": "hab",
    "haggai": "hag", "hebrews": "heb", "hosea": "hos", "isaiah": "isa",
    "james": "jas", "jeremiah": "jer", "job": "job", "joel": "joel",
    "john": "jn", "jonah": "jonah", "joshua": "josh", "jude": "jude",
    "judges": "judg", "judith": "jdt", "lamentations": "lam",
    "leviticus": "lev", "luke": "lk", "malachi": "mal", "mark": "mk",
    "matthew": "mt", "micah": "mic", "nahum": "nah", "nehemiah": "neh",
    "numbers": "num", "obadiah": "obad",
    "philippians": "phil", "phiippians": "phil",  # source typo, seen in the wild
    "philemon": "phlm", "proverbs": "pr", "psalm": "ps", "psalms": "ps",
    "revelation": "rev", "romans": "ro", "ruth": "ruth", "sirach": "sir",
    "sirarch": "sir",  # source typo, seen in the wild
    "song of songs": "song", "titus": "titus", "tobit": "tob",
    "wisdom": "wis", "zechariah": "zech", "zephaniah": "zeph",
}

# Books with exactly one chapter -- lectionary citations for these
# routinely omit the chapter number entirely ("Jude 17", not "Jude 1:17").
_SINGLE_CHAPTER_BOOKS = {"obad", "phlm", "2jo", "3jo", "jude"}

_BOOK_RE = re.compile(
    r"^(" + "|".join(sorted((re.escape(k) for k in BOOK_NAME_TO_ID), key=len, reverse=True)) + r")\s+(.*)$",
    re.IGNORECASE,
)
_CROSS_CHAPTER_RE = re.compile(r"^(\d+):(\d+)[a-zA-Z]*\s*[—–-]\s*(\d+):(\d+)[a-zA-Z]*$")
_VERSE_TOKEN_RE = re.compile(r"^(\d+)[a-zA-Z]*(?:\s*[—–-]\s*(\d+)[a-zA-Z]*)?$")
_CROSS_CHAPTER_TAIL_RE = re.compile(r"^(\d+)[a-zA-Z]*\s*[—–]\s*(\d+):(\d+)[a-zA-Z]*$")
_DIGITS_RE = re.compile(r"\d+")


def _fallback_verse_range(token: str):
    """Last resort for a garbled token (glued numbers, extra dashes): use
    the first digit group found as the start and the last as the end."""
    nums = _DIGITS_RE.findall(token)
    if not nums:
        return None
    return int(nums[0]), int(nums[-1])


def _apply_versification_fixes(book_id, ranges):
    """
    The lectionary's citations follow the NAB/Hebrew-continuous numbering,
    which differs from this app's bundled WEB text for a few books:

    - Malachi: Hebrew numbering runs chapter 3 through v.24; WEB (like most
      English Bibles) splits that into a separate chapter 4 at v.19, so
      anything from Hebrew 3:19+ needs to become WEB chapter 4, verse-18.
    - Joel: the lectionary (Vulgate/Septuagint chapter split) cites a
      chapter 4 that doesn't exist in WEB's 3-chapter Hebrew numbering --
      Vulgate Joel 4 is WEB Joel 3, verse numbers unchanged.
    - Most other mismatches here are individual psalms whose sung heading
      ("For the choirmaster. A psalm of David...") NAB counts as verse 1
      and WEB doesn't -- the citation just runs exactly one verse past
      WEB's real last verse. Not corrected here: the app's own verse
      lookup already only returns verses that exist, so an overshooting
      verse_end silently just stops at the real last verse instead of
      erroring -- a minor, graceful truncation, not a bug.
    """
    if book_id == "mal":
        fixed = []
        for (cs, vs, ce, ve) in ranges:
            def shift(chapter, verse):
                return (4, verse - 18) if chapter == 3 and verse >= 19 else (chapter, verse)
            ncs, nvs = shift(cs, vs)
            nce, nve = shift(ce, ve)
            fixed.append((ncs, nvs, nce, nve))
        return fixed
    if book_id == "joel":
        fixed = []
        for (cs, vs, ce, ve) in ranges:
            def shift(chapter, verse):
                return (3, verse) if chapter == 4 else (chapter, verse)
            ncs, nvs = shift(cs, vs)
            nce, nve = shift(ce, ve)
            fixed.append((ncs, nvs, nce, nve))
        return fixed
    return ranges


def parse_citation(citation: str):
    """
    Returns (book_id, [(chapter_start, verse_start, chapter_end, verse_end), ...])
    or (None, []) if the book or a chapter number can't be resolved
    (e.g. Esther's lettered Greek-addition chapters).
    """
    citation = citation.split(" or ")[0].strip().rstrip(".")
    m = _BOOK_RE.match(citation)
    if not m:
        return None, []
    book_name, rest = m.group(1).lower(), m.group(2).strip()
    book_id = BOOK_NAME_TO_ID[book_name]

    if ":" not in rest and book_id in _SINGLE_CHAPTER_BOOKS:
        rest = f"1:{rest}"

    ranges = []
    for segment in rest.split(";"):
        segment = segment.strip()
        if not segment:
            continue

        # whole segment is one cross-chapter span, e.g. "1:5—2:2"
        cc = _CROSS_CHAPTER_RE.match(segment)
        if cc:
            ranges.append((int(cc.group(1)), int(cc.group(2)), int(cc.group(3)), int(cc.group(4))))
            continue

        if ":" not in segment:
            return None, []  # e.g. Esther "C:12" already failed the book/chapter split upstream
        chapter_str, verse_list_str = segment.split(":", 1)
        chapter_str = chapter_str.strip()
        if not chapter_str.isdigit():
            return None, []
        chapter = int(chapter_str)

        for token in re.split(r",|\band\b", verse_list_str):
            token = token.strip()
            if not token:
                continue
            tail = _CROSS_CHAPTER_TAIL_RE.match(token)
            if tail:
                ranges.append((chapter, int(tail.group(1)), int(tail.group(2)), int(tail.group(3))))
                chapter = int(tail.group(2))
                continue
            vt = _VERSE_TOKEN_RE.match(token)
            if vt:
                vs = int(vt.group(1))
                ve = int(vt.group(2)) if vt.group(2) else vs
            else:
                fb = _fallback_verse_range(token)
                if fb is None:
                    return None, []
                vs, ve = fb
            ranges.append((chapter, vs, chapter, ve))

    return book_id, _apply_versification_fixes(book_id, ranges)


if __name__ == "__main__":
    import json
    import sys

    path = sys.argv[1] if len(sys.argv) > 1 else "app/src/main/assets/content/lectionary/2026-source.json"
    d = json.load(open(path))
    total = 0
    failed = []
    for day in d["days"]:
        for role, citation in day["readings"].items():
            total += 1
            book_id, ranges = parse_citation(citation)
            if book_id is None or not ranges:
                failed.append((day["date"], role, citation))
    print(f"{total} citations, {len(failed)} failed to parse", file=sys.stderr)
    for date, role, citation in failed:
        print(f"  {date} {role}: {citation}", file=sys.stderr)
