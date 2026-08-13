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
import net.minecraft.registry.entry.RegistryEntry;
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

    public static Challenge generate(ServerPlayerEntity player, long seed, int count) {
        Random random = new Random(seed ^ 0x5DEECE66DL);
        List<Type> shuffled = new ArrayList<>(POOL);
        Collections.shuffle(shuffled, random);

        List<com.lumenechallenge.challenge.condition.Condition> conditions = new ArrayList<>(count);
        int index = 0;
        while (conditions.size() < count) {
            if (index >= shuffled.size()) {
                Collections.shuffle(shuffled, random);
                index = 0;
            }
            Type type = shuffled.get(index++);
            conditions.add(create(player, random, type));
        }

        return new Challenge(ChallengeType.DEATH, conditions, 0);
    }

    public static Challenge generate(long seed) {
        throw new UnsupportedOperationException("Use generate(player, seed, count)");
    }

    private static ObjectiveCondition create(ServerPlayerEntity player, Random random, Type type) {
        Modifier modifier = pickModifier(random, type);
        int count = pickCount(random, type);

        NbtCompound data = new NbtCompound();
        fillData(player, random, type, modifier, data, count);
        fillModifierData(player, random, modifier, data);
        int requiredCount = switch (type) {
            case BREAK_TOOL, SHOOT_MOB_FROM_DISTANCE, DEAL_DAMAGE -> 1;
            default -> count;
        };
        return new ObjectiveCondition(type, modifier, requiredCount, data);
    }

    private static int pickCount(Random random, Type type) {
        return switch (type) {
            case JUMP -> randomCount(random, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
                    11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
                    21, 22, 23, 24, 25, 26, 27, 28, 29, 30,
                    31, 32, 33, 34, 35, 36, 37, 38, 39, 40,
                    41, 42, 43, 44, 45, 46, 47, 48, 49, 50);
            case OBTAIN_ITEM, KILL_MOB, FISH, SMELT_ITEM ->
                    randomCount(random, 1, 2, 3, 4);
            case EAT_ANY, ENCHANT_ITEM ->
                    randomCount(random, 1, 2);
            case BREAK_TOOL -> 1;
            case SHOOT_MOB_FROM_DISTANCE -> randomCount(random, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 100);
            case DEAL_DAMAGE -> randomCount(random, 4, 6, 8, 10);
            case WALK_DISTANCE -> randomCount(random, 50, 150, 200, 250);
            case BOAT_DISTANCE -> randomCount(random, 100, 150, 200, 250, 300);
            default -> 1;
        };
    }

    private static int randomCount(Random random, int... values) {
        return values[random.nextInt(values.length)];
    }

    private static Modifier pickModifier(Random random, Type type) {
        List<Modifier> allowed = allowedModifiers(type);
        if (allowed.isEmpty()) {
            return null;
        }
        return random.nextInt(100) < 30 ? allowed.get(random.nextInt(allowed.size())) : null;
    }

    private static List<Modifier> allowedModifiers(Type type) {
        return switch (type) {
            case JUMP -> List.of(Modifier.SHIFT, Modifier.EATING, Modifier.SHIELD, Modifier.BURNING, Modifier.LOOKING_NORTH, Modifier.LOOKING_UP, Modifier.AIMING, Modifier.PUMPKIN, Modifier.NEAR_MOB);
            case OBTAIN_ITEM -> List.of(Modifier.TIME);
            case BREAK_TOOL -> List.of(Modifier.PUMPKIN, Modifier.IN_WATER);
            case ENDER_PEARL -> List.of(Modifier.TIME, Modifier.EFFECT);
            case KILL_MOB -> List.of(Modifier.IN_WATER, Modifier.EMPTY_HAND, Modifier.EFFECT);
            case STAND_ON_BLOCK -> List.of(Modifier.IN_WATER);
            case UNDERWATER -> List.of(Modifier.RIDE_PIG, Modifier.PUMPKIN, Modifier.TIME);
            case EAT_ITEM -> List.of();
            case EAT_ANY -> List.of(Modifier.NEAR_MOB, Modifier.SHIFT, Modifier.WEATHER, Modifier.TIME, Modifier.EFFECT);
            case ENCHANT_ITEM -> List.of();
            case FISH -> List.of(Modifier.RIDE_BOAT, Modifier.LOOKING_UP, Modifier.RIDE_PIG, Modifier.EFFECT);
            case SLEEP -> List.of(Modifier.BURNING);
            case SMELT_ITEM -> List.of(Modifier.RIDE_BOAT, Modifier.WEATHER);
            case SHOOT_MOB_FROM_DISTANCE -> List.of(Modifier.PUMPKIN, Modifier.WEATHER, Modifier.RIDE_PIG, Modifier.EFFECT);
            case DEAL_DAMAGE -> List.of(Modifier.SHIFT);
            case RENAME_ITEM -> List.of(Modifier.BURNING, Modifier.NEAR_MOB);
            case TELEPORT_DIMENSION -> List.of();
            case REACH_Y_LEVEL -> List.of(Modifier.WEATHER, Modifier.ARMOR);
            case TRADE -> List.of();
            case FIND_STRUCTURE -> List.of();
            case VISIT_BIOME -> List.of();
            case WALK_DISTANCE -> List.of(Modifier.AIMING, Modifier.SHIFT, Modifier.EATING, Modifier.EFFECT);
            case BOAT_DISTANCE -> List.of(Modifier.SHIELD, Modifier.EFFECT);
            case TAME_MOB -> List.of();
            case EMPTY_INVENTORY -> List.of();
            case FULL_INVENTORY -> List.of();
            case CATCH_SELF_ON_ROD -> List.of();
            case LEASH_MOB -> List.of();
            case VISIT_COORDS -> List.of();
        };
    }

    private static void fillData(ServerPlayerEntity player, Random random, Type type, Modifier modifier, NbtCompound data, int count) {
        BlockPos pos = player.getBlockPos();
        switch (type) {
            case OBTAIN_ITEM -> {
                pickObtain(player, random, data);
                data.putInt("count", count);
            }
            case BREAK_TOOL -> {
                pickBreakTool(player, random, data);
                data.putInt("count", count);
            }
            case KILL_MOB -> pickEntity(random, data);
            case SHOOT_MOB_FROM_DISTANCE -> {
                pickEntity(random, data);
                data.putInt("distance", count);
            }
            case TAME_MOB -> pickTameMob(random, data);
            case LEASH_MOB -> pickLeashMob(player.getServerWorld(), random, data);
            case STAND_ON_BLOCK -> pickBlock(random, data);
            case EAT_ITEM -> pickFood(random, data);
            case EAT_ANY -> {
                pickFood(random, data);
                data.putInt("count", count);
            }
            case ENCHANT_ITEM -> {
                pickEnchantable(random, data);
                data.putInt("count", count);
            }
            case SMELT_ITEM -> {
                pickSmelt(random, data);
                data.putInt("count", count);
            }
            case DEAL_DAMAGE -> data.putDouble("damage", count);
            case RENAME_ITEM -> pickRename(random, data);
            case TELEPORT_DIMENSION -> pickDimension(player, random, data);
            case REACH_Y_LEVEL -> data.putInt("y", clampY(player.getServerWorld(), pos.getY() + random.nextInt(81) - 40));
            case FIND_STRUCTURE -> pickStructure(random, data);
            case VISIT_BIOME -> pickBiome(random, data);
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

    private static int clampY(ServerWorld world, int y) {
        int minY = world.getBottomY() + 1;
        int maxY = world.getBottomY() + world.getHeight() - 1;
        return MathHelper.clamp(y, minY, maxY);
    }

    private static void pickObtain(ServerPlayerEntity player, Random random, NbtCompound data) {
        List<Item> items = Registries.ITEM.stream()
                .filter(ChallengeGenerator::isSurvivalItem)
                .filter(item -> !item.getDefaultStack().isEmpty())
                .filter(item -> !player.getInventory().contains(item.getDefaultStack()))
                .toList();

        Item item = items.isEmpty() ? Items.STONE : items.get(random.nextInt(items.size()));
        data.putString("item", Registries.ITEM.getId(item).toString());
        storePhrase(data, "itemName", item.getName().getString());
    }

    private static void pickBreakTool(ServerPlayerEntity player, Random random, NbtCompound data) {
        List<Item> items = Registries.ITEM.stream()
                .filter(item -> item.getDefaultStack().getMaxDamage() > 0)
                .filter(item -> item != Items.CARROT_ON_A_STICK && item != Items.WARPED_FUNGUS_ON_A_STICK)
                .toList();

        Item item = items.isEmpty() ? Items.STONE_AXE : items.get(random.nextInt(items.size()));
        data.putString("item", Registries.ITEM.getId(item).toString());
        storePhrase(data, "itemName", item.getName().getString());
    }

    private static void pickEntity(Random random, NbtCompound data) {
        List<EntityType<?>> types = Registries.ENTITY_TYPE.stream()
                .filter(ChallengeGenerator::isGoodMob)
                .toList();
        EntityType<?> entityType = types.isEmpty() ? EntityType.ZOMBIE : types.get(random.nextInt(types.size()));
        data.putString("entity", Registries.ENTITY_TYPE.getId(entityType).toString());
        storePhrase(data, "entityName", entityType.getName().getString());
    }

    private static void pickTameMob(Random random, NbtCompound data) {
        String[][] options = {
                {"minecraft:wolf", "собака"},
                {"minecraft:cat", "кошка"},
                {"minecraft:horse", "лошадь"},
                {"minecraft:donkey", "осёл"},
                {"minecraft:mule", "мул"},
                {"minecraft:parrot", "попугай"}
        };
        String[] chosen = options[random.nextInt(options.length)];
        data.putString("entity", chosen[0]);
        storePhrase(data, "entityName", chosen[1]);
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
        List<EntityType<?>> types = Registries.ENTITY_TYPE.stream()
                .filter(ChallengeGenerator::isGoodMob)
                .filter(type -> {
                    BlockPos pos = world.getSpawnPos();
                    Entity entity = type.create(world, created -> {}, pos, SpawnReason.SPAWN_ITEM_USE, false, false);
                    return entity instanceof Leashable leashable && leashable.canBeLeashed();
                })
                .toList();

        EntityType<?> type = types.isEmpty() ? EntityType.CAT : types.get(random.nextInt(types.size()));
        data.putString("entity", Registries.ENTITY_TYPE.getId(type).toString());
        storePhrase(data, "entityName", type.getName().getString());
    }

    private static void pickBlock(Random random, NbtCompound data) {
        List<Block> blocks = Registries.BLOCK.stream().filter(ChallengeGenerator::isSafeBlock).toList();
        Block block = blocks.isEmpty() ? Blocks.STONE : blocks.get(random.nextInt(blocks.size()));
        data.putString("block", Registries.BLOCK.getId(block).toString());
        storePhrase(data, "blockName", block.getName().getString());
    }

    private static void pickFood(Random random, NbtCompound data) {
        List<Item> items = Registries.ITEM.stream().filter(i -> i.getDefaultStack().contains(DataComponentTypes.FOOD)).toList();
        Item item = items.isEmpty() ? Items.BREAD : items.get(random.nextInt(items.size()));
        data.putString("item", Registries.ITEM.getId(item).toString());
        storePhrase(data, "itemName", item.getName().getString());
    }

    private static void pickEnchantable(Random random, NbtCompound data) {
        List<Item> items = Registries.ITEM.stream().filter(i -> i.getDefaultStack().isEnchantable()).toList();
        Item item = items.isEmpty() ? Items.IRON_SWORD : items.get(random.nextInt(items.size()));
        data.putString("item", Registries.ITEM.getId(item).toString());
        storePhrase(data, "itemName", item.getName().getString());
    }

    private static void pickSmelt(Random random, NbtCompound data) {
        String[][] options = {
                {"minecraft:copper_ingot", "медь"},
                {"minecraft:iron_ingot", "железо"},
                {"minecraft:gold_ingot", "золото"}
        };
        String[] chosen = options[random.nextInt(options.length)];
        data.putString("item", chosen[0]);
        storePhrase(data, "itemName", chosen[1]);
    }

    private static void pickRename(Random random, NbtCompound data) {
        List<Item> items = Registries.ITEM.stream().filter(ChallengeGenerator::isSurvivalItem).toList();
        Item item = items.isEmpty() ? Items.STICK : items.get(random.nextInt(items.size()));
        String[] names = {
                "Александр", "Дмитрий", "Максим", "Артём", "Иван", "Михаил", "Андрей", "Кирилл", "Никита", "Егор", "Илья", "Денис", "Роман", "Владислав", "Павел", "Сергей", "Алексей", "Константин", "Тимофей", "Степан", "Виктор", "Олег", "Евгений", "Вадим", "Юрий", "Глеб", "Борис", "Ярослав", "Захар", "Данил", "Лев", "Матвей", "Савелий", "Фёдор", "Пётр", "Руслан", "Арсений", "Виталий",
                "Игнат", "Святослав", "Антон", "Валерий", "Георгий", "Семён", "Эдуард", "Герман", "Ростислав", "Мирон", "Марк", "Всеволод", "Анна", "Мария", "София", "Алиса", "Екатерина", "Дарья", "Полина", "Виктория", "Анастасия", "Елизавета", "Ксения", "Вероника", "Валерия", "Арина", "Милана", "Ульяна", "Диана", "Василиса", "Кира", "Алёна", "Татьяна", "Юлия", "Ирина", "Ольга", "Светлана", "Наталья", "Любовь", "Надежда", "Галина", "Людмила", "Злата", "Ева", "Ангелина", "Варвара", "Карина", "Инна", "Нина", "Лилия", "Марина", "Тамара", "Клавдия", "Раиса", "Элина", "Алина", "Яна", "Евгения", "Жанна", "Снежана", "Таисия", "Агата"
        };
        String name = names[random.nextInt(names.length)];
        data.putString("item", Registries.ITEM.getId(item).toString());
        storePhrase(data, "itemName", item.getName().getString());
        data.putString("name", name);
    }

    private static void pickDimension(ServerPlayerEntity player, Random random, NbtCompound data) {
        String[] dims = {"minecraft:the_nether", "minecraft:the_end"};
        String current = player.getServerWorld().getRegistryKey().getValue().toString();
        List<String> list = new ArrayList<>(List.of(dims));
        list.remove(current);
        String dim = random.nextInt(100) < 60 && !list.isEmpty() ? list.get(random.nextInt(list.size())) : current;
        data.putString("dimension", dim);
        storePhrase(data, "dimensionName", switch (dim) {
            case "minecraft:the_nether" -> "ад";
            case "minecraft:the_end" -> "энд";
            default -> "верхний мир";
        });
    }

    private static void pickStructure(Random random, NbtCompound data) {
        String[] variants = {"village", "temple", "outpost", "shipwreck", "fortress", "bastion"};
        String s = variants[random.nextInt(variants.length)];
        data.putString("structure", s);
        storePhrase(data, "structureName", switch (s) {
            case "temple" -> "храм";
            case "outpost" -> "аванпост";
            case "shipwreck" -> "корабль";
            case "fortress" -> "крепость";
            case "bastion" -> "бастион";
            default -> "деревню";
        });
    }

    private static void fillModifierData(ServerPlayerEntity player, Random random, Modifier modifier, NbtCompound data) {
        if (modifier == null) return;
        switch (modifier) {
            case TIME -> pickTime(random, data);
            case WEATHER -> pickWeather(random, data);
            case ARMOR -> pickArmor(random, data);
            case EFFECT -> pickEffect(random, data);
            case NEAR_MOB -> pickNearMob(player.getServerWorld(), random, data);
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

    private static void pickWeather(Random random, NbtCompound data) {
        String[][] values = {
                {"clear", "в ясную погоду"},
                {"rain", "во время дождя"},
                {"thunder", "во время грозы"}
        };
        String[] chosen = values[random.nextInt(values.length)];
        data.putString("weather", chosen[0]);
        data.putString("weatherText", chosen[1]);
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

    private static void pickNearMob(ServerWorld world, Random random, NbtCompound data) {
        List<EntityType<?>> types = Registries.ENTITY_TYPE.stream()
                .filter(ChallengeGenerator::isGoodMob)
                .toList();

        EntityType<?> type = types.isEmpty()
                ? EntityType.ZOMBIE
                : types.get(random.nextInt(types.size()));

        data.putString("nearEntity", Registries.ENTITY_TYPE.getId(type).toString());
        storePhrase(data, "nearEntityName", type.getName().getString());
    }

    private static void pickBiome(Random random, NbtCompound data) {
        String[][] groups = {
                {"ocean", "океан"},
                {"desert", "пустыня"},
                {"forest", "лес"},
                {"mountains", "горы"},
                {"swamp", "болото"},
                {"river", "река"}
        };
        String[] chosen = groups[random.nextInt(groups.length)];
        data.putString("biome", chosen[0]);
        data.putString("biomeGroup", chosen[0]);
        storePhrase(data, "biomeName", chosen[1]);
    }

    private static void pickEffect(Random random, NbtCompound data) {
        List<StatusEffect> effects = Registries.STATUS_EFFECT.stream()
                .filter(effect -> effect != StatusEffects.SATURATION.value())
                .toList();

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
        String normalized = normalizePhrase(raw);
        data.putString(key, normalized);
        data.putString(key + "Raw", normalized);
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
        Identifier id = Registries.ITEM.getId(item);
        String path = id.getPath();
        return !path.contains("command_block")
                && !path.contains("barrier")
                && !path.contains("air")
                && !path.contains("structure_block")
                && !path.contains("debug")
                && !path.contains("jigsaw")
                && !path.contains("light")
                && !path.endsWith("_spawn_egg")
                && !path.endsWith("spawn_egg")
                && !path.contains("knowledge_book")
                && !path.contains("debug_stick");
    }

    private static boolean isGoodMob(EntityType<?> type) {
        return type.getSpawnGroup() != SpawnGroup.MISC
                && type != EntityType.PLAYER
                && type != EntityType.ARMOR_STAND
                && type != EntityType.BAT
                && type != EntityType.END_CRYSTAL
                && type != EntityType.ILLUSIONER
                && type != EntityType.SKELETON_HORSE
                && type != EntityType.ZOMBIE_HORSE;
    }

    private static boolean isSafeBlock(Block block) {
        Identifier id = Registries.BLOCK.getId(block);
        String path = id.getPath();
        return !path.contains("air")
                && !path.contains("bedrock")
                && !path.contains("spawner")
                && !path.contains("command")
                && !path.equals("barrier")
                && !path.equals("light")
                && !path.equals("jigsaw")
                && !path.equals("structure_block")
                && !path.equals("command_block")
                && !path.equals("repeating_command_block")
                && !path.equals("chain_command_block");
    }
}
