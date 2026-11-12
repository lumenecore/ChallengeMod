package com.lumenechallenge.mixin;

import com.lumenechallenge.challenge.ChallengeHooks;
import com.lumenechallenge.challenge.NaturalLootTracker;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.LootableInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Records loot when a player opens a concrete vanilla lootable container.
 * Hooking createMenu keeps the event tied to the actual chest/menu interaction
 * and avoids relying on a mixin into a default interface implementation.
 */
@Mixin(LootableContainerBlockEntity.class)
public abstract class LootableContainerBlockEntityMixin {
    @Inject(method = "createMenu", at = @At("HEAD"))
    private void lumenechallenge$captureLootTable(int syncId, PlayerInventory playerInventory, PlayerEntity player,
                                                   CallbackInfoReturnable<ScreenHandler> cir) {
        LootableInventory inventory = (LootableInventory) (Object) this;
        World world = inventory.getWorld();
        if (world == null || world.isClient()) return;

        RegistryKey<LootTable> key = inventory.getLootTable();
        NaturalLootTracker.beginInteraction(world, inventory.getPos(), key == null ? null : key.getValue());
    }

    @Inject(method = "createMenu", at = @At("RETURN"))
    private void lumenechallenge$recordNaturallyGeneratedLoot(int syncId, PlayerInventory playerInventory, PlayerEntity player,
                                                               CallbackInfoReturnable<ScreenHandler> cir) {
        LootableInventory inventory = (LootableInventory) (Object) this;
        World world = inventory.getWorld();
        if (world == null || world.isClient()) return;

        Identifier lootTable = NaturalLootTracker.endInteraction(world, inventory.getPos());
        String chestKey = NaturalLootTracker.chestKey(world, inventory.getPos());
        List<ItemStack> currentStacks = lumenechallenge$currentStacks(inventory);

        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        if (lootTable != null && lumenechallenge$isVanillaChestLoot(lootTable)) {
            NaturalLootTracker.record(chestKey, lootTable, currentStacks);
            ChallengeHooks.onNaturalDungeonLootGenerated(serverPlayer, lootTable, currentStacks);
            return;
        }

        // Some inventories have already generated their table before menu creation.
        // In that case, only items matching the previously captured natural-loot
        // snapshot are allowed to complete the objective.
        NaturalLootTracker.Snapshot snapshot = NaturalLootTracker.get(chestKey);
        if (snapshot == null) return;
        List<ItemStack> verified = NaturalLootTracker.verifiedCurrentStacks(snapshot, currentStacks);
        if (!verified.isEmpty()) {
            ChallengeHooks.onNaturalDungeonLootGenerated(serverPlayer, snapshot.lootTableId(), verified);
        }
    }

    @Unique
    private static List<ItemStack> lumenechallenge$currentStacks(LootableInventory inventory) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack != null && !stack.isEmpty()) items.add(stack.copy());
        }
        return List.copyOf(items);
    }

    @Unique
    private static boolean lumenechallenge$isVanillaChestLoot(Identifier lootTable) {
        if (!"minecraft".equals(lootTable.getNamespace())) return false;
        String path = lootTable.getPath();
        return path.startsWith("chests/") && !path.equals("chests/spawn_bonus_chest");
    }
}
