"""
Merges one new Biblical character -- the character record, its verse
citations, and (optionally) translations into all 6 languages -- into
content/characters.json and content/character_translations/<language>.json
/ content/character_verse_ref_translations/<language>.json. See
.github/ABOUT.md's "Adding a new Biblical character" section for the full
process this feeds into.

Batch file format -- a plain Python module (written fresh per character,
typically in your scratchpad since it's a one-off authoring artifact, not
checked into the repo) defining these top-level names:

    CHARACTER = {
        "id": "deborah", "name": "Deborah", "category": "exodus_judges",
        # category must be one of: central, holy_family, apostles,
        # early_church, women_and_others, opposed_jesus, patriarchs,
        # exodus_judges, kingdom, prophets, post_exile (see
        # character_category_* strings in values/strings.xml)
        "sort_order": 281,  # one past the current max sort_order
        "requires_deuterocanon": False,  # True only if a verse_ref below cites
                                          # Tobit/Judith/Wisdom/Sirach/Baruch/
                                          # 1-2 Maccabees or the Daniel/Esther
                                          # Greek-addition chapters
        "intro": "<one or two sentences, third person, plain -- no reflection/prayer needed for most characters>",
        # reflection/prayer are OPTIONAL -- only ~26/280 existing characters
        # have them (villain-reconciliation content); omit both unless this
        # character genuinely calls for that treatment
        "reflection": None,
        "prayer": None,
    }

    VERSE_REFS = [  # 3-6 is typical; each becomes one CharacterVerseRef row, in order
        {"book_id": "judg", "chapter": 4, "verse_start": 4, "verse_end": 5,
         "caption": "Judging Israel as a prophetess"},
        ...
    ]

    # OPTIONAL -- omit entirely if not translating yet (114/280 characters
    # currently have no translations at all; the app falls back to English
    # per-field, so this is never a functional requirement, just nice-to-have)
    NAME_TRANSLATIONS = {
        "de": {"name": "Debora", "intro": "..."},
        "fr": {...}, "hi": {...}, "it": {...}, "mr": {...}, "pt": {...},
    }

    # OPTIONAL, independent of NAME_TRANSLATIONS -- keyed by the VERSE_REFS
    # list position (0-indexed, matching insertion order)
    CAPTION_TRANSLATIONS = {
        0: {"de": "...", "fr": "...", "hi": "...", "it": "...", "mr": "...", "pt": "..."},
        ...
    }

Does NOT touch character_of_day (the 366-day calendar) -- every day is
already assigned to some character, so putting a new character into
rotation means deliberately choosing which existing day's assignment to
replace, which is a content decision for a human/you to make explicitly
in content/characters.json's "character_of_day" array, not something this
script should do automatically. A character with no calendar day is still
fully valid and reachable from the Personalities tab's list/search.

Re-running with a character that already exists is safe -- skipped, not
duplicated.

Usage:
    python3 tools/merge_character.py <batch_module> [<batch_module2> ...] [--path DIR]

After merging, remember to: bump content_version in manifest.json,
./gradlew assembleDebug, install, spot-check the character on the
Personalities tab (search finds them, verse refs resolve) on a device,
then commit.
"""
import argparse
import importlib
import json
import os
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CHARACTERS_PATH = f"{REPO}/app/src/main/assets/content/characters.json"
LANGS = ["de", "fr", "hi", "it", "mr", "pt"]


def merge_characters(cf):
    data = json.load(open(CHARACTERS_PATH, encoding="utf-8"))

    existing_ids = {c["id"] for c in data["characters"]}
    if cf.CHARACTER["id"] in existing_ids:
        print(f"SKIP: character {cf.CHARACTER['id']!r} already exists")
        return

    record = dict(cf.CHARACTER)
    record["verse_refs"] = cf.VERSE_REFS
    data["characters"].append(record)
    print(f"Added character {cf.CHARACTER['id']!r} ({len(cf.VERSE_REFS)} verse refs, "
          f"sort_order={cf.CHARACTER['sort_order']})")

    with open(CHARACTERS_PATH, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print("Wrote", CHARACTERS_PATH)


def merge_translations(cf):
    name_translations = getattr(cf, "NAME_TRANSLATIONS", None)
    caption_translations = getattr(cf, "CAPTION_TRANSLATIONS", None)
    if not name_translations and not caption_translations:
        print("No NAME_TRANSLATIONS or CAPTION_TRANSLATIONS defined -- skipping translation merge "
              "(fine: the app falls back to English per-field).")
        return

    for lang in LANGS:
        changed = False

        if name_translations and lang in name_translations:
            path = f"{REPO}/app/src/main/assets/content/character_translations/{lang}.json"
            data = json.load(open(path, encoding="utf-8"))
            existing_ids = {t["character_id"] for t in data["character_translations"]}
            if cf.CHARACTER["id"] not in existing_ids:
                nt = name_translations[lang]
                data["character_translations"].append({
                    "character_id": cf.CHARACTER["id"],
                    "name": nt["name"],
                    "intro": nt["intro"],
                })
                with open(path, "w", encoding="utf-8") as f:
                    json.dump(data, f, ensure_ascii=False, indent=2)
                    f.write("\n")
                changed = True

        if caption_translations:
            path = f"{REPO}/app/src/main/assets/content/character_verse_ref_translations/{lang}.json"
            data = json.load(open(path, encoding="utf-8"))
            existing_keys = {(t["character_id"], t["position"]) for t in data["character_verse_ref_translations"]}
            added = 0
            for position, per_lang in caption_translations.items():
                if lang not in per_lang:
                    continue
                key = (cf.CHARACTER["id"], position)
                if key in existing_keys:
                    continue
                data["character_verse_ref_translations"].append({
                    "character_id": cf.CHARACTER["id"],
                    "position": position,
                    "caption": per_lang[lang],
                })
                added += 1
            if added:
                with open(path, "w", encoding="utf-8") as f:
                    json.dump(data, f, ensure_ascii=False, indent=2)
                    f.write("\n")
                changed = True

        print(f"{lang}: {'updated' if changed else 'no change'}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("modules", nargs="+", help="batch module name(s), e.g. new_character_deborah")
    parser.add_argument("--path", default=".", help="directory the batch module(s) live in (default: CWD)")
    args = parser.parse_args()

    sys.path.insert(0, os.path.abspath(args.path))
    for modname in args.modules:
        cf = importlib.import_module(modname)
        print(f"=== {modname} ===")
        merge_characters(cf)
        merge_translations(cf)
