# Font Hinting Guard

Font Hinting Guard is a small client-side mod for **Minecraft 1.21.1 on NeoForge**. It protects your game against booby-trapped fonts in resource packs.

## The problem

In November 2026 a security bug nicknamed **Minecraft2Shell** was made public. Minecraft draws TrueType fonts with a library called FreeType. FreeType had a bug in the code that runs a font's built-in *hinting* instructions (small programs inside a font that nudge letters onto the pixel grid). A specially made font can use that bug to run code on your computer. All an attacker needs is to get that font into a resource pack you load, for example a server resource pack you accept.

FreeType fixed the bug in version 2.15.0 (upstream commits [`1803559c`](https://github.com/freetype/freetype/commit/1803559c4ee407d0bcbf2a67dbe96690cee869d2) and [`7d600a02`](https://github.com/freetype/freetype/commit/7d600a022e1d813e85a8c94ffd395f6135872267), both in `Ins_SHZ`). Mojang has shipped the fixed FreeType for Minecraft 26.x. **Minecraft 1.20.5 through 1.21.11 still ship FreeType 2.13.2**, which has the bug.

## What this mod does

It tells FreeType to **never run a font's hinting instructions**. The buggy code only runs when hinting is on, so with hinting off a malicious font can't reach it. Fonts still load and display.

- **What you'll notice:** almost nothing. Minecraft's own fonts are pixel (bitmap) fonts and look exactly the same. Text drawn with TrueType fonts from some resource packs or mods may look slightly softer at small sizes.
- **No settings and no off switch.** It does one thing.
- **Fails safe.** If the mod can't apply its changes, the game stops at startup with a clear error, instead of running unprotected.

When it's working, your game log contains:

```
[FontHintGuard] TrueType hinting disabled (2 sites, flags 0x8002)
```

## Install

1. Download `fonthintguard-1.0.0-neoforge-1.21.1.jar` from [Releases](https://github.com/slashdaemon/fonthintguard/releases).
2. Put it in your instance's `mods` folder.

It's only needed on the client. Servers don't need it, and players with or without it can join the same server.

**Supported:** Minecraft 1.21.1 only, with any NeoForge 21.1 release (21.1 is NeoForge's version series for Minecraft 1.21.1). Minecraft 26.x doesn't need it, because Mojang already ships the fixed FreeType there.

## How it works

Minecraft 1.21.1 asks FreeType for a glyph in exactly two places:

| Class | Method | What it does |
|---|---|---|
| `com.mojang.blaze3d.font.TrueTypeGlyphProvider` | `getGlyph` | measures the glyph |
| `com.mojang.blaze3d.platform.NativeImage` | `copyFromFont` | draws the glyph |

The mod uses two small mixins to add `FT_LOAD_NO_HINTING | FT_LOAD_NO_AUTOHINT` (`0x8002`) to the `FT_Load_Glyph` call in each. With `FT_LOAD_NO_HINTING`, FreeType never prepares or runs any of the font's bytecode: the `fpgm`, `prep` and per-glyph programs (`tt_loader_init` in FreeType 2.13.2's `src/truetype/ttgload.c`). Both places must use the same flags, because Minecraft checks that the measured and drawn glyph sizes match.

The mod is a **mitigation, not a FreeType upgrade**. It removes the hinting interpreter, where this bug lives, from reach. It doesn't change the FreeType version.

## How it's tested

We test with a harmless font instead of attack code:

- `tools/make_test_font.py` builds `hint-canary.ttf`: one glyph, a plain box, whose hinting program moves it 100 pixels up. If the hinting code runs, the box jumps; if it doesn't, the box stays put.
- **Unit tests** (`HintFlagsFreeTypeTest`) load that font with the same FreeType Minecraft 1.21.1 ships (LWJGL 3.3.3, FreeType 2.13.2). With Minecraft's normal settings the box moves; with this mod's settings it doesn't, and the measuring and drawing passes agree on its size.
- **In game:** `testpack/` is a resource pack that draws every capital A with the canary font. Load it in `./gradlew runClient` (with the mod): the A sits on the line. Load it in `./gradlew runVanillaClient` (without the mod): the A is pushed out of place.

## Building

Requires JDK 21.

```bash
./gradlew build    # jar: build/libs/fonthintguard-<version>-neoforge-1.21.1.jar
./gradlew test     # FreeType tests
```

## Credits

The idea of turning off hinting where Minecraft loads glyphs comes from the community fixes [Ctrl-Creeper/minecraft2shell-fix](https://github.com/Ctrl-Creeper/minecraft2shell-fix) and [sunrisemc-tw/minecraft2shell-fix](https://github.com/sunrisemc-tw/minecraft2shell-fix). Font Hinting Guard is a separate, minimal implementation for NeoForge 1.21.1 and doesn't use their code.

Made by [The Block Academy](https://theblock.academy).

## License

[MIT](LICENSE)
