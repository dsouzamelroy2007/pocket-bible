"""
Merges one new Topics feeling -- the feeling record, its verse entries, any
new scripture passages they need, and all 6 languages of translation --
into content/topics.json and content/topics/<language>.json. See
.github/ABOUT.md's "Adding a new feeling (Topics)" section for the full
design/verse-selection/translation process this feeds into.

Batch file format -- a plain Python module (written fresh per feeling,
typically in your scratchpad since it's a one-off authoring artifact, not
checked into the repo) defining these top-level names:

    FEELING = {
        "id": "courage", "label": "Needing courage", "icon": "ti-shield-check",
        "category": "distress",  # must be one of: distress, moral, relational,
                                  # spiritual, thanksgiving, desire (see
                                  # categoryAccent() in ui/theme/*.kt)
        "description": "For when you know what you should do, and just need the nerve to do it.",
        "sort_order": 36,  # one past the current max sort_order in topics.json
        "aliases": [{"alias": "brave", "weight": 1.0}, ...],  # English only, forever
    }

    NEW_PASSAGES = [  # only passages NOT already in topics.json's "passages" pool
        {
            "id": "web-c:josh:1:9", "translation_id": "web-c", "book_id": "josh",
            "chapter_start": 1, "chapter_end": 1, "verse_start": 9, "verse_end": 9,
            "text": "<exact WEB-CE verse text, pulled from content/scripture/web-c/<book>.json -- never paraphrased>",
            "pull_quote": "<short excerpt>", "reference_display": "Joshua 1:9",
            "reference_alt": None,
        },
        ...
    ]

    # (entry_id, passage_id, intensity, depth_order, reflection_en, prayer_en)
    ENTRIES = [
        ("courage-josh-1-9", "web-c:josh:1:9", "acute", 1, "<reflection>", "<prayer>"),
        ...  # exactly 10, intensity acute (first 2-3) / steady (middle 4) / settled (last 2-3)
    ]

    FEELING_TRANSLATIONS = {
        "de": {"label": "...", "description": "..."},
        "fr": {...}, "hi": {...}, "it": {...}, "mr": {...}, "pt": {...},
    }

    # entry_id -> {lang: (reflection, prayer)}
    ENTRY_TRANSLATIONS = {
        "courage-josh-1-9": {
            "de": ("...", "..."), "fr": ("...", "..."), "hi": ("...", "..."),
            "it": ("...", "..."), "mr": ("...", "..."), "pt": ("...", "..."),
        },
        ...
    }

Validates that all 10 entries have all 6 languages present before writing
anything. Re-running with a feeling/entries that already exist is safe --
already-present feelings/entries/passages are skipped, nothing is
duplicated.

Usage:
    python3 tools/merge_topic_feeling.py <batch_module> [<batch_module2> ...] [--path DIR]

After merging, remember to: bump content_version in manifest.json,
./gradlew assembleDebug, install, spot-check the new card (full
description, no truncation) and at least one entry in English and one
other language on a device, then commit.
"""
import argparse
import importlib
import json
import os
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TOPICS_PATH = f"{REPO}/app/src/main/assets/content/topics.json"
LANGS = ["de", "fr", "hi", "it", "mr", "pt"]


def validate(nf):
    assert len(nf.ENTRIES) == 10, f"expected 10 entries, got {len(nf.ENTRIES)}"
    entry_ids = [e[0] for e in nf.ENTRIES]
    assert len(set(entry_ids)) == 10, "duplicate entry ids"
    for lang in LANGS:
        assert lang in nf.FEELING_TRANSLATIONS, f"missing feeling translation for {lang}"
        for eid in entry_ids:
            assert eid in nf.ENTRY_TRANSLATIONS, f"missing entry translations for {eid}"
            assert lang in nf.ENTRY_TRANSLATIONS[eid], f"{eid} missing {lang}"


def merge_topics(nf):
    data = json.load(open(TOPICS_PATH, encoding="utf-8"))

    existing_feeling_ids = {f["id"] for f in data["feelings"]}
    if nf.FEELING["id"] in existing_feeling_ids:
        print(f"SKIP: feeling {nf.FEELING['id']!r} already exists")
        return
    data["feelings"].append(nf.FEELING)
    print(f"Added feeling {nf.FEELING['id']!r} (sort_order={nf.FEELING['sort_order']})")

    existing_passage_ids = {p["id"] for p in data["passages"]}
    added_passages = 0
    for p in nf.NEW_PASSAGES:
        if p["id"] in existing_passage_ids:
            print(f"  SKIP passage {p['id']} (already exists)")
            continue
        data["passages"].append(p)
        added_passages += 1
    print(f"Added {added_passages} new passages")

    existing_entry_ids = {e["id"] for e in data["entries"]}
    added_entries = 0
    for entry_id, passage_id, intensity, depth_order, reflection, prayer in nf.ENTRIES:
        if entry_id in existing_entry_ids:
            print(f"  SKIP entry {entry_id} (already exists)")
            continue
        data["entries"].append({
            "id": entry_id,
            "feeling_id": nf.FEELING["id"],
            "intensity": intensity,
            "depth_order": depth_order,
            "reflection": reflection,
            "prayer": prayer,
            "ccc_reference": None,
            "saint_quote": None,
            "saint_attribution": None,
            "liturgical_season": None,
        })
        data["entry_passages"].append({
            "entry_id": entry_id,
            "passage_id": passage_id,
            "position": 0,
            "role": "primary",
        })
        added_entries += 1
    print(f"Added {added_entries} entries")

    with open(TOPICS_PATH, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print("Wrote", TOPICS_PATH)


def merge_translations(nf):
    for lang in LANGS:
        path = f"{REPO}/app/src/main/assets/content/topics/{lang}.json"
        data = json.load(open(path, encoding="utf-8"))

        existing_ft_ids = {ft["feeling_id"] for ft in data["feeling_translations"]}
        if nf.FEELING["id"] not in existing_ft_ids:
            ft = nf.FEELING_TRANSLATIONS[lang]
            data["feeling_translations"].append({
                "feeling_id": nf.FEELING["id"],
                "label": ft["label"],
                "description": ft["description"],
            })

        existing_et_ids = {et["entry_id"] for et in data["entry_translations"]}
        added = 0
        for entry_id, translations in nf.ENTRY_TRANSLATIONS.items():
            if entry_id in existing_et_ids:
                continue
            reflection, prayer = translations[lang]
            data["entry_translations"].append({
                "entry_id": entry_id,
                "reflection": reflection,
                "prayer": prayer,
            })
            added += 1

        with open(path, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
            f.write("\n")
        print(f"{lang}.json: added {added} entry_translations, feeling_translation "
              f"{'added' if nf.FEELING['id'] not in existing_ft_ids else 'skipped'}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("modules", nargs="+", help="batch module name(s), e.g. new_feeling_courage")
    parser.add_argument("--path", default=".", help="directory the batch module(s) live in (default: CWD)")
    args = parser.parse_args()

    sys.path.insert(0, os.path.abspath(args.path))
    for modname in args.modules:
        nf = importlib.import_module(modname)
        print(f"=== {modname} ===")
        validate(nf)
        merge_topics(nf)
        merge_translations(nf)
