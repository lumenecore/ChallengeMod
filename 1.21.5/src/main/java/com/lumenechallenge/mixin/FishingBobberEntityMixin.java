package com.lumenechallenge.mixin;

import com.lumenechallenge.challenge.ChallengeHooks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FishingBobberEntity.class)
public abstract class FishingBobberEntityMixin {
    @Inject(method = "use", at = @At("RETURN"))
    private void lumenechallenge$onUse(ItemStack usedStack, CallbackInfoReturnable<Integer> cir) {
        FishingBobberEntity self = (FishingBobberEntity) (Object) this;
        PlayerEntity owner = self.getPlayerOwner();
        if (!(owner instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        Entity hooked = self.getHookedEntity();
        boolean selfCatch = hooked == owner;
        int result = cir.getReturnValue();
        boolean caughtItem = result == 1 || result == 3;
        boolean caughtSelf = result == 5 && selfCatch;
        if (caughtItem || caughtSelf) {
            ChallengeHooks.onFishing(serverPlayer, caughtItem, caughtSelf);
        }
    }
}
