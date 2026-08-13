package com.lumenechallenge.mixin;

import com.lumenechallenge.challenge.ChallengeHooks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    @Inject(method = "wakeUp(ZZ)V", at = @At("HEAD"))
    private void lumenechallenge$wakeUp(boolean bl, boolean updateSleepingPlayers, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity serverPlayer && serverPlayer.getSleepTimer() >= 100) {
            ChallengeHooks.onPlayerSlept(serverPlayer);
        }
    }
}
