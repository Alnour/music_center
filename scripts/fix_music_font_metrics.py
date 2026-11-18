"""Give the bundled Noto Music subset Roboto's vertical metrics.

Noto Music declares an ascent of 1.389 em and a descent of 0.398 em so that tall notation
symbols fit; Roboto, Android's text font, uses 0.927 and 0.244. Android sizes every line of a
paragraph by the tallest font it touched, so a single ♯ set in the music font made the whole
line 1.5 times taller than its neighbours. The five accidentals we ship sit well inside
Roboto's box, so the font's own metrics are replaced with Roboto's and the line heights
match the rest of the screen.

Run after re-subsetting the font (needs fontTools, e.g. in a venv: `pip install fonttools`):
    python3 -I scripts/fix_music_font_metrics.py android/ui/src/main/res/font/noto_music_accidentals.ttf
"""

import sys

from fontTools.ttLib import TTFont

ROBOTO_ASCENT_PER_EM = 1900 / 2048
ROBOTO_DESCENT_PER_EM = 500 / 2048
USE_TYPO_METRICS_BIT = 1 << 7


def main(path):
    font = TTFont(path)
    units_per_em = font["head"].unitsPerEm
    ascent = round(ROBOTO_ASCENT_PER_EM * units_per_em)
    descent = round(ROBOTO_DESCENT_PER_EM * units_per_em)

    hhea = font["hhea"]
    hhea.ascent = ascent
    hhea.descent = -descent
    hhea.lineGap = 0

    os2 = font["OS/2"]
    os2.sTypoAscender = ascent
    os2.sTypoDescender = -descent
    os2.sTypoLineGap = 0
    os2.usWinAscent = ascent
    os2.usWinDescent = descent
    os2.fsSelection = os2.fsSelection | USE_TYPO_METRICS_BIT

    font.save(path)
    print(f"{path}: ascent {ascent}, descent {descent} at {units_per_em} units per em")


if __name__ == "__main__":
    main(sys.argv[1])
