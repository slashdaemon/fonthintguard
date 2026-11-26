"""Build hint-canary.ttf: a harmless one-glyph TrueType font whose glyph program moves the glyph.

Glyph "A" is a plain box. Its hinting program shifts all four points 100 pixels up (SHPIX on the
y axis). If FreeType runs the program, the glyph comes out 100 px too high; if hinting is off, it
sits where its outline says. The unit tests and the canary resource pack use this to prove the
mod's load flags stop FreeType from running a font's bytecode, without any exploit code.

Usage (needs fontTools): python tools/make_test_font.py <out.ttf>
"""
import sys

from fontTools.fontBuilder import FontBuilder
from fontTools.pens.ttGlyphPen import TTGlyphPen
from fontTools.ttLib.tables import ttProgram

SHIFT_PX = 100


def box():
    pen = TTGlyphPen(None)
    pen.moveTo((100, 0))
    pen.lineTo((100, 600))
    pen.lineTo((500, 600))
    pen.lineTo((500, 0))
    pen.closePath()
    return pen.glyph()


def main(out):
    fb = FontBuilder(1000, isTTF=True)
    fb.setupGlyphOrder([".notdef", "A"])
    fb.setupCharacterMap({ord("A"): "A"})
    fb.setupGlyf({".notdef": TTGlyphPen(None).glyph(), "A": box()})

    program = ttProgram.Program()
    program.fromAssembly([
        "SVTCA[0]",                                   # freedom/projection vector = y axis
        f"PUSH[ ] 0 1 2 3 {SHIFT_PX * 64} 4",          # points, distance (F26Dot6), loop count
        "SLOOP[ ]",                                    # repeat the next instruction for 4 points
        "SHPIX[ ]",                                    # shift each point by the distance
    ])
    fb.font["glyf"]["A"].program = program

    fb.setupHorizontalMetrics({".notdef": (600, 0), "A": (600, 100)})
    fb.setupHorizontalHeader(ascent=800, descent=-200)
    fb.setupNameTable({"familyName": "HintCanary", "styleName": "Regular"})
    fb.setupOS2(sTypoAscender=800, sTypoDescender=-200, usWinAscent=800, usWinDescent=200)
    fb.setupPost()

    maxp = fb.font["maxp"]
    maxp.maxZones = 1
    maxp.maxStackElements = 16
    maxp.maxSizeOfInstructions = len(program.getBytecode())
    maxp.maxTwilightPoints = 0
    maxp.maxStorage = 0
    maxp.maxFunctionDefs = 0
    maxp.maxInstructionDefs = 0
    fb.save(out)
    print(f"wrote {out}")


if __name__ == "__main__":
    main(sys.argv[1])
