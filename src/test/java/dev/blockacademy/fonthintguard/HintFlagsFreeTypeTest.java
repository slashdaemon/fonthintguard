package dev.blockacademy.fonthintguard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.util.freetype.FreeType.FT_Done_Face;
import static org.lwjgl.util.freetype.FreeType.FT_Done_FreeType;
import static org.lwjgl.util.freetype.FreeType.FT_Get_Char_Index;
import static org.lwjgl.util.freetype.FreeType.FT_Init_FreeType;
import static org.lwjgl.util.freetype.FreeType.FT_Library_Version;
import static org.lwjgl.util.freetype.FreeType.FT_Load_Glyph;
import static org.lwjgl.util.freetype.FreeType.FT_New_Memory_Face;
import static org.lwjgl.util.freetype.FreeType.FT_Set_Pixel_Sizes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FT_Bitmap;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FT_GlyphSlot;

/**
 * Runs the real FreeType that Minecraft 1.21.1 ships (LWJGL 3.3.3, FreeType 2.13.2) on
 * hint-canary.ttf, whose glyph program moves the glyph 100 px up, using the same load flags as
 * Minecraft's two FT_Load_Glyph call sites, with and without this mod's flags.
 */
class HintFlagsFreeTypeTest {
    // Minecraft 1.21.1's flags (decompiled TrueTypeGlyphProvider.getGlyph / NativeImage.copyFromFont)
    private static final int MC_METRICS = 0x400008; // FT_LOAD_BITMAP_METRICS_ONLY | FT_LOAD_NO_BITMAP
    private static final int MC_RENDER = 0x4;       // FT_LOAD_RENDER
    private static final int PIXEL_SIZE = 16;
    private static final int CANARY_SHIFT_PX = 100;

    private static long library;
    private static ByteBuffer fontData;
    private static FT_Face face;
    private static int glyphIndex;

    record Glyph(int left, int top, int width, int rows) {
    }

    @BeforeAll
    static void open() throws IOException {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pp = stack.mallocPointer(1);
            check(FT_Init_FreeType(pp), "FT_Init_FreeType");
            library = pp.get(0);

            IntBuffer major = stack.mallocInt(1), minor = stack.mallocInt(1), patch = stack.mallocInt(1);
            FT_Library_Version(library, major, minor, patch);
            String version = major.get(0) + "." + minor.get(0) + "." + patch.get(0);
            System.out.println("FreeType " + version);
            assertEquals("2.13.2", version, "tests must run on the FreeType Minecraft 1.21.1 ships");

            byte[] bytes;
            try (InputStream in = HintFlagsFreeTypeTest.class.getResourceAsStream("/hint-canary.ttf")) {
                bytes = in.readAllBytes();
            }
            fontData = MemoryUtil.memAlloc(bytes.length).put(bytes).flip();
            check(FT_New_Memory_Face(library, fontData, 0, pp), "FT_New_Memory_Face");
            face = FT_Face.create(pp.get(0));
            check(FT_Set_Pixel_Sizes(face, PIXEL_SIZE, PIXEL_SIZE), "FT_Set_Pixel_Sizes");
            glyphIndex = FT_Get_Char_Index(face, 'A');
            assertTrue(glyphIndex > 0, "canary font maps 'A'");
        }
    }

    @AfterAll
    static void close() {
        if (face != null) FT_Done_Face(face);
        if (library != 0) FT_Done_FreeType(library);
        if (fontData != null) MemoryUtil.memFree(fontData);
    }

    private static void check(int error, String what) {
        assertEquals(0, error, what + " failed with FreeType error " + error);
    }

    private static Glyph load(int flags) {
        check(FT_Load_Glyph(face, glyphIndex, flags), "FT_Load_Glyph(0x" + Integer.toHexString(flags) + ")");
        FT_GlyphSlot slot = face.glyph();
        FT_Bitmap bitmap = slot.bitmap();
        Glyph g = new Glyph(slot.bitmap_left(), slot.bitmap_top(), bitmap.width(), bitmap.rows());
        System.out.println("flags 0x" + Integer.toHexString(flags) + " -> " + g);
        return g;
    }

    @Test
    void vanillaFlagsRunTheFontsProgram() {
        Glyph guarded = load(HintFlags.guard(MC_METRICS));
        Glyph metrics = load(MC_METRICS);
        Glyph render = load(MC_RENDER);
        // The canary's program moved the glyph up by ~100 px in both of Minecraft's passes
        assertTrue(metrics.top() - guarded.top() >= CANARY_SHIFT_PX - 2, "metrics pass ran bytecode");
        assertTrue(render.top() - guarded.top() >= CANARY_SHIFT_PX - 2, "render pass ran bytecode");
    }

    @Test
    void guardedFlagsRunNoBytecode() {
        Glyph metrics = load(HintFlags.guard(MC_METRICS));
        Glyph render = load(HintFlags.guard(MC_RENDER));
        // Outline is 600 units tall at 1000 units/em: about 10 px at 16 px, sitting on the baseline
        assertTrue(metrics.top() >= 8 && metrics.top() <= 12, "glyph not moved: top=" + metrics.top());
        // Minecraft's copyFromFont requires the two passes to agree on the bitmap size
        assertEquals(metrics.width(), render.width(), "width matches between passes");
        assertEquals(metrics.rows(), render.rows(), "rows match between passes");
        assertEquals(metrics.top(), render.top(), "top matches between passes");
    }

    @Test
    void flagsAreNoHintingAndNoAutohint() {
        assertEquals(0x8002, HintFlags.DISABLE_HINTING);
        assertEquals(0x400008 | 0x8002, HintFlags.guard(MC_METRICS));
    }
}
