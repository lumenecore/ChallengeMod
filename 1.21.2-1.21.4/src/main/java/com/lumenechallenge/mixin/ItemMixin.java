package com.lumenechallenge.mixin;

import com.lumenechallenge.challenge.ChallengeHooks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Item.class)
public abstract class ItemMixin {
    @Inject(
            method = "onCraftByPlayer(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;)V",
            at = @At("TAIL")
    )
    private void lumenechallenge$onCraftByPlayer(ItemStack stack, World world, PlayerEntity player, CallbackInfo ci) {
        if (player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
            ChallengeHooks.onCraftedByPlayer(serverPlayer, stack);
        }
    }
}
