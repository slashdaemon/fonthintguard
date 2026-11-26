package dev.blockacademy.fonthintguard;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

/**
 * NeoForge client-only entry point. On a dedicated server this class is never constructed and the jar
 * does nothing.
 */
@Mod(value = FontHintGuard.MOD_ID, dist = Dist.CLIENT)
public final class FontHintGuard {
    public static final String MOD_ID = "fonthintguard";

    public FontHintGuard() {
        GuardCheck.verify();
    }
}
