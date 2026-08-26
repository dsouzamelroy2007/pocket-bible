# Store assets

Assets for the Google Play listing itself — not part of the installed app, so they
live here rather than under `app/src/main/res/`.

- `play_store_icon_512.png` — the 512×512 app icon for Play Console's Store Listing
  page (Store presence → Main store listing → App icon). Same open-book-and-cross
  design as the in-app launcher icon (`app/src/main/res/mipmap-*/ic_launcher*.png`),
  rendered full-bleed with no adaptive-icon safe-zone padding, since Play applies
  its own icon shape at listing time.
- `listing-copy.md` — draft short/long descriptions and submission notes.
- `screenshots/` — 9 real on-device phone screenshots, cropped to 1440×2880
  (exactly 2:1, within Play's aspect-ratio limit) with the status bar and
  system nav bar removed. Ready to upload as-is under Store presence → Main
  store listing → Phone screenshots.
- `feature_graphic_1024x500.png` — the banner for Store presence → Main
  store listing → Feature graphic. Same navy/gold identity as the app icon,
  built from the icon artwork plus the `01-topics-home.jpg` screenshot in a
  phone frame, generated from `tools/` — regenerate by re-running the build
  script if the icon or headline copy changes rather than editing the PNG
  by hand.

Tablet screenshots are optional unless the listing declares tablet support.
Everything else needed for the Play Console Main store listing page now
exists in this folder.
