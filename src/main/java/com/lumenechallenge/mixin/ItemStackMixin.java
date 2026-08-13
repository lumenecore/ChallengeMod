package com.lumenechallenge.mixin;

import com.lumenechallenge.challenge.ChallengeHooks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Unique
    private ItemStack lumenechallenge$beforeDamage = ItemStack.EMPTY;

    @Inject(method = "damage(ILnet/minecraft/entity/LivingEntity;Lnet/minecraft/entity/EquipmentSlot;)V", at = @At("HEAD"))
    private void lumenechallenge$captureBeforeDamage(int amount, LivingEntity entity, EquipmentSlot slot, CallbackInfo ci) {
        ItemStack stack = (ItemStack) (Object) this;
        if (entity instanceof ServerPlayerEntity && !stack.isEmpty() && stack.isDamageable()) {
            lumenechallenge$beforeDamage = stack.copy();
        } else {
            lumenechallenge$beforeDamage = ItemStack.EMPTY;
        }
    }

    @Inject(method = "damage(ILnet/minecraft/entity/LivingEntity;Lnet/minecraft/entity/EquipmentSlot;)V", at = @At("TAIL"))
    private void lumenechallenge$detectBrokenItem(int amount, LivingEntity entity, EquipmentSlot slot, CallbackInfo ci) {
        try {
            if (entity instanceof ServerPlayerEntity serverPlayer) {
                ItemStack stack = (ItemStack) (Object) this;
                if (!lumenechallenge$beforeDamage.isEmpty() && stack.isEmpty()) {
                    ChallengeHooks.onItemBroken(serverPlayer, lumenechallenge$beforeDamage);
                }
            }
        } finally {
            lumenechallenge$beforeDamage = ItemStack.EMPTY;
        }
    }

    @Inject(method = "finishUsing", at = @At("RETURN"))
    private void lumenechallenge$finishUsing(World world, LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
        if (!(user instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        ItemStack stack = (ItemStack) (Object) this;
        if (stack.contains(DataComponentTypes.FOOD) || stack.isOf(Items.POTION)) {
            ChallengeHooks.onItemConsumed(serverPlayer, stack);
        }
    }
}
