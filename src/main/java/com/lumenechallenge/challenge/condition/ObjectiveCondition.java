package com.lumenechallenge.challenge.condition;

import com.lumenechallenge.challenge.ChallengeContext;
import com.lumenechallenge.challenge.TriggerSource;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.gen.structure.Structure;
import com.lumenechallenge.util.RussianGrammar;

import java.util.List;
import java.util.Locale;

public final class ObjectiveCondition extends AbstractCondition {
    public enum Type {
        JUMP,
        OBTAIN_ITEM,
        BREAK_TOOL,
        ENDER_PEARL,
        KILL_MOB,
        STAND_ON_BLOCK,
        UNDERWATER,
        EAT_ITEM,
        EAT_ANY,
        ENCHANT_ITEM,
        FISH,
        SLEEP,
        SMELT_ITEM,
        SHOOT_MOB_FROM_DISTANCE,
        DEAL_DAMAGE,
        RENAME_ITEM,
        TELEPORT_DIMENSION,
        REACH_Y_LEVEL,
        TRADE,
        FIND_STRUCTURE,
        VISIT_BIOME,
        WALK_DISTANCE,
        BOAT_DISTANCE,
        TAME_MOB,
        EMPTY_INVENTORY,
        FULL_INVENTORY,
        CATCH_SELF_ON_ROD,
        LEASH_MOB,
        VISIT_COORDS
    }

    public enum Modifier {
        SHIFT(2, false),
        EATING(3, true),
        SHIELD(4, true),
        BURNING(5, true),
        LOOKING_NORTH(6, true),
        LOOKING_UP(7, true),
        AIMING(8, false),
        PUMPKIN(9, false),
        NEAR_MOB(10, true),
        TIME(11, false),
        IN_WATER(12, true),
        RIDE_PIG(13, true),
        RIDE_BOAT(14, true),
        EMPTY_HAND(15, false),
        WEATHER(16, false),
        ARMOR(17, false),
        EFFECT(18, false);

        private final int id;
        private final boolean comma;

        Modifier(int id, boolean comma) {
            this.id = id;
            this.comma = comma;
        }

        public int id() {
            return id;
        }

        public boolean comma() {
            return comma;
        }

        public static Modifier fromId(int id) {
            for (Modifier m : values()) {
                if (m.id == id) return m;
            }
            return null;
        }
    }

    private final Type type;
    private final Modifier modifier;
    private final int requiredCount;
    private final NbtCompound data;
    private int progress;

    private double distanceAnchorX = Double.NaN;
    private double distanceAnchorZ = Double.NaN;
    private double distanceRemainder = 0.0D;
    private boolean distanceTracking = false;

    public ObjectiveCondition(Type type, Modifier modifier, int requiredCount, NbtCompound data) {
        this.type = type;
        this.modifier = modifier;
        this.requiredCount = Math.max(1, requiredCount);
        this.data = data == null ? new NbtCompound() : data.copy();
        this.distanceAnchorX = this.data.getDouble("__distanceAnchorX", Double.NaN);
        this.distanceAnchorZ = this.data.getDouble("__distanceAnchorZ", Double.NaN);
        this.distanceRemainder = this.data.getDouble("__distanceRemainder", 0.0D);
        this.distanceTracking = this.data.getBoolean("__distanceTracking", false);
    }

    public Type type() {
        return type;
    }

    @Override
    public boolean check(ChallengeContext context) {
        if (!modifierSatisfied(context)) {
            if (type == Type.WALK_DISTANCE || type == Type.BOAT_DISTANCE) {
                distanceTracking = false;
                distanceAnchorX = Double.NaN;
                distanceAnchorZ = Double.NaN;
            }
            return false;
        }

        if (isEventBased()) {
            if (eventHappened(context)) {
                progress = Math.min(requiredCount, progress + 1);
            }
            return progress >= requiredCount;
        }

        int current = currentProgress(context);
        if (current >= 0) {
            if (type == Type.OBTAIN_ITEM || type == Type.BREAK_TOOL || type == Type.SMELT_ITEM) {
                progress = Math.min(requiredCount, current);
            } else {
                progress = Math.min(requiredCount, Math.max(progress, current));
            }
        }

        return progress >= requiredCount;
    }

