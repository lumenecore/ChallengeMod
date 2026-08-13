package com.lumenechallenge.challenge.condition;

import com.lumenechallenge.client.layout.ModI18n;
import com.lumenechallenge.client.layout.ChallengeModSettings;

import com.lumenechallenge.challenge.ChallengeContext;
import com.lumenechallenge.challenge.TriggerSource;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffect;
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
import net.minecraft.text.MutableText;
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
        COUNT(1, false),
        SHIFT(2, false),
        EATING(3, true),
        SHIELD(4, true),
        BURNING(5, true),
        LOOKING_NORTH(6, true),
        LOOKING_UP_DOWN(7, true),
        AIMING(8, false),
        PUMPKIN(9, false),
        NEAR_MOB(10, true),
        TIME(11, false),
        IN_WATER(12, true),
        RIDE_PIG(13, true),
        RIDE_BOAT(14, true),
        EMPTY_HAND(15, false),
        ARMOR(16, false),
        EFFECT(17, false);

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
            if (id == 18) {
                return EFFECT;
            }
            return null;
        }
    }

    private final Type type;
    private final List<Modifier> modifiers;
    private final int requiredCount;
    private final NbtCompound data;
    private int progress;

    private double distanceAnchorX = Double.NaN;
    private double distanceAnchorZ = Double.NaN;
    private double distanceRemainder = 0.0D;
    private boolean distanceTracking = false;

    public ObjectiveCondition(Type type, Modifier modifier, int requiredCount, NbtCompound data) {
        this(type, modifier == null ? List.of() : List.of(modifier), requiredCount, data);
    }

    public ObjectiveCondition(Type type, List<Modifier> modifiers, int requiredCount, NbtCompound data) {
        this.type = type;
        this.modifiers = modifiers == null ? List.of() : List.copyOf(modifiers);
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

    public Modifier modifier() {
        return modifiers.isEmpty() ? null : modifiers.get(0);
    }

    public List<Modifier> modifiers() {
        return modifiers;
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
        for (Modifier modifier : modifiers) {
            if (!modifierSatisfied(context, modifier)) {
                return false;
            }
        }
        return true;
    }

    private boolean modifierSatisfied(ChallengeContext context, Modifier modifier) {
        return switch (modifier) {
            case COUNT -> true;
            case SHIFT -> context.player().isSneaking();
            case EATING -> context.player().isUsingItem() && context.player().getActiveItem().contains(DataComponentTypes.FOOD);
            case SHIELD -> context.player().isBlocking();
            case BURNING -> context.player().isOnFire();
            case LOOKING_NORTH -> directionOk(context);
            case LOOKING_UP_DOWN -> lookVerticalOk(context);
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
            case ARMOR -> armorOk(context);
            case EFFECT -> effectOk(context);
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
                    "minecraft:cherry_grove");
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
        MutableText base = switch (type) {
            case JUMP -> ModI18n.text("condition.lumenechallenge.objective.jump");
            case OBTAIN_ITEM -> ModI18n.text("condition.lumenechallenge.objective.obtain_item", directObjectText("itemName", RussianGrammar.GrammaticalCase.ACCUSATIVE));
            case BREAK_TOOL -> ModI18n.text("condition.lumenechallenge.objective.break_tool", directObjectText("itemName", RussianGrammar.GrammaticalCase.ACCUSATIVE));
            case ENDER_PEARL -> ModI18n.text("condition.lumenechallenge.objective.ender_pearl");
            case KILL_MOB -> ModI18n.text("condition.lumenechallenge.objective.kill_mob", directObjectText("entityName", RussianGrammar.GrammaticalCase.ACCUSATIVE));
            case STAND_ON_BLOCK -> ModI18n.text("condition.lumenechallenge.objective.stand_on_block", directObjectText("blockName", RussianGrammar.GrammaticalCase.ACCUSATIVE));
            case UNDERWATER -> ModI18n.text("condition.lumenechallenge.objective.underwater");
            case EAT_ITEM -> ModI18n.text("condition.lumenechallenge.objective.eat_item", directObjectText("itemName", RussianGrammar.GrammaticalCase.ACCUSATIVE));
            case EAT_ANY -> ModI18n.text("condition.lumenechallenge.objective.eat_any");
            case ENCHANT_ITEM -> ModI18n.text("condition.lumenechallenge.objective.enchant_item");
            case FISH -> ModI18n.text("condition.lumenechallenge.objective.fish");
            case SLEEP -> ModI18n.text("condition.lumenechallenge.objective.sleep");
            case SMELT_ITEM -> ModI18n.text("condition.lumenechallenge.objective.smelt_item", directObjectText("itemName", RussianGrammar.GrammaticalCase.ACCUSATIVE));
            case SHOOT_MOB_FROM_DISTANCE -> ModI18n.text("condition.lumenechallenge.objective.shoot_mob_distance", dataInt("distance", 0));
            case DEAL_DAMAGE -> ModI18n.text("condition.lumenechallenge.objective.deal_damage", (int) dataDouble("damage", 1.0D));
            case RENAME_ITEM -> ModI18n.text("condition.lumenechallenge.objective.rename_item", directObjectText("itemName", RussianGrammar.GrammaticalCase.ACCUSATIVE), phrase("name"));
            case TELEPORT_DIMENSION -> ModI18n.text("condition.lumenechallenge.objective.teleport_dimension", ModI18n.text("condition.lumenechallenge.dimension." + dataString("dimensionNameKey")));
            case REACH_Y_LEVEL -> ModI18n.text("condition.lumenechallenge.objective.reach_y", dataInt("y", 0));
            case TRADE -> ModI18n.text("condition.lumenechallenge.objective.trade");
            case FIND_STRUCTURE -> ModI18n.text("condition.lumenechallenge.objective.find_structure", ModI18n.text("condition.lumenechallenge.structure." + dataString("structureNameKey")));
            case VISIT_BIOME -> ModI18n.text(
                    "condition.lumenechallenge.objective.visit_biome",
                    biomeNameText(dataString("biomeNameKey"))
            );
            case WALK_DISTANCE -> ModI18n.text("condition.lumenechallenge.objective.walk_distance", dataInt("distance", 0));
            case BOAT_DISTANCE -> ModI18n.text("condition.lumenechallenge.objective.boat_distance", dataInt("distance", 0));
            case TAME_MOB -> ModI18n.text("condition.lumenechallenge.objective.tame_mob", directObjectText("entityName", RussianGrammar.GrammaticalCase.ACCUSATIVE));
            case EMPTY_INVENTORY -> ModI18n.text("condition.lumenechallenge.objective.empty_inventory");
            case FULL_INVENTORY -> ModI18n.text("condition.lumenechallenge.objective.full_inventory");
            case CATCH_SELF_ON_ROD -> ModI18n.text("condition.lumenechallenge.objective.catch_self");
            case LEASH_MOB -> ModI18n.text("condition.lumenechallenge.objective.leash_mob", directObjectText("entityName", RussianGrammar.GrammaticalCase.ACCUSATIVE));
            case VISIT_COORDS -> ModI18n.text("condition.lumenechallenge.objective.visit_coords", dataInt("x", 0), dataInt("z", 0));
        };

        appendModifierTexts(base);

        if (requiresCountSuffix() && requiredCount > 1) {
            base.append(Text.literal(" "));
            base.append(countSuffixText());
        }

        if (shouldShowProgress()) {
            base.append(Text.literal(" (" + Math.max(0, progress) + "/" + progressTarget() + ")"));
        }

        return base;
    }

    private void appendModifierTexts(MutableText base) {
        boolean first = true;
        for (Modifier modifier : modifiers) {
            if (modifier == null || modifier == Modifier.COUNT) {
                continue;
            }
            base.append(modifier.comma() ? Text.literal(", ") : Text.literal(" "));
            base.append(modifierText(modifier));
            first = false;
        }
    }

    private Text countSuffixText() {
        return switch (requiredCount) {
            case 2 -> ModI18n.text("condition.lumenechallenge.count.twice");
            case 3 -> ModI18n.text("condition.lumenechallenge.count.three");
            default -> ModI18n.text("condition.lumenechallenge.count.times", requiredCount);
        };
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

    private Text modifierText(Modifier modifier) {
        return switch (modifier) {
            case COUNT -> Text.empty();
            case SHIFT -> ModI18n.text("condition.lumenechallenge.modifier.shift");
            case EATING -> ModI18n.text("condition.lumenechallenge.modifier.eating");
            case SHIELD -> ModI18n.text("condition.lumenechallenge.modifier.shield");
            case BURNING -> ModI18n.text("condition.lumenechallenge.modifier.burning");
            case LOOKING_NORTH -> ModI18n.text("condition.lumenechallenge.modifier.direction." + directionKey());
            case LOOKING_UP_DOWN -> ModI18n.text("condition.lumenechallenge.modifier.vertical." + ("down".equalsIgnoreCase(dataString("lookVertical")) ? "down" : "up"));
            case AIMING -> ModI18n.text("condition.lumenechallenge.modifier.aiming");
            case PUMPKIN -> ModI18n.text("condition.lumenechallenge.modifier.pumpkin");
            case NEAR_MOB -> {
                String rawNear = dataString("nearEntityNameRaw");
                String entityId = dataString("nearEntity");
                String translationKey = entityTranslationKey(entityId);
                String localNear = translationKey.isBlank()
                        ? rawNear
                        : ModI18n.minecraftText(ChallengeModSettings.getModLanguage(), translationKey);
                if (localNear.isBlank() || localNear.equals(translationKey)) {
                    String legacy = dataString("nearEntityName");
                    yield ModI18n.text("condition.lumenechallenge.modifier.near_mob_fallback", legacy.isBlank() ? "mob" : legacy);
                }
                if ("ru_ru".equals(ChallengeModSettings.getModLanguage())) {
                    yield ModI18n.text("condition.lumenechallenge.modifier.near_mob_ru",
                            Text.literal(RussianGrammar.withPreposition(localNear)));
                }
                yield ModI18n.text("condition.lumenechallenge.modifier.near_mob_en", Text.literal(localNear.toLowerCase(Locale.ROOT)));
            }
            case TIME -> ModI18n.text("condition.lumenechallenge.modifier.time." + ("night".equalsIgnoreCase(dataString("time")) ? "night" : "day"));
            case IN_WATER -> ModI18n.text("condition.lumenechallenge.modifier.in_water");
            case RIDE_PIG -> ModI18n.text("condition.lumenechallenge.modifier.ride_pig");
            case RIDE_BOAT -> ModI18n.text("condition.lumenechallenge.modifier.ride_boat");
            case EMPTY_HAND -> ModI18n.text("condition.lumenechallenge.modifier.empty_hand");
            case ARMOR -> ModI18n.text("condition.lumenechallenge.modifier.armor." + ("full".equalsIgnoreCase(dataString("armor")) ? "full" : "none"));
            case EFFECT -> {
                String effectId = dataString("effect");
                String translationKey = statusEffectTranslationKey(effectId);
                String effect = translationKey.isBlank()
                        ? phrase("effectName")
                        : ModI18n.minecraftText(ChallengeModSettings.getModLanguage(), translationKey);
                if ("ru_ru".equals(ChallengeModSettings.getModLanguage()) && !effect.isBlank()) {
                    effect = RussianGrammar.effect(effect);
                }
                yield effect.isBlank()
                        ? ModI18n.text("condition.lumenechallenge.modifier.effect_empty")
                        : ModI18n.text("condition.lumenechallenge.modifier.effect", Text.literal(effect));
            }
        };
    }

    private String directionKey() {
        return switch (dataString("direction")) {
            case "south" -> "south";
            case "west" -> "west";
            case "east" -> "east";
            default -> "north";
        };
    }

    private boolean isRussianText(String value) {
        return value != null && value.codePoints().anyMatch(cp -> cp >= 0x0400 && cp <= 0x04FF);
    }

    private boolean directionOk(ChallengeContext context) {
        String mode = dataString("direction");
        return switch (mode) {
            case "south" -> context.player().getHorizontalFacing() == Direction.SOUTH;
            case "west" -> context.player().getHorizontalFacing() == Direction.WEST;
            case "east" -> context.player().getHorizontalFacing() == Direction.EAST;
            default -> context.player().getHorizontalFacing() == Direction.NORTH;
        };
    }

    private boolean lookVerticalOk(ChallengeContext context) {
        String mode = dataString("lookVertical");
        return "down".equalsIgnoreCase(mode)
                ? context.player().getPitch() > 35.0f
                : context.player().getPitch() < -35.0f;
    }

    private String directionText() {
        String text = dataString("directionText");
        if (!text.isBlank()) {
            return text;
        }
        return switch (dataString("direction")) {
            case "south" -> "смотря на юг";
            case "west" -> "смотря на запад";
            case "east" -> "смотря на восток";
            default -> "смотря на север";
        };
    }

    private String lookVerticalText() {
        String text = dataString("lookVerticalText");
        if (!text.isBlank()) {
            return text;
        }
        return "down".equalsIgnoreCase(dataString("lookVertical")) ? "смотря вниз" : "смотря вверх";
    }

    private String phrase(String key) {
        String raw = dataString(key + "Raw");
        if (!raw.isBlank()) {
            return raw;
        }
        return dataString(key);
    }

    private Text directObjectText(String key, RussianGrammar.GrammaticalCase grammaticalCase) {
        String raw = dataString(key + "Raw");
        if (raw.isBlank()) raw = dataString(key);

        String translationKey = "";
        if (key.startsWith("entity")) {
            translationKey = entityTranslationKey(dataString("entity"));
        } else if (key.startsWith("block")) {
            translationKey = blockTranslationKey(dataString("block"));
        } else {
            translationKey = itemTranslationKey(dataString("item"));
        }

        String language = ChallengeModSettings.getModLanguage();
        String localized = translationKey.isBlank()
                ? raw
                : ModI18n.minecraftText(language, translationKey);
        if (localized.isBlank() || localized.equals(translationKey)) {
            localized = raw;
        }
        if (localized.isBlank()) return Text.literal("");

        if ("ru_ru".equals(language)) {
            if (key.startsWith("entity")) {
                return Text.literal(RussianGrammar.mob(localized, grammaticalCase));
            }
            if (key.startsWith("block")) {
                return Text.literal(RussianGrammar.block(localized, grammaticalCase));
            }
            return Text.literal(RussianGrammar.item(localized, grammaticalCase));
        }

        return Text.literal(localized);
    }

    private String itemTranslationKey(String id) {
        if (id == null || id.isBlank()) return "";
        try {
            Item item = Registries.ITEM.get(Identifier.of(id));
            return item == null ? "" : item.getTranslationKey();
        } catch (Exception ignored) {
            return "";
        }
    }

    private String blockTranslationKey(String id) {
        if (id == null || id.isBlank()) return "";
        try {
            Block block = Registries.BLOCK.get(Identifier.of(id));
            return block == null ? "" : block.getTranslationKey();
        } catch (Exception ignored) {
            return "";
        }
    }

    private String entityTranslationKey(String id) {
        if (id == null || id.isBlank()) return "";
        try {
            EntityType<?> type = Registries.ENTITY_TYPE.get(Identifier.of(id));
            return type == null ? "" : type.getTranslationKey();
        } catch (Exception ignored) {
            return "";
        }
    }

    private String statusEffectTranslationKey(String id) {
        if (id == null || id.isBlank()) return "";
        try {
            StatusEffect effect = Registries.STATUS_EFFECT.get(Identifier.of(id));
            return effect == null ? "" : effect.getTranslationKey();
        } catch (Exception ignored) {
            return "";
        }
    }

    private Text biomeNameText(String biomeKey) {
        String language = ChallengeModSettings.getModLanguage();
        if ("ru_ru".equals(language)) {
            return ModI18n.text("condition.lumenechallenge.biome." + biomeKey);
        }

        String translationKey = "biome.minecraft." + biomeKey;
        String localized = ModI18n.minecraftText(language, translationKey);
        if (localized.isBlank() || localized.equals(translationKey)) {
            // Keep the existing mod translation as a safe fallback for old saves/resource packs.
            return ModI18n.text("condition.lumenechallenge.biome." + biomeKey);
        }
        return Text.literal(localized);
    }

    private Text objectText(String key) {
        String raw = dataString(key + "Raw");
        if (raw.isBlank()) raw = dataString(key);
        return Text.literal(raw);
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
        if (!modifiers.isEmpty()) {
            net.minecraft.nbt.NbtList modifierList = new net.minecraft.nbt.NbtList();
            for (Modifier modifier : modifiers) {
                modifierList.add(net.minecraft.nbt.NbtInt.of(modifier.id()));
            }
            tag.put("modifierIds", modifierList);
        }
        if (modifier() != null) tag.putInt("modifierId", modifier().id());
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
        List<Modifier> modifiers = new java.util.ArrayList<>();
        net.minecraft.nbt.NbtList modifierList = tag.getList("modifierIds").orElse(new net.minecraft.nbt.NbtList());
        for (int i = 0; i < modifierList.size(); i++) {
            Modifier modifier = Modifier.fromId(modifierList.getInt(i).orElse(-1));
            if (modifier != null) modifiers.add(modifier);
        }
        if (modifiers.isEmpty()) {
            Modifier legacy = Modifier.fromId(tag.getInt("modifierId", -1));
            if (legacy != null) modifiers.add(legacy);
        }
        ObjectiveCondition condition = new ObjectiveCondition(
                type,
                modifiers,
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
