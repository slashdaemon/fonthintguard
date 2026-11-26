# Changelog

## [1.1.0]

- Minecraft 1.20.5 through 1.21.11 on Fabric and NeoForge (was NeoForge 1.21.1 only). Five jars from one source tree: NeoForge 20.6–21.1 and 21.2–21.11; Fabric 1.20.5–1.21.1, 1.21.2–1.21.8 and 1.21.9–1.21.11. The Fabric jars need only Fabric Loader (so Quilt should load them; not tested on Quilt).
- 1.21.2+ has the metrics-pass `FT_Load_Glyph` call in `TrueTypeGlyphProvider.loadGlyph` instead of `getGlyph`; the mixin targets the right method per jar. No change in behaviour: same flags (`0x8002`), same fail-safe startup check (now `GuardCheck`, shared by both loaders).
- NeoForge 1.21.1 jar renamed `fonthintguard-1.1.0-neoforge-1.20.6-1.21.1.jar` (it now also covers 1.20.6–1.21). NeoForge 20.5 (MC 1.20.5) is not supported: that line only ever shipped betas, which ModDevGradle cannot resolve, so it cannot be boot-tested. Use the Fabric jar on 1.20.5.
- `smoke/`: boot test harness that starts a real client per Minecraft version / NeoForge line with the canary pack, plus a negative control.

## [1.0.0]

- First release: turns off TrueType hinting (`FT_LOAD_NO_HINTING | FT_LOAD_NO_AUTOHINT`) at both of Minecraft 1.21.1's `FT_Load_Glyph` call sites (`TrueTypeGlyphProvider.getGlyph`, `NativeImage.copyFromFont`). Required mixins plus a startup check, so it never runs half-applied.
- FreeType tests on LWJGL 3.3.3 / FreeType 2.13.2 with the harmless `hint-canary.ttf`, and a canary resource pack for in-game comparison.
