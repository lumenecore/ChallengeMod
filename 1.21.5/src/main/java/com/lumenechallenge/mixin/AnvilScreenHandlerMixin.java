package com.lumenechallenge.mixin;

import com.lumenechallenge.challenge.ChallengeHooks;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin {
    @Unique
    private ItemStack lumenechallenge$pendingAnvilResult = ItemStack.EMPTY;

    @Unique
    private ItemStack lumenechallenge$pendingAnvilInput = ItemStack.EMPTY;

    @Unique
    private boolean lumenechallenge$pendingEnchantedBook = false;

    /**
     * Save the actual calculated anvil output while both input slots are still present.
     * The output slot can already be emptied by the time onTakeOutput is called.
     */
    @Inject(method = "updateResult()V", at = @At("TAIL"))
    private void lumenechallenge$captureAnvilResult(CallbackInfo ci) {
        AnvilScreenHandler handler = (AnvilScreenHandler) (Object) this;
        ItemStack result = handler.getSlot(2).getStack();
        ItemStack input = handler.getSlot(0).getStack();
        ItemStack secondInput = handler.getSlot(1).getStack();

        lumenechallenge$pendingAnvilResult = result.isEmpty() ? ItemStack.EMPTY : result.copy();
        lumenechallenge$pendingAnvilInput = input.isEmpty() ? ItemStack.EMPTY : input.copy();
        lumenechallenge$pendingEnchantedBook = !secondInput.isEmpty()
                && secondInput.isOf(Items.ENCHANTED_BOOK);
    }

    /**
     * Count the enchantment only when the player actually takes the anvil output.
     * The challenge condition itself checks the requested item type and enchantment.
     */
    @Inject(method = "onTakeOutput(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;)V", at = @At("HEAD"))
    private void lumenechallenge$onTakeOutput(PlayerEntity player, ItemStack takenStack, CallbackInfo ci) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            lumenechallenge$clearPendingResult();
            return;
        }

        ItemStack result = !lumenechallenge$pendingAnvilResult.isEmpty()
                ? lumenechallenge$pendingAnvilResult
                : takenStack;
        if (result != null && !result.isEmpty()) {
            if (result.getCustomName() != null) {
                ChallengeHooks.onItemRenamed(serverPlayer, result.copy());
            }
            if (result.hasEnchantments()
                    && (lumenechallenge$pendingEnchantedBook
                        || lumenechallenge$hasNewEnchantment(lumenechallenge$pendingAnvilInput, result))) {
                ChallengeHooks.onItemEnchanted(serverPlayer, result.copy());
            }
        }

        lumenechallenge$clearPendingResult();
    }

    @Unique
    private void lumenechallenge$clearPendingResult() {
        lumenechallenge$pendingAnvilResult = ItemStack.EMPTY;
        lumenechallenge$pendingAnvilInput = ItemStack.EMPTY;
        lumenechallenge$pendingEnchantedBook = false;
    }

    @Unique
    private static boolean lumenechallenge$hasNewEnchantment(ItemStack original, ItemStack result) {
        ItemEnchantmentsComponent before = original == null || original.isEmpty()
                ? ItemEnchantmentsComponent.DEFAULT
                : original.getEnchantments();
        ItemEnchantmentsComponent after = result.getEnchantments();

        for (RegistryEntry<Enchantment> enchantment : after.getEnchantments()) {
            if (after.getLevel(enchantment) > before.getLevel(enchantment)) {
                return true;
            }
        }
        return false;
    }
}
