"""Builds android/ui/src/main/res/font/noto_music_accidentals.ttf, the five accidentals.

Why a font of our own: Fire OS 7 and some Android TV builds have no glyph for 𝄪 and 𝄫, and
Android draws a box in their place, so C♯ Lydian's double sharp would be unreadable. Why not
Noto Music alone: its accidentals are drawn for a staff, thin and hanging below the baseline,
and next to Roboto's bold capitals they read like footnote marks. So ♭ ♮ ♯ come from
Noto Sans Symbols at weight 700, whose strokes match Roboto Bold, and only 𝄪 𝄫 come from
Noto Music. Every outline is then lifted to where San Francisco places it beside a capital
(sharps and naturals span the baseline to the cap height, flats stand on the baseline, the
double sharp floats mid-height) and the font takes Roboto's vertical metrics, so a line that
holds one accidental is no taller than its neighbours.

Both source fonts are SIL Open Font License 1.1; the licence text ships beside the app's
assets. Needs fontTools (`pip install fonttools` in a venv). Usage:

    python3 -I scripts/build_music_font.py <NotoSansSymbols[wght].ttf> <NotoMusic-Regular.ttf> \
        android/ui/src/main/res/font

Two files come out, `accidentals_regular.ttf` (weight 400) and `accidentals_bold.ttf`
(weight 700), because Android picks the fallback font for the surrounding letters by the
declared weight of the font it was asked for: one bold file would turn every "G B D" bold.
"""

import sys

from fontTools import subset
from fontTools.pens.transformPen import TransformPen
from fontTools.pens.ttGlyphPen import TTGlyphPen
from fontTools.ttLib import TTFont
from fontTools.varLib import instancer

ROBOTO_ASCENT_PER_EM = 1900 / 2048
ROBOTO_DESCENT_PER_EM = 500 / 2048
USE_TYPO_METRICS_BIT = 1 << 7
WEIGHTS = {"regular": 400, "bold": 700}

# Code point, source ("symbols" or "music"), uniform scale, and vertical lift in units at
# 1000 per em.
GLYPHS = [
    (0x266D, "symbols", 1.10, 60),   # ♭
    (0x266E, "symbols", 1.15, 180),  # ♮
    (0x266F, "symbols", 1.10, 180),  # ♯
    (0x1D12A, "music", 1.12, 200),   # 𝄪
    (0x1D12B, "music", 1.12, 40),    # 𝄫
]


def subset_to(font, code_points):
    options = subset.Options()
    options.layout_features = []
    options.name_IDs = ["*"]
    options.notdef_outline = True
    subsetter = subset.Subsetter(options)
    subsetter.populate(unicodes=code_points)
    subsetter.subset(font)


def symbols_at(path, weight):
    """Noto Sans Symbols is a variable font; its 400 and 700 instances match Roboto's."""
    font = TTFont(path)
    instancer.instantiateVariableFont(font, {"wght": weight}, inplace=True)
    return font


def copy_glyph(source, source_name, target, target_name, scale, lift):
    """Copies one outline between fonts of the same units per em, scaled and lifted."""
    pen = TTGlyphPen(target.getGlyphSet())
    transform = (scale, 0, 0, scale, 0, lift)
    source.getGlyphSet()[source_name].draw(TransformPen(pen, transform))
    target["glyf"][target_name] = pen.glyph()
    advance, left_side_bearing = source["hmtx"][source_name]
    target["hmtx"].metrics[target_name] = (round(advance * scale), round(left_side_bearing * scale))


def apply_roboto_metrics(font):
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


def build(symbols_path, music_path, output_path, weight):
    output = symbols_at(symbols_path, weight)
    symbols = symbols_at(symbols_path, weight)
    music = TTFont(music_path)
    for font in (output, music):
        if font["head"].unitsPerEm != 1000:
            raise SystemExit("the lifts assume 1000 units per em")

    # The two Noto Music glyphs join the full Symbols font first, so the subsetter can then
    # rebuild the glyph order, metrics and character maps for all five at once.
    cmap = output["cmap"].getBestCmap()
    glyph_order = output.getGlyphOrder()
    for code, source, scale, lift in GLYPHS:
        if source == "symbols":
            name = cmap[code]
            copy_glyph(symbols, name, output, name, scale, lift)
            continue
        new_name = f"u{code:X}"
        glyph_order.append(new_name)
        output.setGlyphOrder(glyph_order)
        output["maxp"].numGlyphs = len(glyph_order)
        copy_glyph(music, music.getBestCmap()[code], output, new_name, scale, lift)
        for table in output["cmap"].tables:
            if table.isUnicode() and table.format == 12:
                table.cmap[code] = new_name

    # Vertical-writing metrics have no entry for the added glyphs and no use here.
    for tag in ("vmtx", "vhea", "VORG"):
        if tag in output:
            del output[tag]
    subset_to(output, [code for code, _, _, _ in GLYPHS])
    apply_roboto_metrics(output)
    output["OS/2"].usWeightClass = weight
    output["name"].setName("Scales Accidentals", 1, 3, 1, 0x409)
    output["name"].setName("Scales Accidentals", 4, 3, 1, 0x409)
    output.save(output_path)
    print(f"wrote {output_path}: {len(output.getGlyphOrder())} glyphs at weight {weight}")


def main(symbols_path, music_path, output_folder):
    for suffix, weight in WEIGHTS.items():
        build(symbols_path, music_path, f"{output_folder}/accidentals_{suffix}.ttf", weight)


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2], sys.argv[3])
