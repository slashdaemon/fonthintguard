package dev.blockacademy.fonthintguard;

/**
 * FreeType load flags this mod adds to every glyph load. Kept free of Minecraft references so the
 * unit tests can use the same value.
 */
public final class HintFlags {
    /** FT_LOAD_NO_HINTING: FreeType runs no TrueType bytecode (fpgm, prep or glyph programs). */
    public static final int FT_LOAD_NO_HINTING = 0x2;
    /** FT_LOAD_NO_AUTOHINT: and doesn't fall back to the auto-hinter either. */
    public static final int FT_LOAD_NO_AUTOHINT = 0x8000;

    public static final int DISABLE_HINTING = FT_LOAD_NO_HINTING | FT_LOAD_NO_AUTOHINT;

    public static int guard(int flags) {
        return flags | DISABLE_HINTING;
    }

    private HintFlags() {
    }
}
