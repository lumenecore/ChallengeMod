package com.lumenechallenge.challenge;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.minecraft.block.Block;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ChallengeCategoryManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String RESOURCE_PATH = "/assets/lumenechallenge/challenge/categories.json";
    private static final String[] WARMUP_TIERS = {"ORDINARY", "HARD", "INSANE"};
    private static final ChallengeCategoryManager INSTANCE = createInstance();

    private final CategoriesConfig config;

    // Registry-wide scans are expensive and the registries are stable for a server session.
    private final Map<String, List<Item>> survivalItemsCache = new ConcurrentHashMap<>();
    private final Map<String, List<Item>> foodItemsCache = new ConcurrentHashMap<>();
    private final Map<String, List<Item>> toolItemsCache = new ConcurrentHashMap<>();
    private final Map<String, List<Block>> blocksCache = new ConcurrentHashMap<>();
    private final Map<String, List<EntityType<?>>> mobsCache = new ConcurrentHashMap<>();
    private final Map<String, List<StatusEffect>> effectsCache = new ConcurrentHashMap<>();
    private final Map<String, List<String>> taskPoolsCache = new ConcurrentHashMap<>();
    private final Map<String, List<String>> biomeGroupsCache = new ConcurrentHashMap<>();
    private final Map<String, List<EntityType<?>>> taskMobsCache = new ConcurrentHashMap<>();
    private final Map<String, List<Block>> taskBlocksCache = new ConcurrentHashMap<>();
    private final Map<String, List<Item>> taskItemsCache = new ConcurrentHashMap<>();
    private final Map<String, List<StatusEffect>> taskEffectsCache = new ConcurrentHashMap<>();
    private volatile List<EntityType<?>> leashableMobsCache;
    private volatile List<String> villagerProfessionsCache;
    private volatile List<Item> allChallengeItemsCache;
    private volatile List<Item> allChallengeFoodsCache;
    private volatile List<Item> allEnchantableItemsCache;
    private volatile List<Item> allChallengeToolsCache;
    private volatile List<Block> allChallengeBlocksCache;
    private volatile List<EntityType<?>> allChallengeMobsCache;
    private volatile List<StatusEffect> allChallengeEffectsCache;
    private int warmupStep;
    private boolean warmupComplete;

    private static ChallengeCategoryManager createInstance() {
        return new ChallengeCategoryManager(loadConfigSafely());
    }

    private ChallengeCategoryManager(CategoriesConfig config) {
        this.config = config == null ? defaultConfig() : config;
        this.config.sanitize();
    }

    public static ChallengeCategoryManager get() {
        return INSTANCE;
    }

    /**
     * Warms one registry-backed cache per call. SeedSelectionScreen invokes this
     * from its normal UI tick, spreading the initial registry scans across frames
     * before the player presses Start.
     */
    public void warmupStep() {
        if (warmupComplete) return;
        int step = warmupStep++;
        if (step < 18) {
            String tier = WARMUP_TIERS[step / 6];
            switch (step % 6) {
                case 0 -> survivalItems(tier);
                case 1 -> foodItems(tier);
                case 2 -> toolItems(tier);
                case 3 -> blocks(tier);
                case 4 -> mobs(tier);
                case 5 -> effects(tier);
                default -> { }
            }
        } else if (step < 25) {
            switch (step - 18) {
                case 0 -> leashableMobs();
                case 1 -> villagerProfessions();
                case 2 -> allChallengeItems();
                case 3 -> allChallengeTools();
                case 4 -> allChallengeBlocks();
                case 5 -> allChallengeMobs();
                case 6 -> allChallengeEffects();
                default -> { }
            }
        } else {
            int taskStep = step - 25;
            switch (taskStep) {
                case 0 -> warmupTaskPools();
                case 1 -> taskMobs("effectMobs", "ORDINARY");
                case 2 -> taskMobs("effectMobs", "HARD");
                case 3 -> taskMobs("effectMobs", "INSANE");
                case 4 -> taskEffects("effectMobEffects", "ORDINARY");
                case 5 -> taskEffects("effectMobEffects", "HARD");
                case 6 -> taskEffects("effectMobEffects", "INSANE");
                case 7 -> taskMobs("damageMobs", "HARD");
                case 8 -> taskMobs("tameMobs", "HARD");
                case 9 -> allChallengeFoods();
                case 10 -> allEnchantableItems();
                default -> { }
            }
        }
        if (warmupStep >= 36) warmupComplete = true;
    }

    private void warmupTaskPools() {
        if (config.taskPools == null || config.taskPools.isEmpty()) return;
        for (String poolName : config.taskPools.keySet()) {
            for (String tier : WARMUP_TIERS) {
                taskPool(poolName, tier);
            }
        }
        if (config.biomeGroups != null) {
            for (String groupName : config.biomeGroups.keySet()) biomeGroup(groupName);
        }
    }

    public boolean isSurvivalItem(Item item) {
        if (item == null) {
            return false;
        }
        Identifier id = Registries.ITEM.getId(item);
        if (matchesAny(id, config.technical.items)) {
            return false;
        }
        String path = id.getPath();
        return !path.isBlank()
                && !path.endsWith("_spawn_egg")
                && !path.endsWith("spawn_egg")
                && !path.contains("command_block")
                && !path.contains("barrier")
                && !path.contains("structure_block")
                && !path.contains("debug")
                && !path.contains("jigsaw")
                && !path.contains("light")
                && !path.contains("knowledge_book")
                && !path.contains("debug_stick");
    }

    public boolean isSafeBlock(Block block) {
        if (block == null) {
            return false;
        }
        Identifier id = Registries.BLOCK.getId(block);
        if (matchesAny(id, config.technical.blocks)) {
            return false;
        }
        return !id.getPath().isBlank();
    }

    public boolean isGoodMob(EntityType<?> type) {
        if (type == null) {
            return false;
        }
        Identifier id = Registries.ENTITY_TYPE.getId(type);
        if (matchesAny(id, config.technical.mobs)) {
            return false;
        }
        return type.getSpawnGroup() != SpawnGroup.MISC
                && type != EntityType.PLAYER
                && type != EntityType.ARMOR_STAND
                && type != EntityType.END_CRYSTAL
                && type != EntityType.BAT;
    }

    public List<Item> survivalItems(String tierName) {
        String tier = normalizeTier(tierName);
        return survivalItemsCache.computeIfAbsent(tier,
                key -> itemsForTier(config.items, key, this::isSurvivalItem));
    }

    public List<Item> foodItems(String tierName) {
        String tier = normalizeTier(tierName);
        return foodItemsCache.computeIfAbsent(tier,
                key -> itemsForTier(config.food, key, item -> isSurvivalItem(item)
                        && item.getDefaultStack().contains(DataComponentTypes.FOOD)));
    }

    public List<Item> toolItems(String tierName) {
        String tier = normalizeTier(tierName);
        return toolItemsCache.computeIfAbsent(tier,
                key -> itemsForTier(config.tools, key, item -> isSurvivalItem(item)
                        && item.getDefaultStack().getMaxDamage() > 0
                        && !item.getDefaultStack().contains(DataComponentTypes.EQUIPPABLE)));
    }

    public List<Block> blocks(String tierName) {
        String tier = normalizeTier(tierName);
        return blocksCache.computeIfAbsent(tier, key -> blocksForTier(config.blocks, key));
    }

    public List<EntityType<?>> mobs(String tierName) {
        String tier = normalizeTier(tierName);
        return mobsCache.computeIfAbsent(tier, key -> mobsForTier(config.mobs, key));
    }

    public List<EntityType<?>> leashableMobs() {
        if (leashableMobsCache != null) return leashableMobsCache;
        List<EntityType<?>> result = new ArrayList<>();
        for (String idText : config.mobs.leashable) {
            if (idText == null || idText.isBlank()) {
                continue;
            }
            try {
                Identifier id = Identifier.of(idText.trim());
                EntityType<?> type = Registries.ENTITY_TYPE.get(id);
                if (type != null && id.equals(Registries.ENTITY_TYPE.getId(type)) && isGoodMob(type)) {
                    result.add(type);
                }
            } catch (Exception ignored) {
                // Skip malformed ids from external config.
            }
        }
        leashableMobsCache = List.copyOf(result);
        return leashableMobsCache;
    }

    public List<StatusEffect> effects(String tierName) {
        String tier = normalizeTier(tierName);
        return effectsCache.computeIfAbsent(tier, key -> effectsForTier(config.effects, key));
    }

    /**
     * Returns a configured task-specific pool exactly as authored in categories.json.
     * Unknown or unavailable registry entries are filtered by the typed accessors below.
     */
    public List<String> taskPool(String poolName, String tierName) {
        if (poolName == null || poolName.isBlank() || config.taskPools == null) return List.of();
        String tier = normalizeTier(tierName);
        String cacheKey = poolName + ':' + tier;
        return taskPoolsCache.computeIfAbsent(cacheKey, key -> {
            CategoriesConfig.TaskPool pool = config.taskPools.get(poolName);
            return pool == null ? List.of() : pool.forTier(tier);
        });
    }

    public List<String> biomeGroup(String groupName) {
        if (groupName == null || config.biomeGroups == null) return List.of();
        return biomeGroupsCache.computeIfAbsent(groupName, key -> {
            List<String> values = config.biomeGroups.get(key);
            return values == null ? List.of() : List.copyOf(values);
        });
    }

    public synchronized List<String> villagerProfessions() {
        if (villagerProfessionsCache == null) {
            villagerProfessionsCache = config.villagerProfessions == null
                    ? List.of() : List.copyOf(config.villagerProfessions);
        }
        return villagerProfessionsCache;
    }

    public boolean isEffectImmune(String mobId, String effectId) {
        if (mobId == null || effectId == null || config.effectImmunities == null) return false;
        List<String> immuneMobs = config.effectImmunities.get(effectId);
        return immuneMobs != null && immuneMobs.contains(mobId);
    }

    public List<EntityType<?>> taskMobs(String poolName, String tierName) {
        String cacheKey = cacheKey(poolName, tierName);
        if (cacheKey == null) return List.of();
        return taskMobsCache.computeIfAbsent(cacheKey, key -> {
        List<EntityType<?>> result = new ArrayList<>();
        for (String idText : taskPool(poolName, tierName)) {
            try {
                Identifier id = Identifier.of(idText);
                EntityType<?> type = Registries.ENTITY_TYPE.get(id);
                if (type != null && id.equals(Registries.ENTITY_TYPE.getId(type)) && isGoodMob(type)) result.add(type);
            } catch (Exception ignored) {
                // Invalid or unavailable IDs are intentionally ignored for cross-version data.
            }
        }
        return List.copyOf(result);
        });
    }

    public List<Block> taskBlocks(String poolName, String tierName) {
        String cacheKey = cacheKey(poolName, tierName);
        if (cacheKey == null) return List.of();
        return taskBlocksCache.computeIfAbsent(cacheKey, key -> {
        List<Block> result = new ArrayList<>();
        for (String idText : taskPool(poolName, tierName)) {
            try {
                Identifier id = Identifier.of(idText);
                Block block = Registries.BLOCK.get(id);
                if (block != null && id.equals(Registries.BLOCK.getId(block)) && isSafeBlock(block)) result.add(block);
            } catch (Exception ignored) {
                // Invalid or unavailable IDs are intentionally ignored for cross-version data.
            }
        }
        return List.copyOf(result);
        });
    }

    public List<Item> taskItems(String poolName, String tierName) {
        String cacheKey = cacheKey(poolName, tierName);
        if (cacheKey == null) return List.of();
        return taskItemsCache.computeIfAbsent(cacheKey, key -> {
        List<Item> result = new ArrayList<>();
        for (String idText : taskPool(poolName, tierName)) {
            try {
                Identifier id = Identifier.of(idText);
                Item item = Registries.ITEM.get(id);
                if (item != null && id.equals(Registries.ITEM.getId(item)) && isSurvivalItem(item)) result.add(item);
            } catch (Exception ignored) {
                // Invalid or unavailable IDs are intentionally ignored for cross-version data.
            }
        }
        return List.copyOf(result);
        });
    }

    public List<StatusEffect> taskEffects(String poolName, String tierName) {
        String cacheKey = cacheKey(poolName, tierName);
        if (cacheKey == null) return List.of();
        return taskEffectsCache.computeIfAbsent(cacheKey, key -> {
        List<StatusEffect> result = new ArrayList<>();
        for (String idText : taskPool(poolName, tierName)) {
            try {
                Identifier id = Identifier.of(idText);
                StatusEffect effect = Registries.STATUS_EFFECT.get(id);
                if (effect != null && id.equals(Registries.STATUS_EFFECT.getId(effect))) result.add(effect);
            } catch (Exception ignored) {
                // Invalid or unavailable IDs are intentionally ignored for cross-version data.
            }
        }
        return List.copyOf(result);
        });
    }

    private static String cacheKey(String poolName, String tierName) {
        if (poolName == null || poolName.isBlank()) return null;
        return poolName + ':' + normalizeTier(tierName);
    }

    /** Items that can actually be selected by any challenge category. */
    public synchronized List<Item> allChallengeItems() {
        if (allChallengeItemsCache != null) return allChallengeItemsCache;
        LinkedHashSet<Item> result = new LinkedHashSet<>();
        for (String tier : List.of("ORDINARY", "HARD", "INSANE")) {
            result.addAll(survivalItems(tier));
            result.addAll(foodItems(tier));
            result.addAll(toolItems(tier));
        }
        allChallengeItemsCache = List.copyOf(result);
        return allChallengeItemsCache;
    }

    /** All food items represented by the configured challenge categories. */
    public synchronized List<Item> allChallengeFoods() {
        if (allChallengeFoodsCache != null) return allChallengeFoodsCache;
        List<Item> result = new ArrayList<>();
        for (Item item : allChallengeItems()) {
            if (item.getDefaultStack().contains(DataComponentTypes.FOOD)) result.add(item);
        }
        allChallengeFoodsCache = List.copyOf(result);
        return allChallengeFoodsCache;
    }

    /** Items eligible for the enchant-item challenge; computed once per game session. */
    public synchronized List<Item> allEnchantableItems() {
        if (allEnchantableItemsCache != null) return allEnchantableItemsCache;
        List<Item> result = new ArrayList<>();
        for (Item item : allChallengeItems()) {
            if (item.getDefaultStack().isEnchantable()) result.add(item);
        }
        allEnchantableItemsCache = List.copyOf(result);
        return allEnchantableItemsCache;
    }

    /** Tools that can actually be selected by the break-tool challenge category. */
    public synchronized List<Item> allChallengeTools() {
        if (allChallengeToolsCache != null) return allChallengeToolsCache;
        LinkedHashSet<Item> result = new LinkedHashSet<>();
        for (String tier : List.of("ORDINARY", "HARD", "INSANE")) {
            result.addAll(toolItems(tier));
        }
        allChallengeToolsCache = List.copyOf(result);
        return allChallengeToolsCache;
    }

    /** Blocks that can actually be selected by the block challenge category. */
    public synchronized List<Block> allChallengeBlocks() {
        if (allChallengeBlocksCache != null) return allChallengeBlocksCache;
        LinkedHashSet<Block> result = new LinkedHashSet<>();
        for (String tier : List.of("ORDINARY", "HARD", "INSANE")) {
            result.addAll(blocks(tier));
        }
        allChallengeBlocksCache = List.copyOf(result);
        return allChallengeBlocksCache;
    }

    /** Mobs that can actually be selected by configured mob challenges. */
    public synchronized List<EntityType<?>> allChallengeMobs() {
        if (allChallengeMobsCache != null) return allChallengeMobsCache;
        LinkedHashSet<EntityType<?>> result = new LinkedHashSet<>();
        for (String tier : List.of("ORDINARY", "HARD", "INSANE")) {
            result.addAll(mobs(tier));
        }
        result.addAll(leashableMobs());
        allChallengeMobsCache = List.copyOf(result);
        return allChallengeMobsCache;
    }

    /** Effects that can actually be selected by configured effect modifiers. */
    public synchronized List<StatusEffect> allChallengeEffects() {
        if (allChallengeEffectsCache != null) return allChallengeEffectsCache;
        LinkedHashSet<StatusEffect> result = new LinkedHashSet<>();
        for (String tier : List.of("ORDINARY", "HARD", "INSANE")) {
            result.addAll(effects(tier));
        }
        allChallengeEffectsCache = List.copyOf(result);
        return allChallengeEffectsCache;
    }

    private static CategoriesConfig loadConfigSafely() {
        try (InputStream stream = ChallengeCategoryManager.class.getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null) {
                return defaultConfig();
            }
            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            CategoriesConfig config = GSON.fromJson(json, CategoriesConfig.class);
            return config == null ? defaultConfig() : config;
        } catch (IOException | JsonParseException ex) {
            return defaultConfig();
        }
    }

    private static CategoriesConfig defaultConfig() {
        CategoriesConfig config = new CategoriesConfig();

        config.technical.items = list(
                "minecraft:air",
                "minecraft:cave_air",
                "minecraft:void_air",
                "minecraft:barrier",
                "minecraft:light",
                "minecraft:jigsaw",
                "minecraft:structure_block",
                "minecraft:command_block",
                "minecraft:repeating_command_block",
                "minecraft:chain_command_block",
                "minecraft:knowledge_book",
                "minecraft:debug_stick"
        );
        config.technical.blocks = list(
                "minecraft:air",
                "minecraft:cave_air",
                "minecraft:void_air",
                "minecraft:barrier",
                "minecraft:light",
                "minecraft:jigsaw",
                "minecraft:structure_block",
                "minecraft:command_block",
                "minecraft:repeating_command_block",
                "minecraft:chain_command_block",
                "minecraft:spawner",
                "minecraft:mob_spawner",
                "minecraft:bedrock"
        );
        config.technical.mobs = list(
                "minecraft:player",
                "minecraft:armor_stand",
                "minecraft:end_crystal",
                "minecraft:bat"
        );

        config.sanitize();
        return config;
    }

    private static <T> List<T> list(T... values) {
        List<T> result = new ArrayList<>();
        if (values != null) {
            for (T value : values) {
                if (value != null) {
                    result.add(value);
                }
            }
        }
        return result;
    }

    private static boolean matchesAny(Identifier id, List<String> patterns) {
        if (id == null || patterns == null || patterns.isEmpty()) {
            return false;
        }
        String full = id.toString();
        String path = id.getPath();
        for (String pattern : patterns) {
            if (pattern == null || pattern.isBlank()) {
                continue;
            }
            String needle = pattern.trim();
            if (needle.startsWith("#")) {
                needle = needle.substring(1);
            }
            if (full.equals(needle) || path.equals(needle) || full.contains(needle) || path.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesExact(Identifier id, List<String> ids) {
        if (id == null || ids == null || ids.isEmpty()) {
            return false;
        }
        String full = id.toString();
        String path = id.getPath();
        for (String text : ids) {
            if (text == null || text.isBlank()) {
                continue;
            }
            String needle = text.trim();
            if (full.equals(needle) || path.equals(needle)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeTier(String tierName) {
        if (tierName == null || tierName.isBlank()) {
            return "ORDINARY";
        }
        return switch (tierName.trim().toUpperCase(Locale.ROOT)) {
            case "HARD" -> "HARD";
            case "INSANE" -> "INSANE";
            default -> "ORDINARY";
        };
    }

    private static CategoriesConfig.TierRule ruleFor(CategoriesConfig.TieredCategory category, String tierName) {
        if (category == null) {
            return new CategoriesConfig.TierRule();
        }
        return switch (normalizeTier(tierName)) {
            case "HARD" -> category.hard;
            case "INSANE" -> category.insane;
            default -> category.ordinary;
        };
    }

    private static boolean matchesRule(Identifier id, CategoriesConfig.TierRule rule) {
        if (id == null || rule == null) {
            return false;
        }
        return matchesExact(id, rule.exactIds) || matchesAny(id, rule.containsAny);
    }

    private List<Item> itemsForTier(CategoriesConfig.TieredCategory category, String tierName, java.util.function.Predicate<Item> predicate) {
        CategoriesConfig.TierRule rule = ruleFor(category, tierName);
        return Registries.ITEM.stream()
                .filter(predicate)
                .filter(item -> matchesRule(Registries.ITEM.getId(item), rule))
                .toList();
    }

    private List<Block> blocksForTier(CategoriesConfig.TieredCategory category, String tierName) {
        CategoriesConfig.TierRule rule = ruleFor(category, tierName);
        return Registries.BLOCK.stream()
                .filter(this::isSafeBlock)
                .filter(block -> matchesRule(Registries.BLOCK.getId(block), rule))
                .toList();
    }

    private List<EntityType<?>> mobsForTier(CategoriesConfig.TieredCategory category, String tierName) {
        CategoriesConfig.TierRule rule = ruleFor(category, tierName);
        return Registries.ENTITY_TYPE.stream()
                .filter(this::isGoodMob)
                .filter(type -> matchesRule(Registries.ENTITY_TYPE.getId(type), rule))
                .toList();
    }

    private List<StatusEffect> effectsForTier(CategoriesConfig.TieredCategory category, String tierName) {
        CategoriesConfig.TierRule rule = ruleFor(category, tierName);
        return Registries.STATUS_EFFECT.stream()
                .filter(effect -> matchesRule(Registries.STATUS_EFFECT.getId(effect), rule))
                .toList();
    }

    public static final class CategoriesConfig {
        public Technical technical = new Technical();
        public TieredCategory items = new TieredCategory();
        public TieredCategory food = new TieredCategory();
        public TieredCategory tools = new TieredCategory();
        public TieredCategory blocks = new TieredCategory();
        public MobCategory mobs = new MobCategory();
        public TieredCategory effects = new TieredCategory();
        public Map<String, TaskPool> taskPools = new LinkedHashMap<>();
        public Map<String, List<String>> biomeGroups = new LinkedHashMap<>();
        public List<String> villagerProfessions = new ArrayList<>();
        public Map<String, List<String>> effectImmunities = new LinkedHashMap<>();

        public void sanitize() {
            if (technical == null) technical = new Technical();
            if (items == null) items = new TieredCategory();
            if (food == null) food = new TieredCategory();
            if (tools == null) tools = new TieredCategory();
            if (blocks == null) blocks = new TieredCategory();
            if (mobs == null) mobs = new MobCategory();
            if (effects == null) effects = new TieredCategory();
            if (taskPools == null) taskPools = new LinkedHashMap<>();
            if (biomeGroups == null) biomeGroups = new LinkedHashMap<>();
            if (villagerProfessions == null) villagerProfessions = new ArrayList<>();
            if (effectImmunities == null) effectImmunities = new LinkedHashMap<>();

            taskPools.values().removeIf(java.util.Objects::isNull);
            taskPools.values().forEach(TaskPool::sanitize);
            biomeGroups.values().removeIf(java.util.Objects::isNull);
            effectImmunities.values().removeIf(java.util.Objects::isNull);

            technical.sanitize();
            items.sanitize();
            food.sanitize();
            tools.sanitize();
            blocks.sanitize();
            mobs.sanitize();
            effects.sanitize();
        }

        public static final class TaskPool {
            public List<String> ordinary = new ArrayList<>();
            public List<String> hard = new ArrayList<>();
            public List<String> insane = new ArrayList<>();

            public void sanitize() {
                if (ordinary == null) ordinary = new ArrayList<>();
                if (hard == null) hard = new ArrayList<>();
                if (insane == null) insane = new ArrayList<>();
            }

            public List<String> forTier(String tierName) {
                return switch (normalizeTier(tierName)) {
                    case "HARD" -> List.copyOf(hard);
                    case "INSANE" -> List.copyOf(insane);
                    default -> List.copyOf(ordinary);
                };
            }
        }

        public static final class Technical {
            public List<String> items = new ArrayList<>();
            public List<String> blocks = new ArrayList<>();
            public List<String> mobs = new ArrayList<>();

            public void sanitize() {
                if (items == null) items = new ArrayList<>();
                if (blocks == null) blocks = new ArrayList<>();
                if (mobs == null) mobs = new ArrayList<>();
            }
        }

        public static class TieredCategory {
            public TierRule ordinary = new TierRule();
            public TierRule hard = new TierRule();
            public TierRule insane = new TierRule();

            public void sanitize() {
                if (ordinary == null) ordinary = new TierRule();
                if (hard == null) hard = new TierRule();
                if (insane == null) insane = new TierRule();
                ordinary.sanitize();
                hard.sanitize();
                insane.sanitize();
            }
        }

        public static final class MobCategory extends TieredCategory {
            public List<String> leashable = new ArrayList<>();

            @Override
            public void sanitize() {
                super.sanitize();
                if (leashable == null) leashable = new ArrayList<>();
            }
        }

        public static final class TierRule {
            public List<String> exactIds = new ArrayList<>();
            public List<String> containsAny = new ArrayList<>();

            public void sanitize() {
                if (exactIds == null) exactIds = new ArrayList<>();
                if (containsAny == null) containsAny = new ArrayList<>();
            }
        }
    }
}
