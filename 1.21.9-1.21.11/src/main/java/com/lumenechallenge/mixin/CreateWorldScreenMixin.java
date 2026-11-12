package com.lumenechallenge.mixin;

import com.lumenechallenge.client.layout.ChallengeModSettings;
import com.lumenechallenge.util.WorldMarkerUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.WorldCreator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;

@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin {
    @Shadow @Final private WorldCreator worldCreator;

    @Inject(method = "createLevel", at = @At("HEAD"))
    private void lumenechallenge$markNewChallengeWorld(CallbackInfo ci) {
        if (!ChallengeModSettings.isChallengeModEnabled()) return;

        String directoryName = worldCreator.getWorldDirectoryName();
        if (directoryName == null || directoryName.isBlank()) return;

        Path savesDirectory = FabricLoader.getInstance().getGameDir().resolve("saves");
        WorldMarkerUtil.mark(savesDirectory.resolve(directoryName));
    }
}
