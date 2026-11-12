package com.lumenechallenge.mixin;

import com.lumenechallenge.challenge.ChallengeHooks;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityRidingMixin {
    @Inject(method = "startRiding(Lnet/minecraft/entity/Entity;ZZ)Z", at = @At("TAIL"))
    private void lumenechallenge$onStartedRiding(Entity vehicle, boolean force, boolean emitEvent,
                                                 CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            ChallengeHooks.onEntityStartedRiding((Entity) (Object) this, vehicle);
        }
    }
}
