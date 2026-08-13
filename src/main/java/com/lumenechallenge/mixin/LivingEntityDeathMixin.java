package com.lumenechallenge.mixin;

import com.lumenechallenge.challenge.ChallengeHooks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDeathMixin {
    @Inject(method = "onDeath", at = @At("TAIL"))
    private void lumenechallenge$onDeath(DamageSource damageSource, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity player) {
            ChallengeHooks.onPlayerDeath(player);
        }
    }
}
