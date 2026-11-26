# CLAUDE.md

Font Hinting Guard: client-only mod (MIT, public `slashdaemon/fonthintguard`) for Minecraft 1.20.5 through 1.21.11 on Fabric and NeoForge (Quilt should load the Fabric jars, which use only Fabric Loader, but is not boot-tested). It adds `FT_LOAD_NO_HINTING | FT_LOAD_NO_AUTOHINT` to Minecraft's two `FT_Load_Glyph` calls, which mitigates the Minecraft2Shell FreeType bug that Mojang hasn't patched on those versions. Releases go to GitHub Releases only. The TBA pack bundles a NeoForge jar (1.0.0 `neoforge-1.21.1` as of 1.1.0; its successor is the `neoforge-1.20.6-1.21.1` jar). It is deliberately not on CurseForge or Modrinth (owner decisions 2026-11-25; see `TBA/scripts/audit-artifacts.py` `ALLOWED_OVERRIDES`). MinecraftForge (LexForge) is out of scope.

## Layout

One source tree, five jars. No per-version copies of logic.

| Path | What |
|---|---|
| `common/src/main` | `HintFlags`, `Guarded`, `GuardCheck` (startup proof), `NativeImageMixin`, `fonthintguard.mixins.json` |
| `common/src/getglyph`, `common/src/loadglyph` | The one per-era file: `TrueTypeGlyphProviderMixin` |
| `common/src/test` | FreeType unit tests (root project; only `HintFlags` is compiled for them) |
| `fabric/`, `neoforge/` | Loader entry point + metadata |
| `gradle/fabric.gradle`, `gradle/neoforge.gradle` | Shared per-loader build; `versions/<name>/gradle.properties` holds the coordinates, `glyph_era` and the shipped range |
| `smoke/` | Boot test harness (separate Gradle build): `run.sh` one client, `matrix.sh` every version |

## Jars

| Jar | MC | Built on | Metrics call | Why the split |
|---|---|---|---|---|
| `neoforge-1.20.6-1.21.1` | NeoForge 20.6–21.1 | 21.1.265 | `getGlyph` | call moved at 1.21.2; NeoForge 20.5 is beta-only and ModDevGradle cannot resolve it, so it is left out (Fabric covers MC 1.20.5) |
| `neoforge-1.21.2-1.21.11` | NeoForge 21.2–21.11 | 21.11.48 | `loadGlyph` | |
| `fabric-1.20.5-1.21.1` | 1.20.5–1.21.1 | 1.21.1 | `getGlyph` `method_2040` → `class_379` | |
| `fabric-1.21.2-1.21.8` | 1.21.2–1.21.8 | 1.21.8 | `loadGlyph` `method_61902` → `class_379` | |
| `fabric-1.21.9-1.21.11` | 1.21.9–1.21.11 | 1.21.11 | `loadGlyph` `method_61902` → `class_11759` | `GlyphInfo` became `UnbakedGlyph`; Loom writes full intermediary descriptors into the mixin |

The render pass is `NativeImage.copyFromFont(FT_Face, int)` (`method_4316`) in every version. All of 1.20.5–1.21.11 ship LWJGL 3.3.3 / FreeType 2.13.2, and 1.20.4 ships no FreeType at all. All of this was checked per version against Mojang's jars (javap) and mappings plus Fabric intermediary.

## Build and test

Never on this PC (mindfulent). Use a fleet node (`fleet` skill): `bin/fleet sync <node> ../fonthintguard`, then on the node (JDK 21; on mbp `~/.gradle/jdks/eclipse_adoptium-21-*`; `chmod +x gradlew smoke/*.sh` after a sync):

```bash
./gradlew build test collectJars   # five jars in dist/
smoke/matrix.sh                    # boots every MC version / NeoForge line; smoke/runs/results.txt
```

`smoke/run.sh` boots one real client with one shipped jar and the canary pack enabled. PASS means the startup check logged, the canary pack was in the resource reload, and the client survived the title screen (which draws a capital A through both patched calls). `matrix.sh` ends with a negative control: the getGlyph-era jar forced onto 1.21.3 must FAIL. Run the matrix before every release.

The unit tests need `common/src/test/resources/hint-canary.ttf`, made by `tools/make_test_font.py` (fontTools) and committed. Regenerate it only if the script changes, and copy it to `testpack/assets/minecraft/font/` too.

## Rules

- Both mixins must stay in sync. `NativeImage.copyFromFont` throws if the render pass's bitmap size differs from the metrics pass.
- Mixins are `required` with `defaultRequire 1`, and `GuardCheck` (run from each loader's client entry point) checks both targets carry `Guarded`. Don't relax this: a half-applied guard must crash, never run silently.
- Don't add a kill switch or config. The mod's only job is to keep hinting off.
- Don't widen a jar's range without rerunning the matrix: Fabric mixin targets carry exact intermediary descriptors, and the metrics call has already moved once (1.21.2).
- New MC version: re-check the `FT_Load_Glyph` call sites in its `TrueTypeGlyphProvider` and `NativeImage`, and its intermediary names. 26.x doesn't need this, because Mojang ships FreeType 2.15.0 there.
