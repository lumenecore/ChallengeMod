package com.lumenechallenge.mixin;

import com.lumenechallenge.challenge.ChallengeHooks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityChallengeEventsMixin {
    @Inject(method = "addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;Lnet/minecraft/entity/Entity;)Z", at = @At("RETURN"))
    private void lumenechallenge$effectApplied(StatusEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue())) {
            LivingEntity entity = (LivingEntity) (Object) this;
            if (entity instanceof net.minecraft.server.network.ServerPlayerEntity player) {
                ChallengeHooks.onPlayerEffectApplied(player, effect);
            } else {
                ChallengeHooks.onEffectApplied(entity, effect);
            }
        }
    }

    @Inject(method = "setOnFireForTicks", at = @At("HEAD"))
    private void lumenechallenge$beforeIgnition(int ticks, CallbackInfo ci) {
        // Snapshot the previous state; the TAIL injection records only a real ignition transition.
        lumenechallenge$wasOnFire = ((LivingEntity) (Object) this).isOnFire();
    }

    @Inject(method = "setOnFireForTicks", at = @At("TAIL"))
    private void lumenechallenge$afterIgnition(int ticks, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!lumenechallenge$wasOnFire && entity.isOnFire()) {
            ChallengeHooks.onMobBurned(entity);
        }
    }

    @org.spongepowered.asm.mixin.Unique
    private boolean lumenechallenge$wasOnFire;
}
