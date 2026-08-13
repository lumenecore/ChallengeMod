package com.lumenechallenge.challenge;

import com.lumenechallenge.challenge.condition.Condition;
import com.lumenechallenge.challenge.condition.ObjectiveCondition;
import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryKey;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.stat.Stats;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.util.List;
import java.util.Random;
import java.util.UUID;

public class ChallengeState extends PersistentState {
    private static final Codec<ChallengeState> CODEC = StringNbtReader.NBT_COMPOUND_CODEC.xmap(ChallengeState::fromNbt, ChallengeState::toNbt);
    private static final PersistentStateType<ChallengeState> TYPE = new PersistentStateType<>("lumene_challenge", ChallengeState::new, CODEC, net.minecraft.datafixer.DataFixTypes.LEVEL);

    private UUID player;
    private Challenge challenge;
    private int progress;
    private long startTime;
    private long finishTime;
    private boolean completed;
    private boolean timerStarted;
    private long challengeSeed;
    private String challengeSeedText = "";
    private boolean seedSelected;
    private boolean modifierEnabled;
    private ModifierType modifier = ModifierType.NONE;
    private int challengeCount = 10;
    private float appliedScale = Float.NaN;
    private double appliedMovementSpeed = Double.NaN;
    private long hangoverNextEffectTime = -1L;
    private String hangoverCurrentEffectId = "";
    private long hangoverCurrentEffectEndTime = -1L;
    private long cataclysmActiveUntil = -1L;
    private long cataclysmNextStart = -1L;
    private transient java.util.Map<RegistryKey<Recipe<?>>, ItemStack> chaosCraftRecipeMap;
    private transient java.util.Map<java.util.UUID, Double> cataclysmBaseHeights;

    // transient event markers
    private BlockState lastMinedBlock;
    private BlockState lastPlacedBlock;
    private LivingEntity lastKilledEntity;
    private ItemStack lastCraftedItem;
    private ItemStack lastConsumedItem;
    private ItemStack lastEnchantedItem;
    private ItemStack lastBrokenItem;
    private boolean damagedThisTick;
    private double damageAmountThisTick;
    private LivingEntity damagedEntity;
    private boolean jumpedThisTick;
    private boolean fishedThisTick;
    private boolean caughtSelfOnRodThisTick;
    private boolean enderPearlTeleportedThisTick;
    private boolean shotMobFromDistanceThisTick;
    private double shotMobDistanceThisTick;
    private LivingEntity tamedEntity;
    private boolean sleptThisTick;
    private boolean tradedThisTick;
    private boolean diedThisTick;

    private transient int lastFishCaughtStat = -1;
    private transient int lastEnchantItemStat = -1;

    private double lastX = Double.NaN;
    private double lastY = Double.NaN;
    private double lastZ = Double.NaN;
    private double distanceRemainder = 0.0D;
    private boolean trackingDistance = false;
    private long lastMovementTime = 0L;
    private boolean lastOnGround = true;

    public ChallengeState() {}

    public static void bootstrap() {}

    public static ChallengeState getServerState(MinecraftServer server) {
        ServerWorld world = server.getWorld(World.OVERWORLD);
        if (world == null) throw new IllegalStateException("Overworld is not available");
        return world.getPersistentStateManager().getOrCreate(TYPE);
    }

    public void ensurePlayer(ServerPlayerEntity player) {
        if (this.player == null) {
            this.player = player.getUuid();
            markDirty();
        }
    }

    public void selectSeed(ServerPlayerEntity player, long seed, ModifierType selectedModifier, int challengeCount) {
        if (challenge != null) return;
        this.player = player.getUuid();
        this.challengeSeed = seed;
        this.challengeSeedText = Long.toString(seed);
        this.seedSelected = true;
        this.modifier = selectedModifier == null ? ModifierType.NONE : selectedModifier;
        if (this.modifier == ModifierType.RANDOM) {
            this.modifier = ModifierType.random(seed);
        }
        this.modifierEnabled = this.modifier != ModifierType.NONE;
        this.challengeCount = Math.max(1, Math.min(50, challengeCount));
        this.challenge = ChallengeGenerator.generate(player, seed, this.challengeCount);
        this.progress = 0;
        this.startTime = player.getWorld().getTime();
        this.finishTime = 0L;
        this.completed = false;
        this.timerStarted = true;
        resetMarkers();
        initializeActionStatBaselines(player);
        this.lastMovementTime = player.getServerWorld().getTime();
        this.lastOnGround = true;
        this.lastX = Double.NaN;
        this.lastY = Double.NaN;
        this.lastZ = Double.NaN;
        this.distanceRemainder = 0.0D;
        this.trackingDistance = false;
        this.appliedScale = Float.NaN;
        this.appliedMovementSpeed = Float.NaN;
        this.hangoverNextEffectTime = this.startTime + 600L;
        this.hangoverCurrentEffectId = "";
        this.hangoverCurrentEffectEndTime = -1L;
        this.cataclysmActiveUntil = -1L;
        this.cataclysmNextStart = this.startTime + 200L;
        this.chaosCraftRecipeMap = null;
        this.cataclysmBaseHeights = new java.util.HashMap<>();
        if (this.modifier == ModifierType.CHAOS) {
            ensureChaosCraftRecipeMap(player.getServerWorld());
        }
        applyScale(player);
        applyModifierEffects(player);
        markDirty();
    }

