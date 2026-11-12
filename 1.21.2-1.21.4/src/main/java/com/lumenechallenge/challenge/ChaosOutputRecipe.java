package com.lumenechallenge.challenge;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.IngredientPlacement;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;

public final class ChaosOutputRecipe implements CraftingRecipe {

    private final CraftingRecipe original;
    private final ItemStack output;

    public ChaosOutputRecipe(CraftingRecipe original, ItemStack output) {
        this.original = original;
        this.output = output.copy();
    }

    @Override
    public boolean matches(CraftingRecipeInput input, World world) {
        return original.matches(input, world);
    }

    @Override
    public ItemStack craft(
            CraftingRecipeInput input,
            RegistryWrapper.WrapperLookup registries
    ) {
        return output.copy();
    }

    @Override
    public CraftingRecipeCategory getCategory() {
        return original.getCategory();
    }

    @Override
    public RecipeSerializer<? extends CraftingRecipe> getSerializer() {
        return original.getSerializer();
    }

    @Override
    public IngredientPlacement getIngredientPlacement() {
        return original.getIngredientPlacement();
    }
}