# Changelog

## [1.0.0]

- First release: turns off TrueType hinting (`FT_LOAD_NO_HINTING | FT_LOAD_NO_AUTOHINT`) at both of Minecraft 1.21.1's `FT_Load_Glyph` call sites (`TrueTypeGlyphProvider.getGlyph`, `NativeImage.copyFromFont`). Required mixins plus a startup check, so it never runs half-applied.
- FreeType tests on LWJGL 3.3.3 / FreeType 2.13.2 with the harmless `hint-canary.ttf`, and a canary resource pack for in-game comparison.
