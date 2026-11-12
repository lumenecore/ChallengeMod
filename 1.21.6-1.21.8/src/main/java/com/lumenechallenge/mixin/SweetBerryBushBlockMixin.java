package com.lumenechallenge.mixin;

import com.lumenechallenge.challenge.ChallengeHooks;
import net.minecraft.block.BlockState;
import net.minecraft.block.SweetBerryBushBlock;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SweetBerryBushBlock.class)
public abstract class SweetBerryBushBlockMixin {
    @Unique
    private static final ThreadLocal<BerryHarvestSnapshot> lumenechallenge$harvestSnapshot = new ThreadLocal<>();

    @Inject(
            method = "onUse(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;",
            at = @At("HEAD")
    )
    private void lumenechallenge$captureBerryDrops(BlockState state, World world, BlockPos pos,
                                                    PlayerEntity player,
                                                    net.minecraft.util.hit.BlockHitResult hit,
                                                    CallbackInfoReturnable<net.minecraft.util.ActionResult> cir) {
        if (world instanceof ServerWorld serverWorld && player instanceof ServerPlayerEntity serverPlayer
                && state.get(SweetBerryBushBlock.AGE) > 1) {
            lumenechallenge$harvestSnapshot.set(new BerryHarvestSnapshot(
                    serverWorld, pos.toImmutable(), serverPlayer, state, lumenechallenge$countNearbyBerries(serverWorld, pos)
            ));
        } else {
            lumenechallenge$harvestSnapshot.remove();
        }
    }

    @Inject(
            method = "onUse(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;",
            at = @At("RETURN")
    )
    private void lumenechallenge$recordOnlyActualBerryDrops(BlockState state, World world, BlockPos pos,
                                                             PlayerEntity player,
                                                             net.minecraft.util.hit.BlockHitResult hit,
                                                             CallbackInfoReturnable<net.minecraft.util.ActionResult> cir) {
        BerryHarvestSnapshot snapshot = lumenechallenge$harvestSnapshot.get();
        lumenechallenge$harvestSnapshot.remove();
        if (snapshot == null || cir.getReturnValue() != net.minecraft.util.ActionResult.SUCCESS) return;

        int berriesAfter = lumenechallenge$countNearbyBerries(snapshot.world(), snapshot.pos());
        if (berriesAfter > snapshot.berriesBefore()) {
            ChallengeHooks.onBerryCropHarvest(snapshot.player(), snapshot.state());
        }
    }

    @Unique
    private static int lumenechallenge$countNearbyBerries(ServerWorld world, BlockPos pos) {
        Box dropArea = new Box(pos).expand(0.25D);
        int count = 0;
        for (ItemEntity itemEntity : world.getEntitiesByClass(
                ItemEntity.class,
                dropArea,
                entity -> entity.isAlive() && entity.getStack().isOf(Items.SWEET_BERRIES)
        )) {
            ItemStack stack = itemEntity.getStack();
            if (!stack.isEmpty()) count += stack.getCount();
        }
        return count;
    }

    private record BerryHarvestSnapshot(ServerWorld world, BlockPos pos, ServerPlayerEntity player,
                                        BlockState state, int berriesBefore) {}
}
