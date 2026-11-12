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
     * The intermediary method id is stable across 1.21.9-1.21.11, but its
     * descriptor changed in 1.21.11 from levelUp() to levelUp(ServerWorld).
     * Matching by intermediary name only keeps one injection compatible with
     * both signatures. The handler intentionally does not capture target args.
     */
    @Inject(method = "method_16918", at = @At("TAIL"), remap = false)
    private void lumenechallenge$onLevelUp(CallbackInfo ci) {
        ChallengeHooks.onVillagerLevelUp((VillagerEntity) (Object) this);
    }
}
