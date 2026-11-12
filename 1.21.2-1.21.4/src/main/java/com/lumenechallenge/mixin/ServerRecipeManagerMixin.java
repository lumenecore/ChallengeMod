
package com.lumenechallenge.mixin;

import com.lumenechallenge.challenge.ChaosRecipeManager;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.ServerRecipeManager;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(ServerRecipeManager.class)
public abstract class ServerRecipeManagerMixin {
    @Inject(method = "getFirstMatch(Lnet/minecraft/recipe/RecipeType;Lnet/minecraft/recipe/input/RecipeInput;Lnet/minecraft/world/World;)Ljava/util/Optional;", at = @At("RETURN"), cancellable = true)
    private <I extends RecipeInput, T extends net.minecraft.recipe.Recipe<I>> void replaceRecipe(
            RecipeType<T> type, I input, World world, CallbackInfoReturnable<Optional<RecipeEntry<T>>> cir) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        Optional<RecipeEntry<T>> value = cir.getReturnValue();
        if (value.isEmpty()) return;
        if (!(value.get().value() instanceof CraftingRecipe)) return;
        RecipeEntry<CraftingRecipe> replaced = ChaosRecipeManager.replace((RecipeEntry<CraftingRecipe>)(Object)value.get(), serverWorld);
        cir.setReturnValue(Optional.of((RecipeEntry<T>)(Object)replaced));
    }
}
