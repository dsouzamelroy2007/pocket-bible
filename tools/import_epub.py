"""
Converts an eBible.org-style EPUB Bible (one XHTML file per book, Haiola-
generated markup) into this app's per-book scripture JSON files.

This is a second, more direct import path alongside import_scripture.py's
VPL-based one -- eBible.org publishes most of its translations as EPUBs (in
addition to plain-text/USFM), and the EPUB's XHTML already carries clean,
structured chapter/verse markup, so it's parsed directly rather than
converted to an intermediate format first.

This script contains no Bible text itself. It only reshapes whatever EPUB
you point it at, and only from a book file you provide -- see the project
README's scripture-import section for why bulk scripture text is never
generated or machine-translated here.

How the markup maps to chapters/verses (Haiola/eBible EPUB convention):
  - Each verse's first word is preceded by <span class="verse" id="XX#_#">,
    where the id is <book-prefix><chapter>_<verse>. The verse text itself is
    the sibling content that follows, up to the next verse span.
  - A chapter (or major-section) heading carries an id ending in "_0"
    (e.g. id="PS3_0" on a class='psalmlabel' div) -- used to track the
    current chapter, its own label text is not verse content.
  - Headings, footnotes, cross-references, and speaker labels all carry
    their own recognizable classes (mt/s/d/sp/footnote/x/...) and are
    excluded from the extracted text; poetry/paragraph/character-style
    classes (p/q/q2/m/wj/...) are kept.
  - A handful of bridged verses (e.g. id="SR16_15-16") are stored under
    their first verse number, same as most Bible software does.

Usage
-----
    python3 import_epub.py path/to/bible.epub \
        --translation-id web-c \
        --language en \
        --license public-domain \
        --source "eBible.org World English Bible Classic" \
        --book-map book_map_web_classic.json \
        --out-dir ../app/src/main/assets/content/scripture/web-c

book-map.json is {"<epub-file-stem>": "<app-book-id>", ...}, e.g.
{"GEN": "gen", "PSA": "ps", "MAT": "mt", ...} -- only books present in the
map are extracted; everything else in the EPUB (front matter, glossary,
books outside this app's canon) is ignored.
"""

import argparse
import json
import re
import zipfile
from pathlib import Path

from bs4 import BeautifulSoup, NavigableString, Tag

EXCLUDE_CLASSES = {
    "b", "bibleref", "d", "f", "fe", "fl", "footnote", "fq", "fqa", "fk",
    "fr", "fv", "fdc", "ft", "ili", "ip", "ipi", "im", "io", "io1", "io2",
    "iot", "is", "is1", "is2", "ms", "ms1", "ms2", "mt", "mt1", "mt2", "mt3",
    "mte", "notebackref", "notemark", "noteref", "psalmlabel", "r", "rem",
    "rq", "s", "s1", "s2", "sp", "sts", "tnav", "x", "xk", "xo", "xt",
}

ID_RE = re.compile(r"^[A-Za-z0-9]+?(\d+)_(\d+(?:-\d+)?)$")


def has_class(tag: Tag, cls: str) -> bool:
    classes = tag.get("class") or []
    return cls in classes


def any_excluded_class(tag: Tag) -> bool:
    classes = tag.get("class") or []
    return any(c in EXCLUDE_CLASSES for c in classes)


class BookExtractor:
    def __init__(self):
        self.chapter = 0
        self.verse = 0
        self.buffer: list[str] = []
        self.verses: list[dict] = []

    def flush(self):
        if self.verse > 0:
            text = "".join(self.buffer)
            text = re.sub(r"\s+", " ", text).strip()
            if text:
                self.verses.append({"chapter": self.chapter, "verse": self.verse, "text": text})
        self.buffer = []

    def walk(self, node):
        if isinstance(node, NavigableString):
            if self.verse > 0:
                self.buffer.append(str(node))
            return
        if not isinstance(node, Tag):
            return

        id_attr = node.get("id")
        if id_attr:
            m = ID_RE.match(id_attr)
            if m:
                chapter = int(m.group(1))
                verse_part = m.group(2).split("-")[0]
                verse = int(verse_part)
                self.flush()
                self.chapter = chapter
                self.verse = verse if has_class(node, "verse") else 0
                return  # marker itself carries no body text; don't recurse

        if any_excluded_class(node):
            return

        for child in node.contents:
            self.walk(child)

    def extract(self, main_div: Tag) -> list[dict]:
        for child in main_div.contents:
            self.walk(child)
        self.flush()
        return self.verses


def load_book_html(epub_zip: zipfile.ZipFile, stem: str) -> str | None:
    for name in epub_zip.namelist():
        if name.lower().endswith(f"/{stem.lower()}.xhtml") or name.lower() == f"oebps/{stem.lower()}.xhtml":
            return epub_zip.read(name).decode("utf-8")
    return None


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("epub", help="Path to the source .epub")
    parser.add_argument("--translation-id", required=True)
    parser.add_argument("--language", required=True)
    parser.add_argument("--license", required=True)
    parser.add_argument("--source", required=True)
    parser.add_argument("--book-map", required=True, help="JSON: {epub-file-stem: app-book-id}")
    parser.add_argument("--out-dir", required=True)
    parser.add_argument("--note", default="")
    args = parser.parse_args()

    book_map = json.loads(Path(args.book_map).read_text(encoding="utf-8"))
    out_dir = Path(args.out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)

    manifest_lines = []
    with zipfile.ZipFile(args.epub) as z:
        for stem, book_id in book_map.items():
            html = load_book_html(z, stem)
            if html is None:
                print(f"WARNING: {stem} not found in epub, skipping ({book_id})")
                continue
            soup = BeautifulSoup(html, "html.parser")
            main_div = soup.find("div", class_="main")
            if main_div is None:
                print(f"WARNING: no main div in {stem}, skipping ({book_id})")
                continue
            verses = BookExtractor().extract(main_div)
            if not verses:
                print(f"WARNING: no verses extracted for {stem} ({book_id})")
                continue

            out = {
                "translation_id": args.translation_id,
                "book_id": book_id,
                "language": args.language,
                "license": args.license,
                "source": args.source,
                "verified": True,
                "note": args.note,
                "verses": verses,
            }
            out_path = out_dir / f"{book_id}.json"
            out_path.write_text(json.dumps(out, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
            chapters = len({v["chapter"] for v in verses})
            print(f"{stem} -> {book_id}: {chapters} chapters, {len(verses)} verses -> {out_path}")
            manifest_lines.append(
                f'    {{"translation_id": "{args.translation_id}", "book_id": "{book_id}", '
                f'"path": "content/scripture/{args.translation_id}/{book_id}.json"}}'
            )

    print("\nmanifest.json \"scripture\" entries:")
    print(",\n".join(manifest_lines))


if __name__ == "__main__":
    main()