    private void resetMarkers() {
        lastMinedBlock = null;
        lastPlacedBlock = null;
        lastKilledEntity = null;
        lastCraftedItem = null;
        lastConsumedItem = null;
        lastEnchantedItem = null;
        lastBrokenItem = null;
        damagedThisTick = false;
        damageAmountThisTick = 0.0D;
        damagedEntity = null;
        jumpedThisTick = false;
        fishedThisTick = false;
        caughtSelfOnRodThisTick = false;
        enderPearlTeleportedThisTick = false;
        shotMobFromDistanceThisTick = false;
        shotMobDistanceThisTick = 0.0D;
        tamedEntity = null;
        sleptThisTick = false;
        tradedThisTick = false;
        diedThisTick = false;
    }

    public void tick(ServerPlayerEntity player) {
        if (challenge == null || completed) return;
        if (player.getUuid() == null || this.player != null && !this.player.equals(player.getUuid())) return;

        applyScale(player);
        applyModifierEffects(player);

        ServerWorld world = player.getServerWorld();
        if (Double.isNaN(lastX)) {
            lastX = player.getX();
            lastY = player.getY();
            lastZ = player.getZ();
            lastMovementTime = world.getTime();
        } else {
            double dx = player.getX() - lastX;
            double dy = player.getY() - lastY;
            double dz = player.getZ() - lastZ;
            if (dx * dx + dy * dy + dz * dz > 0.0001D) {
                lastMovementTime = world.getTime();
                lastX = player.getX();
                lastY = player.getY();
                lastZ = player.getZ();
            }
        }

        boolean onGround = player.isOnGround();
        if (lastOnGround && !onGround) jumpedThisTick = true;
        lastOnGround = onGround;

        syncActionStats(player);
        checkAndAdvance(player, false);
        resetMarkers();
    }

    private void applyScale(ServerPlayerEntity player) {
        float scale = modifier.scale();
        EntityAttributeInstance instance = player.getAttributeInstance(EntityAttributes.SCALE);
        if (instance == null) {
            return;
        }
        if (Float.isNaN(appliedScale) || appliedScale != scale) {
            appliedScale = scale;
            instance.setBaseValue(scale);
            player.calculateDimensions();
            player.refreshPositionAfterTeleport(player.getX(), player.getY(), player.getZ());
        }
    }

    private void applyModifierEffects(ServerPlayerEntity player) {
        if (!modifierEnabled) {
            resetModifierEffects(player);
            return;
        }

        ServerWorld world = player.getServerWorld();

        switch (modifier) {
            case SPEEDRUN -> {
                long timeOfDay = world.getTimeOfDay();
                world.setTimeOfDay(timeOfDay + 1L);
            }
            case HANGOVER -> {
                if (hangoverNextEffectTime < 0L) {
                    hangoverNextEffectTime = world.getTime() + 600L;
                }
                if (world.getTime() >= hangoverNextEffectTime) {
                    applyRandomEffect(player);
                    hangoverNextEffectTime = world.getTime() + 600L;
                }
            }
            case POCKETS -> enforceLeakyPockets(player);
            case OBESITY -> applyObesity(player);
            case CATACLYSM -> applyCataclysm(player);
            default -> resetModifierEffects(player);
        }
    }

    private void resetModifierEffects(ServerPlayerEntity player) {
        EntityAttributeInstance speed = player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        if (speed != null && !Double.isNaN(appliedMovementSpeed)) {
            speed.setBaseValue(appliedMovementSpeed);
        }

        appliedMovementSpeed = Double.NaN;
        hangoverCurrentEffectId = "";
        hangoverCurrentEffectEndTime = -1L;
        player.setNoGravity(false);
        clearCataclysmState(player.getServerWorld());
    }