    private boolean isEventBased() {
        return switch (type) {
            case JUMP,
                 BREAK_TOOL,
                 ENDER_PEARL,
                 KILL_MOB,
                 EAT_ITEM,
                 EAT_ANY,
                 ENCHANT_ITEM,
                 FISH,
                 SLEEP,
                 TRADE,
                 DEAL_DAMAGE,
                 TAME_MOB,
                 CATCH_SELF_ON_ROD,
                 SHOOT_MOB_FROM_DISTANCE -> true;
            default -> false;
        };
    }

    private boolean eventHappened(ChallengeContext context) {
        return switch (type) {
            case JUMP -> context.jumpedThisTick();
            case ENDER_PEARL -> context.enderPearlTeleportedThisTick();
            case KILL_MOB -> {
                Entity killed = context.killedEntity();
                yield killed != null && matchesEntity(killed, dataString("entity"));
            }
            case EAT_ITEM -> {
                ItemStack consumed = context.consumedItem();
                yield consumed != null && matchesItem(consumed, dataString("item"));
            }
            case EAT_ANY -> context.consumedItem() != null;
            case ENCHANT_ITEM -> context.enchantedItem() != null;
            case BREAK_TOOL -> {
                ItemStack broken = context.brokenItem();
                yield broken != null && matchesItem(broken, dataString("item"));
            }
            case FISH -> context.fishedThisTick();
            case SLEEP -> context.sleptThisTick();
            case TRADE -> context.tradedThisTick();
            case DEAL_DAMAGE -> context.damagedThisTick() && context.damageAmountThisTick() > dataDouble("damage", 1.0D);
            case TAME_MOB -> {
                Entity tamed = context.tamedEntity();
                yield tamed != null && matchesEntity(tamed, dataString("entity"));
            }
            case CATCH_SELF_ON_ROD -> context.caughtSelfOnRodThisTick();
            case SHOOT_MOB_FROM_DISTANCE -> context.shotMobFromDistanceThisTick()
                    && context.shotMobDistance() >= dataInt("distance", 0);
            default -> false;
        };
    }

    private int currentProgress(ChallengeContext context) {
        return switch (type) {
            case OBTAIN_ITEM -> countItem(context, dataString("item"));
            case STAND_ON_BLOCK -> onBlock(context, dataString("block")) ? 1 : 0;
            case UNDERWATER -> context.player().isSubmergedInWater() ? 1 : 0;
            case SMELT_ITEM -> countItem(context, dataString("item"));
            case SHOOT_MOB_FROM_DISTANCE -> -1;
            case RENAME_ITEM -> renamedItem(context, dataString("item"), dataString("name")) ? 1 : 0;
            case TELEPORT_DIMENSION -> context.world().getRegistryKey().getValue().toString().equals(dataString("dimension")) ? 1 : 0;
            case REACH_Y_LEVEL -> context.position().getY() == dataInt("y", context.position().getY()) ? 1 : 0;
            case FIND_STRUCTURE -> foundStructure(context) ? 1 : 0;
            case VISIT_BIOME -> visitBiome(context, dataString("biome")) ? 1 : 0;
            case WALK_DISTANCE -> distanceProgress(context, false);
            case BOAT_DISTANCE -> distanceProgress(context, true);
            case EMPTY_INVENTORY -> mainInventoryEmpty(context) ? 1 : 0;
            case FULL_INVENTORY -> fullMainInventory(context) ? 1 : 0;
            case LEASH_MOB -> leashedMob(context, dataString("entity")) ? 1 : 0;
            case VISIT_COORDS -> visitCoords(context) ? 1 : 0;
            default -> -1;
        };
    }

