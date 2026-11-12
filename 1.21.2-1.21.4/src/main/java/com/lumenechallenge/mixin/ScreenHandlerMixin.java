package com.lumenechallenge.mixin;

import com.lumenechallenge.challenge.ChallengeHooks;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.block.entity.DropperBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.BlastFurnaceScreenHandler;
import net.minecraft.screen.FurnaceScreenHandler;
import net.minecraft.screen.Generic3x3ContainerScreenHandler;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.HopperScreenHandler;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.SmokerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenHandler.class)
public abstract class ScreenHandlerMixin {
    @Unique private ItemStack[] lumenechallenge$beforeStacks;
    @Unique private String[] lumenechallenge$storageIds;

    @Inject(method = "onSlotClick", at = @At("HEAD"))
    private void lumenechallenge$captureStorageSlots(int slotId, int button, SlotActionType actionType,
                                                      PlayerEntity player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        ScreenHandler handler = (ScreenHandler) (Object) this;
        lumenechallenge$beforeStacks = new ItemStack[handler.slots.size()];
        lumenechallenge$storageIds = new String[handler.slots.size()];
        for (int i = 0; i < handler.slots.size(); i++) {
            Slot slot = handler.slots.get(i);
            lumenechallenge$beforeStacks[i] = slot.getStack().copy();
            if (slot.inventory != serverPlayer.getInventory()) {
                lumenechallenge$storageIds[i] = lumenechallenge$storageId(handler, slot.inventory);
            }
        }
    }

    @Inject(method = "onSlotClick", at = @At("TAIL"))
    private void lumenechallenge$recordStorageInsert(int slotId, int button, SlotActionType actionType,
                                                      PlayerEntity player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        ScreenHandler handler = (ScreenHandler) (Object) this;
        if (lumenechallenge$beforeStacks != null && lumenechallenge$storageIds != null) {
            for (int i = 0; i < Math.min(handler.slots.size(), lumenechallenge$beforeStacks.length); i++) {
                String storage = lumenechallenge$storageIds[i];
                if (storage == null || storage.isBlank()) continue;
                ItemStack before = lumenechallenge$beforeStacks[i];
                ItemStack after = handler.slots.get(i).getStack();
                if (after != null && !after.isEmpty()
                        && after.getCount() > (before == null || before.isEmpty() ? 0 : before.getCount())
                        && (before == null || before.isEmpty() || before.isOf(after.getItem()))) {
                    ChallengeHooks.onItemStored(serverPlayer, after, storage);
                }
            }
        }
        lumenechallenge$beforeStacks = null;
        lumenechallenge$storageIds = null;

        if (slotId == 2 && ((Object) this) instanceof MerchantScreenHandler
                && (actionType == SlotActionType.PICKUP || actionType == SlotActionType.QUICK_MOVE)) {
            ChallengeHooks.onTrade(serverPlayer);
        }
    }

    @Unique
    private static String lumenechallenge$storageId(ScreenHandler handler, Inventory inventory) {
        if (handler instanceof SmokerScreenHandler) return "smoker";
        if (handler instanceof BlastFurnaceScreenHandler) return "blast_furnace";
        if (handler instanceof FurnaceScreenHandler) return "furnace";
        if (handler instanceof HopperScreenHandler) return "hopper";
        if (handler instanceof Generic3x3ContainerScreenHandler) {
            if (inventory instanceof DropperBlockEntity) return "dropper";
            if (inventory instanceof DispenserBlockEntity) return "dispenser";
            String name = inventory.getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT);
            if (name.contains("dropper")) return "dropper";
            if (name.contains("dispenser")) return "dispenser";
        }
        if (handler instanceof GenericContainerScreenHandler) {
            if (inventory instanceof BarrelBlockEntity) return "barrel";
            if (inventory instanceof ChestBlockEntity) return "chest";
            String name = inventory.getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT);
            if (name.contains("barrel")) return "barrel";
            if (name.contains("chest") || name.contains("doubleinventory")) return "chest";
        }
        return "";
    }
}
