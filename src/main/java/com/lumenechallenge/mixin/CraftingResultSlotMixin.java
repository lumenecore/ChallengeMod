package com.lumenechallenge.mixin;

import net.minecraft.screen.slot.CraftingResultSlot;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(CraftingResultSlot.class)
public abstract class CraftingResultSlotMixin {
    // Intentionally left blank. Crafting replacement is handled in CraftingScreenHandler.updateResult.
}