    private int brokenToolCount(ChallengeContext context, String itemId) {
        if (itemId.isBlank()) return 0;

        Item item = Registries.ITEM.get(Identifier.of(itemId));
        if (item == null) return 0;

        int broken = 0;
        for (int i = 0; i < context.inventory().size(); i++) {
            ItemStack stack = context.inventory().getStack(i);
            if (stack.isOf(item) && stack.isDamaged() && stack.getDamage() >= stack.getMaxDamage() - 1) {
                broken++;
            }
        }
        return broken;
    }

    private boolean modifierSatisfied(ChallengeContext context) {
        if (modifier == null) return true;

        return switch (modifier) {
            case SHIFT -> context.player().isSneaking();
            case EATING -> context.player().isUsingItem() && context.player().getActiveItem().contains(DataComponentTypes.FOOD);
            case SHIELD -> context.player().isBlocking();
            case BURNING -> context.player().isOnFire();
            case LOOKING_NORTH -> context.player().getHorizontalFacing() == Direction.NORTH;
            case LOOKING_UP -> context.player().getPitch() < -35.0f;
            case AIMING -> context.player().isUsingItem()
                    && (context.player().getActiveItem().isOf(Items.BOW)
                    || context.player().getActiveItem().isOf(Items.CROSSBOW)
                    || context.player().getActiveItem().isOf(Items.TRIDENT));
            case PUMPKIN -> isWearingPumpkin(context);
            case NEAR_MOB -> nearMob(context);
            case TIME -> timeOk(context);
            case IN_WATER -> context.player().isTouchingWater();
            case RIDE_PIG -> context.player().getVehicle() instanceof PigEntity;
            case RIDE_BOAT -> context.player().getVehicle() instanceof BoatEntity;
            case EMPTY_HAND -> context.player().getMainHandStack().isEmpty();
            case WEATHER -> weatherOk(context);
            case ARMOR -> armorOk(context);
            case EFFECT -> effectOk(context);
        };
    }

    private boolean weatherOk(ChallengeContext context) {
        String mode = dataString("weather");
        return switch (mode) {
            case "rain" -> context.world().isRaining() && !context.world().isThundering();
            case "thunder" -> context.world().isThundering();
            default -> !context.world().isRaining();
        };
    }

    private boolean timeOk(ChallengeContext context) {
        String mode = dataString("time");
        long t = context.world().getTimeOfDay() % 24000L;
        return "night".equalsIgnoreCase(mode) ? t >= 12000L : t < 12000L;
        
    }

    private boolean isWearingPumpkin(ChallengeContext context) {
        ItemStack head = context.player().getEquippedStack(EquipmentSlot.HEAD);
        return head.isOf(Items.PUMPKIN) || head.isOf(Items.CARVED_PUMPKIN);
    }

    private boolean nearMob(ChallengeContext context) {
        String entityId = dataString("nearEntity");
        if (entityId.isBlank()) return false;

        EntityType<?> type = Registries.ENTITY_TYPE.get(Identifier.of(entityId));
        if (type == null) return false;

        Box box = context.player().getBoundingBox().expand(10.0D);
        return !context.world().getOtherEntities(context.player(), box, entity -> entity.getType() == type).isEmpty();
    }

    private boolean armorOk(ChallengeContext context) {
        String mode = dataString("armor");
        boolean full =
                !context.player().getEquippedStack(EquipmentSlot.HEAD).isEmpty()
                        && !context.player().getEquippedStack(EquipmentSlot.CHEST).isEmpty()
                        && !context.player().getEquippedStack(EquipmentSlot.LEGS).isEmpty()
                        && !context.player().getEquippedStack(EquipmentSlot.FEET).isEmpty();

        boolean none =
                context.player().getEquippedStack(EquipmentSlot.HEAD).isEmpty()
                        && context.player().getEquippedStack(EquipmentSlot.CHEST).isEmpty()
                        && context.player().getEquippedStack(EquipmentSlot.LEGS).isEmpty()
                        && context.player().getEquippedStack(EquipmentSlot.FEET).isEmpty();

        return "none".equals(mode) ? none : full;
    }

