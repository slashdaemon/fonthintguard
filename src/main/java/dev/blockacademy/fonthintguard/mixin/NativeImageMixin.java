package dev.blockacademy.fonthintguard.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import dev.blockacademy.fonthintguard.Guarded;
import dev.blockacademy.fonthintguard.HintFlags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** The render pass: copyFromFont loads the glyph with FT_LOAD_RENDER into a grayscale bitmap. */
@Mixin(NativeImage.class)
public abstract class NativeImageMixin implements Guarded {
    @ModifyArg(
            method = "copyFromFont",
            at = @At(value = "INVOKE",
                    target = "Lorg/lwjgl/util/freetype/FreeType;FT_Load_Glyph(Lorg/lwjgl/util/freetype/FT_Face;II)I"),
            index = 2)
    private int fonthintguard$noHinting(int flags) {
        return HintFlags.guard(flags);
    }
}
