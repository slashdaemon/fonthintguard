package dev.blockacademy.fonthintguard;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Client-only entry point. On a dedicated server this class is never constructed and the jar does
 * nothing.
 */
@Mod(value = FontHintGuard.MOD_ID, dist = Dist.CLIENT)
public final class FontHintGuard {
    public static final String MOD_ID = "fonthintguard";
    private static final Logger LOGGER = LogUtils.getLogger();

    // The two classes that call FT_Load_Glyph in Minecraft 1.21.1
    private static final String[] TARGETS = {
            "com.mojang.blaze3d.font.TrueTypeGlyphProvider",
            "com.mojang.blaze3d.platform.NativeImage",
    };

    public FontHintGuard() {
        // Load both targets now so a missing patch fails at startup, not when a font first loads.
        for (String name : TARGETS) {
            Class<?> target;
            try {
                target = Class.forName(name, false, FontHintGuard.class.getClassLoader());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("[FontHintGuard] target class missing: " + name, e);
            }
            if (!Guarded.class.isAssignableFrom(target)) {
                throw new IllegalStateException("[FontHintGuard] patch not applied to " + name);
            }
        }
        LOGGER.info("[FontHintGuard] TrueType hinting disabled ({} sites, flags 0x{})",
                TARGETS.length, Integer.toHexString(HintFlags.DISABLE_HINTING));
    }
}
