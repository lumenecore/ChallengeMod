
package com.lumenechallenge.challenge;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryKey;
import net.minecraft.recipe.RecipeType;
import net.minecraft.world.World;
import com.lumenechallenge.util.WorldMarkerUtil;
import net.minecraft.registry.DynamicRegistryManager;

import java.util.Optional;

public final class ChaosRecipeManager {
    private ChaosRecipeManager() {}

    public static RecipeEntry<CraftingRecipe> replace(RecipeEntry<CraftingRecipe> original, net.minecraft.server.world.ServerWorld world) {
        if (original == null) return null;
        if (!WorldMarkerUtil.isMarked(world.getServer())) return original;
        ChallengeState state = ChallengeState.getServerState(world.getServer());
        if (!state.modifierEnabled() || state.modifier() != ModifierType.CHAOS) return original;
        ItemStack stack = state.getChaosReplacementOutput(world, original.id());
        if (stack.isEmpty()) return original;
        return new RecipeEntry<>(original.id(), new ChaosOutputRecipe(original.value(), stack));
    }
}