    private void applyObesity(ServerPlayerEntity player) {
        EntityAttributeInstance speed = player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        if (speed != null && Double.isNaN(appliedMovementSpeed)) {
            appliedMovementSpeed = speed.getBaseValue();
            speed.setBaseValue(appliedMovementSpeed * 0.75D);
        }

        if (player.getServerWorld().getTime() % 20L == 0L) {
            player.addExhaustion(0.05F);
        }
    }

    private void applyRandomEffect(ServerPlayerEntity player) {
        List<RegistryEntry.Reference<StatusEffect>> effects = Registries.STATUS_EFFECT.streamEntries().toList();
        if (effects.isEmpty()) {
            return;
        }

        long time = player.getServerWorld().getTime();
        int index = (int) Math.floorMod(time ^ player.getUuid().getLeastSignificantBits(), effects.size());
        RegistryEntry.Reference<StatusEffect> effect = effects.get(index);

        hangoverCurrentEffectId = Registries.STATUS_EFFECT.getId(effect.value()).toString();
        hangoverCurrentEffectEndTime = time + 600L;
        player.addStatusEffect(new StatusEffectInstance(effect, 600, 0, false, false, true));
    }

    private void applyStoredHangoverEffect(ServerPlayerEntity player) {
        if (hangoverCurrentEffectId.isBlank()) {
            return;
        }

        long time = player.getServerWorld().getTime();
        long remaining = hangoverCurrentEffectEndTime - time;
        if (remaining <= 0L) {
            return;
        }

        RegistryEntry.Reference<StatusEffect> effect = findHangoverEffect();
        if (effect == null) {
            return;
        }

        player.addStatusEffect(new StatusEffectInstance(effect, (int) Math.min(Integer.MAX_VALUE, remaining), 0, false, false, true));
    }

    private RegistryEntry.Reference<StatusEffect> findHangoverEffect() {
        for (RegistryEntry.Reference<StatusEffect> effect : Registries.STATUS_EFFECT.streamEntries().toList()) {
            if (Registries.STATUS_EFFECT.getId(effect.value()).toString().equals(hangoverCurrentEffectId)) {
                return effect;
            }
        }
        return null;
    }

    private void applyCataclysm(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        long elapsed = Math.max(0L, world.getTime() - startTime);
        long cycle = elapsed / 600L;
        long cycleTime = elapsed % 600L;
        long offset = getCataclysmCycleOffset(cycle);

        boolean active = cycleTime >= offset && cycleTime < offset + 200L;
        if (!active) {
            clearCataclysmState(world);
            return;
        }

        if (cataclysmBaseHeights == null) {
            cataclysmBaseHeights = new java.util.HashMap<>();
        }

        java.util.List<LivingEntity> affected = world.getEntitiesByClass(
                LivingEntity.class,
                player.getBoundingBox().expand(256.0D),
                entity -> entity.isAlive()
        );

        java.util.Set<java.util.UUID> activeEntities = new java.util.HashSet<>();
        for (LivingEntity entity : affected) {
            java.util.UUID uuid = entity.getUuid();
            activeEntities.add(uuid);

            double baseY = cataclysmBaseHeights.computeIfAbsent(uuid, key -> entity.getY());
            entity.setNoGravity(true);

            double lifted = entity.getY() - baseY;
            if (lifted >= 20.0D) {
                entity.setVelocity(entity.getVelocity().x * 0.9D, 0.0D, entity.getVelocity().z * 0.9D);
            } else {
                double remaining = 20.0D - lifted;
                double upward = Math.max(0.02D, Math.min(0.12D, remaining / 40.0D));
                entity.setVelocity(entity.getVelocity().x * 0.98D, upward, entity.getVelocity().z * 0.98D);
            }

            entity.velocityModified = true;
        }

        if (!cataclysmBaseHeights.isEmpty()) {
            java.util.Iterator<java.util.Map.Entry<java.util.UUID, Double>> iterator = cataclysmBaseHeights.entrySet().iterator();
            while (iterator.hasNext()) {
                java.util.Map.Entry<java.util.UUID, Double> entry = iterator.next();
                if (activeEntities.contains(entry.getKey())) {
                    continue;
                }
                Entity entity = world.getEntity(entry.getKey());
                if (entity instanceof LivingEntity livingEntity) {
                    livingEntity.setNoGravity(false);
                }
                iterator.remove();
            }
        }
    }

