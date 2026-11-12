package com.lumenechallenge.mixin;

import com.lumenechallenge.challenge.ChallengeHooks;
import net.minecraft.entity.passive.AbstractCowEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractCowEntity.class)
public abstract class CowEntityMixin {
    @Unique private boolean lumenechallenge$startedWithEmptyBucket;

    @Inject(method = "interactMob(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;", at = @At("HEAD"))
    private void lumenechallenge$rememberBucket(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        lumenechallenge$startedWithEmptyBucket = player.getStackInHand(hand).isOf(Items.BUCKET);
    }

    @Inject(method = "interactMob(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;", at = @At("RETURN"))
    private void lumenechallenge$detectMilking(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        boolean isRegularCow = ((Object) this) instanceof CowEntity;
        boolean actualMilking = isRegularCow && lumenechallenge$startedWithEmptyBucket && cir.getReturnValue().isAccepted();
        lumenechallenge$startedWithEmptyBucket = false;
        if (actualMilking && player instanceof ServerPlayerEntity serverPlayer) {
            ChallengeHooks.onCowMilked(serverPlayer);
        }
    }
}