    private boolean effectOk(ChallengeContext context) {
        String effectId = dataString("effect");
        if (effectId.isBlank()) return false;

        for (var entry : context.player().getActiveStatusEffects().entrySet()) {
            if (Registries.STATUS_EFFECT.getId(entry.getKey().value()).toString().equals(effectId)) {
                return true;
            }
        }
        return false;
    }

    private boolean visitBiome(ChallengeContext context, String wantedBiome) {
        if (wantedBiome.isBlank()) return false;

        String biomeId = context.biomeEntry()
                .getKey()
                .map(key -> key.getValue().toString())
                .orElse("");

        return switch (wantedBiome) {
            case "ocean" -> matchesAnyBiome(biomeId,
                    "minecraft:ocean",
                    "minecraft:deep_ocean",
                    "minecraft:warm_ocean",
                    "minecraft:lukewarm_ocean",
                    "minecraft:deep_lukewarm_ocean",
                    "minecraft:cold_ocean",
                    "minecraft:deep_cold_ocean",
                    "minecraft:frozen_ocean",
                    "minecraft:deep_frozen_ocean");
            case "desert" -> matchesAnyBiome(biomeId, "minecraft:desert");
            case "forest" -> matchesAnyBiome(biomeId,
                    "minecraft:forest",
                    "minecraft:flower_forest",
                    "minecraft:birch_forest",
                    "minecraft:old_growth_birch_forest",
                    "minecraft:dark_forest",
                    "minecraft:windswept_forest",
                    "minecraft:taiga",
                    "minecraft:old_growth_pine_taiga",
                    "minecraft:old_growth_spruce_taiga",
                    "minecraft:snowy_taiga",
                    "minecraft:jungle",
                    "minecraft:sparse_jungle",
                    "minecraft:bamboo_jungle",
                    "minecraft:cherry_grove",
                    "minecraft:pale_garden");
            case "mountains" -> matchesAnyBiome(biomeId,
                    "minecraft:windswept_hills",
                    "minecraft:windswept_gravelly_hills",
                    "minecraft:frozen_peaks",
                    "minecraft:jagged_peaks",
                    "minecraft:stony_peaks",
                    "minecraft:snowy_slopes",
                    "minecraft:grove",
                    "minecraft:meadow");
            case "swamp" -> matchesAnyBiome(biomeId, "minecraft:swamp", "minecraft:mangrove_swamp");
            case "river" -> matchesAnyBiome(biomeId, "minecraft:river", "minecraft:frozen_river");
            default -> biomeId.equals(wantedBiome);
        };
    }

    private boolean matchesAnyBiome(String biomeId, String... ids) {
        for (String id : ids) {
            if (biomeId.equals(id)) {
                return true;
            }
        }
        return false;
    }

    private boolean visitCoords(ChallengeContext context) {
        int x = dataInt("x", context.position().getX());
        int z = dataInt("z", context.position().getZ());
        return context.position().getX() == x && context.position().getZ() == z;
    }

    private boolean onBlock(ChallengeContext context, String id) {
        if (id.isBlank()) return false;
        BlockState at = context.world().getBlockState(context.position());
        BlockState below = context.world().getBlockState(context.position().down());
        Block block = Registries.BLOCK.get(Identifier.of(id));
        return block != null && (at.isOf(block) || below.isOf(block));
    }

