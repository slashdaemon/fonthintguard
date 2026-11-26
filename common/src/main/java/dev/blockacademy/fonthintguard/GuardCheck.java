package dev.blockacademy.fonthintguard;

import com.mojang.blaze3d.font.TrueTypeGlyphProvider;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Startup proof that both patches applied, called from each loader's client entry point. Loading the
 * two targets here also makes a missing patch fail at startup, not when a font first loads.
 */
public final class GuardCheck {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void verify() {
        // The two classes that call FT_Load_Glyph in Minecraft 1.20.5 through 1.21.11. Class
        // literals, so Loom remaps them to the runtime (intermediary) names on Fabric.
        Class<?>[] targets = {TrueTypeGlyphProvider.class, NativeImage.class};
        for (Class<?> target : targets) {
            if (!Guarded.class.isAssignableFrom(target)) {
                throw new IllegalStateException("[FontHintGuard] patch not applied to " + target.getName());
            }
        }
        LOGGER.info("[FontHintGuard] TrueType hinting disabled ({} sites, flags 0x{})",
                targets.length, Integer.toHexString(HintFlags.DISABLE_HINTING));
    }

    private GuardCheck() {
    }
}
