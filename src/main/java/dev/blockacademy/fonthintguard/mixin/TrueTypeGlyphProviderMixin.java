package dev.blockacademy.fonthintguard.mixin;

import com.mojang.blaze3d.font.TrueTypeGlyphProvider;
import dev.blockacademy.fonthintguard.Guarded;
import dev.blockacademy.fonthintguard.HintFlags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * The metrics pass: getGlyph loads each glyph with FT_LOAD_BITMAP_METRICS_ONLY | FT_LOAD_NO_BITMAP
 * to size it. It must use the same hinting mode as the render pass in NativeImage, or the bitmap
 * sizes differ and NativeImage.copyFromFont throws.
 */
@Mixin(TrueTypeGlyphProvider.class)
public abstract class TrueTypeGlyphProviderMixin implements Guarded {
    @ModifyArg(
            method = "getGlyph",
            at = @At(value = "INVOKE",
                    target = "Lorg/lwjgl/util/freetype/FreeType;FT_Load_Glyph(Lorg/lwjgl/util/freetype/FT_Face;II)I"),
            index = 2)
    private int fonthintguard$noHinting(int flags) {
        return HintFlags.guard(flags);
    }
}