    private int distanceProgress(ChallengeContext context, boolean boat) {
        double target = dataDouble("distance", 0.0D);
        if (target <= 0.0D) return -1;

        boolean active = boat ? context.player().getVehicle() instanceof BoatEntity : context.player().getVehicle() == null;
        if (!active) {
            distanceTracking = false;
            distanceAnchorX = Double.NaN;
            distanceAnchorZ = Double.NaN;
            return progress;
        }

        double x = context.position().getX();
        double z = context.position().getZ();

        if (!distanceTracking || Double.isNaN(distanceAnchorX) || Double.isNaN(distanceAnchorZ)) {
            distanceTracking = true;
            distanceAnchorX = x;
            distanceAnchorZ = z;
            return progress;
        }

        double delta = Math.hypot(x - distanceAnchorX, z - distanceAnchorZ);
        if (delta > 0.0D) {
            distanceRemainder += delta;
            distanceAnchorX = x;
            distanceAnchorZ = z;
            while (distanceRemainder >= 1.0D && progress < requiredCount) {
                progress++;
                distanceRemainder -= 1.0D;
            }
        }

        return progress;
    }

    private boolean mainInventoryEmpty(ChallengeContext context) {
        for (ItemStack stack : context.inventory().getMainStacks()) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    private boolean fullMainInventory(ChallengeContext context) {
        for (ItemStack stack : context.inventory().getMainStacks()) {
            if (stack.isEmpty()) return false;
        }
        return true;
    }

    private boolean leashedMob(ChallengeContext context, String entityId) {
        if (entityId.isBlank()) return false;

        EntityType<?> type = Registries.ENTITY_TYPE.get(Identifier.of(entityId));
        if (type == null) return false;

        Box box = context.player().getBoundingBox().expand(10.0D);
        return !context.world().getOtherEntities(
                context.player(),
                box,
                entity -> entity.getType() == type
                        && entity instanceof net.minecraft.entity.Leashable leashable
                        && leashable.isLeashed()
                        && leashable.getLeashHolder() == context.player()
        ).isEmpty();
    }

    private boolean shotFromDistance(ChallengeContext context) {
        Entity victim = context.killedEntity();
        if (!(victim instanceof LivingEntity living)) return false;

        double target = dataDouble("distance", 0.0D);
        return target > 0.0D && context.player().getPos().distanceTo(living.getPos()) >= target;
    }

    private int countItem(ChallengeContext context, String itemId) {
        if (itemId.isBlank()) return 0;

        Item item = Registries.ITEM.get(Identifier.of(itemId));
        if (item == null) return 0;

        int total = 0;
        for (int i = 0; i < context.inventory().size(); i++) {
            ItemStack stack = context.inventory().getStack(i);
            if (stack.isOf(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private boolean matchesItem(ItemStack stack, String itemId) {
        if (itemId.isBlank()) return false;
        Item item = Registries.ITEM.get(Identifier.of(itemId));
        return item != null && stack.isOf(item);
    }

    private boolean matchesEntity(Entity entity, String entityId) {
        if (entityId.isBlank()) return false;
        EntityType<?> type = Registries.ENTITY_TYPE.get(Identifier.of(entityId));
        return type != null && entity.getType() == type;
    }

    private boolean renamedItem(ChallengeContext context, String itemId, String name) {
        if (itemId.isBlank() || name.isBlank()) return false;

        Item item = Registries.ITEM.get(Identifier.of(itemId));
        if (item == null) return false;

        for (int i = 0; i < context.inventory().size(); i++) {
            ItemStack stack = context.inventory().getStack(i);
            if (stack.isOf(item) && stack.getCustomName() != null && stack.getName().getString().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private boolean foundStructure(ChallengeContext context) {
        if (!(context.world() instanceof ServerWorld serverWorld)) return false;

        String structure = dataString("structure");
        BlockPos pos = context.position();
        RegistryWrapper.Impl<Structure> structureRegistry = serverWorld.getRegistryManager().getOrThrow(RegistryKeys.STRUCTURE);

        return switch (structure) {
            case "temple" -> locateAnyStructure(serverWorld, structureRegistry, pos,
                    "minecraft:desert_pyramid",
                    "minecraft:jungle_pyramid",
                    "minecraft:igloo",
                    "minecraft:swamp_hut");
            case "outpost" -> locateAnyStructure(serverWorld, structureRegistry, pos, "minecraft:pillager_outpost");
            case "shipwreck" -> locateAnyStructure(serverWorld, structureRegistry, pos, "minecraft:shipwreck");
            case "fortress" -> locateAnyStructure(serverWorld, structureRegistry, pos, "minecraft:fortress");
            case "bastion" -> locateAnyStructure(serverWorld, structureRegistry, pos, "minecraft:bastion_remnant");
            case "village" -> context.world().isNearOccupiedPointOfInterest(pos);
            default -> false;
        };
    }

    private boolean locateAnyStructure(ServerWorld world, RegistryWrapper.Impl<Structure> structureRegistry, BlockPos pos, String... ids) {
        for (String id : ids) {
            RegistryKey<Structure> key = RegistryKey.of(RegistryKeys.STRUCTURE, Identifier.of(id));
            RegistryEntry<Structure> entry = structureRegistry.getOrThrow(key);
            RegistryEntryList<Structure> list = RegistryEntryList.of(List.of(entry));

            var located = world.getChunkManager().getChunkGenerator().locateStructure(world, list, pos, 64, false);
            if (located != null && located.getFirst().getSquaredDistance(pos) <= 128.0D * 128.0D) {
                return true;
            }
        }
        return false;
    }

    private String dataString(String key) {
        return data.getString(key).orElse("");
    }

    private int dataInt(String key, int defaultValue) {
        return data.getInt(key, defaultValue);
    }

    private double dataDouble(String key, double defaultValue) {
        return data.getDouble(key, defaultValue);
    }

    private String countSuffix() {
        if (requiredCount <= 1) return "";
        int mod100 = requiredCount % 100;
        int mod10 = requiredCount % 10;
        if (mod100 >= 11 && mod100 <= 14) return requiredCount + " раз";
        return requiredCount + switch (mod10) {
            case 2, 3, 4 -> " раза";
            default -> " раз";
        };
    }

    @Override
    public Text getDescription() {
        String base = switch (type) {
            case JUMP -> "Прыгните";
            case OBTAIN_ITEM -> "Добудьте " + itemPhrase("itemName", RussianGrammar.GrammaticalCase.ACCUSATIVE);
            case BREAK_TOOL -> "Сломайте " + itemPhrase("itemName", RussianGrammar.GrammaticalCase.ACCUSATIVE);
            case ENDER_PEARL -> "Телепортируйтесь с помощью эндер-жемчуга";
            case KILL_MOB -> "Убейте " + mobPhrase("entityName", RussianGrammar.GrammaticalCase.ACCUSATIVE);
            case STAND_ON_BLOCK -> "Встаньте на " + blockPhrase("blockName", RussianGrammar.GrammaticalCase.ACCUSATIVE);
            case UNDERWATER -> "Нырните под воду";
            case EAT_ITEM -> "Съешьте " + itemPhrase("itemName", RussianGrammar.GrammaticalCase.ACCUSATIVE);
            case EAT_ANY -> "Поешьте";
            case ENCHANT_ITEM -> "Зачаруйте любой предмет";
            case FISH -> "Порыбачьте";
            case SLEEP -> "Поспите";
            case SMELT_ITEM -> "Переплавьте " + itemPhrase("itemName", RussianGrammar.GrammaticalCase.ACCUSATIVE);
            case SHOOT_MOB_FROM_DISTANCE -> "Выстрелите в моба с " + dataInt("distance", 0) + " блоков";
            case DEAL_DAMAGE -> "Нанесите более " + (int) dataDouble("damage", 1.0D) + " урона мобу за один удар";
            case RENAME_ITEM -> "Переименуйте " + itemPhrase("itemName", RussianGrammar.GrammaticalCase.ACCUSATIVE) + " в \"" + phrase("name") + "\"";
            case TELEPORT_DIMENSION -> "Телепортируйтесь в " + phrase("dimensionName");
            case REACH_Y_LEVEL -> "Достигните высоты Y=" + dataInt("y", 0);
            case TRADE -> "Поторгуйте с жителем";
            case FIND_STRUCTURE -> "Найдите " + structurePhrase("structureName", RussianGrammar.GrammaticalCase.ACCUSATIVE);
            case VISIT_BIOME -> "Посетите " + biomePhrase("biomeName", RussianGrammar.GrammaticalCase.ACCUSATIVE);
            case WALK_DISTANCE -> "Пройдите " + dataInt("distance", 0) + " блоков";
            case BOAT_DISTANCE -> "Проплывите " + dataInt("distance", 0) + " блоков";
            case TAME_MOB -> "Приручите " + mobPhrase("entityName", RussianGrammar.GrammaticalCase.ACCUSATIVE);
            case EMPTY_INVENTORY -> "Опустошите инвентарь";
            case FULL_INVENTORY -> "Заполните инвентарь до максимума";
            case CATCH_SELF_ON_ROD -> "Поймайте самого себя на удочку";
            case LEASH_MOB -> "Привяжите на поводок " + mobPhrase("entityName", RussianGrammar.GrammaticalCase.ACCUSATIVE);
            case VISIT_COORDS -> "Посетите координаты X=" + dataInt("x", 0) + ", Z=" + dataInt("z", 0);
        };

        if (modifier != null) {
            base += modifier.comma() ? ", " : " ";
            base += modifierText();
        }

        if (requiresCountSuffix() && requiredCount > 1) {
            base += " " + countSuffix();
        }

        if (shouldShowProgress()) {
            base += " (" + Math.max(0, progress) + "/" + progressTarget() + ")";
        }

        return Text.literal(base);
    }

    private boolean requiresCountSuffix() {
        return switch (type) {
            case JUMP,
                 OBTAIN_ITEM,
                 KILL_MOB,
                 EAT_ITEM,
                 EAT_ANY,
                 ENCHANT_ITEM,
                 FISH,
                 SMELT_ITEM -> true;
            default -> false;
        };
    }

    private boolean shouldShowProgress() {
        if (progressTarget() <= 1) {
            return false;
        }
        return switch (type) {
            case JUMP,
                 OBTAIN_ITEM,
                 KILL_MOB,
                 EAT_ITEM,
                 EAT_ANY,
                 ENCHANT_ITEM,
                 FISH,
                 SMELT_ITEM,
                 WALK_DISTANCE,
                 BOAT_DISTANCE -> true;
            default -> false;
        };
    }

    private int progressTarget() {
        return switch (type) {
            case WALK_DISTANCE, BOAT_DISTANCE -> Math.max(1, dataInt("distance", requiredCount));
            case OBTAIN_ITEM, SMELT_ITEM -> Math.max(1, requiredCount);
            case ENDER_PEARL, KILL_MOB, EAT_ITEM, EAT_ANY, ENCHANT_ITEM, FISH, TRADE, DEAL_DAMAGE, TAME_MOB, CATCH_SELF_ON_ROD, JUMP -> Math.max(1, requiredCount);
            default -> Math.max(1, requiredCount);
        };
    }

    private String modifierText() {
        return switch (modifier) {
            case SHIFT -> "с зажатым Shift";
            case EATING -> "пока кушаете";
            case SHIELD -> "используя щит";
            case BURNING -> "пока горите";
            case LOOKING_NORTH -> "смотря на север";
            case LOOKING_UP -> "смотря вверх";
            case AIMING -> "во время прицеливания";
            case PUMPKIN -> "с тыквой на голове";
            case NEAR_MOB -> {
                String rawNear = dataString("nearEntityNameRaw");
                if (!rawNear.isBlank()) {
                    yield "находясь рядом " + RussianGrammar.withPreposition(rawNear);
                }
                String legacyNear = dataString("nearEntityName");
                yield legacyNear.isBlank() ? "находясь рядом с мобом" : "находясь рядом с " + legacyNear;
            }
            case TIME -> phrase("timeText");
            case IN_WATER -> "находясь в воде";
            case RIDE_PIG -> "сидя верхом на свинье";
            case RIDE_BOAT -> "сидя в лодке";
            case EMPTY_HAND -> "рукой";
            case WEATHER -> phrase("weatherText");
            case ARMOR -> phrase("armorText");
            case EFFECT -> {
                String effect = phrase("effectName");
                yield effect.isBlank() ? "под эффектом" : "под эффектом \"" + effect + "\"";
            }
        };
    }


    private String phrase(String key) {
        String raw = dataString(key + "Raw");
        if (!raw.isBlank()) {
            return raw;
        }
        return dataString(key);
    }

    private String itemPhrase(String key, RussianGrammar.GrammaticalCase grammaticalCase) {
        String raw = dataString(key + "Raw");
        if (!raw.isBlank()) {
            return RussianGrammar.item(raw, grammaticalCase);
        }
        return dataString(key);
    }

    private String mobPhrase(String key, RussianGrammar.GrammaticalCase grammaticalCase) {
        String raw = dataString(key + "Raw");
        if (!raw.isBlank()) {
            return RussianGrammar.mob(raw, grammaticalCase);
        }
        return dataString(key);
    }

    private String blockPhrase(String key, RussianGrammar.GrammaticalCase grammaticalCase) {
        String raw = dataString(key + "Raw");
        if (!raw.isBlank()) {
            return RussianGrammar.block(raw, grammaticalCase);
        }
        return dataString(key);
    }

    private String structurePhrase(String key, RussianGrammar.GrammaticalCase grammaticalCase) {
        String raw = dataString(key + "Raw");
        if (!raw.isBlank()) {
            return RussianGrammar.structure(raw, grammaticalCase);
        }
        return dataString(key);
    }

    private String biomePhrase(String key, RussianGrammar.GrammaticalCase grammaticalCase) {
        String raw = dataString(key + "Raw");
        if (!raw.isBlank()) {
            return RussianGrammar.biome(raw, grammaticalCase);
        }
        return dataString(key);
    }

    @Override
    public ConditionKind getKind() {
        return ConditionKind.OBJECTIVE;
    }

    @Override
    public TriggerSource trigger() {
        return TriggerSource.TICK;
    }

    @Override
    protected void writeFields(NbtCompound tag) {
        tag.putString("type", type.name());
        if (modifier != null) tag.putInt("modifierId", modifier.id());
        tag.putInt("requiredCount", requiredCount);
        tag.putInt("progress", progress);
        tag.put("data", data.copy());
        tag.putDouble("__distanceAnchorX", distanceAnchorX);
        tag.putDouble("__distanceAnchorZ", distanceAnchorZ);
        tag.putDouble("__distanceRemainder", distanceRemainder);
        tag.putBoolean("__distanceTracking", distanceTracking);
    }

    public static ObjectiveCondition fromNbt(NbtCompound tag) {
        Type type = Type.valueOf(tag.getString("type").orElse(Type.JUMP.name()));
        Modifier modifier = Modifier.fromId(tag.getInt("modifierId", -1));
        ObjectiveCondition condition = new ObjectiveCondition(
                type,
                modifier,
                tag.getInt("requiredCount", 1),
                tag.getCompound("data").orElse(new NbtCompound())
        );
        condition.progress = tag.getInt("progress", 0);
        condition.distanceAnchorX = tag.getDouble("__distanceAnchorX", Double.NaN);
        condition.distanceAnchorZ = tag.getDouble("__distanceAnchorZ", Double.NaN);
        condition.distanceRemainder = tag.getDouble("__distanceRemainder", 0.0D);
        condition.distanceTracking = tag.getBoolean("__distanceTracking", false);
        return condition;
    }
}