    private void clearCataclysmState(ServerWorld world) {
        if (cataclysmBaseHeights == null || cataclysmBaseHeights.isEmpty()) {
            return;
        }

        for (java.util.UUID uuid : cataclysmBaseHeights.keySet()) {
            Entity entity = world.getEntity(uuid);
            if (entity instanceof LivingEntity livingEntity) {
                livingEntity.setNoGravity(false);
            }
        }
        cataclysmBaseHeights.clear();
    }

    private long getCataclysmCycleOffset(long cycle) {
        Random random = new Random(challengeSeed ^ (cycle * 341873128712L));
        return random.nextInt(401);
    }

    public ItemStack getChaosReplacementOutput(ServerWorld world, RegistryKey<Recipe<?>> sourceKey) {
        ensureChaosCraftRecipeMap(world);
        if (chaosCraftRecipeMap == null) return ItemStack.EMPTY;

        ItemStack replacement = chaosCraftRecipeMap.get(sourceKey);
        return replacement == null ? ItemStack.EMPTY : replacement.copy();
    }

    private void ensureChaosCraftRecipeMap(ServerWorld world) {
        if (chaosCraftRecipeMap != null) return;

        List<RecipeEntry<CraftingRecipe>> recipes = world.getServer().getRecipeManager().values().stream()
                .filter(entry -> entry.value() instanceof CraftingRecipe)
                .map(entry -> (RecipeEntry<CraftingRecipe>) entry)
                .sorted(java.util.Comparator.comparing(entry -> entry.id().getValue().toString()))
                .toList();

        if (recipes.isEmpty()) {
            chaosCraftRecipeMap = java.util.Map.of();
            return;
        }

        List<RecipeEntry<CraftingRecipe>> shuffled = new java.util.ArrayList<>(recipes);
        java.util.Collections.shuffle(shuffled, new Random(challengeSeed));

        java.util.Map<RegistryKey<Recipe<?>>, ItemStack> result = new java.util.HashMap<>();
        for (int i = 0; i < recipes.size(); i++) {
            result.put(
                recipes.get(i).id(),
                ChaosRecipeOutputExtractor.extract(shuffled.get((i + 1) % shuffled.size()).value())
        );
        }
        chaosCraftRecipeMap = result;
    }

