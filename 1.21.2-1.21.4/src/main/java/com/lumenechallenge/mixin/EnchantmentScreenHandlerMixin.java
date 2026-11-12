package com.lumenechallenge.mixin;

import com.lumenechallenge.challenge.ChallengeHooks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.EnchantmentScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentScreenHandler.class)
public abstract class EnchantmentScreenHandlerMixin {
    @Inject(method = "onButtonClick", at = @At("TAIL"))
    private void lumenechallenge$onButtonClick(PlayerEntity player, int id, CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(cir.getReturnValue())) {
            return;
        }
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        EnchantmentScreenHandler handler = (EnchantmentScreenHandler) (Object) this;
        ItemStack stack = handler.getSlot(0).getStack();
        if (!stack.isEmpty()) {
            ChallengeHooks.onItemEnchanted(serverPlayer, stack);
        }
    }
}
