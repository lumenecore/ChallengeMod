package com.lumenechallenge.challenge;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.ShapelessRecipe;

public final class ChaosRecipeOutputExtractor {
    private ChaosRecipeOutputExtractor() {}

    public static ItemStack extract(CraftingRecipe recipe) {
        if (recipe instanceof ShapedRecipe shaped) {
            return ((com.lumenechallenge.mixin.ShapedRecipeAccessor)(Object) shaped)
                    .chaos$getResult().copy();
        }

        if (recipe instanceof ShapelessRecipe shapeless) {
            return ((com.lumenechallenge.mixin.ShapelessRecipeAccessor)(Object) shapeless)
                    .chaos$getResult().copy();
        }

        return ItemStack.EMPTY;
    }
}