    private void enforceLeakyPockets(ServerPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (stack.isEmpty() || stack.getCount() <= 1) {
                continue;
            }

            int surplus = stack.getCount() - 1;
            stack.setCount(1);

            while (surplus > 0) {
                ItemStack single = stack.copy();
                single.setCount(1);

                int emptySlot = findEmptyMainSlot(inventory);
                if (emptySlot >= 0) {
                    inventory.setStack(emptySlot, single);
                } else {
                    player.dropItem(single, false);
                }
                surplus--;
            }
        }
    }

    private int findEmptyMainSlot(PlayerInventory inventory) {
        for (int slot = 0; slot < 36; slot++) {
            if (inventory.getStack(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    public void onPlayerJoin(ServerPlayerEntity player) {
        ensurePlayer(player);
        if (challenge == null && seedSelected) {
            selectSeed(player, challengeSeed, modifier, challengeCount);
        } else if (challenge != null) {
            applyScale(player);
            initializeActionStatBaselines(player);
        }
    }

    public void onPlayerRespawn(ServerPlayerEntity player) {
        if (modifierEnabled && modifier == ModifierType.HANGOVER) {
            applyStoredHangoverEffect(player);
        }
    }

    public void onDeath(ServerPlayerEntity player) {
        diedThisTick = true;
        checkAndAdvance(player, false);
    }

    public void onBlockBreak(ServerPlayerEntity player, BlockState blockState) {
        lastMinedBlock = blockState;
    }

    public void onBlockPlace(ServerPlayerEntity player, BlockState blockState) {
        lastPlacedBlock = blockState;
    }

    public void onKill(ServerPlayerEntity player, LivingEntity victim) {
        lastKilledEntity = victim;
    }

    public void onCraft(ServerPlayerEntity player, ItemStack stack) {
        lastCraftedItem = stack.copy();
    }

    public void onConsume(ServerPlayerEntity player, ItemStack stack) {
        lastConsumedItem = stack.copy();
    }

    public void onEnchant(ServerPlayerEntity player, ItemStack stack) {
        lastEnchantedItem = stack == null ? null : stack.copy();
    }

    public void onItemBroken(ServerPlayerEntity player, ItemStack stack) {
        lastBrokenItem = stack == null ? null : stack.copy();
    }

    public void onDamage(ServerPlayerEntity player, LivingEntity damaged, DamageSource source, float amount) {
        damagedThisTick = true;
        damagedEntity = damaged;
        damageAmountThisTick = amount;
    }

    public void onFish(ServerPlayerEntity player, boolean caughtItem, boolean selfCatch) {
        if (caughtItem) {
            fishedThisTick = true;
        }
        if (selfCatch) {
            caughtSelfOnRodThisTick = true;
        }
    }

    public void onPearlTeleport(ServerPlayerEntity player) {
        enderPearlTeleportedThisTick = true;
    }

    public void onProjectileHit(ServerPlayerEntity player, LivingEntity victim, double distance) {
        shotMobFromDistanceThisTick = true;
        shotMobDistanceThisTick = distance;
    }

    public void onTame(ServerPlayerEntity player, LivingEntity entity) {
        tamedEntity = entity;
    }

    public void onSleep(ServerPlayerEntity player) {
        sleptThisTick = true;
    }

    public void onTrade(ServerPlayerEntity player) {
        tradedThisTick = true;
    }

    private void initializeActionStatBaselines(ServerPlayerEntity player) {
        lastFishCaughtStat = player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.FISH_CAUGHT));
        lastEnchantItemStat = player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.ENCHANT_ITEM));
    }

    private void syncActionStats(ServerPlayerEntity player) {
        int fishCaughtStat = player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.FISH_CAUGHT));
        if (fishCaughtStat > lastFishCaughtStat) {
            fishedThisTick = true;
            lastFishCaughtStat = fishCaughtStat;
        }

        int enchantedItemStat = player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.ENCHANT_ITEM));
        if (enchantedItemStat > lastEnchantItemStat) {
            lastEnchantedItem = ItemStack.EMPTY;
            lastEnchantItemStat = enchantedItemStat;
        }
    }

    private ChallengeContext buildContext(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        BlockPos pos = player.getBlockPos();
        RegistryEntry<Biome> biome = world.getBiome(pos);
        return new ChallengeContext(
                player,
                world,
                pos,
                player.getInventory(),
                player.getMainHandStack(),
                world.getTime(),
                biome,
                world.isRaining(),
                world.isThundering(),
                (int) Math.max(0L, world.getTime() - lastMovementTime),
                diedThisTick,
                lastMinedBlock,
                lastPlacedBlock,
                lastKilledEntity,
                lastCraftedItem,
                lastConsumedItem,
                lastEnchantedItem,
                lastBrokenItem,
                damagedThisTick,
                damageAmountThisTick,
                damagedEntity,
                jumpedThisTick,
                player.isOnFire(),
                null,
                fishedThisTick,
                caughtSelfOnRodThisTick,
                enderPearlTeleportedThisTick,
                shotMobFromDistanceThisTick,
                shotMobDistanceThisTick,
                tamedEntity,
                sleptThisTick,
                tradedThisTick
        );
    }

    private void checkAndAdvance(ServerPlayerEntity player, boolean force) {
        if (challenge == null || completed) return;
        Condition current = challenge.currentCondition();
        if (current == null) return;
        if (force || current.check(buildContext(player))) {
            advance(player.getServerWorld());
        }
    }

    private void advance(ServerWorld world) {
        progress++;
        if (challenge != null) challenge.setCompletedSteps(progress);
        if (challenge != null && progress >= challenge.conditions().size()) {
            completed = true;
            finishTime = world.getTime();
        }
        markDirty();
    }

    public Challenge challenge() { return challenge; }
    public int completedSteps() { return progress; }
    public boolean completed() { return completed; }
    public boolean timerStarted() { return timerStarted; }
    public long startTime() { return startTime; }
    public long finishTime() { return finishTime; }
    public long challengeSeed() { return challengeSeed; }
    public String challengeSeedText() { return challengeSeedText; }
    public void setChallengeSeedText(String text) {
        if (text == null || text.isBlank()) {
            this.challengeSeedText = Long.toString(challengeSeed);
        } else {
            this.challengeSeedText = text;
        }
    }
    public boolean seedSelected() { return seedSelected; }
    public UUID playerId() { return player; }
    public boolean modifierEnabled() { return modifierEnabled; }
    public ModifierType modifier() { return modifier; }
    public int challengeCount() { return challengeCount; }

    public boolean allowsMobDamage() {
        if (challenge == null || completed) {
            return false;
        }
        Condition current = challenge.currentCondition();
        if (!(current instanceof ObjectiveCondition objectiveCondition)) {
            return false;
        }
        return switch (objectiveCondition.type()) {
            case KILL_MOB, SHOOT_MOB_FROM_DISTANCE, DEAL_DAMAGE -> true;
            default -> false;
        };
    }

    public long getCurrentElapsedTicks(MinecraftServer server) {
        if (!timerStarted) return 0L;
        if (completed && finishTime > 0L) return Math.max(0L, finishTime - startTime);
        if (server == null) return 0L;
        ServerWorld world = server.getWorld(World.OVERWORLD);
        if (world == null) return 0L;
        return Math.max(0L, world.getTime() - startTime);
    }

    private NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        if (player != null) nbt.putString("player", player.toString());
        if (challenge != null) nbt.put("challenge", challenge.toNbt());
        nbt.putInt("progress", progress);
        nbt.putLong("startTime", startTime);
        nbt.putLong("finishTime", finishTime);
        nbt.putBoolean("completed", completed);
        nbt.putBoolean("timerStarted", timerStarted);
        nbt.putLong("challengeSeed", challengeSeed);
        nbt.putString("challengeSeedText", challengeSeedText);
        nbt.putBoolean("seedSelected", seedSelected);
        nbt.putBoolean("modifierEnabled", modifierEnabled);
        nbt.putString("modifier", modifier.name());
        nbt.putInt("challengeCount", challengeCount);
        nbt.putDouble("distanceRemainder", distanceRemainder);
        nbt.putBoolean("trackingDistance", trackingDistance);
        nbt.putDouble("lastX", lastX);
        nbt.putDouble("lastY", lastY);
        nbt.putDouble("lastZ", lastZ);
        nbt.putDouble("appliedMovementSpeed", appliedMovementSpeed);
        nbt.putLong("hangoverNextEffectTime", hangoverNextEffectTime);
        nbt.putString("hangoverCurrentEffectId", hangoverCurrentEffectId);
        nbt.putLong("hangoverCurrentEffectEndTime", hangoverCurrentEffectEndTime);
        nbt.putLong("cataclysmActiveUntil", cataclysmActiveUntil);
        nbt.putLong("cataclysmNextStart", cataclysmNextStart);
        return nbt;
    }

    private static ChallengeState fromNbt(NbtCompound nbt) {
        ChallengeState state = new ChallengeState();
        nbt.getString("player").ifPresent(value -> state.player = UUID.fromString(value));
        nbt.getCompound("challenge").ifPresent(value -> state.challenge = Challenge.fromNbt(value));
        state.progress = nbt.getInt("progress", 0);
        state.startTime = nbt.getLong("startTime", 0L);
        state.finishTime = nbt.getLong("finishTime", 0L);
        state.completed = nbt.getBoolean("completed", false);
        state.timerStarted = nbt.getBoolean("timerStarted", false);
        state.challengeSeed = nbt.getLong("challengeSeed", 0L);
        state.challengeSeedText = nbt.getString("challengeSeedText", Long.toString(state.challengeSeed));
        if (state.challengeSeedText.isBlank()) state.challengeSeedText = Long.toString(state.challengeSeed);
        state.seedSelected = nbt.getBoolean("seedSelected", false);
        state.modifierEnabled = nbt.getBoolean("modifierEnabled", false);
        state.modifier = ModifierType.fromId(nbt.getString("modifier", ModifierType.NONE.name()));
        state.challengeCount = nbt.getInt("challengeCount", 10);
        state.distanceRemainder = nbt.getDouble("distanceRemainder", 0.0D);
        state.trackingDistance = nbt.getBoolean("trackingDistance", false);
        state.lastX = nbt.getDouble("lastX", Double.NaN);
        state.lastY = nbt.getDouble("lastY", Double.NaN);
        state.lastZ = nbt.getDouble("lastZ", Double.NaN);
        state.appliedMovementSpeed = nbt.getFloat("appliedMovementSpeed", Float.NaN);
        state.hangoverNextEffectTime = nbt.getLong("hangoverNextEffectTime", -1L);
        state.hangoverCurrentEffectId = nbt.getString("hangoverCurrentEffectId", "");
        state.hangoverCurrentEffectEndTime = nbt.getLong("hangoverCurrentEffectEndTime", -1L);
        state.cataclysmActiveUntil = nbt.getLong("cataclysmActiveUntil", -1L);
        state.cataclysmNextStart = nbt.getLong("cataclysmNextStart", -1L);
        if (state.challenge != null) state.challenge.setCompletedSteps(state.progress);
        return state;
    }
}
