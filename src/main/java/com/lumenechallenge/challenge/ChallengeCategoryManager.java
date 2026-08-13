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
import java.util.List;
import java.util.Locale;

public final class ChallengeCategoryManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String RESOURCE_PATH = "/assets/lumenechallenge/challenge/categories.json";
    private static final ChallengeCategoryManager INSTANCE = createInstance();

    private final CategoriesConfig config;

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
        return itemsForTier(config.items, tierName, this::isSurvivalItem);
    }

    public List<Item> foodItems(String tierName) {
        return itemsForTier(config.food, tierName, item -> isSurvivalItem(item) && item.getDefaultStack().contains(DataComponentTypes.FOOD));
    }

    public List<Item> toolItems(String tierName) {
        return itemsForTier(config.tools, tierName, item ->
                isSurvivalItem(item)
                        && item.getDefaultStack().getMaxDamage() > 0
                        && !item.getDefaultStack().contains(DataComponentTypes.EQUIPPABLE));
    }

    public List<Block> blocks(String tierName) {
        return blocksForTier(config.blocks, tierName);
    }

    public List<EntityType<?>> mobs(String tierName) {
        return mobsForTier(config.mobs, tierName);
    }

    public List<EntityType<?>> leashableMobs() {
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
        return result;
    }

    public List<StatusEffect> effects(String tierName) {
        return effectsForTier(config.effects, tierName);
    }

    /** Items that can actually be selected by any challenge category. */
    public List<Item> allChallengeItems() {
        java.util.LinkedHashSet<Item> result = new java.util.LinkedHashSet<>();
        for (String tier : List.of("ORDINARY", "HARD", "INSANE")) {
            result.addAll(survivalItems(tier));
            result.addAll(foodItems(tier));
            result.addAll(toolItems(tier));
        }
        return List.copyOf(result);
    }

    /** Blocks that can actually be selected by the block challenge category. */
    public List<Block> allChallengeBlocks() {
        java.util.LinkedHashSet<Block> result = new java.util.LinkedHashSet<>();
        for (String tier : List.of("ORDINARY", "HARD", "INSANE")) {
            result.addAll(blocks(tier));
        }
        return List.copyOf(result);
    }

    /** Mobs that can actually be selected by configured mob challenges. */
    public List<EntityType<?>> allChallengeMobs() {
        java.util.LinkedHashSet<EntityType<?>> result = new java.util.LinkedHashSet<>();
        for (String tier : List.of("ORDINARY", "HARD", "INSANE")) {
            result.addAll(mobs(tier));
        }
        result.addAll(leashableMobs());
        return List.copyOf(result);
    }

    /** Effects that can actually be selected by configured effect modifiers. */
    public List<StatusEffect> allChallengeEffects() {
        java.util.LinkedHashSet<StatusEffect> result = new java.util.LinkedHashSet<>();
        for (String tier : List.of("ORDINARY", "HARD", "INSANE")) {
            result.addAll(effects(tier));
        }
        return List.copyOf(result);
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

        public void sanitize() {
            if (technical == null) technical = new Technical();
            if (items == null) items = new TieredCategory();
            if (food == null) food = new TieredCategory();
            if (tools == null) tools = new TieredCategory();
            if (blocks == null) blocks = new TieredCategory();
            if (mobs == null) mobs = new MobCategory();
            if (effects == null) effects = new TieredCategory();

            technical.sanitize();
            items.sanitize();
            food.sanitize();
            tools.sanitize();
            blocks.sanitize();
            mobs.sanitize();
            effects.sanitize();
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
