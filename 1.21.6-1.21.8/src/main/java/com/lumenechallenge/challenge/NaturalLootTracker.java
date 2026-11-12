package com.lumenechallenge.challenge;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks items that came from a vanilla chest loot table while the server is running. */
public final class NaturalLootTracker {
    public record Snapshot(Identifier lootTableId, Map<String, Integer> itemCounts) {}

    private static final Map<String, Snapshot> CHESTS = new ConcurrentHashMap<>();
    private static final Map<String, PendingInteraction> PENDING_INTERACTIONS = new ConcurrentHashMap<>();

    private record PendingInteraction(Identifier lootTableId) {}

    private NaturalLootTracker() {}

    public static String chestKey(World world, BlockPos pos) {
        MinecraftServer server = world.getServer();
        int serverIdentity = server == null ? 0 : System.identityHashCode(server);
        return serverIdentity + ":" + world.getRegistryKey().getValue() + ":" + pos.asLong();
    }

    /** Capture the loot-table identifier before generateLoot clears it. */
    public static void beginInteraction(World world, BlockPos pos, Identifier lootTableId) {
        if (world == null || world.isClient() || pos == null) return;
        PENDING_INTERACTIONS.put(chestKey(world, pos), new PendingInteraction(lootTableId));
    }

    /** Retrieve and clear the pre-generation identifier after generateLoot returns. */
    public static Identifier endInteraction(World world, BlockPos pos) {
        if (world == null || world.isClient() || pos == null) return null;
        PendingInteraction pending = PENDING_INTERACTIONS.remove(chestKey(world, pos));
        return pending == null ? null : pending.lootTableId();
    }

    public static void record(String chestKey, Identifier lootTableId, List<ItemStack> stacks) {
        Map<String, Integer> counts = new HashMap<>();
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) continue;
            Identifier itemId = Registries.ITEM.getId(stack.getItem());
            if (itemId != null) counts.merge(itemId.toString(), stack.getCount(), Integer::sum);
        }
        CHESTS.put(chestKey, new Snapshot(lootTableId, Map.copyOf(counts)));
    }

    public static Snapshot get(String chestKey) {
        return CHESTS.get(chestKey);
    }

    /**
     * Returns only item types whose current total is no greater than the amount
     * recorded when vanilla generated the loot. This prevents ordinary additions
     * to a chest from being mistaken for original loot in the common case.
     */
    public static List<ItemStack> verifiedCurrentStacks(Snapshot snapshot, List<ItemStack> currentStacks) {
        if (snapshot == null || currentStacks == null || currentStacks.isEmpty()) return List.of();
        Map<String, Integer> currentCounts = new HashMap<>();
        for (ItemStack stack : currentStacks) {
            if (stack == null || stack.isEmpty()) continue;
            Identifier id = Registries.ITEM.getId(stack.getItem());
            if (id != null) currentCounts.merge(id.toString(), stack.getCount(), Integer::sum);
        }

        List<ItemStack> verified = new ArrayList<>();
        for (ItemStack stack : currentStacks) {
            if (stack == null || stack.isEmpty()) continue;
            Identifier id = Registries.ITEM.getId(stack.getItem());
            if (id == null) continue;
            String key = id.toString();
            int original = snapshot.itemCounts().getOrDefault(key, 0);
            int remaining = currentCounts.getOrDefault(key, 0);
            if (original > 0 && remaining > 0 && remaining <= original) {
                verified.add(stack.copy());
            }
        }
        return List.copyOf(verified);
    }

    public static void clearServer(MinecraftServer server) {
        if (server == null) return;
        String prefix = System.identityHashCode(server) + ":";
        CHESTS.keySet().removeIf(key -> key.startsWith(prefix));
        PENDING_INTERACTIONS.keySet().removeIf(key -> key.startsWith(prefix));
    }
}
