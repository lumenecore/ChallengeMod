package com.lumenechallenge.challenge.condition;

import net.minecraft.nbt.NbtCompound;

public enum ConditionKind {
    STEP("step"),
    DEATH("death"),
    MINING("mining"),
    PLACING("placing"),
    KILLING("killing"),
    CRAFT("craft"),
    EATING("eating"),
    DRINKING("drinking"),
    DAMAGE("damage"),
    JUMP("jump"),
    HOLDING_ITEM("holding_item"),
    HUNGER("hunger"),
    TIME("time"),
    HEIGHT("height"),
    BIOME("biome"),
    ARMOR("armor"),
    POSITION("position"),
    DIRECTION("direction"),
    INVENTORY("inventory"),
    EFFECT("effect"),
    WEATHER("weather"),
    ENTITY_NEARBY("entity_nearby"),
    LOCATION("location"),
    DIMENSION("dimension"),
    VILLAGE("village"),
    WATER("water"),
    HEALTH("health"),
    BURNING("burning"),
    SNEAKING("sneaking"),
    SPRINTING("sprinting"),
    MOVEMENT("movement"),
    COMPOSITE("composite"),
    OBJECTIVE("objective");

    private final String id;

    ConditionKind(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static Condition fromNbt(NbtCompound tag) {
        if (!tag.contains("kind")) {
            throw new IllegalArgumentException("Missing condition kind");
        }
        String kind = tag.getString("kind").orElseThrow(() -> new IllegalArgumentException("Missing condition kind"));
        return switch (kind) {
            case "step" -> StepCondition.fromNbt(tag);
            case "death" -> DeathCondition.fromNbt(tag);
            case "mining" -> MiningCondition.fromNbt(tag);
            case "placing" -> BlockPlacementCondition.fromNbt(tag);
            case "killing" -> KillingCondition.fromNbt(tag);
            case "craft" -> CraftCondition.fromNbt(tag);
            case "eating" -> ConsumeCondition.fromNbt(tag);
            case "drinking" -> ConsumeCondition.fromNbt(tag);
            case "damage" -> DamageCondition.fromNbt(tag);
            case "jump" -> JumpCondition.fromNbt(tag);
            case "holding_item" -> HoldingItemCondition.fromNbt(tag);
            case "hunger" -> HungerCondition.fromNbt(tag);
            case "time" -> TimeCondition.fromNbt(tag);
            case "height" -> HeightCondition.fromNbt(tag);
            case "biome" -> BiomeCondition.fromNbt(tag);
            case "armor" -> ArmorCondition.fromNbt(tag);
            case "position" -> PositionCondition.fromNbt(tag);
            case "direction" -> DirectionCondition.fromNbt(tag);
            case "inventory" -> InventoryCondition.fromNbt(tag);
            case "effect" -> EffectCondition.fromNbt(tag);
            case "weather" -> WeatherCondition.fromNbt(tag);
            case "entity_nearby" -> EntityNearbyCondition.fromNbt(tag);
            case "location" -> LocationCondition.fromNbt(tag);
            case "dimension" -> DimensionCondition.fromNbt(tag);
            case "village" -> VillageCondition.fromNbt(tag);
            case "water" -> WaterCondition.fromNbt(tag);
            case "health" -> HealthCondition.fromNbt(tag);
            case "burning" -> BurningCondition.fromNbt(tag);
            case "sneaking" -> SneakingCondition.fromNbt(tag);
            case "sprinting" -> SprintingCondition.fromNbt(tag);
            case "movement" -> MovementCondition.fromNbt(tag);
            case "composite" -> CompositeCondition.fromNbt(tag);
            case "objective" -> ObjectiveCondition.fromNbt(tag);
            default -> throw new IllegalArgumentException("Unknown condition kind: " + kind);
        };
    }
}
