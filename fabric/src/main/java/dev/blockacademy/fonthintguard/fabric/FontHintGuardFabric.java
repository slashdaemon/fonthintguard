package dev.blockacademy.fonthintguard.fabric;

import dev.blockacademy.fonthintguard.GuardCheck;
import net.fabricmc.api.ClientModInitializer;

/** Fabric client entry point. fabric.mod.json marks the mod client-only. */
public final class FontHintGuardFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        GuardCheck.verify();
    }
}
