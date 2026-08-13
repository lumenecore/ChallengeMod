package com.lumenechallenge.mixin;

import com.lumenechallenge.challenge.ChallengeHooks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin {
    @Inject(method = "onTakeOutput", at = @At("TAIL"))
    private void lumenechallenge$onTakeOutput(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        if (!stack.isEmpty() && stack.hasEnchantments()) {
            ChallengeHooks.onItemEnchanted(serverPlayer, stack);
        }
    }
}
