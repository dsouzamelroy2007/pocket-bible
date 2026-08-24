"""
Post-processing pass over already-imported scripture JSON: renders the
divine name as "the LORD" instead of "Yahweh"/"Yah", matching the
convention familiar from Catholic liturgical translations. Run once after
tools/import_epub.py (or import_scripture.py) against a source that uses
the transliterated form -- e.g. World English Bible Classic.

This only ever touches the literal word(s) naming God, and is sentence-
boundary aware so the result reads naturally, not just find/replace:
  - "Yahweh" / "Yahweh's" -> "the LORD" / "the LORD's" (or "The LORD"/
    "The LORD's" when it starts a sentence or opens a quotation).
  - "Yah" (the short poetic form, e.g. in "Praise Yah") -> "the LORD" the
    same way.
  - After a bare vocative "O" (e.g. "O Yahweh, ...") -> "O LORD", since
    English vocative address drops the article ("O the LORD" is not
    idiomatic).
No other wording is changed. Every book file this touches gets a note
appended recording that this pass ran, so the change is visible next to
the text, not just in a commit message.

Usage
-----
    python3 apply_lord_rendering.py ../app/src/main/assets/content/scripture/web-c
"""

import argparse
import json
import re
from pathlib import Path

QUOTE_OPENERS = "“‘\""

PATTERNS = [
    re.compile(r"Yahweh’s\b"),
    re.compile(r"Yahweh\b"),
    re.compile(r"Yah\b"),
]
REPLACEMENTS = ["LORD’s", "LORD", "LORD"]

DISCLOSURE = (
    "Divine-name rendering adjusted from the source text's 'Yahweh'/'Yah' "
    "to 'the LORD' (capitalized 'The LORD' at sentence/quotation starts, "
    "'O LORD' in vocative address), matching the convention used in "
    "Catholic liturgical translations. No other wording was changed."
)


def needs_cap(prefix: str) -> bool:
    s = prefix.rstrip()
    if s == "":
        return True
    if s[-1] in QUOTE_OPENERS:
        return True
    return s[-1] in ".!?"


def is_vocative(prefix: str) -> bool:
    s = prefix.rstrip()
    if not s.endswith("O"):
        return False
    if len(s) == 1:
        return True
    prev = s[-2]
    return prev.isspace() or prev in QUOTE_OPENERS


def substitute(text: str) -> str:
    out = []
    i, n = 0, len(text)
    while i < n:
        matched = False
        for idx, pat in enumerate(PATTERNS):
            m = pat.match(text, i)
            if m:
                prefix = text[:i]
                if is_vocative(prefix):
                    out.append(REPLACEMENTS[idx])
                else:
                    article = "The" if needs_cap(prefix) else "the"
                    out.append(f"{article} {REPLACEMENTS[idx]}")
                i = m.end()
                matched = True
                break
        if not matched:
            out.append(text[i])
            i += 1
    return "".join(out)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("scripture_dir", help="Directory of per-book JSON files to process in place")
    args = parser.parse_args()

    scripture_dir = Path(args.scripture_dir)
    total_files = total_subs = 0
    for path in sorted(scripture_dir.glob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        changed = False
        for v in data["verses"]:
            hits = sum(1 for p in PATTERNS for _ in p.finditer(v["text"]))
            if hits:
                v["text"] = substitute(v["text"])
                total_subs += hits
                changed = True
        if changed:
            note = (data.get("note") or "").strip()
            data["note"] = f"{note} {DISCLOSURE}".strip()
            path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
            total_files += 1
    print(f"Updated {total_files} files, {total_subs} divine-name occurrences replaced.")


if __name__ == "__main__":
    main()
