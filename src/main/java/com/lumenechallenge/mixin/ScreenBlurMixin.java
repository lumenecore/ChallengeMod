package com.lumenechallenge.mixin;

import com.lumenechallenge.client.CompleteChallengeScreen;
import com.lumenechallenge.client.SeedSelectionScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenBlurMixin {
    @Inject(method = "applyBlur", at = @At("HEAD"), cancellable = true)
    private void lumenechallenge$disableBlurForCustomScreens(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        Screen current = client.currentScreen;
        if (current instanceof SeedSelectionScreen || current instanceof CompleteChallengeScreen) {
            ci.cancel();
        }
    }
}
