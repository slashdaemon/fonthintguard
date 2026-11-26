# Font Hinting Guard

A tiny client-only NeoForge mod for Minecraft **1.21.1** that turns off TrueType font hinting, so a malicious font in a resource pack can't reach FreeType's hinting interpreter.

## Why

In November 2026 a remote-code-execution bug in FreeType's TrueType hinting ("Minecraft2Shell", fixed upstream in FreeType 2.15.0) was disclosed. A booby-trapped `.ttf` inside a resource pack is enough to trigger it. Mojang fixed the 26.x versions by shipping its own FreeType build, but Minecraft 1.20.5 to 1.21.11 still use stock LWJGL 3.3.3 with FreeType 2.13.2, which is affected.

## What it does

Minecraft 1.21.1 loads TrueType glyphs in exactly two places, both through `FT_Load_Glyph`:

| Class | Method | Purpose |
|---|---|---|
| `com.mojang.blaze3d.font.TrueTypeGlyphProvider` | `getGlyph` | measures the glyph |
| `com.mojang.blaze3d.platform.NativeImage` | `copyFromFont` | renders the glyph |

The mod adds `FT_LOAD_NO_HINTING | FT_LOAD_NO_AUTOHINT` to both calls. With `FT_LOAD_NO_HINTING`, FreeType's TrueType loader never prepares or runs any of the font's bytecode (`fpgm`, `prep` or glyph programs; see `tt_loader_init` in FreeType 2.13.2's `ttgload.c`), so the vulnerable interpreter is never reached.

- Both mixins are required. If either can't apply, the game stops at startup instead of running unprotected.
- At startup the client log shows `[FontHintGuard] TrueType hinting disabled (2 sites, flags 0x8002)`.
- Vanilla's own fonts are bitmap fonts and look exactly the same. Text from TrueType fonts (some resource packs and mods) may look slightly softer at small sizes.
- On a dedicated server the mod does nothing, and it doesn't need to be on the server.

## Scope

- Minecraft 1.21.1, NeoForge 21.1.x only.
- It's a mitigation, not a FreeType upgrade: it removes the hinting interpreter from reach.

## Building and testing

```bash
./gradlew build      # jar in build/libs/fonthintguard-<ver>-neoforge-1.21.1.jar
./gradlew test       # FreeType tests (see below)
```

`HintFlagsFreeTypeTest` runs the real FreeType that Minecraft 1.21.1 ships (LWJGL 3.3.3 natives, FreeType 2.13.2) on `hint-canary.ttf`. That is a harmless test font whose glyph program moves the glyph 100 px up. With Minecraft's own load flags the glyph moves, which shows the bytecode ran. With this mod's flags it doesn't move, and the measuring and rendering passes agree on size. `tools/make_test_font.py` (fontTools) builds the font.

`testpack/` is a resource pack that draws every capital A with the canary font. To see the difference in game, load it in `./gradlew runClient` (with the mod) and in `./gradlew runVanillaClient` (without).

## Credits

The approach of turning off hinting at the glyph-load call sites follows the community fixes [Ctrl-Creeper/minecraft2shell-fix](https://github.com/Ctrl-Creeper/minecraft2shell-fix) and [sunrisemc-tw/minecraft2shell-fix](https://github.com/sunrisemc-tw/minecraft2shell-fix). This is an independent minimal implementation for NeoForge 1.21.1.

## License

MIT
