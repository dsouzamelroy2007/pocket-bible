"""
Generates store/feature_graphic_1024x500.png -- the Play Console "Feature
graphic" banner (Store presence -> Main store listing -> Feature graphic).

Requires cairosvg (`pip install cairosvg`); not an app build dependency,
just a one-off asset generator. Re-run this after changing the launcher
icon artwork, the headline copy, or the screenshot it embeds, rather than
hand-editing the PNG.
"""

import base64
import os

import cairosvg

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SCREENSHOT = os.path.join(ROOT, "store", "screenshots", "01-topics-home.jpg")
OUT_PATH = os.path.join(ROOT, "store", "feature_graphic_1024x500.png")

with open(SCREENSHOT, "rb") as f:
    screenshot_b64 = base64.b64encode(f.read()).decode()

W, H = 1024, 500

# Device frame geometry (right side); the screenshot is exactly 1:2, matching
# the frame's inner aspect ratio, so it fills it with no distortion or crop.
frame_w, frame_h = 236, 472
frame_x, frame_y = 726, 14
inner_pad = 8
inner_w, inner_h = frame_w - inner_pad * 2, frame_h - inner_pad * 2
rotate_deg = -3
cx, cy = frame_x + frame_w / 2, frame_y + frame_h / 2

svg = f'''<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="{W}" height="{H}" viewBox="0 0 {W} {H}">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="#33447a"/>
      <stop offset="1" stop-color="#151d34"/>
    </linearGradient>
    <linearGradient id="cross" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0" stop-color="#f3d38c"/>
      <stop offset="1" stop-color="#c48b32"/>
    </linearGradient>
    <radialGradient id="glow" cx="0.5" cy="0.5" r="0.5">
      <stop offset="0" stop-color="#ffe9b8" stop-opacity="0.35"/>
      <stop offset="1" stop-color="#ffe9b8" stop-opacity="0"/>
    </radialGradient>
    <clipPath id="frameClip">
      <rect x="{frame_x + inner_pad}" y="{frame_y + inner_pad}" width="{inner_w}" height="{inner_h}" rx="18"/>
    </clipPath>
  </defs>

  <rect width="{W}" height="{H}" fill="url(#bg)"/>

  <!-- soft ambient glow behind the whole composition -->
  <circle cx="330" cy="230" r="260" fill="url(#glow)"/>

  <!-- icon glyph: open book + cross, matching the launcher icon -->
  <g transform="translate(38,34) scale(0.22)">
    <circle cx="256" cy="190" r="150" fill="url(#glow)"/>
    <rect x="236" y="110" width="40" height="230" rx="4" fill="url(#cross)"/>
    <rect x="176" y="170" width="160" height="40" rx="4" fill="url(#cross)"/>
    <path d="M130 358 L256 330 L256 434 L130 462 Z" fill="#f5eedd"/>
    <path d="M382 358 L256 330 L256 434 L382 462 Z" fill="#eee4cc"/>
    <rect x="252" y="326" width="8" height="112" fill="#d9c9a4"/>
  </g>

  <text x="42" y="205" font-family="Liberation Serif, serif" font-weight="700" font-size="72" fill="#f6ecd8">Pocket Bible</text>
  <text x="44" y="256" font-family="Liberation Sans, sans-serif" font-size="27" fill="#e6d9bd">A verse for how you feel today</text>

  <g font-family="Liberation Sans, sans-serif" font-size="21" fill="#d8cdb8">
    <text x="44" y="332">&#8226;  27 feelings matched to real Scripture, with a reflection and a prayer</text>
    <text x="44" y="368">&#8226;  Full Bible text in 7 languages — read, save, and bookmark</text>
    <text x="44" y="404">&#8226;  114 Bible personalities, with the verses that tell their story</text>
    <text x="44" y="440">&#8226;  Works fully offline. No account, no ads, no data collected</text>
  </g>

  <!-- phone frame with a real screenshot inside -->
  <g transform="rotate({rotate_deg} {cx} {cy})">
    <rect x="{frame_x}" y="{frame_y}" width="{frame_w}" height="{frame_h}" rx="26" fill="#0e1524"/>
    <g clip-path="url(#frameClip)">
      <image x="{frame_x + inner_pad}" y="{frame_y + inner_pad}" width="{inner_w}" height="{inner_h}"
             xlink:href="data:image/jpeg;base64,{screenshot_b64}" preserveAspectRatio="xMidYMid slice"/>
    </g>
    <rect x="{frame_x}" y="{frame_y}" width="{frame_w}" height="{frame_h}" rx="26" fill="none" stroke="#3a3020" stroke-width="3"/>
  </g>
</svg>'''

cairosvg.svg2png(
    bytestring=svg.encode("utf-8"),
    write_to=OUT_PATH,
    output_width=W,
    output_height=H,
)
print("wrote", OUT_PATH)
