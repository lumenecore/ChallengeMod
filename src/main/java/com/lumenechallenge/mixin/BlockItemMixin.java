package com.lumenechallenge.mixin;

import com.lumenechallenge.challenge.ChallengeHooks;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
    @Inject(method = "place", at = @At("RETURN"))
    private void lumenechallenge$onPlace(ItemPlacementContext context, CallbackInfoReturnable<ActionResult> cir) {
        if (!cir.getReturnValue().isAccepted()) {
            return;
        }
        PlayerEntity player = context.getPlayer();
        if (player instanceof ServerPlayerEntity serverPlayer) {
            BlockState placed = context.getWorld().getBlockState(context.getBlockPos());
            ChallengeHooks.onBlockPlaced(serverPlayer, placed);
        }
    }
}
