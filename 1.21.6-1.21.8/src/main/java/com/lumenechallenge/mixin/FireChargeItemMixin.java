package com.lumenechallenge.mixin;

import com.lumenechallenge.challenge.ChallengeHooks;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.FireChargeItem;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.gen.structure.Structure;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FireChargeItem.class)
public abstract class FireChargeItemMixin {
    @Inject(method = "useOnBlock", at = @At("RETURN"))
    private void lumenechallenge$detectRuinedPortalIgnition(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        if (!cir.getReturnValue().isAccepted() || context.getWorld().isClient()) return;
        if (!(context.getPlayer() instanceof ServerPlayerEntity player)) return;
        if (!(context.getWorld() instanceof ServerWorld world)) return;
        Structure ruinedPortal;
        try {
            ruinedPortal = world.getRegistryManager().getOrThrow(RegistryKeys.STRUCTURE)
                    .getOrThrow(RegistryKey.of(RegistryKeys.STRUCTURE, Identifier.of("minecraft:ruined_portal")))
                    .value();
        } catch (RuntimeException missingStructure) {
            return;
        }
        BlockPos center = context.getBlockPos();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos pos = center.add(dx, dy, dz);
                    if (!world.getBlockState(pos).isOf(Blocks.NETHER_PORTAL)) continue;
                    var start = world.getStructureAccessor().getStructureContaining(pos, ruinedPortal);
                    if (start != null && start.hasChildren()) {
                        ChallengeHooks.onRuinedPortalRestored(player);
                        return;
                    }
                }
            }
        }
    }
}
