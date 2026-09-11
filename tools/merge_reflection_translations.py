"""
Merges a batch of translated daily-lectionary-reflection text into
content/reflections/<year>/<language>.json for every language it covers.
See .github/ABOUT.md's "Adding a new lectionary year" section (the
translation step) for the full authoring process this feeds into.

Batch file format -- a plain Python module (written fresh each batch,
typically in your scratchpad since it's a one-off authoring artifact, not
checked into the repo) defining a single top-level dict:

    TRANSLATIONS = {
        "2026-11-17": {
            "de": "...", "fr": "...", "hi": "...",
            "it": "...", "mr": "...", "pt": "...",
        },
        "2026-11-18": { ... },
        ...
    }

Every date must have all 6 languages present (de, fr, hi, it, mr, pt) --
this is enforced before anything is written. Re-running with a batch file
whose dates are already merged is safe: already-present dates are skipped
per language, nothing is duplicated or overwritten.

Usage:
    python3 tools/merge_reflection_translations.py <year> <batch_module> [<batch_module2> ...]

    # batch module resolved via the given --path (defaults to CWD), e.g.
    # if your batch file lives in the session scratchpad:
    python3 tools/merge_reflection_translations.py 2026 reflection_translations_2026_11_batch3 \\
        --path /private/tmp/.../scratchpad

After merging, remember to: bump content_version in manifest.json,
./gradlew assembleDebug, install, spot-check on a device, then commit.
"""
import argparse
import importlib
import json
import os
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LANGS = ["de", "fr", "hi", "it", "mr", "pt"]


def merge(year: int, module_name: str):
    mod = importlib.import_module(module_name)
    translations = mod.TRANSLATIONS

    for date, entry in translations.items():
        missing = [lang for lang in LANGS if lang not in entry]
        if missing:
            raise ValueError(f"{date} in {module_name} is missing languages: {missing}")

    print(f"Merging {len(translations)} days from {module_name}")

    reflections_dir = f"{REPO}/app/src/main/assets/content/reflections/{year}"
    os.makedirs(reflections_dir, exist_ok=True)

    for lang in LANGS:
        path = f"{reflections_dir}/{lang}.json"
        if os.path.exists(path):
            existing = json.load(open(path, encoding="utf-8"))
        else:
            existing = {"year": year, "language": lang, "reflections": []}
        existing_dates = {e["date"] for e in existing["reflections"]}

        added = 0
        for date, entry in translations.items():
            if date in existing_dates:
                continue
            existing["reflections"].append({"date": date, "reflection": entry[lang]})
            added += 1

        existing["reflections"].sort(key=lambda e: e["date"])
        with open(path, "w", encoding="utf-8") as f:
            json.dump(existing, f, ensure_ascii=False, indent=2)
            f.write("\n")
        print(f"  {lang}.json: added {added}, total {len(existing['reflections'])}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("year", type=int)
    parser.add_argument("modules", nargs="+", help="batch module name(s), e.g. reflection_translations_2026_11_batch3")
    parser.add_argument("--path", default=".", help="directory the batch module(s) live in (default: CWD)")
    args = parser.parse_args()

    sys.path.insert(0, os.path.abspath(args.path))
    for modname in args.modules:
        merge(args.year, modname)
