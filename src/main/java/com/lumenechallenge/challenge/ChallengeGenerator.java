package com.lumenechallenge.challenge;

import com.lumenechallenge.challenge.condition.ObjectiveCondition;
import com.lumenechallenge.challenge.condition.ObjectiveCondition.Type;
import com.lumenechallenge.challenge.condition.ObjectiveCondition.Modifier;
import com.lumenechallenge.util.RussianGrammar;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.Leashable;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class ChallengeGenerator {
    private static final List<Type> POOL = List.of(
            Type.JUMP,
            Type.OBTAIN_ITEM,
            Type.BREAK_TOOL,
            Type.ENDER_PEARL,
            Type.KILL_MOB,
            Type.STAND_ON_BLOCK,
            Type.UNDERWATER,
            Type.EAT_ITEM,
            Type.EAT_ANY,
            Type.ENCHANT_ITEM,
            Type.FISH,
            Type.SLEEP,
            Type.SMELT_ITEM,
            Type.SHOOT_MOB_FROM_DISTANCE,
            Type.DEAL_DAMAGE,
            Type.RENAME_ITEM,
            Type.TELEPORT_DIMENSION,
            Type.REACH_Y_LEVEL,
            Type.TRADE,
            Type.FIND_STRUCTURE,
            Type.VISIT_BIOME,
            Type.WALK_DISTANCE,
            Type.BOAT_DISTANCE,
            Type.TAME_MOB,
            Type.EMPTY_INVENTORY,
            Type.FULL_INVENTORY,
            Type.CATCH_SELF_ON_ROD,
            Type.LEASH_MOB,
            Type.VISIT_COORDS
    );

    private ChallengeGenerator() {
    }

    private enum VariantTier {
        ORDINARY,
        HARD,
        INSANE
    }


    public static GeneratedTask generateTask(ServerPlayerEntity player, Random random, ChallengeTaskDefinition definition) {
        return generateTask(player, random, definition, RunDifficulty.fromId(definition.difficulty()));
    }

    private static GeneratedTask generateTask(ServerPlayerEntity player, Random random, ChallengeTaskDefinition definition, RunDifficulty runDifficulty) {
        Type type = definition.typeEnum();
        VariantTier tier = tierFor(runDifficulty);

        List<Modifier> modifiers = pickModifiers(random, definition, runDifficulty);
        int count = pickCount(random, type, modifiers, runDifficulty);

        NbtCompound data = new NbtCompound();
        fillData(player, random, type, data, count, runDifficulty, modifiers);
        for (Modifier modifier : modifiers) {
            fillModifierData(player, random, modifier, data, runDifficulty, tier);
        }

        int requiredCount = switch (type) {
            case JUMP -> count;
            case EAT_ANY, ENCHANT_ITEM, FISH, SMELT_ITEM -> modifiers.contains(Modifier.COUNT) ? count : 1;
            default -> 1;
        };

        ChallengeTaskDefinition adjusted = definition.copy();
        ObjectiveCondition condition = new ObjectiveCondition(type, modifiers, requiredCount, data);
        if (startsSatisfied(player, type, data)) {
            return null;
        }
        return new GeneratedTask(condition, adjusted);
    }

    public static Challenge generate(ServerPlayerEntity player, long seed, int count, RunDifficulty runDifficulty) {
        ChallengeConfig config = ChallengeConfigManager.get();
        List<ChallengeTaskDefinition> allowed = config.allowedTasks(runDifficulty);
        if (allowed.isEmpty()) {
            allowed = ChallengeConfig.defaults().allowedTasks(runDifficulty);
        }

        Random random = new Random(seed ^ 0x5DEECE66DL);
        List<ChallengeTaskDefinition> available = new ArrayList<>();
        for (ChallengeTaskDefinition task : allowed) {
            if (task != null) {
                available.add(task.copy());
            }
        }

        List<com.lumenechallenge.challenge.condition.Condition> conditions = new ArrayList<>(count);
        List<ChallengeTaskDefinition> definitions = new ArrayList<>(count);
        while (conditions.size() < count) {
            ChallengeTaskDefinition definition = pickWeightedTask(random, available, runDifficulty);
            if (definition == null) {
                available.clear();
                for (ChallengeTaskDefinition task : allowed) {
                    if (task != null) {
                        available.add(task.copy());
                    }
                }
                definition = pickWeightedTask(random, available, runDifficulty);
                if (definition == null) {
                    break;
                }
            }

            GeneratedTask task = generateTask(player, random, definition, runDifficulty);
            if (task == null) {
                removeFirstByType(available, definition.type());
                continue;
            }
            conditions.add(task.condition());
            definitions.add(task.definition());
            removeFirstByType(available, definition.type());
        }

        while (conditions.size() < count) {
            ChallengeTaskDefinition fallback = allowed.isEmpty() ? new ChallengeTaskDefinition() : allowed.get(0).copy();
            GeneratedTask task = generateTask(player, random, fallback, runDifficulty);
            if (task == null) {
                break;
            }
            conditions.add(task.condition());
            definitions.add(task.definition());
        }

        return new Challenge(ChallengeType.DEATH, conditions, definitions, 0);
    }

    public static GeneratedTask rerollTask(ServerPlayerEntity player, long seed, int slotIndex, RunDifficulty runDifficulty, String avoidType, int rerollSalt) {
        ChallengeConfig config = ChallengeConfigManager.get();
        List<ChallengeTaskDefinition> allowed = config.allowedTasks(runDifficulty);
        if (allowed.isEmpty()) {
            allowed = ChallengeConfig.defaults().allowedTasks(runDifficulty);
        }

        List<ChallengeTaskDefinition> candidates = new ArrayList<>();
        for (ChallengeTaskDefinition task : allowed) {
            if (task != null && (avoidType == null || avoidType.isBlank() || !avoidType.equalsIgnoreCase(task.type()))) {
                candidates.add(task.copy());
            }
        }
        if (candidates.isEmpty()) {
            for (ChallengeTaskDefinition task : allowed) {
                if (task != null) {
                    candidates.add(task.copy());
                }
            }
        }

        Random random = new Random(seed ^ 0x5DEECE66DL ^ ((long) slotIndex << 32) ^ rerollSalt);
        for (int attempt = 0; attempt < Math.max(1, candidates.size() * 2); attempt++) {
            ChallengeTaskDefinition definition = pickWeightedTask(random, candidates, runDifficulty);
            if (definition == null) {
                break;
            }
            GeneratedTask task = generateTask(player, random, definition, runDifficulty);
            if (task != null) {
                return task;
            }
        }

        ChallengeTaskDefinition fallback = candidates.isEmpty() ? new ChallengeTaskDefinition() : candidates.get(0);
        GeneratedTask task = generateTask(player, random, fallback, runDifficulty);
        if (task != null) {
            return task;
        }
        return new GeneratedTask(new ObjectiveCondition(Type.JUMP, List.of(), 1, new NbtCompound()), fallback.copy());
    }

    public static Challenge generate(long seed) {
        throw new UnsupportedOperationException("Use generate(player, seed, count)");
    }

    public record GeneratedTask(ObjectiveCondition condition, ChallengeTaskDefinition definition) {}

    private static ChallengeTaskDefinition pickWeightedTask(Random random, List<ChallengeTaskDefinition> available, RunDifficulty runDifficulty) {
        if (random == null || available == null || available.isEmpty()) {
            return null;
        }

        List<ChallengeTaskDefinition> normal = new ArrayList<>();
        List<ChallengeTaskDefinition> hard = new ArrayList<>();
        List<ChallengeTaskDefinition> insane = new ArrayList<>();

        for (ChallengeTaskDefinition task : available) {
            if (task == null) {
                continue;
            }
            switch (task.difficultyEnum()) {
                case NORMAL -> normal.add(task);
                case HARD -> hard.add(task);
                case INSANE -> insane.add(task);
            }
        }

        RunDifficulty safe = runDifficulty == null ? RunDifficulty.NORMAL : runDifficulty;
        return switch (safe) {
            case NORMAL -> weightedTaskChoice(random, normal);
            case HARD -> weightedTaskChoice(random, pickByChance(random, normal, hard, 40, 60));
            case INSANE -> weightedTaskChoice(random, pickByChance(random, normal, hard, insane, 20, 30, 50));
        };
    }

    private static List<ChallengeTaskDefinition> pickByChance(Random random, List<ChallengeTaskDefinition> a, List<ChallengeTaskDefinition> b, int aChance, int bChance) {
        if (random == null) return List.of();
        int roll = random.nextInt(Math.max(1, aChance + bChance));
        if (roll < aChance && !a.isEmpty()) {
            return a;
        }
        if (!b.isEmpty()) {
            return b;
        }
        return a.isEmpty() ? b : a;
    }

    private static List<ChallengeTaskDefinition> pickByChance(Random random, List<ChallengeTaskDefinition> a, List<ChallengeTaskDefinition> b, List<ChallengeTaskDefinition> c, int aChance, int bChance, int cChance) {
        if (random == null) return List.of();
        int total = Math.max(1, aChance + bChance + cChance);
        int roll = random.nextInt(total);
        if (roll < aChance && !a.isEmpty()) {
            return a;
        }
        if (roll < aChance + bChance && !b.isEmpty()) {
            return b;
        }
        if (!c.isEmpty()) {
            return c;
        }
        if (!b.isEmpty()) {
            return b;
        }
        return a;
    }

    private static ChallengeTaskDefinition weightedTaskChoice(Random random, List<ChallengeTaskDefinition> pool) {
        if (random == null || pool == null || pool.isEmpty()) {
            return null;
        }
        List<Integer> weights = new ArrayList<>(pool.size());
        for (ChallengeTaskDefinition task : pool) {
            weights.add(Math.max(1, task.frequency()));
        }
        return weightedChoice(random, pool, weights);
    }

    private static void removeFirstByType(List<ChallengeTaskDefinition> list, String type) {
        if (list == null || type == null) {
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            ChallengeTaskDefinition task = list.get(i);
            if (task != null && type.equalsIgnoreCase(task.type())) {
                list.remove(i);
                return;
            }
        }
    }

    private static <T> T weightedChoice(Random random, List<T> options, List<Integer> weights) {
        if (random == null || options == null || weights == null || options.isEmpty() || options.size() != weights.size()) {
            return null;
        }
        int total = 0;
        for (int weight : weights) {
            total += Math.max(1, weight);
        }
        if (total <= 0) {
            return options.get(0);
        }
        int roll = random.nextInt(total);
        int cumulative = 0;
        for (int i = 0; i < options.size(); i++) {
            cumulative += Math.max(1, weights.get(i));
            if (roll < cumulative) {
                return options.get(i);
            }
        }
        return options.get(options.size() - 1);
    }

    private static Modifier modifierFromTaskId(int id) {
        return switch (id) {
            case 1 -> Modifier.COUNT;
            case 2 -> Modifier.SHIFT;
            case 3 -> Modifier.EATING;
            case 4 -> Modifier.SHIELD;
            case 5 -> Modifier.BURNING;
            case 6 -> Modifier.LOOKING_NORTH;
            case 7 -> Modifier.LOOKING_UP_DOWN;
            case 8 -> Modifier.AIMING;
            case 9 -> Modifier.PUMPKIN;
            case 10 -> Modifier.NEAR_MOB;
            case 11 -> Modifier.TIME;
            case 12 -> Modifier.IN_WATER;
            case 13 -> Modifier.RIDE_PIG;
            case 14 -> Modifier.RIDE_BOAT;
            case 15 -> Modifier.EMPTY_HAND;
            case 16 -> Modifier.ARMOR;
            case 17, 18 -> Modifier.EFFECT;
            default -> null;
        };
    }

    private static boolean modifierAllowedIn(ChallengeConfig config, Modifier modifier, RunDifficulty runDifficulty) {
        if (modifier == null || config == null) {
            return false;
        }
        ChallengeConfig.ModifierRule rule = config.modifierRuleById(modifier.id());
        if (rule == null) {
            return false;
        }
        return rule.allowedIn(runDifficulty);
    }

    private static int modifierWeight(ChallengeConfig config, Modifier modifier) {
        if (modifier == null || config == null) {
            return 0;
        }
        ChallengeConfig.ModifierRule rule = config.modifierRuleById(modifier.id());
        return rule == null ? 0 : Math.max(1, rule.weight());
    }

    private static void fillData(ServerPlayerEntity player, Random random, Type type, NbtCompound data, int count, RunDifficulty runDifficulty, List<Modifier> modifiers) {
        BlockPos pos = player.getBlockPos();
        VariantTier tier = tierFor(runDifficulty);
        switch (type) {
            case OBTAIN_ITEM -> pickObtain(player, random, data, tier);
            case BREAK_TOOL -> pickBreakTool(player, random, data, tier);
            case KILL_MOB -> pickEntity(random, data, tier);
            case SHOOT_MOB_FROM_DISTANCE -> {
                pickEntity(random, data, tier);
                data.putInt("distance", count);
            }
            case TAME_MOB -> pickTameMob(random, data, tier);
            case LEASH_MOB -> pickLeashMob(player.getServerWorld(), random, data);
            case STAND_ON_BLOCK -> pickBlock(random, data, tier);
            case EAT_ITEM -> pickFood(random, data, tier);
            case EAT_ANY -> pickFood(random, data, tier);
            case ENCHANT_ITEM -> pickEnchantable(random, data);
            case SMELT_ITEM -> pickSmelt(random, data);
            case DEAL_DAMAGE -> data.putDouble("damage", count);
            case RENAME_ITEM -> pickRename(random, data, tier);
            case TELEPORT_DIMENSION -> pickDimension(player, random, data, runDifficulty);
            case REACH_Y_LEVEL -> data.putInt("y", random.nextInt(231) - 50);
            case FIND_STRUCTURE -> pickStructure(random, data, runDifficulty);
            case VISIT_BIOME -> pickBiome(random, data, runDifficulty);
            case WALK_DISTANCE, BOAT_DISTANCE -> {
                data.putDouble("distance", count);
                data.putDouble("startX", pos.getX());
                data.putDouble("startZ", pos.getZ());
            }
            case VISIT_COORDS -> {
                data.putInt("x", pos.getX() + random.nextInt(1001) - 500);
                data.putInt("z", pos.getZ() + random.nextInt(1001) - 500);
            }
            case ENDER_PEARL, FISH, JUMP, SLEEP, TRADE, EMPTY_INVENTORY, FULL_INVENTORY, CATCH_SELF_ON_ROD, UNDERWATER -> {
            }
        }
    }

    private static VariantTier tierFor(RunDifficulty difficulty) {
        return switch (difficulty == null ? RunDifficulty.NORMAL : difficulty) {
            case NORMAL -> VariantTier.ORDINARY;
            case HARD -> VariantTier.HARD;
            case INSANE -> VariantTier.INSANE;
        };
    }

    private static int recommendedSecondsFor(Type type, RunDifficulty difficulty, int fallback) {
        RunDifficulty safe = difficulty == null ? RunDifficulty.NORMAL : difficulty;
        return switch (type) {
            case JUMP -> 240;
            case OBTAIN_ITEM -> switch (safe) {
                case NORMAL -> 400;
                case HARD -> 650;
                case INSANE -> 1300;
            };
            case BREAK_TOOL -> switch (safe) {
                case NORMAL -> 650;
                case HARD -> 950;
                case INSANE -> 1500;
            };
            case KILL_MOB -> switch (safe) {
                case NORMAL -> 600;
                case HARD -> 1500;
                case INSANE -> 2500;
            };
            case STAND_ON_BLOCK -> switch (safe) {
                case NORMAL -> 600;
                case HARD -> 1200;
                case INSANE -> 2400;
            };
            case UNDERWATER -> 180;
            case EAT_ITEM -> switch (safe) {
                case NORMAL -> 480;
                case HARD -> 960;
                case INSANE -> 1400;
            };
            case EAT_ANY -> 400;
            case ENCHANT_ITEM -> 1500;
            case FISH -> 660;
            case SLEEP -> 1800;
            case SMELT_ITEM -> 800;
            case SHOOT_MOB_FROM_DISTANCE -> switch (safe) {
                case NORMAL -> 700;
                case HARD -> 700;
                case INSANE -> 1000;
            };
            case DEAL_DAMAGE -> switch (safe) {
                case NORMAL -> 300;
                case HARD -> 600;
                case INSANE -> 1000;
            };
            case RENAME_ITEM -> 1800;
            case TELEPORT_DIMENSION -> switch (safe) {
                case NORMAL -> 1200;
                case HARD -> 1200;
                case INSANE -> 2400;
            };
            case REACH_Y_LEVEL -> 450;
            case TRADE -> 2200;
            case FIND_STRUCTURE -> 1200;
            case VISIT_BIOME -> 900;
            case WALK_DISTANCE -> 500;
            case BOAT_DISTANCE -> 500;
            case TAME_MOB -> 700;
            case EMPTY_INVENTORY -> 120;
            case FULL_INVENTORY -> 180;
            case CATCH_SELF_ON_ROD -> 300;
            case LEASH_MOB -> 500;
            case VISIT_COORDS -> 500;
            default -> fallback;
        };
    }

    private static List<Modifier> pickModifiers(Random random, ChallengeTaskDefinition definition, RunDifficulty runDifficulty) {
        if (random == null || definition == null) {
            return List.of();
        }

        List<Integer> allowed = definition.modifierIds();
        if (allowed.isEmpty()) {
            return List.of();
        }

        ChallengeConfig config = ChallengeConfigManager.get();
        List<Modifier> candidates = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();

        for (Integer id : allowed) {
            if (id == null || id <= 0) {
                continue;
            }
            Modifier modifier = modifierFromTaskId(id);
            if (modifier == null || (definition.typeEnum() == Type.JUMP && modifier == Modifier.COUNT)) {
                continue;
            }
            if (!modifierAllowedIn(config, modifier, runDifficulty)) {
                continue;
            }
            int weight = modifierWeight(config, modifier);
            if (weight <= 0) {
                continue;
            }
            candidates.add(modifier);
            weights.add(weight);
        }

        if (candidates.isEmpty()) {
            return List.of();
        }

        RunDifficulty safe = runDifficulty == null ? RunDifficulty.NORMAL : runDifficulty;
        int firstChance = switch (safe) {
            case NORMAL -> 30;
            case HARD -> 45;
            case INSANE -> 60;
        };
        int secondChance = switch (safe) {
            case NORMAL -> 0;
            case HARD -> 15;
            case INSANE -> 40;
        };
        int thirdChance = safe == RunDifficulty.INSANE ? 20 : 0;

        List<Modifier> selected = new ArrayList<>(3);
        if (random.nextInt(100) >= firstChance) {
            return selected;
        }

        Modifier first = weightedChoice(random, candidates, weights);
        if (first == null) {
            return selected;
        }
        selected.add(first);

        if (random.nextInt(100) < secondChance) {
            Modifier second = weightedChoiceExcluding(random, candidates, weights, selected);
            if (second != null) {
                selected.add(second);
            }
        }

        if (selected.size() >= 2 && random.nextInt(100) < thirdChance) {
            Modifier third = weightedChoiceExcluding(random, candidates, weights, selected);
            if (third != null) {
                selected.add(third);
            }
        }

        return selected;
    }

    private static Modifier weightedChoiceExcluding(Random random, List<Modifier> options, List<Integer> weights, List<Modifier> excluded) {
        List<Modifier> filtered = new ArrayList<>();
        List<Integer> filteredWeights = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            Modifier option = options.get(i);
            if (!excluded.contains(option)) {
                filtered.add(option);
                filteredWeights.add(weights.get(i));
            }
        }
        return weightedChoice(random, filtered, filteredWeights);
    }

    private static int pickCount(Random random, Type type, List<Modifier> modifiers, RunDifficulty runDifficulty) {
        if (random == null || type == null) {
            return 1;
        }

        return switch (type) {
            case JUMP -> 30 + random.nextInt(31);
            case EAT_ANY -> modifiers.contains(Modifier.COUNT) ? 1 + random.nextInt(3) : 1;
            case ENCHANT_ITEM -> modifiers.contains(Modifier.COUNT) ? 1 + random.nextInt(2) : 1;
            case FISH, SMELT_ITEM -> modifiers.contains(Modifier.COUNT) ? 1 + random.nextInt(4) : 1;
            case SHOOT_MOB_FROM_DISTANCE -> {
                RunDifficulty safe = runDifficulty == null ? RunDifficulty.NORMAL : runDifficulty;
                if (safe == RunDifficulty.INSANE) {
                    yield 100;
                }
                int[] distances = {5, 10, 15, 25, 30, 35, 40, 45, 50, 55, 60};
                yield distances[random.nextInt(distances.length)];
            }
            case DEAL_DAMAGE -> {
                RunDifficulty safe = runDifficulty == null ? RunDifficulty.NORMAL : runDifficulty;
                if (safe == RunDifficulty.INSANE) {
                    yield 10;
                }
                if (safe == RunDifficulty.HARD) {
                    yield 8;
                }
                int[] damage = {4, 6};
                yield damage[random.nextInt(damage.length)];
            }
            case WALK_DISTANCE -> {
                int[] distances = {50, 150, 200, 250};
                yield distances[random.nextInt(distances.length)];
            }
            case BOAT_DISTANCE -> {
                int[] distances = {100, 150, 200, 250, 300};
                yield distances[random.nextInt(distances.length)];
            }
            default -> 1;
        };
    }

    private static boolean startsSatisfied(ServerPlayerEntity player, Type type, NbtCompound data) {
        return switch (type) {
            case EMPTY_INVENTORY -> isInventoryEmpty(player);
            case FULL_INVENTORY -> isInventoryFull(player);
            case TELEPORT_DIMENSION -> player.getServerWorld().getRegistryKey().getValue().toString().equals(data.getString("dimension", ""));
            default -> false;
        };
    }

    private static boolean isInventoryEmpty(ServerPlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            if (!player.getInventory().getStack(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean isInventoryFull(ServerPlayerEntity player) {
        for (var stack : player.getInventory().getMainStacks()) {
            if (stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static int clampY(ServerWorld world, int y) {
        int minY = world.getBottomY() + 1;
        int maxY = world.getBottomY() + world.getHeight() - 1;
        return MathHelper.clamp(y, minY, maxY);
    }

    private static void pickObtain(ServerPlayerEntity player, Random random, NbtCompound data) {
        pickObtain(player, random, data, VariantTier.ORDINARY);
    }

    private static void pickObtain(ServerPlayerEntity player, Random random, NbtCompound data, VariantTier tier) {
        List<Item> items = tieredSurvivalItems(tier).stream()
                .filter(item -> !item.getDefaultStack().isEmpty())
                .filter(item -> !player.getInventory().contains(item.getDefaultStack()))
                .toList();

        Item item = items.isEmpty() ? pickFallbackItem(random, tier) : items.get(random.nextInt(items.size()));
        data.putString("item", Registries.ITEM.getId(item).toString());
        storePhrase(data, "itemName", item.getName().getString());
    }

    private static void pickBreakTool(ServerPlayerEntity player, Random random, NbtCompound data) {
        pickBreakTool(player, random, data, VariantTier.ORDINARY);
    }

    private static void pickBreakTool(ServerPlayerEntity player, Random random, NbtCompound data, VariantTier tier) {
        List<Item> items = tieredBreakTools(tier).stream()
                .filter(item -> item.getDefaultStack().getMaxDamage() > 0)
                .toList();

        Item item = items.isEmpty() ? Items.STONE_AXE : items.get(random.nextInt(items.size()));
        data.putString("item", Registries.ITEM.getId(item).toString());
        storePhrase(data, "itemName", item.getName().getString());
    }

    private static void pickEntity(Random random, NbtCompound data) {
        pickEntity(random, data, VariantTier.ORDINARY);
    }

    private static void pickEntity(Random random, NbtCompound data, VariantTier tier) {
        List<EntityType<?>> types = tieredMobs(tier).stream()
                .filter(ChallengeGenerator::isGoodMob)
                .toList();
        EntityType<?> entityType = types.isEmpty() ? EntityType.ZOMBIE : types.get(random.nextInt(types.size()));
        data.putString("entity", Registries.ENTITY_TYPE.getId(entityType).toString());
        storePhrase(data, "entityName", entityType.getName().getString());
    }

    private static void pickTameMob(Random random, NbtCompound data, VariantTier tier) {
        EntityType<?>[] ordinary = {EntityType.WOLF, EntityType.HORSE, EntityType.DONKEY};
        EntityType<?>[] hard = {EntityType.CAT, EntityType.MULE, EntityType.PARROT};
        EntityType<?>[] pool = tier == VariantTier.ORDINARY ? ordinary : hard;
        EntityType<?> chosen = pool[random.nextInt(pool.length)];
        data.putString("entity", Registries.ENTITY_TYPE.getId(chosen).toString());
        storePhrase(data, "entityName", chosen.getName().getString());
    }

    private static final EntityType<?>[] LEASHABLE_TYPES = {
            EntityType.ALLAY,
            EntityType.ARMADILLO,
            EntityType.AXOLOTL,
            EntityType.BEE,
            EntityType.CAMEL,
            EntityType.CAT,
            EntityType.CHICKEN,
            EntityType.COW,
            EntityType.DONKEY,
            EntityType.FOX,
            EntityType.FROG,
            EntityType.GOAT,
            EntityType.HORSE,
            EntityType.LLAMA,
            EntityType.MOOSHROOM,
            EntityType.MULE,
            EntityType.OCELOT,
            EntityType.PIG,
            EntityType.POLAR_BEAR,
            EntityType.RABBIT,
            EntityType.SHEEP,
            EntityType.SNIFFER,
            EntityType.STRIDER,
            EntityType.TRADER_LLAMA,
            EntityType.WOLF,
            EntityType.SKELETON_HORSE,
            EntityType.ZOMBIE_HORSE,
            EntityType.IRON_GOLEM
    };

    private static void pickLeashMob(ServerWorld world, Random random, NbtCompound data) {
        List<EntityType<?>> types = ChallengeCategoryManager.get().leashableMobs();
        if (types.isEmpty()) {
            types = Registries.ENTITY_TYPE.stream()
                    .filter(ChallengeGenerator::isGoodMob)
                    .toList();
        }

        EntityType<?> type = types.isEmpty() ? EntityType.CAT : types.get(random.nextInt(types.size()));
        data.putString("entity", Registries.ENTITY_TYPE.getId(type).toString());
        storePhrase(data, "entityName", type.getName().getString());
    }

    private static void pickBlock(Random random, NbtCompound data) {
        pickBlock(random, data, VariantTier.ORDINARY);
    }

    private static void pickBlock(Random random, NbtCompound data, VariantTier tier) {
        List<Block> blocks = tieredBlocks(tier).stream().filter(ChallengeGenerator::isSafeBlock).toList();
        Block block = blocks.isEmpty() ? Blocks.STONE : blocks.get(random.nextInt(blocks.size()));
        data.putString("block", Registries.BLOCK.getId(block).toString());
        storePhrase(data, "blockName", block.getName().getString());
    }

    private static void pickFood(Random random, NbtCompound data) {
        pickFood(random, data, VariantTier.ORDINARY);
    }

    private static void pickFood(Random random, NbtCompound data, VariantTier tier) {
        List<Item> items = tieredFoods(tier);
        Item item = items.isEmpty() ? Items.BREAD : items.get(random.nextInt(items.size()));
        data.putString("item", Registries.ITEM.getId(item).toString());
        storePhrase(data, "itemName", item.getName().getString());
    }

    private static List<Item> tieredSurvivalItems(VariantTier tier) {
        return ChallengeCategoryManager.get().survivalItems(tier.name());
    }

    private static List<Item> tieredBreakTools(VariantTier tier) {
        return ChallengeCategoryManager.get().toolItems(tier.name());
    }

    private static List<Item> tieredFoods(VariantTier tier) {
        return ChallengeCategoryManager.get().foodItems(tier.name());
    }

    private static List<Block> tieredBlocks(VariantTier tier) {
        return ChallengeCategoryManager.get().blocks(tier.name());
    }

    private static List<EntityType<?>> tieredMobs(VariantTier tier) {
        return ChallengeCategoryManager.get().mobs(tier.name());
    }

    private static List<StatusEffect> tieredEffects(VariantTier tier) {
        return ChallengeCategoryManager.get().effects(tier.name());
    }

    private static Item pickFallbackItem(Random random, VariantTier tier) {
        List<Item> items = tieredSurvivalItems(tier).stream()
                .filter(item -> !item.getDefaultStack().isEmpty())
                .toList();
        if (items.isEmpty()) {
            items = Registries.ITEM.stream()
                    .filter(ChallengeGenerator::isSurvivalItem)
                    .filter(item -> !item.getDefaultStack().isEmpty())
                    .toList();
        }
        return items.isEmpty() ? Items.STONE : items.get(random.nextInt(items.size()));
    }

    private static VariantTier itemTier(Item item) {
        String path = Registries.ITEM.getId(item).getPath();
        if (path.contains("diamond") || path.contains("netherite") || path.contains("elytra") || path.contains("nether_star")
                || path.contains("dragon_breath") || path.contains("shulker_shell") || path.contains("chorus_fruit")
                || path.contains("totem_of_undying") || path.contains("beacon") || path.contains("conduit")
                || path.contains("suspicious_stew") || path.contains("ancient_debris")) {
            return VariantTier.INSANE;
        }
        if (path.contains("obsidian") || path.contains("crying_obsidian") || path.contains("end_") || path.contains("ender")
                || path.contains("quartz") || path.contains("prismarine") || path.contains("coral")
                || path.contains("sponge") || path.contains("sculk") || path.contains("amethyst")
                || path.contains("honey") || path.contains("golden_apple") || path.contains("golden_carrot")
                || path.contains("cake") || path.contains("cookie") || path.contains("milk_bucket")
                || path.contains("beetroot_soup") || path.contains("mushroom_stew") || path.contains("glow_berries")
                || path.contains("heart_of_the_sea") || path.contains("echo_shard") || path.contains("trident")
                || path.contains("blaze_rod") || path.contains("ghast_tear") || path.contains("iron_sword") || path.contains("iron_pickaxe")
                || path.contains("iron_axe") || path.contains("iron_shovel") || path.contains("iron_hoe")
                || path.contains("iron_helmet") || path.contains("iron_chestplate") || path.contains("iron_leggings")
                || path.contains("iron_boots") || path.contains("iron_horse_armor") || path.contains("chainmail")
                || path.contains("bow") || path.contains("crossbow") || path.contains("flint_and_steel")) {
            return VariantTier.HARD;
        }
        return VariantTier.ORDINARY;
    }

    private static VariantTier foodTier(Item item) {
        String path = Registries.ITEM.getId(item).getPath();
        if (path.contains("enchanted_golden_apple") || path.contains("chorus_fruit") || path.contains("suspicious_stew")) {
            return VariantTier.INSANE;
        }
        if (path.equals("honey_bottle") || path.equals("golden_apple") || path.equals("golden_carrot")
                || path.equals("cake") || path.equals("cookie") || path.equals("mushroom_stew")
                || path.equals("beetroot_soup") || path.equals("milk_bucket") || path.equals("glow_berries")) {
            return VariantTier.HARD;
        }
        return VariantTier.ORDINARY;
    }

    private static VariantTier blockTier(Block block) {
        String path = Registries.BLOCK.getId(block).getPath();
        if (path.contains("end") || path.contains("purpur") || path.contains("chorus") || path.contains("shulker")
                || path.contains("dragon") || path.contains("beacon") || path.contains("conduit")
                || path.contains("netherite") || path.contains("ancient_debris")) {
            return VariantTier.INSANE;
        }
        if (path.contains("obsidian") || path.contains("quartz") || path.contains("nether") || path.contains("soul_")
                || path.contains("bone_block") || path.contains("glowstone") || path.contains("blackstone")
                || path.contains("basalt") || path.contains("prismarine") || path.contains("coral")
                || path.contains("sponge") || path.contains("sculk") || path.contains("amethyst")
                || path.contains("deepslate") || path.contains("emerald") || path.contains("copper")
                || path.contains("magma") || path.contains("crying_obsidian") || path.contains("lodestone")
                || path.contains("respawn_anchor")) {
            return VariantTier.HARD;
        }
        return VariantTier.ORDINARY;
    }

    private static VariantTier mobTier(EntityType<?> type) {
        String path = Registries.ENTITY_TYPE.getId(type).getPath();
        if (path.contains("ender_dragon") || path.contains("wither") || path.contains("warden")
                || path.contains("shulker") || path.contains("endermite") || path.contains("silverfish")
                || path.contains("elder_guardian") || path.contains("piglin_brute") || path.contains("hoglin")
                || path.contains("zoglin") || path.contains("strider") || path.contains("ghast")
                || path.contains("blaze") || path.contains("wither_skeleton") || path.contains("enderman")
                || path.contains("phantom") || path.contains("ravager") || path.contains("evoker")
                || path.contains("vindicator") || path.contains("illusioner")) {
            return VariantTier.INSANE;
        }
        if (path.contains("witch") || path.contains("slime") || path.contains("magma_cube")
                || path.contains("drowned") || path.contains("guardian") || path.contains("pillager")
                || path.contains("husk") || path.contains("stray") || path.contains("piglin")
                || path.contains("hoglin") || path.contains("zombified_piglin") || path.contains("zombie_villager")
                || path.contains("panda") || path.contains("llama") || path.contains("trader_llama")
                || path.contains("iron_golem") || path.contains("snow_golem") || path.contains("bee")
                || path.contains("axolotl") || path.contains("goat") || path.contains("sniffer")
                || path.contains("armadillo") || path.contains("frog") || path.contains("camel")) {
            return VariantTier.HARD;
        }
        return VariantTier.ORDINARY;
    }

    private static void pickEnchantable(Random random, NbtCompound data) {
        List<Item> items = Registries.ITEM.stream()
                .filter(ChallengeGenerator::isSurvivalItem)
                .filter(i -> i.getDefaultStack().isEnchantable())
                .toList();
        Item item = items.isEmpty() ? Items.IRON_SWORD : items.get(random.nextInt(items.size()));
        data.putString("item", Registries.ITEM.getId(item).toString());
        storePhrase(data, "itemName", item.getName().getString());
    }

    private static void pickSmelt(Random random, NbtCompound data) {
        String[] ids = {
                "minecraft:copper_ingot",
                "minecraft:iron_ingot",
                "minecraft:gold_ingot"
        };
        String id = ids[random.nextInt(ids.length)];
        Item item = Registries.ITEM.get(Identifier.of(id));
        data.putString("item", id);
        storePhrase(data, "itemName", item.getName().getString());
    }

    private static void pickRename(Random random, NbtCompound data, VariantTier tier) {
        List<Item> items = tieredSurvivalItems(tier).stream().filter(item -> !item.getDefaultStack().isEmpty()).toList();
        Item item = items.isEmpty() ? pickFallbackItem(random, tier) : items.get(random.nextInt(items.size()));
        String[] names = {
                "Александр", "Дмитрий", "Максим", "Артём", "Иван", "Михаил", "Андрей", "Кирилл", "Никита", "Егор", "Илья", "Денис", "Роман", "Владислав", "Павел", "Сергей", "Алексей", "Константин", "Тимофей", "Степан", "Виктор", "Олег", "Евгений", "Вадим", "Юрий", "Глеб", "Борис", "Ярослав", "Захар", "Данил", "Лев", "Матвей", "Савелий", "Фёдор", "Пётр", "Руслан", "Арсений", "Виталий",
                "Игнат", "Святослав", "Антон", "Валерий", "Георгий", "Семён", "Эдуард", "Герман", "Ростислав", "Мирон", "Марк", "Всеволод", "Анна", "Мария", "София", "Алиса", "Екатерина", "Дарья", "Полина", "Виктория", "Анастасия", "Елизавета", "Ксения", "Вероника", "Валерия", "Арина", "Милана", "Ульяна", "Диана", "Василиса", "Кира", "Алёна", "Татьяна", "Юлия", "Ирина", "Ольга", "Светлана", "Наталья", "Любовь", "Надежда", "Галина", "Людмила", "Злата", "Ева", "Ангелина", "Варвара", "Карина", "Инна", "Нина", "Лилия", "Марина", "Тамара", "Клавдия", "Раиса", "Элина", "Алина", "Яна", "Евгения", "Жанна", "Снежана", "Таисия", "Агата"
        };
        String name = names[random.nextInt(names.length)];
        data.putString("item", Registries.ITEM.getId(item).toString());
        storePhrase(data, "itemName", item.getName().getString());
        data.putString("name", name);
    }

    private static void pickDimension(ServerPlayerEntity player, Random random, NbtCompound data, RunDifficulty runDifficulty) {
        String dim = switch (runDifficulty == null ? RunDifficulty.NORMAL : runDifficulty) {
            case NORMAL -> "minecraft:the_nether";
            case HARD -> "minecraft:the_nether";
            case INSANE -> "minecraft:the_end";
        };
        data.putString("dimension", dim);
        data.putString("dimensionNameKey", switch (dim) {
            case "minecraft:the_nether" -> "nether";
            case "minecraft:the_end" -> "end";
            default -> "overworld";
        });
    }

    private static void pickStructure(Random random, NbtCompound data, RunDifficulty runDifficulty) {
        String[] hard = {"village", "temple", "outpost", "shipwreck"};
        String[] insane = {"fortress", "bastion"};
        String s;
        if (runDifficulty == RunDifficulty.INSANE) {
            s = insane[random.nextInt(insane.length)];
        } else {
            s = hard[random.nextInt(hard.length)];
        }
        data.putString("structure", s);
        data.putString("structureNameKey", s);
    }

    private static void fillModifierData(ServerPlayerEntity player, Random random, Modifier modifier, NbtCompound data, RunDifficulty runDifficulty, VariantTier tier) {
        if (modifier == null) return;
        switch (modifier) {
            case TIME -> pickTime(random, data);
            case LOOKING_NORTH -> pickDirection(random, data);
            case LOOKING_UP_DOWN -> pickLookVertical(random, data);
            case ARMOR -> pickArmor(random, data);
            case EFFECT -> pickEffect(random, data, tier);
            case NEAR_MOB -> pickNearMob(player.getServerWorld(), random, data, runDifficulty);
            default -> {
            }
        }
    }

    private static void pickTime(Random random, NbtCompound data) {
        String[][] values = {
                {"day", "днём"},
                {"night", "ночью"}
        };
        String[] chosen = values[random.nextInt(values.length)];
        data.putString("time", chosen[0]);
        data.putString("timeText", chosen[1]);
    }

    private static void pickDirection(Random random, NbtCompound data) {
        String[][] values = {
                {"north", "смотря на север"},
                {"south", "смотря на юг"},
                {"west", "смотря на запад"},
                {"east", "смотря на восток"}
        };
        String[] chosen = values[random.nextInt(values.length)];
        data.putString("direction", chosen[0]);
        data.putString("directionText", chosen[1]);
    }

    private static void pickLookVertical(Random random, NbtCompound data) {
        String[][] values = {
                {"up", "смотря вверх"},
                {"down", "смотря вниз"}
        };
        String[] chosen = values[random.nextInt(values.length)];
        data.putString("lookVertical", chosen[0]);
        data.putString("lookVerticalText", chosen[1]);
    }

    private static void pickArmor(Random random, NbtCompound data) {
        String[][] values = {
                {"full", "в полной броне"},
                {"none", "без брони"}
        };
        String[] chosen = values[random.nextInt(values.length)];
        data.putString("armor", chosen[0]);
        data.putString("armorText", chosen[1]);
    }

    private static void pickNearMob(ServerWorld world, Random random, NbtCompound data, RunDifficulty runDifficulty) {
        VariantTier tier = tierFor(runDifficulty);
        List<EntityType<?>> types = tieredMobs(tier);
        if (types.isEmpty()) {
            types = Registries.ENTITY_TYPE.stream()
                    .filter(ChallengeGenerator::isGoodMob)
                    .toList();
        }

        EntityType<?> type = types.isEmpty()
                ? EntityType.ZOMBIE
                : types.get(random.nextInt(types.size()));

        data.putString("nearEntity", Registries.ENTITY_TYPE.getId(type).toString());
        storePhrase(data, "nearEntityName", type.getName().getString());
    }

    private static void pickBiome(Random random, NbtCompound data, RunDifficulty runDifficulty) {
        String[][] ordinary = {
                {"ocean", "океан"},
                {"forest", "лес"},
                {"mountains", "горы"},
                {"river", "река"}
        };
        String[][] hard = {
                {"desert", "пустыню"},
                {"swamp", "болото"}
        };
        String[] chosen;
        RunDifficulty safe = runDifficulty == null ? RunDifficulty.NORMAL : runDifficulty;
        if (safe == RunDifficulty.NORMAL) {
            chosen = ordinary[random.nextInt(ordinary.length)];
        } else {
            chosen = hard[random.nextInt(hard.length)];
        }
        data.putString("biome", chosen[0]);
        data.putString("biomeGroup", chosen[0]);
        data.putString("biomeNameKey", chosen[0]);
    }

    private static void pickEffect(Random random, NbtCompound data, VariantTier tier) {
        List<StatusEffect> effects = tieredEffects(tier);
        StatusEffect effect = effects.isEmpty()
                ? StatusEffects.SPEED.value()
                : effects.get(random.nextInt(effects.size()));

        data.putString("effect", Registries.STATUS_EFFECT.getId(effect).toString());
        storePhrase(data, "effectName", effect.getName().getString());
    }

    private static String formatEntityPhrase(String raw) {
        return inflectPhrase(raw, true);
    }

    private static String formatItemPhrase(String raw) {
        return inflectPhrase(raw, false);
    }

    private static String formatObjectPhrase(String raw) {
        return normalizePhrase(raw);
    }

    private static String normalizePhrase(String raw) {
        if (raw == null || raw.isBlank()) return "";
        return raw.toLowerCase(Locale.ROOT).trim();
    }

    private static void storePhrase(NbtCompound data, String key, String raw) {
        String original = raw == null ? "" : raw.trim();
        data.putString(key, normalizePhrase(original));
        data.putString(key + "Raw", original);
    }

    private static String inflectPhrase(String raw, boolean animate) {
        if (raw == null || raw.isBlank()) return "";
        String lower = raw.toLowerCase(Locale.ROOT).trim();
        StringBuilder out = new StringBuilder();
        for (String part : lower.split("\\s+")) {
            if (out.length() > 0) out.append(' ');
            out.append(inflectWord(part, animate));
        }
        return out.toString();
    }

    private static String inflectWord(String word, boolean animate) {
        if (word.isBlank()) return word;

        String[] hyphen = word.split("-", -1);
        if (hyphen.length > 1) {
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < hyphen.length; i++) {
                if (i > 0) out.append('-');
                out.append(inflectWord(hyphen[i], animate));
            }
            return out.toString();
        }

        if (animate) {
            switch (word) {
                case "утопленник" -> {
                    return "утопленника";
                }
                case "пчела" -> {
                    return "пчелу";
                }
                case "собака" -> {
                    return "собаку";
                }
                case "волк" -> {
                    return "волка";
                }
                case "курица" -> {
                    return "курицу";
                }
                case "брутальный" -> {
                    return "брутального";
                }
                case "деревенский" -> {
                    return "деревенского";
                }
                case "скелет" -> {
                    return "скелета";
                }
                case "зомби" -> {
                    return "зомби";
                }
                case "лошадь" -> {
                    return "лошадь";
                }
            }
            if (word.endsWith("ый")) return word.substring(0, word.length() - 2) + "ого";
            if (word.endsWith("ий")) return word.substring(0, word.length() - 2) + "его";
            if (word.endsWith("ой")) return word.substring(0, word.length() - 2) + "ого";
            if (word.endsWith("а")) return word.substring(0, word.length() - 1) + "у";
            if (word.endsWith("я")) return word.substring(0, word.length() - 1) + "ю";
            if (word.endsWith("ь") || word.endsWith("й")) return word.substring(0, word.length() - 1) + "я";
            if (word.matches(".*[бвгджзклмнпрстфхцчшщ]$")) return word + "а";
            return word;
        } else {
            if (word.endsWith("ая")) return word.substring(0, word.length() - 2) + "ую";
            if (word.endsWith("яя")) return word.substring(0, word.length() - 2) + "юю";
            if (word.endsWith("а")) return word.substring(0, word.length() - 1) + "у";
            if (word.endsWith("я")) return word.substring(0, word.length() - 1) + "ю";
            if (word.endsWith("ка")) return word.substring(0, word.length() - 2) + "ку";
            return word;
        }
    }

    private static boolean isSurvivalItem(Item item) {
        return ChallengeCategoryManager.get().isSurvivalItem(item);
    }

    private static boolean isGoodMob(EntityType<?> type) {
        return ChallengeCategoryManager.get().isGoodMob(type);
    }

    private static boolean isSafeBlock(Block block) {
        return ChallengeCategoryManager.get().isSafeBlock(block);
    }
}
