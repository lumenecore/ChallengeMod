package com.lumenechallenge.challenge.condition;

import com.lumenechallenge.compat.NbtCompat;

import com.lumenechallenge.client.layout.ModI18n;
import com.lumenechallenge.client.layout.ChallengeModSettings;

import com.lumenechallenge.challenge.ChallengeContext;
import com.lumenechallenge.challenge.ChallengeCategoryManager;
import com.lumenechallenge.challenge.TriggerSource;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.Identifier;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.gen.structure.Structure;
import com.lumenechallenge.util.RussianGrammar;

import java.util.List;
import java.util.HashSet;
import java.util.Set;
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
        VISIT_COORDS,
        DIE,
        RESTORE_PORTAL,
        FIND_DUNGEON_LOOT,
        GET_ADVANCEMENT,
        CREEPER_EXPLODE,
        MILK_COW,
        TILL_SOIL,
        APPLY_EFFECT_TO_MOB,
        BURN_MOB,
        SKELETON_HIT_MOB,
        KILL_MOB_WITH_EFFECT,
        GET_EFFECT,
        DIE_BY_CAUSE,
        ENCHANT_ITEM_WITH,
        RECEIVE_DAMAGE,
        VILLAGER_LEVEL_UP,
        PROJECTILE_HIT_TARGET,
        GROW_TREE,
        HARVEST_CROP,
        STORE_ITEM,
        PUT_MOB_IN_BOAT,
        RIDE_MOB;

        /** Stable IDs from the task spreadsheet. Never derive these from enum order. */
        public int id() {
            return switch (this) {
                case JUMP -> 1;
                case OBTAIN_ITEM -> 2;
                case BREAK_TOOL -> 3;
                case VISIT_COORDS -> 4;
                case ENDER_PEARL -> 5;
                case KILL_MOB -> 6;
                case STAND_ON_BLOCK -> 7;
                case UNDERWATER -> 8;
                case EAT_ITEM -> 9;
                case EAT_ANY -> 10;
                case ENCHANT_ITEM -> 11;
                case FISH -> 12;
                case SLEEP -> 13;
                case SMELT_ITEM -> 14;
                case SHOOT_MOB_FROM_DISTANCE -> 15;
                case DEAL_DAMAGE -> 16;
                case RENAME_ITEM -> 17;
                case TELEPORT_DIMENSION -> 18;
                case REACH_Y_LEVEL -> 19;
                case TRADE -> 20;
                case FIND_STRUCTURE -> 21;
                case VISIT_BIOME -> 22;
                case WALK_DISTANCE -> 23;
                case BOAT_DISTANCE -> 24;
                case TAME_MOB -> 25;
                case EMPTY_INVENTORY -> 26;
                case FULL_INVENTORY -> 27;
                case CATCH_SELF_ON_ROD -> 28;
                case LEASH_MOB -> 29;
                case DIE -> 30;
                case RESTORE_PORTAL -> 31;
                case FIND_DUNGEON_LOOT -> 32;
                case GET_ADVANCEMENT -> 33;
                case CREEPER_EXPLODE -> 34;
                case MILK_COW -> 35;
                case TILL_SOIL -> 36;
                case APPLY_EFFECT_TO_MOB -> 37;
                case BURN_MOB -> 38;
                case SKELETON_HIT_MOB -> 39;
                case KILL_MOB_WITH_EFFECT -> 40;
                case GET_EFFECT -> 41;
                case DIE_BY_CAUSE -> 42;
                case ENCHANT_ITEM_WITH -> 43;
                case RECEIVE_DAMAGE -> 44;
                case VILLAGER_LEVEL_UP -> 45;
                case PROJECTILE_HIT_TARGET -> 46;
                case GROW_TREE -> 47;
                case HARVEST_CROP -> 48;
                case STORE_ITEM -> 49;
                case PUT_MOB_IN_BOAT -> 50;
                case RIDE_MOB -> 51;
            };
        }

        public static Type fromId(int id) {
            for (Type type : values()) {
                if (type.id() == id) return type;
            }
            return null;
        }
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
        EFFECT(17, false),
        IN_DUNGEON(18, true),
        IN_DIMENSION(19, true),
        NO_MOVEMENT(20, true),
        IN_AIR(21, true),
        PERSPECTIVE(22, false),
        LOOK_AT_TARGET(23, true);

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

    private static final List<String> VILLAGE_STRUCTURE_IDS = List.of(
            "minecraft:village_plains", "minecraft:village_desert", "minecraft:village_savanna",
            "minecraft:village_snowy", "minecraft:village_taiga"
    );
    private static final List<String> TEMPLE_STRUCTURE_IDS = List.of(
            "minecraft:desert_pyramid", "minecraft:jungle_pyramid"
    );
    private static final List<String> OCEAN_RUIN_STRUCTURE_IDS = List.of(
            "minecraft:ocean_ruin_cold", "minecraft:ocean_ruin_warm"
    );

    private final Type type;
    private final List<Modifier> modifiers;
    private final int requiredCount;
    private final NbtCompound data;
    private int progress;

    private double distanceAnchorX = Double.NaN;
    private double distanceAnchorZ = Double.NaN;
    private double distanceRemainder = 0.0D;
    private boolean distanceTracking = false;

    // Structure checks are cached only for the exact block position, never an entire chunk.
    private transient ServerWorld structureCacheWorld;
    private transient long structureCachePos = Long.MIN_VALUE;
    private transient long nextStructureSearchTick = Long.MIN_VALUE;
    private transient boolean cachedStructureResult;

    // Advancement baseline is immutable between completions; do not rebuild a HashSet every tick.
    private transient Set<String> advancementBaselineCache;
    private transient Object advancementLoaderCache;
    private transient long nextAdvancementScanTick = Long.MIN_VALUE;

    public ObjectiveCondition(Type type, Modifier modifier, int requiredCount, NbtCompound data) {
        this(type, modifier == null ? List.of() : List.of(modifier), requiredCount, data);
    }

    public ObjectiveCondition(Type type, List<Modifier> modifiers, int requiredCount, NbtCompound data) {
        this.type = type;
        this.modifiers = modifiers == null ? List.of() : List.copyOf(modifiers);
        this.requiredCount = Math.max(1, requiredCount);
        this.data = data == null ? new NbtCompound() : data.copy();
        this.distanceAnchorX = NbtCompat.getDouble(this.data, "__distanceAnchorX", Double.NaN);
        this.distanceAnchorZ = NbtCompat.getDouble(this.data, "__distanceAnchorZ", Double.NaN);
        this.distanceRemainder = NbtCompat.getDouble(this.data, "__distanceRemainder", 0.0D);
        this.distanceTracking = NbtCompat.getBoolean(this.data, "__distanceTracking", false);
    }

    /** Called exactly when this generated task becomes the active task. */
    public void begin(ServerPlayerEntity player) {
        if (player == null) return;
        if (type == Type.GET_ADVANCEMENT) {
            NbtList baseline = new NbtList();
            Set<String> baselineIds = new HashSet<>();
            var server = player.getWorld().getServer();
            if (server != null) {
                var loader = server.getAdvancementLoader();
                for (AdvancementEntry advancement : loader.getAdvancements()) {
                    if (player.getAdvancementTracker().getProgress(advancement).isDone()) {
                        String id = advancement.id().toString();
                        baselineIds.add(id);
                        baseline.add(NbtString.of(id));
                    }
                }
                advancementLoaderCache = loader;
            }
            advancementBaselineCache = baselineIds;
            nextAdvancementScanTick = Long.MIN_VALUE;
            data.put("advancementBaseline", baseline);
            data.putBoolean("advancementBaselineInitialized", true);
        }
        if (!NbtCompat.getBoolean(data, "noMovementTracking", false)) {
            data.putDouble("noMovementLastX", player.getX());
            data.putDouble("noMovementLastY", player.getY());
            data.putDouble("noMovementLastZ", player.getZ());
            data.putBoolean("noMovementTracking", true);
        }
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

    public String stringData(String key) { return NbtCompat.getString(data, key, ""); }
    public int intData(String key, int fallback) { return NbtCompat.getInt(data, key, fallback); }
    public String assignedTierName() { return NbtCompat.getString(data, "__assignedTier", ""); }

    /**
     * Stable identity of a generated objective variant. Runtime progress and tracking
     * fields are deliberately excluded so an already-started objective remains the
     * same variant after ticks, saves, and reloads.
     */
    public String variantKey() {
        NbtCompound identity = new NbtCompound();
        identity.putString("type", type.name());
        identity.putInt("requiredCount", requiredCount);

        NbtList modifierIds = new NbtList();
        for (Modifier modifier : modifiers) {
            if (modifier != null) modifierIds.add(net.minecraft.nbt.NbtInt.of(modifier.id()));
        }
        identity.put("modifiers", modifierIds);

        NbtCompound variantData = data.copy();
        // Assignment and runtime tracking describe the slot/session, not the task variant.
        NbtCompat.remove(variantData, "__assignedTier");
        NbtCompat.remove(variantData, "__distanceAnchorX");
        NbtCompat.remove(variantData, "__distanceAnchorZ");
        NbtCompat.remove(variantData, "__distanceRemainder");
        NbtCompat.remove(variantData, "__distanceTracking");
        NbtCompat.remove(variantData, "advancementBaseline");
        NbtCompat.remove(variantData, "advancementBaselineInitialized");
        NbtCompat.remove(variantData, "noMovementLastX");
        NbtCompat.remove(variantData, "noMovementLastY");
        NbtCompat.remove(variantData, "noMovementLastZ");
        NbtCompat.remove(variantData, "noMovementTracking");
        identity.put("data", variantData);
        return identity.toString();
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
                int increment = type == Type.SMELT_ITEM && context.smeltedItem() != null
                        ? Math.max(1, context.smeltedItem().getCount())
                        : 1;
                progress = Math.min(requiredCount, progress + increment);
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
                 SMELT_ITEM,
                 SLEEP,
                 TRADE,
                 DEAL_DAMAGE,
                 TAME_MOB,
                 CATCH_SELF_ON_ROD,
                 SHOOT_MOB_FROM_DISTANCE,
                 DIE,
                 RESTORE_PORTAL,
                 FIND_DUNGEON_LOOT,
                 GET_ADVANCEMENT,
                 CREEPER_EXPLODE,
                 MILK_COW,
                 TILL_SOIL,
                 APPLY_EFFECT_TO_MOB, BURN_MOB, SKELETON_HIT_MOB, KILL_MOB_WITH_EFFECT,
                 GET_EFFECT, DIE_BY_CAUSE, ENCHANT_ITEM_WITH, RECEIVE_DAMAGE,
                 VILLAGER_LEVEL_UP, PROJECTILE_HIT_TARGET, GROW_TREE, HARVEST_CROP,
                 STORE_ITEM, PUT_MOB_IN_BOAT, RIDE_MOB, RENAME_ITEM -> true;
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
            case SMELT_ITEM -> {
                ItemStack smelted = context.smeltedItem();
                yield smelted != null && !smelted.isEmpty() && matchesItem(smelted, dataString("item"));
            }
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
            case DIE -> context.diedThisTick();
            case GET_EFFECT -> dataString("effect").equals(context.playerAppliedEffectId());
            case DIE_BY_CAUSE -> context.diedThisTick() && dataString("cause").equals(context.deathCauseId());
            case ENCHANT_ITEM_WITH -> context.enchantedItem() != null
                    && matchesEnchantTargetItem(context.enchantedItem())
                    && hasEnchantment(context, context.enchantedItem(), dataString("enchantment"));
            case RECEIVE_DAMAGE -> context.receivedDamageId() != null
                    && (!dataString("entity").isEmpty()
                        ? dataString("entity").equals(context.receivedDamageEntityId())
                        : dataString("damage").equals(context.receivedDamageId()));
            case RESTORE_PORTAL -> context.restoredRuinedPortalThisTick();
            case FIND_DUNGEON_LOOT -> naturalLootMatches(context);
            case GET_ADVANCEMENT -> hasNewlyCompletedAdvancement(context.player());
            case CREEPER_EXPLODE -> context.creeperExplodedByPlayerThisTick();
            case MILK_COW -> context.milkedCowThisTick();
            case TILL_SOIL -> context.tilledSoilThisTick();
            case APPLY_EFFECT_TO_MOB -> context.effectAppliedEntity() != null
                    && matchesEntity(context.effectAppliedEntity(), dataString("entity"))
                    && dataString("effect").equals(context.appliedEffectId());
            case BURN_MOB -> context.burnedEntity() != null
                    && matchesEntity(context.burnedEntity(), dataString("entity"));
            case SKELETON_HIT_MOB -> context.skeletonShotEntity() != null
                    && matchesEntity(context.skeletonShotEntity(), dataString("entity"));
            case KILL_MOB_WITH_EFFECT -> {
                LivingEntity killed = context.killedEntity();
                yield killed != null && matchesEntity(killed, dataString("entity"))
                        && hasStatusEffect(killed, dataString("effect"));
            }
            case VILLAGER_LEVEL_UP -> dataString("profession").equals(context.villagerProfessionId())
                    && context.villagerLevel() >= dataInt("level", 99);
            case PROJECTILE_HIT_TARGET -> dataString("projectile").equals(context.hitProjectileId())
                    && dataString("targetKind").equals(context.projectileTargetKind())
                    && dataString("target").equals(context.projectileTargetId());
            case GROW_TREE -> dataString("sapling").equals(context.grownSaplingId());
            case HARVEST_CROP -> dataString("crop").equals(context.harvestedCropId());
            case STORE_ITEM -> dataString("item").equals(context.storedItemId())
                    && dataString("storage").equals(context.storageId());
            case PUT_MOB_IN_BOAT -> dataString("entity").equals(context.mobInBoatId());
            case RIDE_MOB -> dataString("entity").equals(context.riddenMobId());
            default -> false;
        };
    }

    private boolean matchesEnchantTargetItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        String kind = dataString("itemKind");
        if (kind.isBlank()) {
            // Keep compatibility with challenges generated by older saved runs.
            return matchesItem(stack, dataString("item"));
        }
        String path = Registries.ITEM.getId(stack.getItem()).getPath();
        boolean armor = path.endsWith("_helmet") || path.endsWith("_chestplate")
                || path.endsWith("_leggings") || path.endsWith("_boots");
        boolean tool = path.endsWith("_pickaxe") || path.endsWith("_shovel")
                || (path.endsWith("_axe") && !path.endsWith("_pickaxe"))
                || path.endsWith("_hoe") || path.endsWith("_sword");
        return switch (kind) {
            case "pickaxe" -> path.endsWith("_pickaxe");
            case "sword" -> path.endsWith("_sword");
            case "axe" -> path.endsWith("_axe") && !path.endsWith("_pickaxe");
            case "shovel" -> path.endsWith("_shovel");
            case "hoe" -> path.endsWith("_hoe");
            case "bow" -> path.equals("bow");
            case "trident" -> path.equals("trident");
            case "armor" -> armor;
            case "rod" -> path.equals("fishing_rod");
            case "xbow" -> path.equals("crossbow");
            case "tool" -> tool;
            case "shears" -> path.equals("shears");
            default -> false;
        };
    }

    private static boolean hasEnchantment(ChallengeContext context, ItemStack stack, String enchantmentId) {
        if (stack == null || stack.isEmpty() || enchantmentId == null || enchantmentId.isEmpty()) return false;
        var enchantments = context.world().getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        for (RegistryEntry<net.minecraft.enchantment.Enchantment> entry : stack.getEnchantments().getEnchantments()) {
            if (enchantments.getId(entry.value()).toString().equals(enchantmentId)) return true;
        }
        return false;
    }

    private int currentProgress(ChallengeContext context) {
        return switch (type) {
            case OBTAIN_ITEM -> countItem(context, dataString("item"));
            case STAND_ON_BLOCK -> onBlock(context, dataString("block")) ? 1 : 0;
            case UNDERWATER -> context.player().isSubmergedInWater() ? 1 : 0;
            case SHOOT_MOB_FROM_DISTANCE -> -1;
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
            case IN_WATER -> (type == Type.EAT_ANY || type == Type.EAT_ITEM)
                    ? context.consumedItem() != null && context.consumedUnderwater()
                    : context.player().isTouchingWater();
            case RIDE_PIG -> context.player().getVehicle() instanceof PigEntity;
            case RIDE_BOAT -> context.player().getVehicle() instanceof BoatEntity;
            case EMPTY_HAND -> context.player().getMainHandStack().isEmpty();
            case ARMOR -> armorOk(context);
            case EFFECT -> effectOk(context);
            case IN_DUNGEON -> inSelectedDungeon(context);
            case IN_DIMENSION -> context.world().getRegistryKey().getValue().toString().equals(dataString("requiredDimension"));
            case NO_MOVEMENT -> isStationaryForThisCheck(context);
            case IN_AIR -> !context.player().isOnGround();
            case PERSPECTIVE -> context.cameraPerspective() == dataInt("perspective", 0);
            case LOOK_AT_TARGET -> lookingAtTarget(context);
        };
    }

    private boolean lookingAtTarget(ChallengeContext context) {
        String wanted = dataString("lookTarget");
        if (wanted.isBlank()) return false;
        var player = context.player();
        var start = player.getEyePos();
        var look = player.getRotationVec(1.0F);
        var end = start.add(look.multiply(8.0D));
        var blockHit = player.raycast(8.0D, 1.0F, false);
        if ("block".equals(dataString("lookTargetKind"))) {
            return blockHit.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK
                    && Registries.BLOCK.getId(context.world().getBlockState(((net.minecraft.util.hit.BlockHitResult) blockHit).getBlockPos()).getBlock()).toString().equals(wanted);
        }
        double nearest = blockHit.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK
                ? Math.sqrt(start.squaredDistanceTo(blockHit.getPos())) : 8.0D;
        Entity nearestEntity = null;
        var search = player.getBoundingBox().stretch(look.multiply(8.0D)).expand(1.0D);
        for (Entity entity : context.world().getOtherEntities(player, search, candidate -> candidate instanceof LivingEntity && candidate.isAlive())) {
            var intercept = entity.getBoundingBox().expand(0.3D).raycast(start, end);
            if (intercept.isEmpty()) continue;
            double distance = start.squaredDistanceTo(intercept.get());
            if (distance < nearest * nearest) {
                nearest = Math.sqrt(distance);
                nearestEntity = entity;
            }
        }
        return nearestEntity != null && Registries.ENTITY_TYPE.getId(nearestEntity.getType()).toString().equals(wanted);
    }

    private boolean naturalLootMatches(ChallengeContext context) {
        if (context.naturalLootTableIdThisTick() == null || context.naturalLootTableIdThisTick().isBlank()) return false;
        String wanted = dataString("lootTarget");
        if (wanted.isBlank()) return false;
        for (ItemStack stack : context.naturalLootItemsThisTick()) {
            if (stack == null || stack.isEmpty()) continue;
            if ("diamond_armor".equals(wanted)) {
                if (stack.isOf(Items.DIAMOND_HELMET) || stack.isOf(Items.DIAMOND_CHESTPLATE)
                        || stack.isOf(Items.DIAMOND_LEGGINGS) || stack.isOf(Items.DIAMOND_BOOTS)) return true;
            } else if ("smithing_template".equals(wanted)) {
                Identifier itemId = Registries.ITEM.getId(stack.getItem());
                if (itemId != null && itemId.getNamespace().equals("minecraft")
                        && itemId.getPath().endsWith("_smithing_template")) return true;
            } else {
                // The display/category key can be a bare path ("golden_apple").
                // Prefer the fully-qualified item ID written by the generator.
                String targetItemId = dataString("item");
                if (targetItemId.isBlank()) targetItemId = wanted;
                if (matchesItem(stack, targetItemId)) return true;
            }
        }
        return false;
    }

    private boolean hasNewlyCompletedAdvancement(ServerPlayerEntity player) {
        if (player == null || player.getWorld().getServer() == null
                || !NbtCompat.getBoolean(data, "advancementBaselineInitialized", false)) return false;

        var server = player.getWorld().getServer();
        long now = player.getWorld().getLevelProperties().getTime();
        if (nextAdvancementScanTick != Long.MIN_VALUE && now < nextAdvancementScanTick) {
            return false;
        }
        // An advancement is not time-critical at frame/tick precision. Scan at
        // most twice per second rather than walking the full advancement registry every tick.
        nextAdvancementScanTick = now + 10L;

        var loader = server.getAdvancementLoader();
        if (advancementBaselineCache == null || advancementLoaderCache != loader) {
            advancementBaselineCache = new HashSet<>();
            NbtList stored = NbtCompat.getList(data, "advancementBaseline", NbtElement.STRING_TYPE);
            for (int i = 0; i < stored.size(); i++) {
                advancementBaselineCache.add(NbtCompat.getString(stored, i));
            }
            advancementLoaderCache = loader;
        }

        // Treat advancement completion as a one-shot event. Once a newly earned
        // advancement is observed, immediately add it to both the cache and NBT.
        boolean newlyCompleted = false;
        NbtList baseline = NbtCompat.getList(data, "advancementBaseline", NbtElement.STRING_TYPE);
        for (AdvancementEntry advancement : loader.getAdvancements()) {
            String id = advancement.id().toString();
            if (!advancementBaselineCache.contains(id)
                    && player.getAdvancementTracker().getProgress(advancement).isDone()) {
                advancementBaselineCache.add(id);
                baseline.add(NbtString.of(id));
                newlyCompleted = true;
            }
        }
        if (newlyCompleted) {
            data.put("advancementBaseline", baseline);
        }
        return newlyCompleted;
    }

    private boolean isStationaryForThisCheck(ChallengeContext context) {
        double x = context.player().getX();
        double y = context.player().getY();
        double z = context.player().getZ();
        boolean tracking = NbtCompat.getBoolean(data, "noMovementTracking", false);
        double lastX = NbtCompat.getDouble(data, "noMovementLastX", x);
        double lastY = NbtCompat.getDouble(data, "noMovementLastY", y);
        double lastZ = NbtCompat.getDouble(data, "noMovementLastZ", z);
        data.putDouble("noMovementLastX", x);
        data.putDouble("noMovementLastY", y);
        data.putDouble("noMovementLastZ", z);
        data.putBoolean("noMovementTracking", true);
        if (!tracking) return true;
        return Double.compare(x, lastX) == 0 && Double.compare(y, lastY) == 0 && Double.compare(z, lastZ) == 0;
    }

    private boolean inSelectedDungeon(ChallengeContext context) {
        String selected = dataString("dungeon");
        List<String> ids = switch (selected) {
            case "village" -> List.of("minecraft:village_plains", "minecraft:village_desert", "minecraft:village_savanna", "minecraft:village_snowy", "minecraft:village_taiga");
            case "temple" -> List.of("minecraft:desert_pyramid", "minecraft:jungle_pyramid", "minecraft:igloo", "minecraft:swamp_hut");
            case "fortress" -> List.of("minecraft:fortress");
            case "mansion" -> List.of("minecraft:woodland_mansion");
            case "bastion" -> List.of("minecraft:bastion_remnant");
            default -> List.of();
        };
        for (String id : ids) {
            try {
                RegistryKey<Structure> key = RegistryKey.of(RegistryKeys.STRUCTURE, Identifier.of(id));
                RegistryWrapper.Impl<Structure> registry = context.world().getRegistryManager().getOrThrow(RegistryKeys.STRUCTURE);
                Structure structure = registry.getOrThrow(key).value();
                var start = context.world().getStructureAccessor().getStructureContaining(context.position(), structure);
                if (start != null && start.hasChildren()) return true;
            } catch (RuntimeException ignored) {
                // A particular structure may not be available in a modded or customized world.
            }
        }
        return false;
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
        if (wantedBiome == null || wantedBiome.isBlank()) return false;
        String biomeId = context.biomeEntry().getKey()
                .map(key -> key.getValue().toString()).orElse("");
        return ChallengeCategoryManager.get().biomeGroup(wantedBiome).contains(biomeId);
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
        for (ItemStack stack : context.inventory().main) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    private boolean fullMainInventory(ChallengeContext context) {
        for (ItemStack stack : context.inventory().main) {
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

    private boolean hasStatusEffect(LivingEntity entity, String effectId) {
        if (entity == null || effectId == null || effectId.isBlank()) return false;
        for (var entry : entity.getActiveStatusEffects().entrySet()) {
            if (Registries.STATUS_EFFECT.getId(entry.getKey().value()).toString().equals(effectId)) return true;
        }
        return false;
    }

    /** Called only after an anvil output with a newly assigned custom name is taken. */
    public boolean matchesRenamedItem(ChallengeContext context, ItemStack renamedStack) {
        return type == Type.RENAME_ITEM
                && context != null
                && renamedStack != null
                && !renamedStack.isEmpty()
                && modifierSatisfied(context)
                && matchesItem(renamedStack, dataString("item"));
    }

    private boolean foundStructure(ChallengeContext context) {
        if (!(context.world() instanceof ServerWorld serverWorld)) return false;

        BlockPos pos = context.position();
        long posKey = pos.asLong();
        long now = serverWorld.getLevelProperties().getTime();
        if (structureCacheWorld == serverWorld
                && structureCachePos == posKey
                && nextStructureSearchTick != Long.MIN_VALUE
                && now < nextStructureSearchTick) {
            return cachedStructureResult;
        }

        String selected = dataString("structure");
        String normalized = selected.contains(":") ? selected : "minecraft:" + selected;

        // These variants intentionally share one visible task name, so any member
        // of the displayed structure group must satisfy the objective.
        List<String> structureIds = switch (normalized) {
            case "minecraft:village" -> VILLAGE_STRUCTURE_IDS;
            case "minecraft:desert_pyramid", "minecraft:jungle_pyramid", "minecraft:temple" -> TEMPLE_STRUCTURE_IDS;
            case "minecraft:ocean_ruin_cold", "minecraft:ocean_ruin_warm", "minecraft:ocean_ruins" -> OCEAN_RUIN_STRUCTURE_IDS;
            default -> normalized.startsWith("minecraft:village_") ? VILLAGE_STRUCTURE_IDS : List.of(normalized);
        };

        // Check the structure at the player's actual position. locateStructure() only
        // returns a nearby locate point; a distance cutoff can reject someone already
        // inside a large structure such as a village.
        var structureAccessor = serverWorld.getStructureAccessor();
        var structureRegistry = serverWorld.getRegistryManager().getOrThrow(RegistryKeys.STRUCTURE);
        boolean result = false;
        for (String id : structureIds) {
            try {
                RegistryKey<Structure> key = RegistryKey.of(RegistryKeys.STRUCTURE, Identifier.of(id));
                Structure structure = structureRegistry.getOrThrow(key).value();
                var start = structureAccessor.getStructureAt(pos, structure);
                if (start != null && start.hasChildren()) {
                    result = true;
                    break;
                }
            } catch (RuntimeException ignored) {
                // Skip structures unavailable in customized or modded worlds.
            }
        }

        structureCacheWorld = serverWorld;
        structureCachePos = posKey;
        nextStructureSearchTick = now + 20L;
        cachedStructureResult = result;
        return result;
    }

    private String dataString(String key) {
        return NbtCompat.getString(data, key, "");
    }

    private int dataInt(String key, int defaultValue) {
        return NbtCompat.getInt(data, key, defaultValue);
    }

    private double dataDouble(String key, double defaultValue) {
        return NbtCompat.getDouble(data, key, defaultValue);
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
            case SMELT_ITEM -> ModI18n.text("condition.lumenechallenge.objective.smelt_item", smeltMaterialText());
            case SHOOT_MOB_FROM_DISTANCE -> ModI18n.text("condition.lumenechallenge.objective.shoot_mob_distance", dataInt("distance", 0));
            case DEAL_DAMAGE -> ModI18n.text("condition.lumenechallenge.objective.deal_damage", (int) dataDouble("damage", 1.0D));
            case RENAME_ITEM -> ModI18n.text("condition.lumenechallenge.objective.rename_item", directObjectText("itemName", RussianGrammar.GrammaticalCase.ACCUSATIVE));
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
            case DIE -> ModI18n.text("condition.lumenechallenge.objective.die");
            case GET_EFFECT -> ModI18n.text("condition.lumenechallenge.objective.get_effect", effectNameText());
            case DIE_BY_CAUSE -> ModI18n.text("condition.lumenechallenge.objective.die_by_cause", ModI18n.text("condition.lumenechallenge.death_cause." + dataString("cause")));
            case ENCHANT_ITEM_WITH -> ModI18n.text("condition.lumenechallenge.objective.enchant_item_with", directObjectText("itemName", RussianGrammar.GrammaticalCase.ACCUSATIVE), enchantmentNameText());
            case RECEIVE_DAMAGE -> dataString("entity").isBlank()
                    ? ModI18n.text("condition.lumenechallenge.objective.receive_damage", ModI18n.text("condition.lumenechallenge.damage_source." + dataString("damage")))
                    : ModI18n.text("condition.lumenechallenge.objective.receive_damage_from_mob", directObjectText("entityName", RussianGrammar.GrammaticalCase.NOMINATIVE));
            case RESTORE_PORTAL -> ModI18n.text("condition.lumenechallenge.objective.restore_portal");
            case FIND_DUNGEON_LOOT -> ModI18n.text("condition.lumenechallenge.objective.find_dungeon_loot", dungeonLootNameText());
            case GET_ADVANCEMENT -> ModI18n.text("condition.lumenechallenge.objective.get_advancement");
            case CREEPER_EXPLODE -> ModI18n.text("condition.lumenechallenge.objective.creeper_explode");
            case MILK_COW -> ModI18n.text("condition.lumenechallenge.objective.milk_cow");
            case TILL_SOIL -> ModI18n.text("condition.lumenechallenge.objective.till_soil");
            case APPLY_EFFECT_TO_MOB -> ModI18n.text("condition.lumenechallenge.objective.apply_effect_to_mob",
                    effectNameText(), directObjectText("entityName", RussianGrammar.GrammaticalCase.ACCUSATIVE));
            case BURN_MOB -> ModI18n.text("condition.lumenechallenge.objective.burn_mob",
                    directObjectText("entityName", RussianGrammar.GrammaticalCase.ACCUSATIVE));
            case SKELETON_HIT_MOB -> ModI18n.text("condition.lumenechallenge.objective.skeleton_hit_mob",
                    directObjectText("entityName", RussianGrammar.GrammaticalCase.ACCUSATIVE));
            case KILL_MOB_WITH_EFFECT -> ModI18n.text("condition.lumenechallenge.objective.kill_mob_with_effect",
                    directObjectText("entityName", RussianGrammar.GrammaticalCase.ACCUSATIVE), effectNameText());
            case VILLAGER_LEVEL_UP -> ModI18n.text("condition.lumenechallenge.objective.villager_level_up",
                    professionNameText(), levelNameText());
            case PROJECTILE_HIT_TARGET -> ModI18n.text("condition.lumenechallenge.objective.projectile_hit_target",
                    projectileNameText(), targetNameText());
            case GROW_TREE -> ModI18n.text("condition.lumenechallenge.objective.grow_tree", ModI18n.text("condition.lumenechallenge.tree." + dataString("saplingPath")));
            case HARVEST_CROP -> ModI18n.text("condition.lumenechallenge.objective.harvest_crop", ModI18n.text("condition.lumenechallenge.crop." + dataString("cropPath")));
            case STORE_ITEM -> ModI18n.text("condition.lumenechallenge.objective.store_item",
                    directObjectText("itemName", RussianGrammar.GrammaticalCase.ACCUSATIVE),
                    ModI18n.text("condition.lumenechallenge.storage." + dataString("storage")));
            case PUT_MOB_IN_BOAT -> ModI18n.text("condition.lumenechallenge.objective.put_mob_in_boat",
                    directObjectText("entityName", RussianGrammar.GrammaticalCase.ACCUSATIVE));
            case RIDE_MOB -> ModI18n.text("condition.lumenechallenge.objective.ride_mob",
                    directObjectText("entityName", RussianGrammar.GrammaticalCase.ACCUSATIVE));
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

    private Text professionNameText() {
        String key = "entity.minecraft.villager." + dataString("profession");
        String localized = ModI18n.minecraftText(ChallengeModSettings.getModLanguage(), key);
        return Text.literal(localized == null || localized.isBlank() || localized.equals(key) ? dataString("profession") : localized);
    }

    private Text levelNameText() {
        return switch (dataInt("level", 2)) {
            case 2 -> ModI18n.text("condition.lumenechallenge.villager_level.apprentice");
            case 3 -> ModI18n.text("condition.lumenechallenge.villager_level.journeyman");
            case 4 -> ModI18n.text("condition.lumenechallenge.villager_level.expert");
            default -> ModI18n.text("condition.lumenechallenge.villager_level.master");
        };
    }

    private Text projectileNameText() {
        return ModI18n.text("condition.lumenechallenge.projectile." + dataString("projectilePath"));
    }

    private Text targetNameText() {
        return dataString("targetKind").equals("block")
                ? directObjectText("blockName", RussianGrammar.GrammaticalCase.ACCUSATIVE)
                : directObjectText("entityName", RussianGrammar.GrammaticalCase.ACCUSATIVE);
    }

    private Text dungeonLootNameText() {
        String target = dataString("lootTarget");
        if ("diamond_armor".equals(target)) return ModI18n.text("condition.lumenechallenge.loot_target.diamond_armor");
        if ("smithing_template".equals(target)) return ModI18n.text("condition.lumenechallenge.loot_target.smithing_template");
        return directObjectText("itemName", RussianGrammar.GrammaticalCase.ACCUSATIVE);
    }

    private Text enchantmentNameText() {
        String id = dataString("enchantment");
        String path = id.startsWith("minecraft:") ? id.substring("minecraft:".length()) : id;
        String key = "enchantment.minecraft." + path;
        String localized = ModI18n.minecraftText(ChallengeModSettings.getModLanguage(), key);
        if (localized.isBlank() || localized.equals(key)) localized = phrase("enchantmentName");
        return Text.literal(localized);
    }

    private Text effectNameText() {
        String id = dataString("effect");
        String key = statusEffectTranslationKey(id);
        String localized = key.isBlank() ? phrase("effectName")
                : ModI18n.minecraftText(ChallengeModSettings.getModLanguage(), key);
        if (localized.isBlank() || localized.equals(key)) localized = phrase("effectName");
        if ("ru_ru".equals(ChallengeModSettings.getModLanguage()) && !localized.isBlank()) {
            localized = RussianGrammar.effect(localized);
        }
        return Text.literal(localized);
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
        // Russian has three numeral-dependent forms: "раз", "раза", "раз".
        // Reuse the already defined 11–14 / last-digit rule so 4, 22–24, etc.
        // are rendered correctly without changing other locales.
        if ("ru_ru".equals(ChallengeModSettings.getModLanguage())) {
            return Text.literal(countSuffix());
        }

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
            case IN_DUNGEON -> ModI18n.text("condition.lumenechallenge.modifier.in_dungeon", ModI18n.text("condition.lumenechallenge.dungeon." + dataString("dungeon")));
            case IN_DIMENSION -> ModI18n.text("condition.lumenechallenge.modifier.in_dimension", ModI18n.text("condition.lumenechallenge.dimension.in." + dataString("requiredDimensionNameKey")));
            case NO_MOVEMENT -> ModI18n.text("condition.lumenechallenge.modifier.no_movement");
            case IN_AIR -> ModI18n.text("condition.lumenechallenge.modifier.in_air");
            case PERSPECTIVE -> ModI18n.text("condition.lumenechallenge.modifier.perspective." + switch (dataInt("perspective", 0)) {
                case 1 -> "third_back";
                case 2 -> "third_front";
                default -> "first";
            });
            case LOOK_AT_TARGET -> ModI18n.text("condition.lumenechallenge.modifier.look_at_target", lookTargetText());
        };
    }

    private Text lookTargetText() {
        boolean blockTarget = "block".equals(dataString("lookTargetKind"));
        String id = dataString("lookTarget");
        String key = blockTarget ? blockTranslationKey(id) : entityTranslationKey(id);
        String localized = key.isBlank() ? "" : ModI18n.minecraftText(ChallengeModSettings.getModLanguage(), key);
        if (localized.isBlank() || localized.equals(key)) localized = dataString("lookTargetName");
        if ("ru_ru".equals(ChallengeModSettings.getModLanguage())) {
            return Text.literal(blockTarget
                    ? RussianGrammar.block(localized, RussianGrammar.GrammaticalCase.ACCUSATIVE)
                    : RussianGrammar.mob(localized, RussianGrammar.GrammaticalCase.ACCUSATIVE));
        }
        return Text.literal(localized);
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


    private Text smeltMaterialText() {
        String key = dataString("materialNameKey");
        if (key.equals("copper") || key.equals("iron") || key.equals("gold")) {
            return ModI18n.text("condition.lumenechallenge.material." + key);
        }
        // Compatibility with saved challenges created before material display keys existed.
        return directObjectText("itemName", RussianGrammar.GrammaticalCase.ACCUSATIVE);
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
        Type type = Type.valueOf(NbtCompat.getString(tag, "type", Type.JUMP.name()));
        List<Modifier> modifiers = new java.util.ArrayList<>();
        net.minecraft.nbt.NbtList modifierList = NbtCompat.getList(tag, "modifierIds", NbtElement.INT_TYPE);
        for (int i = 0; i < modifierList.size(); i++) {
            Modifier modifier = Modifier.fromId(NbtCompat.getInt(modifierList, i));
            if (modifier != null) modifiers.add(modifier);
        }
        if (modifiers.isEmpty()) {
            Modifier legacy = Modifier.fromId(NbtCompat.getInt(tag, "modifierId", -1));
            if (legacy != null) modifiers.add(legacy);
        }
        ObjectiveCondition condition = new ObjectiveCondition(
                type,
                modifiers,
                NbtCompat.getInt(tag, "requiredCount", 1),
                NbtCompat.getCompound(tag, "data")
        );
        condition.progress = NbtCompat.getInt(tag, "progress", 0);
        condition.distanceAnchorX = NbtCompat.getDouble(tag, "__distanceAnchorX", Double.NaN);
        condition.distanceAnchorZ = NbtCompat.getDouble(tag, "__distanceAnchorZ", Double.NaN);
        condition.distanceRemainder = NbtCompat.getDouble(tag, "__distanceRemainder", 0.0D);
        condition.distanceTracking = NbtCompat.getBoolean(tag, "__distanceTracking", false);
        return condition;
    }
}
