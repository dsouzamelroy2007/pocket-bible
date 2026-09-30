"""
Backfills name/intro/verse-caption translations for characters that
already exist in content/characters.json -- as opposed to
tools/merge_character.py, which adds a brand-new character (record +
verse refs + optional translations) all at once.

Use this to close gaps where a character exists in English but has no
entry yet in content/character_translations/<language>.json and/or
content/character_verse_ref_translations/<language>.json (the app falls
back to English per-field for whatever is missing, so this is never a
functional requirement -- just closes the translation gap).

Batch file format -- a plain Python module (typically written fresh per
batch in your scratchpad, not checked into the repo) defining these
top-level names:

    TRANSLATIONS = {
        "abiathar": {
            "de": {"name": "Abjatar", "intro": "..."},
            "fr": {"name": "...", "intro": "..."},
            "hi": {...}, "it": {...}, "mr": {...}, "pt": {...},
        },
        ...
    }

    # OPTIONAL, independent of TRANSLATIONS -- keyed by character id then
    # by the character's VERSE_REFS list position (0-indexed, matching
    # content/characters.json's insertion order for that character)
    CAPTION_TRANSLATIONS = {
        "abiathar": {
            0: {"de": "...", "fr": "...", "hi": "...", "it": "...", "mr": "...", "pt": "..."},
            1: {...},
        },
        ...
    }

Both dicts are optional but at least one must be present and non-empty.
A character id not present in characters.json raises ValueError (typo
guard). A caption position that doesn't exist for that character's
verse_refs list also raises ValueError. Idempotent -- an id (or
id+position) already present in a language file is left untouched and
skipped, never overwritten; fix mistakes by editing the language file
directly.

Usage:
    python3 tools/add_character_translations.py <batch_module> [...] --path DIR

After merging, remember to: bump content_version in manifest.json,
./gradlew assembleDebug, spot-check on a device if available, then commit.
"""
import argparse
import importlib
import json
import os
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CHARACTERS_PATH = f"{REPO}/app/src/main/assets/content/characters.json"
LANGS = ["de", "fr", "hi", "it", "mr", "pt"]


def load_valid_ids():
    data = json.load(open(CHARACTERS_PATH, encoding="utf-8"))
    verse_ref_counts = {c["id"]: len(c.get("verse_refs", [])) for c in data["characters"]}
    return set(verse_ref_counts), verse_ref_counts


def merge_batch(bf, character_ids, verse_ref_counts):
    name_translations = getattr(bf, "TRANSLATIONS", {}) or {}
    caption_translations = getattr(bf, "CAPTION_TRANSLATIONS", {}) or {}

    for cid in name_translations:
        if cid not in character_ids:
            raise ValueError(f"unknown character id in TRANSLATIONS: {cid!r}")
    for cid, positions in caption_translations.items():
        if cid not in character_ids:
            raise ValueError(f"unknown character id in CAPTION_TRANSLATIONS: {cid!r}")
        n = verse_ref_counts[cid]
        for pos in positions:
            if not (0 <= pos < n):
                raise ValueError(f"{cid!r} has {n} verse_refs, position {pos} out of range")

    for lang in LANGS:
        changed = False

        path = f"{REPO}/app/src/main/assets/content/character_translations/{lang}.json"
        data = json.load(open(path, encoding="utf-8"))
        existing_ids = {t["character_id"] for t in data["character_translations"]}
        added = 0
        for cid, per_lang in name_translations.items():
            if lang not in per_lang or cid in existing_ids:
                continue
            nt = per_lang[lang]
            data["character_translations"].append({
                "character_id": cid,
                "name": nt["name"],
                "intro": nt["intro"],
            })
            existing_ids.add(cid)
            added += 1
        if added:
            with open(path, "w", encoding="utf-8") as f:
                json.dump(data, f, ensure_ascii=False, indent=2)
                f.write("\n")
            changed = True

        path = f"{REPO}/app/src/main/assets/content/character_verse_ref_translations/{lang}.json"
        data = json.load(open(path, encoding="utf-8"))
        existing_keys = {(t["character_id"], t["position"]) for t in data["character_verse_ref_translations"]}
        added_captions = 0
        for cid, positions in caption_translations.items():
            for pos, per_lang in positions.items():
                if lang not in per_lang or (cid, pos) in existing_keys:
                    continue
                data["character_verse_ref_translations"].append({
                    "character_id": cid,
                    "position": pos,
                    "caption": per_lang[lang],
                })
                existing_keys.add((cid, pos))
                added_captions += 1
        if added_captions:
            with open(path, "w", encoding="utf-8") as f:
                json.dump(data, f, ensure_ascii=False, indent=2)
                f.write("\n")
            changed = True

        print(f"{lang}: +{added} character_translations, +{added_captions} caption translations"
              + ("" if changed else " (no change)"))


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("modules", nargs="+", help="batch module name(s)")
    parser.add_argument("--path", default=".", help="directory the batch module(s) live in (default: CWD)")
    args = parser.parse_args()

    character_ids, verse_ref_counts = load_valid_ids()
    sys.path.insert(0, os.path.abspath(args.path))
    for modname in args.modules:
        bf = importlib.import_module(modname)
        print(f"=== {modname} ===")
        merge_batch(bf, character_ids, verse_ref_counts)
