package com.lumenechallenge.mixin;

import com.lumenechallenge.challenge.ChallengeHooks;
import net.minecraft.entity.passive.VillagerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin {
    /**
     * The intermediary method id is used without a descriptor so the callback
     * does not depend on the target method argument list.
     */
    @Inject(method = "method_16918", at = @At("TAIL"), remap = false)
    private void lumenechallenge$onLevelUp(CallbackInfo ci) {
        ChallengeHooks.onVillagerLevelUp((VillagerEntity) (Object) this);
    }
}
