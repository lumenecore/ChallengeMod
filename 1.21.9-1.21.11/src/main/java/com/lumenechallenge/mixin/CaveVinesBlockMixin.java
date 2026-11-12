package com.lumenechallenge.mixin;

import com.lumenechallenge.challenge.ChallengeHooks;
import net.minecraft.block.BlockState;
import net.minecraft.block.CaveVines;
import net.minecraft.block.CaveVinesBodyBlock;
import net.minecraft.block.CaveVinesHeadBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({CaveVinesHeadBlock.class, CaveVinesBodyBlock.class})
public abstract class CaveVinesBlockMixin {
    @Inject(method = "onUse(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;", at = @At("HEAD"))
    private void lumenechallenge$recordGlowBerryHarvest(BlockState state, World world, BlockPos pos,
                                                         PlayerEntity player, BlockHitResult hit,
                                                         CallbackInfoReturnable<ActionResult> cir) {
        if (!world.isClient() && player instanceof ServerPlayerEntity serverPlayer
                && CaveVines.hasBerries(state)) {
            ChallengeHooks.onBerryCropHarvest(serverPlayer, state);
        }
    }
}
