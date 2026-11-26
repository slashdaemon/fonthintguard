# CLAUDE.md

Font Hinting Guard: client-only NeoForge 1.21.1 mod (MIT, public `slashdaemon/fonthintguard`) that adds `FT_LOAD_NO_HINTING | FT_LOAD_NO_AUTOHINT` to Minecraft's two `FT_Load_Glyph` calls. It mitigates the Minecraft2Shell FreeType bug on 1.21.x, which Mojang hasn't patched. It ships inside the TBA pack as a bundled jar, on purpose not on CurseForge or Modrinth (owner decision 2026-11-25; see `TBA/scripts/audit-artifacts.py` `ALLOWED_OVERRIDES`).

## Build and test

Never on this PC (mindfulent). Use a fleet node (`fleet` skill): `bin/fleet sync <node> ../fonthintguard`, then on the node:

```bash
./gradlew build test        # JDK 21; jar in build/libs/
```

The tests need `src/test/resources/hint-canary.ttf`, made by `tools/make_test_font.py` (fontTools) and committed. Regenerate it only if the script changes, and copy it to `testpack/assets/minecraft/font/` too.

## Rules

- Both mixins must stay in sync. `NativeImage.copyFromFont` throws if the render pass's bitmap size differs from the metrics pass in `TrueTypeGlyphProvider.getGlyph`.
- Mixins are `required` with `defaultRequire 1`, and the mod constructor checks both targets carry `Guarded`. Don't relax this: a half-applied guard must crash, never run silently.
- Don't add a kill switch or config. The mod's only job is to keep hinting off.
- Porting to another MC version: re-check the `FT_Load_Glyph` call sites in that version's decompiled `TrueTypeGlyphProvider` and `NativeImage`. 26.x doesn't need this, because Mojang ships FreeType 2.15.0 there.
