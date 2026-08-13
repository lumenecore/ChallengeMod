package com.lumenechallenge.mixin;

import com.lumenechallenge.challenge.ChallengeState;
import com.lumenechallenge.challenge.ModifierType;
import com.lumenechallenge.util.WorldMarkerUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryMixin {
    @Shadow @Final public PlayerEntity player;

    @Inject(method = "canStackAddMore", at = @At("HEAD"), cancellable = true)
    private void lumenechallenge$canStackAddMore(ItemStack existing, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (shouldLeakyPocketsApply()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "insertStack(ILnet/minecraft/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void lumenechallenge$insertStack(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (shouldLeakyPocketsApply()) {
            cir.setReturnValue(insertOnlyIntoEmptySlots((PlayerInventory) (Object) this, stack));
        }
    }

    @Inject(method = "insertStack(Lnet/minecraft/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void lumenechallenge$insertStack(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (shouldLeakyPocketsApply()) {
            cir.setReturnValue(insertOnlyIntoEmptySlots((PlayerInventory) (Object) this, stack));
        }
    }

    private boolean insertOnlyIntoEmptySlots(PlayerInventory inventory, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }

        int remaining = stack.getCount();
        boolean inserted = false;

        while (remaining > 0) {
            int emptySlot = inventory.getEmptySlot();
            if (emptySlot < 0) {
                break;
            }

            ItemStack single = stack.copy();
            single.setCount(1);
            inventory.setStack(emptySlot, single);
            remaining--;
            inserted = true;
        }

        stack.setCount(remaining);
        return remaining == 0 && inserted;
    }

    private boolean shouldLeakyPocketsApply() {
        if (!(player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer)) {
            return false;
        }
        if (!WorldMarkerUtil.isMarked(serverPlayer.getServer())) return false;
        ChallengeState state = ChallengeState.getServerState(serverPlayer.getServer());
        return state.modifierEnabled() && state.modifier() == ModifierType.POCKETS;
    }
}
