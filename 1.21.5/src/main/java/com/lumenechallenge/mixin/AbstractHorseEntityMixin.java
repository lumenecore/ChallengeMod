package com.lumenechallenge.mixin;

import com.lumenechallenge.challenge.ChallengeHooks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractHorseEntity.class)
public abstract class AbstractHorseEntityMixin {
    @Inject(method = "setOwner", at = @At("TAIL"))
    private void lumenechallenge$setOwner(LivingEntity owner, CallbackInfo ci) {
        if (owner instanceof ServerPlayerEntity serverPlayer) {
            ChallengeHooks.onTame(serverPlayer, (LivingEntity) (Object) this);
        }
    }
}
