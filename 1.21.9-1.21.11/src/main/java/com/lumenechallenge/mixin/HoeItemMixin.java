package com.lumenechallenge.mixin;

import com.lumenechallenge.challenge.ChallengeHooks;
import net.minecraft.block.Blocks;
import net.minecraft.item.HoeItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HoeItem.class)
public abstract class HoeItemMixin {
    @Inject(method = "useOnBlock", at = @At("RETURN"))
    private void lumenechallenge$detectTilling(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        if (!cir.getReturnValue().isAccepted() || context.getWorld().isClient()) return;
        if (!(context.getPlayer() instanceof ServerPlayerEntity player)) return;
        if (context.getWorld().getBlockState(context.getBlockPos()).isOf(Blocks.FARMLAND)) {
            ChallengeHooks.onSoilTilled(player);
        }
    }
}
