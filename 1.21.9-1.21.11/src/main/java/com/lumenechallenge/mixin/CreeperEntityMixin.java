package com.lumenechallenge.mixin;

import com.lumenechallenge.challenge.ChallengeHooks;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreeperEntity.class)
public abstract class CreeperEntityMixin {
    @Inject(method = "explode", at = @At("HEAD"))
    private void lumenechallenge$detectPlayerTriggeredExplosion(CallbackInfo ci) {
        CreeperEntity creeper = (CreeperEntity) (Object) this;
        if (creeper.getTarget() instanceof ServerPlayerEntity player
                && creeper.squaredDistanceTo(player) < 16.0D * 16.0D) {
            ChallengeHooks.onCreeperExploded(player);
        }
    }
}
