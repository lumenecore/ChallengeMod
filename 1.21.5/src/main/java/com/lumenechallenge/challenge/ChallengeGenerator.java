package com.lumenechallenge.challenge;

import com.lumenechallenge.challenge.condition.ObjectiveCondition;
import com.lumenechallenge.challenge.condition.ObjectiveCondition.Type;
import com.lumenechallenge.challenge.condition.ObjectiveCondition.Modifier;
import com.lumenechallenge.util.RussianGrammar;
import net.minecraft.block.Block;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
            Type.VISIT_COORDS,
            Type.DIE,
            Type.RESTORE_PORTAL,
            Type.FIND_DUNGEON_LOOT,
            Type.GET_ADVANCEMENT,
            Type.CREEPER_EXPLODE,
            Type.MILK_COW,
            Type.TILL_SOIL,
            Type.APPLY_EFFECT_TO_MOB,
            Type.BURN_MOB,
            Type.SKELETON_HIT_MOB,
            Type.KILL_MOB_WITH_EFFECT,
            Type.GET_EFFECT,
            Type.DIE_BY_CAUSE,
            Type.ENCHANT_ITEM_WITH,
            Type.RECEIVE_DAMAGE,
            Type.VILLAGER_LEVEL_UP,
            Type.PROJECTILE_HIT_TARGET,
            Type.GROW_TREE,
            Type.HARVEST_CROP,
            Type.STORE_ITEM,
            Type.PUT_MOB_IN_BOAT,
            Type.RIDE_MOB
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
        return generateTask(player, random, definition, runDifficulty, null, false);
    }

    private static GeneratedTask generateTask(ServerPlayerEntity player, Random random, ChallengeTaskDefinition definition,
                                              RunDifficulty runDifficulty, Modifier forcedModifier, boolean forceModifierSelection) {
        return generateTask(player, random, definition, runDifficulty, forcedModifier, forceModifierSelection, false);
    }

    private static GeneratedTask generateTask(ServerPlayerEntity player, Random random, ChallengeTaskDefinition definition,
                                              RunDifficulty runDifficulty, Modifier forcedModifier, boolean forceModifierSelection,
                                              boolean ignoreInitiallySatisfied) {
        return generateTask(player, random, definition, runDifficulty, forcedModifier, forceModifierSelection,
                ignoreInitiallySatisfied, null);
    }

    private static GeneratedTask generateTask(ServerPlayerEntity player, Random random, ChallengeTaskDefinition definition,
                                              RunDifficulty runDifficulty, Modifier forcedModifier, boolean forceModifierSelection,
                                              boolean ignoreInitiallySatisfied, VariantTier assignedTier) {
        Type type = definition.typeEnum();
        VariantTier taskTier = assignedTier == null
                ? pickVariantTier(random, runDifficulty, type)
                : pickVariantTierForAssignedTier(random, runDifficulty, type, definition.difficultyEnum(), assignedTier);
        if (taskTier == null) return null;
        boolean requiresTierModifier = assignedTier != null
                && tierFor(definition.difficultyEnum()).ordinal() < assignedTier.ordinal()
                && !supportsVariant(type, assignedTier);
        List<Modifier> modifiers = forceModifierSelection
                ? (forcedModifier == null ? List.of() : List.of(forcedModifier))
                : pickModifiers(random, definition, runDifficulty, assignedTier, requiresTierModifier);
        if (requiresTierModifier && modifiers.isEmpty()) return null;
        int count = pickCount(random, type, modifiers, runDifficulty, taskTier);

        NbtCompound data = new NbtCompound();
        fillData(player, random, type, data, count, runDifficulty, taskTier, modifiers);
        VariantTier effectiveAssignedTier = assignedTier == null
                ? maxTier(tierFor(definition.difficultyEnum()), taskTier)
                : assignedTier;
        data.putString("__assignedTier", effectiveAssignedTier.name());
        for (Modifier modifier : modifiers) {
            fillModifierData(player, random, modifier, data, runDifficulty, taskTier, type, effectiveAssignedTier);
        }

        int requiredCount = switch (type) {
            case JUMP, WALK_DISTANCE, BOAT_DISTANCE, EAT_ANY -> count;
            case ENCHANT_ITEM, FISH, SMELT_ITEM -> modifiers.contains(Modifier.COUNT) ? count : 1;
            default -> 1;
        };

        ChallengeTaskDefinition adjusted = definition.copy();
        ObjectiveCondition condition = new ObjectiveCondition(type, modifiers, requiredCount, data);
        // The developer command deliberately allows already-satisfied tasks so every
        // task ID can be tested on demand. Normal random generation keeps this guard.
        if (!ignoreInitiallySatisfied && startsSatisfied(player, type, data)) {
            return null;
        }
        return new GeneratedTask(condition, adjusted);
    }

    public static Challenge generate(ServerPlayerEntity player, long seed, int count, RunDifficulty runDifficulty) {
        ChallengeConfig config = ChallengeConfigManager.get();
        List<ChallengeTaskDefinition> allowed = config.allowedTasks(runDifficulty);
        if (allowed.isEmpty()) allowed = ChallengeConfig.defaults().allowedTasks(runDifficulty);

        Random random = new Random(seed ^ 0x5DEECE66DL);
        List<ChallengeTaskDefinition> available = new ArrayList<>();
        Set<String> seenTaskTypes = new HashSet<>();
        for (ChallengeTaskDefinition task : allowed) {
            if (task == null || !task.enabled() || task.type() == null || task.type().isBlank()) {
                continue;
            }
            // A configuration mistake must not make a task type repeatable.
            String typeKey = task.type().toUpperCase(Locale.ROOT);
            if (seenTaskTypes.add(typeKey)) {
                available.add(task.copy());
            }
        }

        List<VariantTier> plannedTiers = planTaskTiers(random, count, runDifficulty);
        List<com.lumenechallenge.challenge.condition.Condition> conditions = new ArrayList<>(count);
        List<ChallengeTaskDefinition> definitions = new ArrayList<>(count);
        Set<String> generatedVariants = new HashSet<>();

        for (VariantTier plannedTier : plannedTiers) {
            if (available.isEmpty()) break;

            // A slot is classified by its effective task tier, not by the task
            // definition's minimum difficulty. A Normal task can therefore fill
            // a Hard/Insane slot when its selected item, mob, block, etc. has that rarity.
            VariantTier assignedTier = plannedTier;
            List<ChallengeTaskDefinition> tierCandidates = tasksForAssignedTier(available, assignedTier);
            if (tierCandidates.isEmpty()) {
                // If a tier has no unused compatible task types left, reassign
                // only to another tier that still has eligible unused task types.
                assignedTier = chooseAvailableTier(random, available, runDifficulty);
                tierCandidates = tasksForAssignedTier(available, assignedTier);
            }
            if (tierCandidates.isEmpty()) continue;

            GeneratedTask generated = null;
            int attempts = Math.max(1, tierCandidates.size());
            for (int attempt = 0; attempt < attempts && generated == null; attempt++) {
                ChallengeTaskDefinition definition = pickWeightedTask(random, tierCandidates);
                if (definition == null) break;
                generated = generateUniqueTask(player, random, definition, runDifficulty, assignedTier, generatedVariants);
                if (generated == null) {
                    removeAllByType(tierCandidates, definition.type());
                    removeAllByType(available, definition.type());
                }
            }
            if (generated == null) continue;

            conditions.add(generated.condition());
            definitions.add(generated.definition());
            generatedVariants.add(variantKey(generated.condition()));
            // Weighted sampling without replacement: each task type can appear
            // only once in a run, regardless of how many parameter variants it has.
            removeAllByType(available, generated.definition().type());
        }

        return new Challenge(ChallengeType.DEATH, conditions, definitions, 0);
    }

    /**
     * Creates a balanced, randomized difficulty plan for the whole run.
     * A shared random phase gives exact 20/30/50 counts for ten tasks while
     * preserving the requested probabilities for short runs (including one task).
     */
    private static List<VariantTier> planTaskTiers(Random random, int count, RunDifficulty runDifficulty) {
        int safeCount = Math.max(0, count);
        if (safeCount == 0) return List.of();
        RunDifficulty safe = runDifficulty == null ? RunDifficulty.NORMAL : runDifficulty;
        int[] weights = switch (safe) {
            case NORMAL -> new int[]{100, 0, 0};
            case HARD -> new int[]{40, 60, 0};
            case INSANE -> new int[]{20, 30, 50};
        };
        double phase = random.nextDouble();
        int previous = (int) Math.floor(phase);
        double cumulative = 0.0D;
        List<VariantTier> result = new ArrayList<>(safeCount);
        VariantTier[] tiers = {VariantTier.ORDINARY, VariantTier.HARD, VariantTier.INSANE};
        for (int i = 0; i < tiers.length; i++) {
            cumulative += safeCount * (weights[i] / 100.0D);
            int boundary = (int) Math.floor(phase + cumulative + 1.0E-9D);
            int quota = Math.max(0, boundary - previous);
            for (int n = 0; n < quota; n++) result.add(tiers[i]);
            previous = boundary;
        }
        while (result.size() < safeCount) result.add(tiers[random.nextInt(tiers.length)]);
        Collections.shuffle(result, random);
        return result;
    }

    private static boolean hasVariantForRun(Type type, RunDifficulty runDifficulty) {
        RunDifficulty safe = runDifficulty == null ? RunDifficulty.NORMAL : runDifficulty;
        return switch (safe) {
            case NORMAL -> supportsVariant(type, VariantTier.ORDINARY);
            case HARD -> supportsVariant(type, VariantTier.ORDINARY) || supportsVariant(type, VariantTier.HARD);
            case INSANE -> supportsVariant(type, VariantTier.ORDINARY)
                    || supportsVariant(type, VariantTier.HARD)
                    || supportsVariant(type, VariantTier.INSANE);
        };
    }

    private static List<ChallengeTaskDefinition> tasksForAssignedTier(List<ChallengeTaskDefinition> tasks, VariantTier assignedTier) {
        if (tasks == null || assignedTier == null) return List.of();
        List<ChallengeTaskDefinition> result = new ArrayList<>();
        Set<String> seenTypes = new HashSet<>();
        for (ChallengeTaskDefinition task : tasks) {
            if (task == null || task.type() == null || task.type().isBlank()) continue;
            String key = task.type().toUpperCase(Locale.ROOT);
            if (seenTypes.add(key) && canGenerateAtAssignedTier(task, assignedTier)) {
                result.add(task);
            }
        }
        return result;
    }

    private static boolean canGenerateAtAssignedTier(ChallengeTaskDefinition definition, VariantTier assignedTier) {
        if (definition == null || assignedTier == null) return false;
        VariantTier minimumTier = tierFor(definition.difficultyEnum());
        if (minimumTier.ordinal() > assignedTier.ordinal()) return false;

        // If the task's minimum is lower than the slot's assigned tier, the
        // selected variant itself must carry that higher rarity. If the task's
        // minimum already equals the slot tier, any supported lower/equal
        // variant still leaves the effective task at that minimum tier.
        if (minimumTier.ordinal() < assignedTier.ordinal()) {
            return supportsVariant(definition.typeEnum(), assignedTier)
                    || hasTierCarrierModifier(definition, assignedTier);
        }
        for (VariantTier variantTier : VariantTier.values()) {
            if (variantTier.ordinal() <= assignedTier.ordinal() && supportsVariant(definition.typeEnum(), variantTier)) {
                return true;
            }
        }
        return false;
    }

    private static VariantTier maxTier(VariantTier first, VariantTier second) {
        if (first == null) return second == null ? VariantTier.ORDINARY : second;
        if (second == null) return first;
        return first.ordinal() >= second.ordinal() ? first : second;
    }

    private static VariantTier pickVariantTierForAssignedTier(Random random, RunDifficulty runDifficulty, Type type,
                                                                RunDifficulty minimumDifficulty, VariantTier assignedTier) {
        if (random == null || type == null || assignedTier == null || minimumDifficulty == null) return null;
        VariantTier minimumTier = tierFor(minimumDifficulty);
        if (minimumTier.ordinal() > assignedTier.ordinal()) return null;

        int[] weights = switch (runDifficulty == null ? RunDifficulty.NORMAL : runDifficulty) {
            case NORMAL -> new int[]{100, 0, 0};
            case HARD -> new int[]{40, 60, 0};
            case INSANE -> new int[]{15, 35, 50};
        };
        VariantTier[] all = {VariantTier.ORDINARY, VariantTier.HARD, VariantTier.INSANE};
        List<VariantTier> candidates = new ArrayList<>(3);
        List<Integer> candidateWeights = new ArrayList<>(3);
        for (int i = 0; i < all.length; i++) {
            VariantTier candidate = all[i];
            if (candidate.ordinal() > assignedTier.ordinal() || !supportsVariant(type, candidate)) continue;
            boolean needsModifierCarrier = minimumTier.ordinal() < assignedTier.ordinal()
                    && !supportsVariant(type, assignedTier);
            if (minimumTier.ordinal() < assignedTier.ordinal() && !needsModifierCarrier && candidate != assignedTier) continue;
            if (weights[i] <= 0) continue;
            candidates.add(candidate);
            candidateWeights.add(weights[i]);
        }
        return weightedChoice(random, candidates, candidateWeights);
    }

    private static RunDifficulty runDifficultyForTier(VariantTier tier) {
        return switch (tier == null ? VariantTier.ORDINARY : tier) {
            case ORDINARY -> RunDifficulty.NORMAL;
            case HARD -> RunDifficulty.HARD;
            case INSANE -> RunDifficulty.INSANE;
        };
    }

    private static boolean hasTierCarrierModifier(ChallengeTaskDefinition definition, VariantTier targetTier) {
        if (definition == null || targetTier == null) return false;
        ChallengeConfig config = ChallengeConfigManager.get();
        for (Integer id : definition.modifierIds()) {
            if (id == null || id <= 0) continue;
            Modifier modifier = modifierFromTaskId(id);
            if (modifier == null || !isModifierCompatibleWithTask(definition.typeEnum(), modifier)) continue;
            ChallengeConfig.ModifierRule rule = config.modifierRuleById(modifier.id());
            if (rule == null || !rule.enabled() || rule.difficultyEnum().ordinal() > targetTier.ordinal()) continue;
            if (modifierCanSupplyTier(modifier, rule, targetTier)) return true;
        }
        return false;
    }

    private static boolean modifierCanSupplyTier(Modifier modifier, ChallengeConfig.ModifierRule rule, VariantTier targetTier) {
        if (modifier == null || rule == null || targetTier == null) return false;
        if (rule.difficultyEnum().ordinal() == targetTier.ordinal()) return true;
        return switch (modifier) {
            case NEAR_MOB -> !tieredMobs(targetTier).isEmpty();
            case EFFECT -> !tieredEffects(targetTier).isEmpty();
            case LOOK_AT_TARGET -> !tieredMobs(targetTier).isEmpty() || !tieredBlocks(targetTier).isEmpty();
            default -> false;
        };
    }

    private static VariantTier chooseAvailableTier(Random random, List<ChallengeTaskDefinition> available, RunDifficulty runDifficulty) {
        List<VariantTier> present = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        RunDifficulty safe = runDifficulty == null ? RunDifficulty.NORMAL : runDifficulty;
        int[] targetWeights = switch (safe) {
            case NORMAL -> new int[]{100, 0, 0};
            case HARD -> new int[]{40, 60, 0};
            case INSANE -> new int[]{20, 30, 50};
        };
        VariantTier[] tiers = {VariantTier.ORDINARY, VariantTier.HARD, VariantTier.INSANE};
        for (int i = 0; i < tiers.length; i++) {
            if (targetWeights[i] > 0 && !tasksForAssignedTier(available, tiers[i]).isEmpty()) {
                present.add(tiers[i]);
                weights.add(targetWeights[i]);
            }
        }
        VariantTier selected = weightedChoice(random, present, weights);
        return selected == null ? tierFor(safe) : selected;
    }

    private static GeneratedTask generateUniqueTask(ServerPlayerEntity player, Random random, ChallengeTaskDefinition definition,
                                                    RunDifficulty runDifficulty, VariantTier assignedTier,
                                                    Set<String> generatedVariants) {
        if (definition == null) return null;
        // The task type is sampled without replacement. Most task generators are
        // deterministic once their category pools are cached; repeated 48-attempt
        // loops only regenerated the same impossible/invalid candidate.
        // Teleport dimension and modifier-carrier combinations still need a few
        // rolls because their valid parameter is selected randomly.
        int attempts = definition.typeEnum() == Type.TELEPORT_DIMENSION ? 8 : 4;
        for (int i = 0; i < attempts; i++) {
            GeneratedTask task = generateTask(player, random, definition, runDifficulty, null, false, false, assignedTier);
            if (task == null) continue;
            String key = variantKey(task.condition());
            if (!generatedVariants.contains(key)) return task;
        }
        return null;
    }

    private static String variantKey(ObjectiveCondition condition) {
        return condition == null ? "" : condition.variantKey();
    }


    public static GeneratedTask rerollTask(ServerPlayerEntity player, long seed, int slotIndex, RunDifficulty runDifficulty,
                                           String assignedTierName, int rerollSalt, Set<String> excludedVariants) {
        ChallengeConfig config = ChallengeConfigManager.get();
        List<ChallengeTaskDefinition> allowed = config.allowedTasks(runDifficulty);
        if (allowed.isEmpty()) allowed = ChallengeConfig.defaults().allowedTasks(runDifficulty);

        VariantTier assignedTier = parseTier(assignedTierName, tierFor(runDifficulty));
        List<ChallengeTaskDefinition> candidates = tasksForAssignedTier(allowed, assignedTier);
        Set<String> excluded = excludedVariants == null ? new HashSet<>() : new HashSet<>(excludedVariants);
        if (candidates.isEmpty()) return null;

        Random random = new Random(seed ^ 0x5DEECE66DL ^ ((long) slotIndex << 32) ^ rerollSalt);
        while (!candidates.isEmpty()) {
            ChallengeTaskDefinition definition = pickWeightedTask(random, candidates);
            if (definition == null) return null;

            // A type may reappear during a reroll, but its concrete variant may not.
            // Retry randomized parameters before concluding that this type is exhausted.
            int attempts = rerollVariantAttempts(definition.typeEnum());
            for (int attempt = 0; attempt < attempts; attempt++) {
                GeneratedTask task = generateTask(player, random, definition, runDifficulty,
                        null, false, false, assignedTier);
                if (task == null) continue;
                String key = variantKey(task.condition());
                if (!excluded.contains(key)) return task;
            }
            removeAllByType(candidates, definition.type());
        }
        return null;
    }

    private static int rerollVariantAttempts(Type type) {
        if (type == null) return 8;
        return switch (type) {
            case TELEPORT_DIMENSION, VISIT_COORDS, JUMP, WALK_DISTANCE, BOAT_DISTANCE,
                 DEAL_DAMAGE, REACH_Y_LEVEL, EAT_ANY, SLEEP, DIE, UNDERWATER,
                 EMPTY_INVENTORY, FULL_INVENTORY, CATCH_SELF_ON_ROD, RESTORE_PORTAL,
                 GET_ADVANCEMENT, CREEPER_EXPLODE, MILK_COW, TILL_SOIL -> 8;
            default -> 48;
        };
    }

    private static VariantTier parseTier(String name, VariantTier fallback) {
        if (name == null || name.isBlank()) return fallback;
        try { return VariantTier.valueOf(name.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ignored) { return fallback; }
    }

    /**
     * Builds a specific task for the developer-only /challenge command.
     * A supplied modifier ID is forced even if it would not normally be in the
     * random pool, which makes the command useful for testing rare combinations.
     */
    public static GeneratedTask generateDeveloperTask(ServerPlayerEntity player, int taskId, Integer modifierId,
                                                       RunDifficulty runDifficulty, long seed) {
        Type type = Type.fromId(taskId);
        if (player == null || type == null) return null;

        Modifier forcedModifier = null;
        if (modifierId != null) {
            forcedModifier = Modifier.fromId(modifierId);
            if (forcedModifier == null) return null;
        }

        ChallengeConfig config = ChallengeConfigManager.get();
        ChallengeTaskDefinition template = findDefinition(config, type);
        if (template == null) template = findDefinition(ChallengeConfig.defaults(), type);
        if (template == null) {
            template = new ChallengeTaskDefinition(type.name(), RunDifficulty.NORMAL.name(), 60, true);
        }
        ChallengeTaskDefinition definition = template.copy();
        definition.type = type.name();
        definition.enabled = true;
        definition.modifiers = modifierId == null ? new ArrayList<>() : new ArrayList<>(List.of(modifierId));

        Random random = new Random(seed ^ (((long) taskId) * 0x9E3779B97F4A7C15L) ^ System.nanoTime());
        RunDifficulty difficulty = runDifficulty == null ? RunDifficulty.NORMAL : runDifficulty;
        for (int attempt = 0; attempt < 128; attempt++) {
            GeneratedTask generated = generateTask(player, random, definition, difficulty, forcedModifier, true, true);
            if (generated != null) return generated;
        }
        return null;
    }

    private static ChallengeTaskDefinition findDefinition(ChallengeConfig config, Type type) {
        if (config == null || type == null || config.tasks() == null) return null;
        for (ChallengeTaskDefinition candidate : config.tasks()) {
            if (candidate != null && candidate.typeEnum() == type) return candidate.copy();
        }
        return null;
    }

    public static Challenge generate(long seed) {
        throw new UnsupportedOperationException("Use generate(player, seed, count)");
    }

    public record GeneratedTask(ObjectiveCondition condition, ChallengeTaskDefinition definition) {}

    private static ChallengeTaskDefinition pickWeightedTask(Random random, List<ChallengeTaskDefinition> pool) {
        if (random == null || pool == null || pool.isEmpty()) return null;
        int totalWeight = 0;
        for (ChallengeTaskDefinition task : pool) {
            if (task != null) totalWeight += Math.max(1, task.frequency());
        }
        if (totalWeight <= 0) return pool.get(0);

        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (ChallengeTaskDefinition task : pool) {
            if (task == null) continue;
            cumulative += Math.max(1, task.frequency());
            if (roll < cumulative) return task;
        }
        return pool.get(pool.size() - 1);
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

    private static void removeAllByType(List<ChallengeTaskDefinition> list, String type) {
        if (list == null || type == null) return;
        list.removeIf(task -> task != null && type.equalsIgnoreCase(task.type()));
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
            case 17 -> Modifier.EFFECT;
            case 18 -> Modifier.IN_DUNGEON;
            case 19 -> Modifier.IN_DIMENSION;
            case 20 -> Modifier.NO_MOVEMENT;
            case 21 -> Modifier.IN_AIR;
            case 22 -> Modifier.PERSPECTIVE;
            case 23 -> Modifier.LOOK_AT_TARGET;
            default -> null;
        };
    }

    /**
     * Exact task/modifier allowlist from the user's v1.2 task table.
     * A modifier being globally defined (including IDs 18-23) does not make it
     * compatible with every task. Only the pairs explicitly listed per task
     * are eligible for random generation.
     */
    private static boolean isModifierCompatibleWithTask(Type type, Modifier modifier) {
        if (type == null || modifier == null) return false;
        int id = modifier.id();
        return switch (type) {
            // 1 — Прыгните n раз: 2..10.
            case JUMP -> id >= 2 && id <= 10;

            // 2 — Добудьте предмет.
            case OBTAIN_ITEM -> id == 1;

            // 3-4, 7, 9, 13, 16-22, 25-51: no modifiers in the table.
            case BREAK_TOOL, VISIT_COORDS, STAND_ON_BLOCK, EAT_ITEM, SLEEP,
                 DEAL_DAMAGE, RENAME_ITEM, TELEPORT_DIMENSION, REACH_Y_LEVEL,
                 TRADE, FIND_STRUCTURE, VISIT_BIOME, TAME_MOB, EMPTY_INVENTORY,
                 FULL_INVENTORY, CATCH_SELF_ON_ROD, LEASH_MOB, DIE, RESTORE_PORTAL,
                 FIND_DUNGEON_LOOT, GET_ADVANCEMENT, CREEPER_EXPLODE, MILK_COW,
                 TILL_SOIL, APPLY_EFFECT_TO_MOB, BURN_MOB, SKELETON_HIT_MOB,
                 KILL_MOB_WITH_EFFECT, GET_EFFECT, DIE_BY_CAUSE, ENCHANT_ITEM_WITH,
                 RECEIVE_DAMAGE, VILLAGER_LEVEL_UP, PROJECTILE_HIT_TARGET, GROW_TREE,
                 HARVEST_CROP, STORE_ITEM, PUT_MOB_IN_BOAT, RIDE_MOB -> false;

            // 5 — Эндер-жемчуг.
            case ENDER_PEARL -> id == 2 || id == 4 || id == 7 || id == 8 || id == 9 || id == 10;

            // 6 — Убейте моба.
            case KILL_MOB -> id == 15 || id == 17;

            // 8 — Нырните под воду.
            case UNDERWATER -> id == 9;

            // 10 — Поешьте.
            case EAT_ANY -> id == 2 || id == 5 || id == 12 || id == 13 || id == 14;

            // 11 — Зачаруйте любой предмет; 14 — Переплавьте предмет.
            case ENCHANT_ITEM, SMELT_ITEM -> id == 1;

            // 12 — Порыбачьте.
            case FISH -> id == 1 || id == 2 || id == 10;

            // 15 — Выстрелите в моба с count блоков.
            case SHOOT_MOB_FROM_DISTANCE -> id == 9 || id == 16 || id == 13 || id == 17;

            // 23 — Пройдите пешком x блоков.
            case WALK_DISTANCE -> id == 8 || id == 2 || id == 3;

            // 24 — Преодолейте на лодке x блоков.
            case BOAT_DISTANCE -> id == 4 || id == 17;
        };
    }

    private static boolean modifierAllowedIn(ChallengeConfig config, Modifier modifier, RunDifficulty runDifficulty) {
        if (modifier == null || config == null) {
            return false;
        }
        ChallengeConfig.ModifierRule rule = config.modifierRuleById(modifier.id());
        return rule != null && rule.allowedIn(runDifficulty);
    }

    private static int modifierWeight(ChallengeConfig config, Modifier modifier) {
        if (modifier == null || config == null) {
            return 0;
        }
        ChallengeConfig.ModifierRule rule = config.modifierRuleById(modifier.id());
        return rule == null ? 0 : Math.max(1, rule.weight());
    }

    private static void fillData(ServerPlayerEntity player, Random random, Type type, NbtCompound data, int count, RunDifficulty runDifficulty, VariantTier taskTier, List<Modifier> modifiers) {
        BlockPos pos = player.getBlockPos();
        VariantTier tier = taskTier == null ? tierFor(runDifficulty) : taskTier;
        switch (type) {
            case OBTAIN_ITEM -> pickObtain(player, random, data, tier);
            case BREAK_TOOL -> pickBreakTool(player, random, data, tier);
            case KILL_MOB -> pickEntity(random, data, tier);
            case APPLY_EFFECT_TO_MOB, KILL_MOB_WITH_EFFECT -> pickMobEffectPair(random, data, tier);
            case VILLAGER_LEVEL_UP -> pickVillagerLevel(random, data, tier);
            case PROJECTILE_HIT_TARGET -> pickProjectileTarget(random, data, tier);
            case GROW_TREE -> pickSapling(random, data, tier);
            case HARVEST_CROP -> pickCrop(random, data, tier);
            case STORE_ITEM -> pickStorageTask(player, random, data, tier);
            case PUT_MOB_IN_BOAT -> pickBoatPassenger(random, data, tier);
            case RIDE_MOB -> pickRideableMob(random, data, tier);
            case GET_EFFECT -> pickAnyEffect(random, data, tier);
            case DIE_BY_CAUSE -> pickDeathCause(random, data, tier);
            case ENCHANT_ITEM_WITH -> pickEnchantItemWith(player, random, data, tier);
            case RECEIVE_DAMAGE -> pickDamageSource(random, data, tier);
            case BURN_MOB -> pickBurnTarget(random, data, tier);
            case SKELETON_HIT_MOB -> pickSkeletonTarget(random, data, tier);
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
            case TELEPORT_DIMENSION -> pickDimension(player, random, data, tier);
            case FIND_DUNGEON_LOOT -> pickDungeonLoot(random, data, tier);
            case REACH_Y_LEVEL -> data.putInt("y", random.nextInt(231) - 50);
            case FIND_STRUCTURE -> pickStructure(player, random, data, tier);
            case VISIT_BIOME -> pickBiome(random, data, tier);
            case WALK_DISTANCE, BOAT_DISTANCE -> {
                data.putDouble("distance", count);
                data.putDouble("startX", pos.getX());
                data.putDouble("startZ", pos.getZ());
            }
            case VISIT_COORDS -> {
                data.putInt("x", pos.getX() + random.nextInt(1001) - 500);
                data.putInt("z", pos.getZ() + random.nextInt(1001) - 500);
            }
            case ENDER_PEARL, FISH, JUMP, SLEEP, TRADE, EMPTY_INVENTORY, FULL_INVENTORY, CATCH_SELF_ON_ROD, UNDERWATER,
                 DIE, RESTORE_PORTAL, GET_ADVANCEMENT, CREEPER_EXPLODE, MILK_COW, TILL_SOIL -> {
            }
        }
    }

    private static List<String> configuredPool(String poolName, VariantTier tier) {
        return ChallengeCategoryManager.get().taskPool(poolName, tier == null ? "ORDINARY" : tier.name());
    }

    private static String randomRegisteredId(Random random, List<String> ids, java.util.function.Predicate<Identifier> exists) {
        if (random == null || ids == null || ids.isEmpty() || exists == null) return "";
        // Reservoir sampling selects uniformly from valid IDs without creating a
        // temporary filtered list on every task-generation attempt.
        String selected = "";
        int validCount = 0;
        for (String idText : ids) {
            if (idText == null || idText.isBlank()) continue;
            try {
                if (!exists.test(Identifier.of(idText))) continue;
                validCount++;
                if (random.nextInt(validCount) == 0) selected = idText;
            } catch (Exception ignored) {
                // Invalid or unavailable registry IDs are skipped.
            }
        }
        return selected;
    }
    private static void pickVillagerLevel(Random random, NbtCompound data, VariantTier tier) {
        List<String> professions = ChallengeCategoryManager.get().villagerProfessions();
        String professionId = professions.isEmpty() ? "minecraft:farmer" : professions.get(random.nextInt(professions.size()));
        String profession = Identifier.of(professionId).getPath();
        int level = tier == VariantTier.INSANE ? 4 + random.nextInt(2) : 2 + random.nextInt(2);
        data.putString("profession", profession);
        data.putInt("level", level);
    }

    private static void pickProjectileTarget(Random random, NbtCompound data, VariantTier tier) {
        String projectile = randomRegisteredId(random, configuredPool("projectiles", tier), Registries.ENTITY_TYPE::containsId);
        if (projectile.isBlank()) return;
        Identifier projectileId = Identifier.of(projectile);
        String projectilePath = projectileId.getPath();
        data.putString("projectile", projectile);
        data.putString("projectilePath", projectilePath);

        if (random.nextBoolean()) {
            pickBlock(random, data, tier);
            String target = data.getString("block", "");
            if (target.isBlank()) return;
            data.putString("targetKind", "block");
            data.putString("target", target);
        } else {
            pickEntity(random, data, tier);
            String target = data.getString("entity", "");
            if (target.isBlank()) return;
            data.putString("targetKind", "mob");
            data.putString("target", target);
        }
    }

    private static void pickStorageTask(ServerPlayerEntity player, Random random, NbtCompound data, VariantTier tier) {
        // ID49's item column is the entire Obtain Item registry; only the storage
        // itself has a Normal/Hard rarity split.
        Item item = pickInventoryMissingItem(random, player, ChallengeCategoryManager.get().allChallengeItems());
        if (item == null) return;
        data.putString("item", Registries.ITEM.getId(item).toString());
        storePhrase(data, "itemName", item.getName().getString());

        String storage = randomRegisteredId(random, configuredPool("storages", tier), Registries.BLOCK::containsId);
        if (storage.isBlank()) return;
        data.putString("storage", Identifier.of(storage).getPath());
    }

    private static void pickBoatPassenger(Random random, NbtCompound data, VariantTier tier) {
        pickMobFromTable(random, data, configuredPool("boatMobs", tier));
    }

    private static void pickRideableMob(Random random, NbtCompound data, VariantTier tier) {
        pickMobFromTable(random, data, configuredPool("rideMobs", tier));
    }

    private static void pickMobFromTable(Random random, NbtCompound data, List<String> ids) {
        String idText = randomRegisteredId(random, ids, Registries.ENTITY_TYPE::containsId);
        if (idText.isBlank()) return;
        Identifier id = Identifier.of(idText);
        EntityType<?> entityType = Registries.ENTITY_TYPE.get(id);
        data.putString("entity", idText);
        storePhrase(data, "entityName", entityType.getName().getString());
    }

    private static void pickLookTarget(Random random, NbtCompound data, VariantTier tier) {
        NbtCompound targetData = new NbtCompound();
        if (random.nextBoolean()) {
            pickEntity(random, targetData, tier);
            String target = targetData.getString("entity", "");
            if (target.isBlank()) return;
            data.putString("lookTargetKind", "mob");
            data.putString("lookTarget", target);
            data.putString("lookTargetName", targetData.getString("entityName", ""));
        } else {
            pickBlock(random, targetData, tier);
            String target = targetData.getString("block", "");
            if (target.isBlank()) return;
            data.putString("lookTargetKind", "block");
            data.putString("lookTarget", target);
            data.putString("lookTargetName", targetData.getString("blockName", ""));
        }
    }

    private static void pickSapling(Random random, NbtCompound data, VariantTier tier) {
        String idText = randomRegisteredId(random, configuredPool("saplings", tier), Registries.BLOCK::containsId);
        if (idText.isBlank()) return;
        Identifier id = Identifier.of(idText);
        Block block = Registries.BLOCK.get(id);
        data.putString("sapling", idText);
        data.putString("saplingPath", id.getPath());
        data.putString("block", idText);
        storePhrase(data, "blockName", block.getName().getString());
    }

    private static void pickCrop(Random random, NbtCompound data, VariantTier tier) {
        String idText = randomRegisteredId(random, configuredPool("crops", tier), Registries.BLOCK::containsId);
        if (idText.isBlank()) return;
        Identifier id = Identifier.of(idText);
        Block block = Registries.BLOCK.get(id);
        data.putString("crop", idText);
        data.putString("cropPath", id.getPath());
        data.putString("block", idText);
        storePhrase(data, "blockName", block.getName().getString());
    }

    private static VariantTier tierFor(RunDifficulty difficulty) {
        return switch (difficulty == null ? RunDifficulty.NORMAL : difficulty) {
            case NORMAL -> VariantTier.ORDINARY;
            case HARD -> VariantTier.HARD;
            case INSANE -> VariantTier.INSANE;
        };
    }

    /**
     * Selects the concrete difficulty tier of a task variant.
     * The run difficulty supplies the 100 / 40-60 / 15-35-50 weights;
     * the task supplies which variant tiers actually exist.
     */
    private static VariantTier pickVariantTier(Random random, RunDifficulty runDifficulty, Type type) {
        if (random == null || type == null) {
            return tierFor(runDifficulty);
        }

        RunDifficulty safe = runDifficulty == null ? RunDifficulty.NORMAL : runDifficulty;
        int ordinaryWeight = switch (safe) {
            case NORMAL -> 100;
            case HARD -> 40;
            case INSANE -> 15;
        };
        int hardWeight = switch (safe) {
            case NORMAL -> 0;
            case HARD -> 60;
            case INSANE -> 35;
        };
        int insaneWeight = switch (safe) {
            case NORMAL -> 0;
            case HARD -> 0;
            case INSANE -> 50;
        };

        List<VariantTier> tiers = new ArrayList<>(3);
        List<Integer> weights = new ArrayList<>(3);
        if (supportsVariant(type, VariantTier.ORDINARY) && ordinaryWeight > 0) {
            tiers.add(VariantTier.ORDINARY);
            weights.add(ordinaryWeight);
        }
        if (supportsVariant(type, VariantTier.HARD) && hardWeight > 0) {
            tiers.add(VariantTier.HARD);
            weights.add(hardWeight);
        }
        if (supportsVariant(type, VariantTier.INSANE) && insaneWeight > 0) {
            tiers.add(VariantTier.INSANE);
            weights.add(insaneWeight);
        }

        // If the run has no legal variant for this task, it is not generated in
        // this run. Do not silently promote a Normal run to a Hard/Insane variant.
        if (tiers.isEmpty()) return null;

        VariantTier chosen = weightedChoice(random, tiers, weights);
        return chosen == null ? tiers.get(0) : chosen;
    }

    private static boolean supportsVariant(Type type, VariantTier tier) {
        return switch (type) {
            // Ordinary / Hard / Insane variants.
            case OBTAIN_ITEM, BREAK_TOOL, KILL_MOB, STAND_ON_BLOCK, EAT_ITEM, DEAL_DAMAGE -> true;
            case BURN_MOB -> tier != VariantTier.INSANE;
            case APPLY_EFFECT_TO_MOB, SKELETON_HIT_MOB, KILL_MOB_WITH_EFFECT, ENCHANT_ITEM_WITH,
                 VILLAGER_LEVEL_UP, PROJECTILE_HIT_TARGET -> tier != VariantTier.ORDINARY;
            case GROW_TREE, HARVEST_CROP, STORE_ITEM -> tier != VariantTier.INSANE;
            case PUT_MOB_IN_BOAT, RIDE_MOB -> tier != VariantTier.ORDINARY;
            case GET_EFFECT -> tier == VariantTier.INSANE;
            case DIE_BY_CAUSE, RECEIVE_DAMAGE -> true;
            // Taming targets are all hard variants; biome groups have ordinary/hard variants.
            case TAME_MOB -> tier == VariantTier.HARD;
            case VISIT_BIOME -> tier != VariantTier.INSANE;
            // These tasks can only run in Hard+ sessions, but their individual
            // structure / loot / distance variant can still have ordinary rarity.
            case FIND_STRUCTURE -> true;
            case SHOOT_MOB_FROM_DISTANCE, TELEPORT_DIMENSION, FIND_DUNGEON_LOOT -> tier != VariantTier.ORDINARY;
            // These tasks have one concrete variant and should not be affected by this roll.
            default -> tier == VariantTier.ORDINARY;
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
            case APPLY_EFFECT_TO_MOB, KILL_MOB_WITH_EFFECT -> safe == RunDifficulty.INSANE ? 1500 : 1200;
            case GET_EFFECT -> 900;
            case DIE_BY_CAUSE -> 900;
            case ENCHANT_ITEM_WITH -> 1800;
            case RECEIVE_DAMAGE -> 900;
            case VILLAGER_LEVEL_UP -> 1500;
            case PROJECTILE_HIT_TARGET -> 1200;
            case GROW_TREE, HARVEST_CROP -> 900;
            case STORE_ITEM -> 600;
            case PUT_MOB_IN_BOAT, RIDE_MOB -> 900;
            case BURN_MOB -> safe == RunDifficulty.NORMAL ? 600 : 900;
            case SKELETON_HIT_MOB -> safe == RunDifficulty.INSANE ? 1800 : 1200;
            case EMPTY_INVENTORY -> 120;
            case FULL_INVENTORY -> 180;
            case CATCH_SELF_ON_ROD -> 300;
            case LEASH_MOB -> 500;
            case VISIT_COORDS -> 500;
            default -> fallback;
        };
    }

    private static List<Modifier> pickModifiers(Random random, ChallengeTaskDefinition definition, RunDifficulty runDifficulty,
                                                VariantTier assignedTier, boolean requireTierCarrier) {
        if (random == null || definition == null) return List.of();

        List<Integer> allowed = definition.modifierIds();
        if (allowed.isEmpty()) return List.of();

        ChallengeConfig config = ChallengeConfigManager.get();
        RunDifficulty modifierLimit = assignedTier == null ? runDifficulty : runDifficultyForTier(assignedTier);
        List<Modifier> candidates = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        List<Modifier> carrierCandidates = new ArrayList<>();
        List<Integer> carrierWeights = new ArrayList<>();

        for (Integer id : allowed) {
            if (id == null || id <= 0) continue;
            Modifier modifier = modifierFromTaskId(id);
            if (modifier == null || (definition.typeEnum() == Type.JUMP && modifier == Modifier.COUNT)) continue;
            if (!isModifierCompatibleWithTask(definition.typeEnum(), modifier)) continue;
            if (!modifierAllowedIn(config, modifier, modifierLimit)) continue;

            int weight = modifierWeight(config, modifier);
            if (weight <= 0) continue;
            candidates.add(modifier);
            weights.add(weight);

            ChallengeConfig.ModifierRule rule = config.modifierRuleById(modifier.id());
            if (assignedTier != null && modifierCanSupplyTier(modifier, rule, assignedTier)) {
                carrierCandidates.add(modifier);
                carrierWeights.add(weight);
            }
        }

        if (candidates.isEmpty()) return List.of();

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
        if (requireTierCarrier) {
            Modifier carrier = weightedChoice(random, carrierCandidates, carrierWeights);
            if (carrier == null) return List.of();
            selected.add(carrier);
        } else {
            if (random.nextInt(100) >= firstChance) return selected;
            Modifier first = weightedChoice(random, candidates, weights);
            if (first == null) return selected;
            selected.add(first);
        }

        if (random.nextInt(100) < secondChance) {
            Modifier second = weightedChoiceExcluding(random, candidates, weights, selected);
            if (second != null) selected.add(second);
        }

        if (selected.size() >= 2 && random.nextInt(100) < thirdChance) {
            Modifier third = weightedChoiceExcluding(random, candidates, weights, selected);
            if (third != null) selected.add(third);
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

    private static int pickCount(Random random, Type type, List<Modifier> modifiers, RunDifficulty runDifficulty, VariantTier taskTier) {
        if (random == null || type == null) {
            return 1;
        }

        return switch (type) {
            case JUMP -> 30 + random.nextInt(31);
            case EAT_ANY -> 1 + random.nextInt(3);
            case ENCHANT_ITEM -> modifiers.contains(Modifier.COUNT) ? 1 + random.nextInt(2) : 1;
            case FISH, SMELT_ITEM -> modifiers.contains(Modifier.COUNT) ? 1 + random.nextInt(4) : 1;
            case SHOOT_MOB_FROM_DISTANCE -> {
                VariantTier safeTier = taskTier == null ? tierFor(runDifficulty) : taskTier;
                if (safeTier == VariantTier.INSANE) {
                    int[] distances = {50, 60};
                    yield distances[random.nextInt(distances.length)];
                }
                int[] distances = {5, 10, 20, 30, 40};
                yield distances[random.nextInt(distances.length)];
            }
            case DEAL_DAMAGE -> {
                VariantTier safeTier = taskTier == null ? tierFor(runDifficulty) : taskTier;
                if (safeTier == VariantTier.INSANE) {
                    yield 10;
                }
                if (safeTier == VariantTier.HARD) {
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
            case TELEPORT_DIMENSION -> player.getWorld().getRegistryKey().getValue().toString().equals(data.getString("dimension", ""));
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
        List<Item> source = tieredSurvivalItems(tier);
        Item item = pickInventoryMissingItem(random, player, source);
        if (item == null) {
            item = pickInventoryMissingItem(random, player, ChallengeCategoryManager.get().allChallengeItems());
        }
        if (item == null) return;
        data.putString("item", Registries.ITEM.getId(item).toString());
        storePhrase(data, "itemName", item.getName().getString());
    }

    private static Item pickInventoryMissingItem(Random random, ServerPlayerEntity player, List<Item> source) {
        if (random == null || player == null || source == null || source.isEmpty()) return null;
        // Reservoir sampling avoids allocating a filtered list for each OBTAIN_ITEM task.
        Item selected = null;
        int count = 0;
        for (Item candidate : source) {
            if (candidate == null || candidate.getDefaultStack().isEmpty()
                    || player.getInventory().contains(candidate.getDefaultStack())) continue;
            count++;
            if (random.nextInt(count) == 0) selected = candidate;
        }
        return selected;
    }

    private static void pickBreakTool(ServerPlayerEntity player, Random random, NbtCompound data) {
        pickBreakTool(player, random, data, VariantTier.ORDINARY);
    }

    private static void pickBreakTool(ServerPlayerEntity player, Random random, NbtCompound data, VariantTier tier) {
        List<Item> items = tieredBreakTools(tier);
        if (items.isEmpty()) items = ChallengeCategoryManager.get().allChallengeTools();
        if (items.isEmpty()) return;
        Item item = items.get(random.nextInt(items.size()));
        data.putString("item", Registries.ITEM.getId(item).toString());
        storePhrase(data, "itemName", item.getName().getString());
    }

    private static void pickEntity(Random random, NbtCompound data) {
        pickEntity(random, data, VariantTier.ORDINARY);
    }

    private static void pickEntity(Random random, NbtCompound data, VariantTier tier) {
        List<EntityType<?>> types = tieredMobs(tier);
        if (types.isEmpty()) types = ChallengeCategoryManager.get().allChallengeMobs();
        if (types.isEmpty()) return;
        EntityType<?> entityType = types.get(random.nextInt(types.size()));
        data.putString("entity", Registries.ENTITY_TYPE.getId(entityType).toString());
        storePhrase(data, "entityName", entityType.getName().getString());
    }

    private static void pickMobEffectPair(Random random, NbtCompound data, VariantTier tier) {
        ChallengeCategoryManager categories = ChallengeCategoryManager.get();
        List<EntityType<?>> mobs = categories.taskMobs("effectMobs", tier.name());
        List<StatusEffect> effects = categories.taskEffects("effectMobEffects", tier.name());
        if (mobs.isEmpty() || effects.isEmpty()) return;

        record EffectPair(EntityType<?> mob, StatusEffect effect) {}
        List<EffectPair> validPairs = new ArrayList<>();
        for (EntityType<?> mob : mobs) {
            String mobId = Registries.ENTITY_TYPE.getId(mob).toString();
            for (StatusEffect effect : effects) {
                String effectId = Registries.STATUS_EFFECT.getId(effect).toString();
                if (!categories.isEffectImmune(mobId, effectId)) validPairs.add(new EffectPair(mob, effect));
            }
        }
        if (validPairs.isEmpty()) return;
        EffectPair chosen = validPairs.get(random.nextInt(validPairs.size()));
        storeEntityAndEffect(data, Registries.ENTITY_TYPE.getId(chosen.mob()).getPath(),
                Registries.STATUS_EFFECT.getId(chosen.effect()).getPath());
    }

    private static void storeEntityAndEffect(NbtCompound data, String mob, String effect) {
        String entityId = "minecraft:" + mob;
        String effectId = "minecraft:" + effect;
        data.putString("entity", entityId);
        data.putString("effect", effectId);
        EntityType<?> entityType = Registries.ENTITY_TYPE.get(Identifier.of(entityId));
        storePhrase(data, "entityName", entityType.getName().getString());
        var statusEffect = Registries.STATUS_EFFECT.get(Identifier.of(effectId));
        storePhrase(data, "effectName", statusEffect == null ? effect : net.minecraft.text.Text.translatable(statusEffect.getTranslationKey()).getString());
    }

    private static void pickBurnTarget(Random random, NbtCompound data, VariantTier tier) {
        String idText = randomRegisteredId(random, configuredPool("burnMobs", tier), Registries.ENTITY_TYPE::containsId);
        if (idText.isBlank()) return;
        Identifier id = Identifier.of(idText);
        EntityType<?> type = Registries.ENTITY_TYPE.get(id);
        data.putString("entity", idText);
        storePhrase(data, "entityName", type.getName().getString());
        data.putString("effect", "minecraft:fire_resistance");
    }

    private static void pickSkeletonTarget(Random random, NbtCompound data, VariantTier tier) {
        String idText = randomRegisteredId(random, configuredPool("skeletonTargets", tier), Registries.ENTITY_TYPE::containsId);
        if (idText.isBlank()) return;
        Identifier id = Identifier.of(idText);
        EntityType<?> type = Registries.ENTITY_TYPE.get(id);
        data.putString("entity", idText);
        storePhrase(data, "entityName", type.getName().getString());
    }

    private static void pickAnyEffect(Random random, NbtCompound data, VariantTier assignedTier) {
        if (assignedTier == null) return;
        List<StatusEffect> effects = tieredEffects(assignedTier);
        if (effects.isEmpty()) return;
        StatusEffect chosen = effects.get(random.nextInt(effects.size()));
        data.putString("effect", Registries.STATUS_EFFECT.getId(chosen).toString());
        storePhrase(data, "effectName", net.minecraft.text.Text.translatable(chosen.getTranslationKey()).getString());
    }

    private static void pickDeathCause(Random random, NbtCompound data, VariantTier tier) {
        List<String> pool = configuredPool("deathCauses", tier);
        if (pool.isEmpty()) return;
        String id = pool.get(random.nextInt(pool.size()));
        data.putString("cause", id);
        String name = switch (id) {
            case "drown" -> ", утонув";
            case "fall" -> ", упав с высоты";
            case "explosion" -> " от взрыва";
            case "on_fire" -> ", сгорев дотла";
            case "starve" -> " от голода";
            case "llama_spit" -> " от плевка ламы";
            case "anvil" -> " от падающей наковальни";
            default -> id;
        };
        storePhrase(data, "causeName", name);
    }

    private static void pickEnchantItemWith(ServerPlayerEntity player, Random random, NbtCompound data, VariantTier tier) {
        var enchantments = player.getWorld().getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        List<String> configured = configuredPool("enchantments", tier);
        record EnchantOption(Identifier id, List<String> itemKinds) {}
        List<EnchantOption> options = new ArrayList<>();
        for (String entry : configured) {
            if (entry == null || entry.isBlank()) continue;
            String[] parts = entry.split("\\|", 2);
            if (parts.length != 2) continue;
            try {
                Identifier enchantmentId = Identifier.of(parts[0].trim());
                if (!enchantments.containsId(enchantmentId)) continue;
                List<String> kinds = java.util.Arrays.stream(parts[1].split(","))
                        .map(String::trim).filter(value -> !value.isEmpty()).toList();
                if (!kinds.isEmpty()) options.add(new EnchantOption(enchantmentId, kinds));
            } catch (Exception ignored) {
                // Invalid table entries are skipped, never replaced by an unlisted enchantment.
            }
        }
        if (options.isEmpty()) return;

        for (int tries = 0; tries < 40; tries++) {
            EnchantOption option = options.get(random.nextInt(options.size()));
            String kind = option.itemKinds().get(random.nextInt(option.itemKinds().size()));
            String enchantId = option.id().toString();
            data.putString("item", "");
            data.putString("itemKind", kind);
            data.putString("enchantment", enchantId);
            storePhrase(data, "itemName", enchantItemKindName(kind));
            var enchantment = enchantments.get(option.id());
            storePhrase(data, "enchantmentName", enchantment == null
                    ? option.id().getPath()
                    : enchantment.description().getString());
            return;
        }
    }

    private static String enchantItemKindName(String kind) {
        return switch (kind) {
            case "pickaxe" -> "кирка";
            case "sword" -> "меч";
            case "axe" -> "топор";
            case "shovel" -> "лопата";
            case "hoe" -> "мотыга";
            case "bow" -> "лук";
            case "trident" -> "трезубец";
            case "armor" -> "предмет брони";
            case "rod" -> "удочка";
            case "xbow" -> "арбалет";
            case "tool" -> "инструмент";
            case "shears" -> "ножницы";
            default -> "предмет";
        };
    }

    private static void pickDamageSource(Random random, NbtCompound data, VariantTier tier) {
        List<EntityType<?>> configuredMobs = ChallengeCategoryManager.get().taskMobs("damageMobs", tier.name());
        List<String> causes = configuredPool("damageCauses", tier);
        if (!configuredMobs.isEmpty() && (!causes.isEmpty() ? random.nextBoolean() : true)) {
            EntityType<?> mob = configuredMobs.get(random.nextInt(configuredMobs.size()));
            data.putString("damage", "mob_attack");
            data.putString("entity", Registries.ENTITY_TYPE.getId(mob).toString());
            storePhrase(data, "damageName", "от " + mob.getName().getString());
            return;
        }
        if (!causes.isEmpty()) {
            String cause = causes.get(random.nextInt(causes.size()));
            data.putString("damage", cause);
            data.putString("entity", "");
            String name = switch (cause) {
                case "fall" -> "при падении";
                case "cactus" -> "от кактуса";
                case "fireworks" -> "от фейерверка";
                case "sweet_berry_bush" -> "наступив на ягоды";
                case "in_wall" -> "от удушения в блоке";
                case "fly_into_wall" -> "при столкновении со стеной при полете на элитрах";
                case "ender_pearl" -> "при броске эндер-жемчуга";
                case "freeze" -> "от замерзания";
                case "stalagmite" -> "упав на капельник";
                case "dragon_breath" -> "от дыхания дракона";
                default -> cause;
            };
            storePhrase(data, "damageName", name);
        }
    }

    private static void pickTameMob(Random random, NbtCompound data, VariantTier tier) {
        List<EntityType<?>> available = ChallengeCategoryManager.get().taskMobs("tameMobs", "HARD");
        if (available.isEmpty()) return;
        EntityType<?> chosen = available.get(random.nextInt(available.size()));
        data.putString("entity", Registries.ENTITY_TYPE.getId(chosen).toString());
        storePhrase(data, "entityName", chosen.getName().getString());
    }

    private static void pickLeashMob(ServerWorld world, Random random, NbtCompound data) {
        List<EntityType<?>> types = ChallengeCategoryManager.get().leashableMobs();
        if (types.isEmpty()) return;
        EntityType<?> type = types.get(random.nextInt(types.size()));
        data.putString("entity", Registries.ENTITY_TYPE.getId(type).toString());
        storePhrase(data, "entityName", type.getName().getString());
    }

    private static void pickBlock(Random random, NbtCompound data) {
        pickBlock(random, data, VariantTier.ORDINARY);
    }

    private static void pickBlock(Random random, NbtCompound data, VariantTier tier) {
        List<Block> blocks = tieredBlocks(tier);
        if (blocks.isEmpty()) blocks = ChallengeCategoryManager.get().allChallengeBlocks();
        if (blocks.isEmpty()) return;
        Block block = blocks.get(random.nextInt(blocks.size()));
        data.putString("block", Registries.BLOCK.getId(block).toString());
        storePhrase(data, "blockName", block.getName().getString());
    }

    private static void pickFood(Random random, NbtCompound data) {
        pickFood(random, data, VariantTier.ORDINARY);
    }

    private static void pickFood(Random random, NbtCompound data, VariantTier tier) {
        List<Item> items = tieredFoods(tier);
        if (items.isEmpty()) items = ChallengeCategoryManager.get().allChallengeFoods();
        if (items.isEmpty()) return;
        Item item = items.get(random.nextInt(items.size()));
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

    private static void pickEnchantable(Random random, NbtCompound data) {
        List<Item> items = ChallengeCategoryManager.get().allEnchantableItems();
        if (items.isEmpty()) return;
        Item item = items.get(random.nextInt(items.size()));
        data.putString("item", Registries.ITEM.getId(item).toString());
        storePhrase(data, "itemName", item.getName().getString());
    }

    private static void pickSmelt(Random random, NbtCompound data) {
        // The condition is fulfilled by smelting the corresponding ingot output,
        // but the player-facing target is the material (gold/iron/copper), not "ingot".
        String[][] targets = {
                {"minecraft:copper_ingot", "copper", "медь"},
                {"minecraft:iron_ingot", "iron", "железо"},
                {"minecraft:gold_ingot", "gold", "золото"}
        };
        String[] target = targets[random.nextInt(targets.length)];
        data.putString("item", target[0]);
        data.putString("materialNameKey", target[1]);
        storePhrase(data, "itemName", target[2]);
    }

    private static void pickRename(Random random, NbtCompound data, VariantTier tier) {
        List<Item> items = ChallengeCategoryManager.get().allChallengeItems();
        if (items.isEmpty()) return;
        Item item = items.get(random.nextInt(items.size()));
        data.putString("item", Registries.ITEM.getId(item).toString());
        storePhrase(data, "itemName", item.getName().getString());
    }

    private static void pickDimension(ServerPlayerEntity player, Random random, NbtCompound data, VariantTier tier) {
        String dim = tier == VariantTier.INSANE
                ? "minecraft:the_end"
                : "minecraft:the_nether";
        data.putString("dimension", dim);
        data.putString("dimensionNameKey", switch (dim) {
            case "minecraft:the_nether" -> "nether";
            case "minecraft:the_end" -> "end";
            default -> "overworld";
        });
    }

    private static void pickStructure(ServerPlayerEntity player, Random random, NbtCompound data, VariantTier tier) {
        // Structures are part of the dynamic world registry, not a static Registries entry.
        // Resolve them against the current server world so unavailable cross-version IDs are skipped.
        String idText = randomRegisteredId(random, configuredPool("structures", tier), id -> {
            try {
                player.getWorld().getRegistryManager().getOrThrow(RegistryKeys.STRUCTURE)
                        .getOrThrow(RegistryKey.of(RegistryKeys.STRUCTURE, id));
                return true;
            } catch (RuntimeException ignored) {
                return false;
            }
        });
        if (idText.isBlank()) return;
        data.putString("structure", idText);
        data.putString("structureNameKey", structureNameKey(Identifier.of(idText).getPath()));
    }

    private static String structureNameKey(String path) {
        return switch (path) {
            case "village_plains", "village_desert", "village_savanna", "village_snowy", "village_taiga" -> "village";
            case "desert_pyramid", "jungle_pyramid" -> "temple";
            case "pillager_outpost" -> "outpost";
            case "shipwreck" -> "shipwreck";
            case "fortress" -> "fortress";
            case "bastion_remnant" -> "bastion";
            case "mineshaft" -> "mineshaft";
            case "ruined_portal" -> "ruined_portal";
            case "ocean_ruin_cold", "ocean_ruin_warm" -> "ocean_ruins";
            case "abandoned_camp" -> "abandoned_camp";
            case "igloo" -> "igloo";
            case "swamp_hut" -> "witch_hut";
            case "trial_chambers" -> "trial_chambers";
            case "ocean_monument" -> "monument";
            case "woodland_mansion" -> "mansion";
            case "ancient_city" -> "ancient_city";
            case "end_city" -> "end_city";
            default -> path;
        };
    }

    private static void pickDungeonLoot(Random random, NbtCompound data, VariantTier tier) {
        String target = randomRegisteredId(random, configuredPool("dungeonLoot", tier), id ->
                id.getPath().equals("diamond_armor") || Registries.ITEM.containsId(id));
        if (target.isBlank()) return;
        data.putString("lootTarget", Identifier.of(target).getPath());
        if (target.endsWith(":diamond_armor")) {
            storePhrase(data, "itemName", "алмазную броню");
            return;
        }
        Identifier id = Identifier.of(target);
        Item item = Registries.ITEM.get(id);
        data.putString("item", target);
        storePhrase(data, "itemName", item.getName().getString());
    }

    private static void pickDungeonModifier(Random random, NbtCompound data) {
        String[] dungeons = {"village", "temple", "fortress", "mansion", "bastion"};
        String selected = dungeons[random.nextInt(dungeons.length)];
        data.putString("dungeon", selected);
    }

    private static void pickDimensionModifier(Random random, NbtCompound data, Type taskType) {
        String dim;
        if (taskType == Type.TELEPORT_DIMENSION && !data.getString("dimension", "").isBlank()) {
            dim = data.getString("dimension", "");
        } else {
            String[] dimensions = {"minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"};
            dim = dimensions[random.nextInt(dimensions.length)];
        }
        data.putString("requiredDimension", dim);
        data.putString("requiredDimensionNameKey", switch (dim) {
            case "minecraft:the_nether" -> "nether";
            case "minecraft:the_end" -> "end";
            default -> "overworld";
        });
    }

    private static void fillModifierData(ServerPlayerEntity player, Random random, Modifier modifier, NbtCompound data,
                                         RunDifficulty runDifficulty, VariantTier tier, Type taskType, VariantTier assignedTier) {
        if (modifier == null) return;
        VariantTier modifierTier = assignedTier == null ? tier : assignedTier;
        switch (modifier) {
            case TIME -> pickTime(random, data);
            case LOOKING_NORTH -> pickDirection(random, data);
            case LOOKING_UP_DOWN -> pickLookVertical(random, data);
            case ARMOR -> pickArmor(random, data);
            case EFFECT -> pickEffect(random, data, modifierTier);
            case NEAR_MOB -> pickNearMob(player.getServerWorld(), random, data, modifierTier);
            case IN_DUNGEON -> pickDungeonModifier(random, data);
            case IN_DIMENSION -> pickDimensionModifier(random, data, taskType);
            case PERSPECTIVE -> data.putInt("perspective", random.nextInt(3));
            case LOOK_AT_TARGET -> pickLookTarget(random, data, modifierTier);
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

    private static void pickNearMob(ServerWorld world, Random random, NbtCompound data, VariantTier tier) {
        // The nearby mob contributes directly to the assigned task difficulty,
        // so it must come from the slot's category instead of being rerolled
        // independently using the overall run difficulty.
        List<EntityType<?>> types = tieredMobs(tier);
        if (types.isEmpty()) types = ChallengeCategoryManager.get().allChallengeMobs();
        if (types.isEmpty()) return;
        EntityType<?> type = types.get(random.nextInt(types.size()));
        data.putString("nearEntity", Registries.ENTITY_TYPE.getId(type).toString());
        storePhrase(data, "nearEntityName", type.getName().getString());
    }

    private static VariantTier pickAnyVariantTier(Random random, RunDifficulty runDifficulty) {
        return pickAvailableTier(random, runDifficulty, tier -> !tieredMobs(tier).isEmpty());
    }

    private static VariantTier pickAnyEffectTier(Random random, RunDifficulty runDifficulty) {
        return pickAvailableTier(random, runDifficulty, tier -> !tieredEffects(tier).isEmpty());
    }

    private static VariantTier pickAvailableTier(Random random, RunDifficulty runDifficulty,
                                                   java.util.function.Predicate<VariantTier> available) {
        int[] weights = switch (runDifficulty == null ? RunDifficulty.NORMAL : runDifficulty) {
            case NORMAL -> new int[]{100, 0, 0};
            case HARD -> new int[]{40, 60, 0};
            case INSANE -> new int[]{15, 35, 50};
        };
        List<VariantTier> tiers = new ArrayList<>();
        List<Integer> tierWeights = new ArrayList<>();
        VariantTier[] all = {VariantTier.ORDINARY, VariantTier.HARD, VariantTier.INSANE};
        for (int i = 0; i < all.length; i++) {
            if (weights[i] > 0 && available.test(all[i])) {
                tiers.add(all[i]);
                tierWeights.add(weights[i]);
            }
        }
        return weightedChoice(random, tiers, tierWeights);
    }

    private static void pickBiome(Random random, NbtCompound data, VariantTier tier) {
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
        String[] chosen = tier == VariantTier.ORDINARY
                ? ordinary[random.nextInt(ordinary.length)]
                : hard[random.nextInt(hard.length)];
        data.putString("biome", chosen[0]);
        data.putString("biomeGroup", chosen[0]);
        data.putString("biomeNameKey", chosen[0]);
    }

    private static void pickEffect(Random random, NbtCompound data, VariantTier tier) {
        List<StatusEffect> effects = tieredEffects(tier);
        if (effects.isEmpty()) return;
        StatusEffect effect = effects.get(random.nextInt(effects.size()));
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
