"""
Backfills feeling/entry translations for a language into
content/topics/<language>.json, for feelings and entries that already
exist in topics.json (English source of truth). Unlike
merge_topic_feeling.py -- which adds a brand-new feeling across all six
languages at once -- this tool is for the opposite situation: a language
(e.g. Spanish) that has UI support but no content/topics/<language>.json
yet, and needs the existing feelings/entries translated into it,
feeling by feeling, in batches, across many runs.

Batch file format -- a plain Python module (written fresh per batch,
typically in your scratchpad since it's a one-off authoring artifact, not
checked into the repo) defining these top-level names:

    LANGUAGE = "es"

    FEELING_TRANSLATIONS = {
        "fear": {"label": "Con miedo", "description": "..."},
        ...
    }

    ENTRY_TRANSLATIONS = {
        "fear-isa-41-10": {"reflection": "...", "prayer": "..."},
        ...
    }

Both dicts are optional (a batch can do only feelings, only entries, or
both). Keys are validated against topics.json's real feeling/entry ids
so a typo fails loudly instead of silently writing an orphaned row.

Re-running with ids that already have a translation in the target
language file is safe -- skipped, not duplicated or overwritten (edit
the language file directly for corrections).

Usage:
    python3 tools/add_topic_translations.py <batch_module> [<batch_module2> ...] --path DIR

After merging, remember to: bump content_version in manifest.json (and
add a "topic_translations" entry to manifest.json for this language, if
this is the first batch for it), ./gradlew assembleDebug, spot-check a
few feelings/entries on a device, then commit.
"""
import argparse
import importlib
import json
import os
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TOPICS_PATH = f"{REPO}/app/src/main/assets/content/topics.json"


def load_valid_ids():
    data = json.load(open(TOPICS_PATH, encoding="utf-8"))
    feeling_ids = {f["id"] for f in data["feelings"]}
    entry_ids = {e["id"] for e in data["entries"]}
    return feeling_ids, entry_ids


def merge_batch(bf, feeling_ids, entry_ids):
    lang = bf.LANGUAGE
    path = f"{REPO}/app/src/main/assets/content/topics/{lang}.json"

    if os.path.exists(path):
        data = json.load(open(path, encoding="utf-8"))
    else:
        data = {"language": lang, "feeling_translations": [], "entry_translations": []}

    existing_feelings = {t["feeling_id"] for t in data["feeling_translations"]}
    existing_entries = {t["entry_id"] for t in data["entry_translations"]}

    added_feelings = 0
    for fid, fields in getattr(bf, "FEELING_TRANSLATIONS", {}).items():
        if fid not in feeling_ids:
            raise ValueError(f"Unknown feeling id {fid!r} -- not in topics.json")
        if fid in existing_feelings:
            continue
        data["feeling_translations"].append({
            "feeling_id": fid,
            "label": fields["label"],
            "description": fields["description"],
        })
        added_feelings += 1

    added_entries = 0
    for eid, fields in getattr(bf, "ENTRY_TRANSLATIONS", {}).items():
        if eid not in entry_ids:
            raise ValueError(f"Unknown entry id {eid!r} -- not in topics.json")
        if eid in existing_entries:
            continue
        data["entry_translations"].append({
            "entry_id": eid,
            "reflection": fields["reflection"],
            "prayer": fields["prayer"],
        })
        added_entries += 1

    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")

    print(f"{lang}: +{added_feelings} feeling_translations, +{added_entries} entry_translations "
          f"(now {len(data['feeling_translations'])}/{len(data['entry_translations'])} total)")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("modules", nargs="+", help="batch module name(s)")
    parser.add_argument("--path", default=".", help="directory the batch module(s) live in (default: CWD)")
    args = parser.parse_args()

    feeling_ids, entry_ids = load_valid_ids()
    sys.path.insert(0, os.path.abspath(args.path))
    for modname in args.modules:
        bf = importlib.import_module(modname)
        print(f"=== {modname} ===")
        merge_batch(bf, feeling_ids, entry_ids)
