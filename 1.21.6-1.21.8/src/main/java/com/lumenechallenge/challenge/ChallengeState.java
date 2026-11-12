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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ChallengeState extends PersistentState {
    private static final Codec<ChallengeState> CODEC = StringNbtReader.NBT_COMPOUND_CODEC.xmap(ChallengeState::fromNbt, ChallengeState::toNbt);
    private static final PersistentStateType<ChallengeState> TYPE = new PersistentStateType<>("lumene_challenge", ChallengeState::new, CODEC, net.minecraft.datafixer.DataFixTypes.LEVEL);
    private static final ConcurrentMap<MinecraftServer, ChallengeState> SERVER_STATE_CACHE =
            new ConcurrentHashMap<>();

    private UUID player;
    private Challenge challenge;
    private int progress;
    private long startTime;
    private long finishTime;
    private boolean completed;
    private boolean timerStarted;
    private boolean runStarted;
    private long countdownEndTick = -1L;
    private long challengeSeed;
    private String challengeSeedText = "";
    private boolean seedSelected;
    private boolean modifierEnabled;
    private ModifierType modifier = ModifierType.NONE;
    private RunDifficulty runDifficulty = RunDifficulty.NORMAL;
    private int challengeCount = 10;
    private int maxRerolls = 5;
    private int rerollsRemaining = 5;
    private float appliedScale = Float.NaN;
    private double appliedMovementSpeed = Double.NaN;
    private double appliedMaxHealth = Double.NaN;
    private long wearNextDamageTime = -1L;
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
    private boolean lastConsumedUnderwater;
    private ItemStack lastEnchantedItem;
    private ItemStack lastBrokenItem;
    private ItemStack lastSmeltedItem;
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
    private boolean restoredRuinedPortalThisTick;
    private boolean creeperExplodedByPlayerThisTick;
    private boolean milkedCowThisTick;
    private boolean tilledSoilThisTick;
    private java.util.List<ItemStack> naturalLootItemsThisTick = java.util.List.of();
    private String naturalLootTableIdThisTick = "";
    private LivingEntity effectAppliedEntity;
    private String appliedEffectId = "";
    private String playerAppliedEffectId = "";
    private String deathCauseId = "";
    private String receivedDamageId = "";
    private String receivedDamageEntityId = "";
    private LivingEntity burnedEntity;
    private LivingEntity skeletonShotEntity;
    private String villagerProfessionId = "";
    private int villagerLevel;
    private String hitProjectileId = "";
    private String projectileTargetKind = "";
    private String projectileTargetId = "";
    private String grownSaplingId = "";
    private String harvestedCropId = "";
    private String storedItemId = "";
    private String storageId = "";
    private String mobInBoatId = "";
    private String riddenMobId = "";
    private int cameraPerspective;
    private transient java.util.Map<Long, String> plantedSaplings = new java.util.HashMap<>();

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
        if (server == null) throw new IllegalArgumentException("Server cannot be null");
        return SERVER_STATE_CACHE.computeIfAbsent(server, key -> {
            ServerWorld world = key.getWorld(World.OVERWORLD);
            if (world == null) throw new IllegalStateException("Overworld is not available");
            return world.getPersistentStateManager().getOrCreate(TYPE);
        });
    }

    public static void clearServerCache(MinecraftServer server) {
        if (server != null) SERVER_STATE_CACHE.remove(server);
    }

    public void ensurePlayer(ServerPlayerEntity player) {
        if (this.player == null) {
            this.player = player.getUuid();
            markDirty();
        }
    }

    public void selectSeed(ServerPlayerEntity player, long seed, ModifierType selectedModifier, RunDifficulty selectedDifficulty, int challengeCount) {
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
        this.runDifficulty = selectedDifficulty == null ? RunDifficulty.NORMAL : selectedDifficulty;
        this.challengeCount = Math.max(1, Math.min(100, challengeCount));
        this.challenge = ChallengeGenerator.generate(player, seed, this.challengeCount, this.runDifficulty);
        this.maxRerolls = calculateMaxRerolls(this.challengeCount);
        this.rerollsRemaining = this.maxRerolls;
        this.progress = 0;
        this.startTime = player.getWorld().getLevelProperties().getTime() + 60L;
        this.finishTime = 0L;
        this.completed = false;
        this.runStarted = true;
        this.timerStarted = false;
        this.countdownEndTick = player.getWorld().getLevelProperties().getTime() + 60L;
        if (plantedSaplings != null) plantedSaplings.clear();
        resetMarkers();
        initializeActionStatBaselines(player);
        this.lastMovementTime = player.getWorld().getLevelProperties().getTime();
        this.lastOnGround = true;
        this.lastX = Double.NaN;
        this.lastY = Double.NaN;
        this.lastZ = Double.NaN;
        this.distanceRemainder = 0.0D;
        this.trackingDistance = false;
        this.appliedScale = Float.NaN;
        this.appliedMovementSpeed = Double.NaN;
        this.appliedMaxHealth = Double.NaN;
        this.wearNextDamageTime = this.startTime + 60L;
        this.hangoverNextEffectTime = this.startTime + 600L;
        this.hangoverCurrentEffectId = "";
        this.hangoverCurrentEffectEndTime = -1L;
        this.cataclysmActiveUntil = -1L;
        this.cataclysmNextStart = this.startTime + 200L;
        this.chaosCraftRecipeMap = null;
        this.cataclysmBaseHeights = new java.util.HashMap<>();
        if (this.modifier == ModifierType.CHAOS) {
            ensureChaosCraftRecipeMap(player.getWorld());
        }
        applyScale(player);
        markDirty();
    }

    public void selectSeed(ServerPlayerEntity player, long seed, ModifierType selectedModifier, int challengeCount) {
        selectSeed(player, seed, selectedModifier, RunDifficulty.NORMAL, challengeCount);
    }

    private void resetMarkers() {
        lastMinedBlock = null;
        lastPlacedBlock = null;
        lastKilledEntity = null;
        lastCraftedItem = null;
        lastConsumedItem = null;
        lastConsumedUnderwater = false;
        lastEnchantedItem = null;
        lastBrokenItem = null;
        lastSmeltedItem = null;
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
        restoredRuinedPortalThisTick = false;
        creeperExplodedByPlayerThisTick = false;
        milkedCowThisTick = false;
        tilledSoilThisTick = false;
        naturalLootItemsThisTick = java.util.List.of();
        naturalLootTableIdThisTick = "";
        effectAppliedEntity = null;
        appliedEffectId = "";
        playerAppliedEffectId = "";
        deathCauseId = "";
        receivedDamageId = "";
        receivedDamageEntityId = "";
        burnedEntity = null;
        skeletonShotEntity = null;
        villagerProfessionId = "";
        villagerLevel = 0;
        hitProjectileId = "";
        projectileTargetKind = "";
        projectileTargetId = "";
        grownSaplingId = "";
        harvestedCropId = "";
        storedItemId = "";
        storageId = "";
        mobInBoatId = "";
        riddenMobId = "";
    }

    public void tick(ServerPlayerEntity player) {
        if (challenge == null || completed || !runStarted) return;
        if (player.getUuid() == null || this.player != null && !this.player.equals(player.getUuid())) return;

        applyScale(player);

        ServerWorld world = player.getWorld();
        if (!timerStarted) {
            // The reduced five-heart health pool must already be visible during 3-2-1.
            if (modifierEnabled && modifier == ModifierType.EXHAUSTION) {
                applyExhaustion(player);
            }
            if (countdownEndTick > 0L && world.getLevelProperties().getTime() >= countdownEndTick) {
                timerStarted = true;
                startTime = world.getLevelProperties().getTime();
                // New modifier timers start from the actual run start, not the pre-run countdown.
                wearNextDamageTime = startTime + 60L;
                initializeActionStatBaselines(player);
                resetMarkers();
                beginCurrentTask(player);
                markDirty();
            }
            return;
        }

        applyModifierEffects(player);
        if (Double.isNaN(lastX)) {
            lastX = player.getX();
            lastY = player.getY();
            lastZ = player.getZ();
            lastMovementTime = world.getLevelProperties().getTime();
        } else {
            double dx = player.getX() - lastX;
            double dy = player.getY() - lastY;
            double dz = player.getZ() - lastZ;
            if (dx * dx + dy * dy + dz * dz > 0.0001D) {
                lastMovementTime = world.getLevelProperties().getTime();
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

        ServerWorld world = player.getWorld();

        switch (modifier) {
            case HANGOVER -> {
                if (hangoverNextEffectTime < 0L) {
                    hangoverNextEffectTime = world.getLevelProperties().getTime() + 600L;
                }
                if (world.getLevelProperties().getTime() >= hangoverNextEffectTime) {
                    applyRandomEffect(player);
                    hangoverNextEffectTime = world.getLevelProperties().getTime() + 600L;
                }
            }
            case POCKETS -> enforceLeakyPockets(player);
            case OBESITY -> applyObesity(player);
            case CATACLYSM -> applyCataclysm(player);
            case EXHAUSTION -> applyExhaustion(player);
            case WEAR -> applyWear(player);
            case INVERSION, NONE, RANDOM, GIANT, DWARF, CHAOS, PACIFIST -> resetModifierEffects(player);
            default -> resetModifierEffects(player);
        }
    }

    private void resetModifierEffects(ServerPlayerEntity player) {
        EntityAttributeInstance speed = player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        if (speed != null && !Double.isNaN(appliedMovementSpeed)) {
            speed.setBaseValue(appliedMovementSpeed);
        }

        appliedMovementSpeed = Double.NaN;
        EntityAttributeInstance maxHealth = player.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (maxHealth != null && !Double.isNaN(appliedMaxHealth)) {
            maxHealth.setBaseValue(appliedMaxHealth);
            if (player.getHealth() > maxHealth.getValue()) player.setHealth((float) maxHealth.getValue());
        }
        appliedMaxHealth = Double.NaN;
        hangoverCurrentEffectId = "";
        hangoverCurrentEffectEndTime = -1L;
        player.setNoGravity(false);
        clearCataclysmState(player.getWorld());
    }


    private void applyExhaustion(ServerPlayerEntity player) {
        EntityAttributeInstance maxHealth = player.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (maxHealth == null) return;
        if (Double.isNaN(appliedMaxHealth)) {
            appliedMaxHealth = maxHealth.getBaseValue();
        }
        double reducedBase = Math.max(1.0D, appliedMaxHealth * 0.5D);
        // Reapply after death/respawn, when Minecraft reconstructs player attributes.
        if (Math.abs(maxHealth.getBaseValue() - reducedBase) > 1.0E-6D) {
            maxHealth.setBaseValue(reducedBase);
        }
        if (player.getHealth() > maxHealth.getValue()) {
            player.setHealth((float) maxHealth.getValue());
        }
    }

    private void applyWear(ServerPlayerEntity player) {
        ServerWorld world = player.getWorld();
        if (wearNextDamageTime < 0L) wearNextDamageTime = world.getLevelProperties().getTime() + 60L;
        if (world.getLevelProperties().getTime() < wearNextDamageTime) return;
        // Run one wear cycle every 60 ticks (3 seconds). Advance from current time
        // so a lag spike does not cause repeated damage bursts in a single tick.
        wearNextDamageTime = world.getLevelProperties().getTime() + 60L;

        PlayerInventory inventory = player.getInventory();
        boolean changed = false;
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (stack.isEmpty() || !stack.isDamageable() || stack.getDamage() >= stack.getMaxDamage()) {
                continue;
            }

            int nextDamage = stack.getDamage() + 1;
            if (nextDamage >= stack.getMaxDamage()) {
                // Damageable items normally do not stack; decrement safely if a
                // custom item permits stacking, otherwise remove the broken item.
                if (stack.getCount() > 1) {
                    stack.decrement(1);
                } else {
                    inventory.setStack(slot, ItemStack.EMPTY);
                }
            } else {
                stack.setDamage(nextDamage);
            }
            changed = true;
        }
        if (changed) inventory.markDirty();
    }

    private void applyObesity(ServerPlayerEntity player) {
        EntityAttributeInstance speed = player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        if (speed != null && Double.isNaN(appliedMovementSpeed)) {
            appliedMovementSpeed = speed.getBaseValue();
            speed.setBaseValue(appliedMovementSpeed * 0.75D);
        }

        if (player.getWorld().getLevelProperties().getTime() % 20L == 0L) {
            player.addExhaustion(0.05F);
        }
    }

    private void applyRandomEffect(ServerPlayerEntity player) {
        List<StatusEffect> effects = ChallengeCategoryManager.get().allChallengeEffects();
        if (effects.isEmpty()) return;

        StatusEffect selected = effects.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(effects.size()));
        Identifier effectId = Registries.STATUS_EFFECT.getId(selected);
        var entry = Registries.STATUS_EFFECT.getEntry(effectId);
        if (entry.isEmpty()) return;

        long time = player.getWorld().getLevelProperties().getTime();
        hangoverCurrentEffectId = effectId.toString();
        hangoverCurrentEffectEndTime = time + 600L;
        player.addStatusEffect(new StatusEffectInstance(entry.get(), 600, 0, false, false, true));
    }

    private void applyStoredHangoverEffect(ServerPlayerEntity player) {
        if (hangoverCurrentEffectId.isBlank()) {
            return;
        }

        long time = player.getWorld().getLevelProperties().getTime();
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
        ServerWorld world = player.getWorld();
        long elapsed = Math.max(0L, world.getLevelProperties().getTime() - startTime);
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

        // Extract each recipe output once. The previous implementation extracted
        // outputs again inside nested loops and performed a stream search by key
        // in the validation pass, resulting in cubic work for large recipe sets.
        java.util.Map<RegistryKey<Recipe<?>>, ItemStack> sourceOutputs = new java.util.HashMap<>(recipes.size());
        for (RecipeEntry<CraftingRecipe> recipe : recipes) {
            sourceOutputs.put(recipe.id(), ChaosRecipeOutputExtractor.extract(recipe.value()));
        }

        java.util.List<RecipeEntry<CraftingRecipe>> sourceOrder = new java.util.ArrayList<>(recipes);
        java.util.List<RecipeEntry<CraftingRecipe>> candidateOrder = new java.util.ArrayList<>(recipes);
        java.util.Collections.shuffle(sourceOrder, new Random(challengeSeed ^ 0x6A09E667F3BCC909L));
        java.util.Collections.shuffle(candidateOrder, new Random(challengeSeed));

        java.util.Map<RegistryKey<Recipe<?>>, ItemStack> result = new java.util.HashMap<>(recipes.size());
        java.util.List<RecipeEntry<CraftingRecipe>> available = new java.util.ArrayList<>(candidateOrder);

        for (RecipeEntry<CraftingRecipe> source : sourceOrder) {
            ItemStack sourceOutput = sourceOutputs.get(source.id());
            int chosenIndex = -1;
            for (int i = 0; i < available.size(); i++) {
                ItemStack candidateOutput = sourceOutputs.get(available.get(i).id());
                if (candidateOutput == null || candidateOutput.isEmpty()) continue;
                if (sourceOutput != null && !sourceOutput.isEmpty()
                        && candidateOutput.isOf(sourceOutput.getItem())) continue;
                chosenIndex = i;
                break;
            }

            if (chosenIndex < 0) {
                for (int i = 0; i < available.size(); i++) {
                    ItemStack candidateOutput = sourceOutputs.get(available.get(i).id());
                    if (candidateOutput != null && !candidateOutput.isEmpty()) {
                        chosenIndex = i;
                        break;
                    }
                }
            }

            ItemStack replacement = chosenIndex >= 0
                    ? sourceOutputs.get(available.remove(chosenIndex).id())
                    : ItemStack.EMPTY;
            result.put(source.id(), replacement == null ? ItemStack.EMPTY : replacement);
        }

        if (!result.isEmpty()) {
            java.util.List<java.util.Map.Entry<RegistryKey<Recipe<?>>, ItemStack>> entries =
                    new java.util.ArrayList<>(result.entrySet());
            for (int i = 0; i < entries.size(); i++) {
                java.util.Map.Entry<RegistryKey<Recipe<?>>, ItemStack> entry = entries.get(i);
                ItemStack sourceOutput = sourceOutputs.get(entry.getKey());
                if (sourceOutput == null || sourceOutput.isEmpty()
                        || !entry.getValue().isOf(sourceOutput.getItem())) {
                    continue;
                }

                for (int j = i + 1; j < entries.size(); j++) {
                    java.util.Map.Entry<RegistryKey<Recipe<?>>, ItemStack> other = entries.get(j);
                    ItemStack otherSourceOutput = sourceOutputs.get(other.getKey());
                    if (otherSourceOutput == null || otherSourceOutput.isEmpty()) continue;
                    if (!other.getValue().isEmpty() && other.getValue().isOf(sourceOutput.getItem())) continue;

                    if (!entry.getValue().isOf(otherSourceOutput.getItem())
                            && !other.getValue().isOf(sourceOutput.getItem())) {
                        entries.set(i, new java.util.AbstractMap.SimpleEntry<>(entry.getKey(), other.getValue()));
                        entries.set(j, new java.util.AbstractMap.SimpleEntry<>(other.getKey(), entry.getValue()));
                        break;
                    }
                }
            }
            result.clear();
            for (java.util.Map.Entry<RegistryKey<Recipe<?>>, ItemStack> entry : entries) {
                result.put(entry.getKey(), entry.getValue());
            }
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
        if (challenge != null) {
            applyScale(player);
            initializeActionStatBaselines(player);
        }
    }

    public void onPlayerRespawn(ServerPlayerEntity player) {
        if (modifierEnabled && modifier == ModifierType.HANGOVER) {
            applyStoredHangoverEffect(player);
        }
    }

    public void onDeath(ServerPlayerEntity player, DamageSource source) {
        if (!isActiveRunPlayer(player) || !timerStarted || diedThisTick) return;
        diedThisTick = true;
        deathCauseId = damageTypeId(source);
        if (source != null && ((source.getAttacker() != null
                && source.getAttacker().getType() == net.minecraft.entity.EntityType.LLAMA)
                || source.getSource() instanceof net.minecraft.entity.projectile.LlamaSpitEntity)) {
            deathCauseId = "llama_spit";
        }
        checkAndAdvance(player, false);
    }

    public void onPlayerEffectApplied(ServerPlayerEntity player, net.minecraft.entity.effect.StatusEffectInstance effect) {
        if (!isActiveRunPlayer(player) || !timerStarted || effect == null) return;
        playerAppliedEffectId = Registries.STATUS_EFFECT.getId(effect.getEffectType().value()).toString();
    }

    public void onPlayerReceivedDamage(ServerPlayerEntity player, DamageSource source) {
        if (!isActiveRunPlayer(player) || !timerStarted || source == null) return;
        receivedDamageId = receivedDamageId(source);
        receivedDamageEntityId = source.getAttacker() == null ? "" : Registries.ENTITY_TYPE.getId(source.getAttacker().getType()).toString();
    }

    private static String receivedDamageId(DamageSource source) {
        if (source == null) return "";
        // DragonFireballEntity creates an AreaEffectCloudEntity with DRAGON_BREATH
        // particles and an instant-damage effect. Vanilla applies that effect using
        // magic/indirect_magic damage types, so inspect the direct source entity.
        if (source.getSource() instanceof net.minecraft.entity.AreaEffectCloudEntity cloud
                && cloud.getParticleType().getType() == net.minecraft.particle.ParticleTypes.DRAGON_BREATH) {
            return "dragon_breath";
        }
        return damageTypeId(source);
    }

    private static String damageTypeId(DamageSource source) {
        if (source == null) return "";
        String name = source.getTypeRegistryEntry().getKey()
                .map(key -> key.getValue().getPath())
                .orElse(source.getName());
        return switch (name) {
            // Both direct flame damage and lava deaths use the same configured
            // "burned to death" objective variant.
            case "in_fire", "on_fire", "lava" -> "on_fire";
            case "falling_anvil", "anvil" -> "anvil";
            case "spit" -> "llama_spit";
            default -> name;
        };
    }

    public void onRuinedPortalRestored(ServerPlayerEntity player) {
        if (!isActiveRunPlayer(player) || !timerStarted) return;
        restoredRuinedPortalThisTick = true;
    }

    public void onCreeperExploded(ServerPlayerEntity player) {
        if (!isActiveRunPlayer(player) || !timerStarted) return;
        creeperExplodedByPlayerThisTick = true;
    }

    public void onCowMilked(ServerPlayerEntity player) {
        if (!isActiveRunPlayer(player) || !timerStarted) return;
        milkedCowThisTick = true;
    }

    public void onSoilTilled(ServerPlayerEntity player) {
        if (!isActiveRunPlayer(player) || !timerStarted) return;
        tilledSoilThisTick = true;
    }

    public void onEffectApplied(LivingEntity entity, net.minecraft.entity.effect.StatusEffectInstance effect) {
        if (!isActiveRunPlayerForWorld(entity) || effect == null) return;
        if (!(challenge.currentCondition() instanceof ObjectiveCondition objective)
                || objective.type() != ObjectiveCondition.Type.APPLY_EFFECT_TO_MOB) return;
        effectAppliedEntity = entity;
        appliedEffectId = Registries.STATUS_EFFECT.getId(effect.getEffectType().value()).toString();
    }

    public void onMobBurned(LivingEntity entity) {
        if (!isActiveRunPlayerForWorld(entity)) return;
        if (!(challenge.currentCondition() instanceof ObjectiveCondition objective)
                || objective.type() != ObjectiveCondition.Type.BURN_MOB) return;
        burnedEntity = entity;
    }

    public void onSkeletonShotMob(LivingEntity entity) {
        if (!isActiveRunPlayerForWorld(entity)) return;
        if (!(challenge.currentCondition() instanceof ObjectiveCondition objective)
                || objective.type() != ObjectiveCondition.Type.SKELETON_HIT_MOB) return;
        skeletonShotEntity = entity;
    }

    private boolean isActiveRunPlayerForWorld(Entity entity) {
        return entity != null && entity.getWorld().getServer() != null
                && challenge != null && runStarted && timerStarted && !completed && player != null;
    }

    public void onNaturalDungeonLoot(ServerPlayerEntity player, net.minecraft.util.Identifier lootTableId, java.util.List<ItemStack> items) {
        if (!isActiveRunPlayer(player) || !timerStarted || items == null || items.isEmpty()) return;
        naturalLootTableIdThisTick = lootTableId.toString();
        java.util.ArrayList<ItemStack> combined = new java.util.ArrayList<>(naturalLootItemsThisTick);
        combined.addAll(items.stream().filter(stack -> stack != null && !stack.isEmpty()).map(ItemStack::copy).toList());
        naturalLootItemsThisTick = java.util.List.copyOf(combined);

        // Evaluate the loot task immediately at the end of vanilla loot generation.
        // This avoids losing the one-tick event marker to tick-order differences when
        // opening a generated chest (pyramid/igloo/etc.). Other tasks are unaffected.
        if (challenge != null && challenge.currentCondition() instanceof ObjectiveCondition objective
                && objective.type() == ObjectiveCondition.Type.FIND_DUNGEON_LOOT) {
            checkAndAdvance(player, false);
        }
    }

    private boolean isActiveRunPlayer(ServerPlayerEntity candidate) {
        return candidate != null && player != null && player.equals(candidate.getUuid())
                && challenge != null && runStarted && !completed;
    }

    private void beginCurrentTask(ServerPlayerEntity player) {
        if (challenge == null || completed) return;
        Condition current = challenge.currentCondition();
        if (current instanceof com.lumenechallenge.challenge.condition.ObjectiveCondition objective) {
            objective.begin(player);
        }
    }

    public void onBlockBreak(ServerPlayerEntity player, BlockState blockState) {
        lastMinedBlock = blockState;
        if (!isActiveRunPlayer(player) || !timerStarted || blockState == null
                || !(challenge.currentCondition() instanceof ObjectiveCondition objective)
                || objective.type() != ObjectiveCondition.Type.HARVEST_CROP) return;
        String cropId = Registries.BLOCK.getId(blockState.getBlock()).toString();
        String expectedCrop = objective.stringData("crop");
        boolean sameCrop = cropId.equals(expectedCrop)
                || (expectedCrop.equals("minecraft:cave_vines") && cropId.equals("minecraft:cave_vines_plant"));
        if (sameCrop && isHarvestableCropState(blockState, cropId)) {
            harvestedCropId = expectedCrop;
        }
    }

    private static boolean isHarvestableCropState(BlockState state, String id) {
        String path = Identifier.of(id).getPath();
        if (path.equals("melon") || path.equals("pumpkin")) return true;
        // Sweet berry bushes are harvestable from age 2 (age 3 only gives a larger yield).
        if (path.equals("sweet_berry_bush")
                && state.get(net.minecraft.block.SweetBerryBushBlock.AGE) > 1) return true;
        // Glow berries are harvested from cave vines with the berries property enabled.
        if ((path.equals("cave_vines") || path.equals("cave_vines_plant"))
                && state.getBlock() instanceof net.minecraft.block.CaveVines
                && net.minecraft.block.CaveVines.hasBerries(state)) return true;
        for (var property : state.getProperties()) {
            if (property instanceof net.minecraft.state.property.IntProperty age) {
                int current = state.get(age);
                int max = age.getValues().stream().mapToInt(Integer::intValue).max().orElse(current);
                if (current == max) return true;
            }
            if (property.getName().equals("berries") && Boolean.TRUE.equals(state.get((net.minecraft.state.property.Property) property))) {
                return true;
            }
        }
        return false;
    }

    public void onBlockPlace(ServerPlayerEntity player, BlockPos placedPos, BlockState blockState) {
        lastPlacedBlock = blockState;
        if (!isActiveRunPlayer(player) || !timerStarted || blockState == null
                || !(challenge.currentCondition() instanceof ObjectiveCondition objective)
                || objective.type() != ObjectiveCondition.Type.GROW_TREE) return;
        String id = Registries.BLOCK.getId(blockState.getBlock()).toString();
        String path = Identifier.of(id).getPath();
        if (path.endsWith("_sapling") || path.equals("mangrove_propagule")) {
            if (plantedSaplings == null) plantedSaplings = new java.util.HashMap<>();
            plantedSaplings.put((placedPos == null ? player.getBlockPos() : placedPos).asLong(), id);
        }
    }

    public void onSaplingGrown(ServerWorld world, BlockPos pos, BlockState originalState) {
        if (world == null || pos == null || originalState == null || plantedSaplings == null
                || !(challenge != null && challenge.currentCondition() instanceof ObjectiveCondition objective)
                || objective.type() != ObjectiveCondition.Type.GROW_TREE) return;
        String planted = plantedSaplings.get(pos.asLong());
        if (planted == null) return;
        String grownId = Registries.BLOCK.getId(originalState.getBlock()).toString();
        if (planted.equals(grownId) && planted.equals(objective.stringData("sapling"))
                && !world.getBlockState(pos).isOf(originalState.getBlock())) {
            grownSaplingId = planted;
            plantedSaplings.remove(pos.asLong());
        }
    }

    public void onVillagerLevelUp(net.minecraft.entity.passive.VillagerEntity villager) {
        if (villager == null || !isActiveRunPlayerForWorld(villager)
                || !(challenge.currentCondition() instanceof ObjectiveCondition objective)
                || objective.type() != ObjectiveCondition.Type.VILLAGER_LEVEL_UP) return;
        var data = villager.getVillagerData();
        villagerProfessionId = Registries.VILLAGER_PROFESSION.getId(data.profession().value()).getPath();
        villagerLevel = data.level();
    }

    public void onProjectileHitTarget(ServerPlayerEntity player, Entity projectile, String targetKind, String targetId) {
        if (!isActiveRunPlayer(player) || !timerStarted || projectile == null || targetId == null
                || !(challenge.currentCondition() instanceof ObjectiveCondition objective)
                || objective.type() != ObjectiveCondition.Type.PROJECTILE_HIT_TARGET) return;
        hitProjectileId = Registries.ENTITY_TYPE.getId(projectile.getType()).toString();
        projectileTargetKind = targetKind == null ? "" : targetKind;
        projectileTargetId = targetId;
    }

    public void onCropHarvest(ServerPlayerEntity player, BlockState state) {
        onBlockBreak(player, state);
    }

    /** Records berry crops harvested by right-click instead of block breaking. */
    public void onBerryCropHarvest(ServerPlayerEntity player, BlockState state) {
        if (!isActiveRunPlayer(player) || !timerStarted || state == null
                || !(challenge.currentCondition() instanceof ObjectiveCondition objective)
                || objective.type() != ObjectiveCondition.Type.HARVEST_CROP) return;

        String cropId = Registries.BLOCK.getId(state.getBlock()).toString();
        String expectedCrop = objective.stringData("crop");
        boolean sameCrop = cropId.equals(expectedCrop)
                || (expectedCrop.equals("minecraft:cave_vines") && cropId.equals("minecraft:cave_vines_plant"));
        if (!sameCrop) return;

        // Vanilla allows berry harvesting as soon as the bush reaches age 2;
        // age 3 is not required (age 3 only yields a larger crop).
        boolean ripeSweetBerries = state.getBlock() instanceof net.minecraft.block.SweetBerryBushBlock
                && state.get(net.minecraft.block.SweetBerryBushBlock.AGE) > 1;
        boolean ripeGlowBerries = state.getBlock() instanceof net.minecraft.block.CaveVines
                && net.minecraft.block.CaveVines.hasBerries(state);
        if (ripeSweetBerries || ripeGlowBerries) {
            harvestedCropId = expectedCrop;
        }
    }


    public void onItemStored(ServerPlayerEntity player, String itemId, String storage) {
        if (!isActiveRunPlayer(player) || !timerStarted || itemId == null || storage == null
                || !(challenge.currentCondition() instanceof ObjectiveCondition objective)
                || objective.type() != ObjectiveCondition.Type.STORE_ITEM) return;
        storedItemId = itemId;
        storageId = storage;
    }

    public void onMobEnteredBoat(LivingEntity mob) {
        if (mob == null || !isActiveRunPlayerForWorld(mob) || !timerStarted
                || !(challenge.currentCondition() instanceof ObjectiveCondition objective)
                || objective.type() != ObjectiveCondition.Type.PUT_MOB_IN_BOAT) return;
        MinecraftServer server = mob.getWorld().getServer();
        ServerPlayerEntity activePlayer = server == null || player == null ? null : server.getPlayerManager().getPlayer(player);
        if (activePlayer == null || activePlayer.getWorld() != mob.getWorld()
                || activePlayer.squaredDistanceTo(mob) > 100.0D) return;
        mobInBoatId = Registries.ENTITY_TYPE.getId(mob.getType()).toString();
    }

    public void onPlayerRodeMob(ServerPlayerEntity rider, Entity mount) {
        if (!isActiveRunPlayer(rider) || !timerStarted || mount == null
                || !(challenge.currentCondition() instanceof ObjectiveCondition objective)
                || objective.type() != ObjectiveCondition.Type.RIDE_MOB) return;
        riddenMobId = Registries.ENTITY_TYPE.getId(mount.getType()).toString();
    }

    public void setCameraPerspective(ServerPlayerEntity candidate, int perspective) {
        if (candidate == null || (player != null && !player.equals(candidate.getUuid()))) return;
        cameraPerspective = Math.max(0, Math.min(2, perspective));
    }

    public void onKill(ServerPlayerEntity player, LivingEntity victim) {
        lastKilledEntity = victim;
    }

    public void onCraft(ServerPlayerEntity player, ItemStack stack) {
        lastCraftedItem = stack.copy();
    }

    public void onConsume(ServerPlayerEntity player, ItemStack stack, boolean underwater) {
        if (stack == null || stack.isEmpty()) return;
        lastConsumedItem = stack.copy();
        lastConsumedUnderwater = underwater;
    }

    public void onItemRenamed(ServerPlayerEntity player, ItemStack stack) {
        if (!isActiveRunPlayer(player) || !timerStarted || stack == null || stack.isEmpty() || challenge == null) return;
        if (challenge.currentCondition() instanceof ObjectiveCondition objective
                && objective.matchesRenamedItem(buildContext(player), stack)) {
            advance(player.getWorld());
        }
    }

    public void onEnchant(ServerPlayerEntity player, ItemStack stack) {
        if (!isActiveRunPlayer(player) || !timerStarted || stack == null || stack.isEmpty()
                || !stack.hasEnchantments()) return;
        if (challenge == null || !(challenge.currentCondition() instanceof ObjectiveCondition objective)
                || (objective.type() != ObjectiveCondition.Type.ENCHANT_ITEM
                    && objective.type() != ObjectiveCondition.Type.ENCHANT_ITEM_WITH)) {
            return;
        }
        // The table stat hook runs after vanilla has applied every generated enchantment;
        // the anvil hook runs while the completed output is still in the output slot.
        lastEnchantedItem = stack.copy();
        // Evaluate once, immediately, while the final output stack is available. Clear the
        // marker afterwards so a count-2 objective cannot count the same enchant again on tick.
        checkAndAdvance(player, false);
        lastEnchantedItem = null;
    }

    public void onItemBroken(ServerPlayerEntity player, ItemStack stack) {
        lastBrokenItem = stack == null ? null : stack.copy();
    }

    public void onSmelted(ServerPlayerEntity player, ItemStack stack) {
        lastSmeltedItem = stack == null ? null : stack.copy();
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
        // The PlayerEntity stat hook captures the actual final output stack after vanilla
        // applies all enchantments. Keep the statistic baseline in sync as a fallback only;
        // never synthesize an EMPTY stack, because it cannot represent a successful enchant.
        if (enchantedItemStat > lastEnchantItemStat) {
            lastEnchantItemStat = enchantedItemStat;
        }
    }

    private ChallengeContext buildContext(ServerPlayerEntity player) {
        ServerWorld world = player.getWorld();
        BlockPos pos = player.getBlockPos();
        RegistryEntry<Biome> biome = world.getBiome(pos);
        return new ChallengeContext(
                player,
                world,
                pos,
                player.getInventory(),
                player.getMainHandStack(),
                world.getLevelProperties().getTime(),
                biome,
                world.isRaining(),
                world.isThundering(),
                (int) Math.max(0L, world.getLevelProperties().getTime() - lastMovementTime),
                diedThisTick,
                lastMinedBlock,
                lastPlacedBlock,
                lastKilledEntity,
                lastCraftedItem,
                lastConsumedItem,
                lastConsumedUnderwater,
                lastEnchantedItem,
                lastBrokenItem,
                lastSmeltedItem,
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
                tradedThisTick,
                restoredRuinedPortalThisTick,
                creeperExplodedByPlayerThisTick,
                milkedCowThisTick,
                tilledSoilThisTick,
                naturalLootItemsThisTick,
                naturalLootTableIdThisTick,
                effectAppliedEntity,
                appliedEffectId,
                playerAppliedEffectId,
                deathCauseId,
                receivedDamageId,
                receivedDamageEntityId,
                burnedEntity,
                skeletonShotEntity,
                villagerProfessionId,
                villagerLevel,
                hitProjectileId,
                projectileTargetKind,
                projectileTargetId,
                grownSaplingId,
                harvestedCropId,
                storedItemId,
                storageId,
                mobInBoatId,
                riddenMobId,
                cameraPerspective
        );
    }

    private void checkAndAdvance(ServerPlayerEntity player, boolean force) {
        if (challenge == null || completed) return;
        Condition current = challenge.currentCondition();
        if (current == null) return;

        // Partial objective progress is stored inside the active condition's NBT.
        // PersistentStateManager only writes this state after markDirty(), so mark
        // it whenever evaluating the condition changes serialized data, not only
        // when the entire task is completed. This preserves counters such as 15/30
        // jumps and the movement-distance anchors/remainders across world reloads.
        NbtCompound conditionBefore = current.toNbt();
        boolean conditionCompleted = force || current.check(buildContext(player));
        if (!conditionBefore.equals(current.toNbt())) {
            markDirty();
        }

        if (conditionCompleted) {
            advance(player.getWorld());
        }
    }

    private void advance(ServerWorld world) {
        if (plantedSaplings != null) plantedSaplings.clear();
        progress++;
        if (challenge != null) challenge.setCompletedSteps(progress);
        if (challenge != null && progress >= challenge.conditions().size()) {
            completed = true;
            finishTime = world.getLevelProperties().getTime();
            // Exhaustion changes max-health only; restore the player's normal max health at run end.
            ServerPlayerEntity owner = world.getServer().getPlayerManager().getPlayer(player);
            if (owner != null && modifier == ModifierType.EXHAUSTION) {
                EntityAttributeInstance maxHealth = owner.getAttributeInstance(EntityAttributes.MAX_HEALTH);
                if (maxHealth != null && !Double.isNaN(appliedMaxHealth)) {
                    maxHealth.setBaseValue(appliedMaxHealth);
                    if (owner.getHealth() > maxHealth.getValue()) owner.setHealth((float) maxHealth.getValue());
                }
                appliedMaxHealth = Double.NaN;
            }
        } else {
            resetMarkers();
            // The next task's "new advancement" and movement baselines start now, not when the run was generated.
            for (ServerPlayerEntity online : world.getServer().getPlayerManager().getPlayerList()) {
                if (player != null && player.equals(online.getUuid())) {
                    beginCurrentTask(online);
                    break;
                }
            }
        }
        markDirty();
    }

    public Challenge challenge() { return challenge; }
    public int completedSteps() { return progress; }
    public boolean completed() { return completed; }
    public boolean timerStarted() { return timerStarted; }
    public boolean runStarted() { return runStarted; }
    public boolean isCountdownActive(MinecraftServer server) {
        if (!runStarted || timerStarted || countdownEndTick < 0L || server == null) return false;
        ServerWorld world = server.getWorld(World.OVERWORLD);
        return world != null && world.getLevelProperties().getTime() < countdownEndTick;
    }
    public long countdownRemainingTicks(MinecraftServer server) {
        if (!isCountdownActive(server)) return 0L;
        ServerWorld world = server.getWorld(World.OVERWORLD);
        return world == null ? 0L : Math.max(0L, countdownEndTick - world.getLevelProperties().getTime());
    }
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

    public boolean rerollCurrentTask(ServerPlayerEntity player) {
        if (challenge == null || completed || rerollsRemaining <= 0) {
            return false;
        }
        Condition current = challenge.currentCondition();
        ChallengeTaskDefinition definition = challenge.currentTaskDefinition();
        if (!(current instanceof ObjectiveCondition objectiveCondition) || definition == null) {
            return false;
        }
        Set<String> usedVariants = new HashSet<>();
        List<Condition> conditions = challenge.conditions();
        for (Condition usedCondition : conditions) {
            if (usedCondition instanceof ObjectiveCondition usedObjective) {
                usedVariants.add(usedObjective.variantKey());
            }
        }
        ChallengeGenerator.GeneratedTask replacement = ChallengeGenerator.rerollTask(
                player,
                challengeSeed,
                progress,
                runDifficulty,
                objectiveCondition.assignedTierName().isBlank() ? definition.difficulty() : objectiveCondition.assignedTierName(),
                maxRerolls - rerollsRemaining + 1,
                usedVariants
        );
        if (replacement == null) return false;
        challenge = challenge.withReplacement(progress, replacement.condition(), replacement.definition());
        rerollsRemaining = Math.max(0, rerollsRemaining - 1);
        beginCurrentTask(player);
        resetMarkers();
        markDirty();
        return true;
    }

    public boolean replaceCurrentTaskForDeveloper(ServerPlayerEntity player, ObjectiveCondition condition,
                                                 ChallengeTaskDefinition definition) {
        if (!isActiveRunPlayer(player) || challenge == null || completed || condition == null || definition == null) {
            return false;
        }
        challenge = challenge.withReplacement(progress, condition, definition);
        resetMarkers();
        if (timerStarted) beginCurrentTask(player);
        markDirty();
        return true;
    }

    public ChallengeConfig challengeConfig() {
        return ChallengeConfigManager.get();
    }

    public RunDifficulty runDifficulty() {
        return runDifficulty;
    }

    public int maxRerolls() {
        return maxRerolls;
    }

    /** Dynamic reroll budget based on the task count selected for the run. */
    public static int calculateMaxRerolls(int taskCount) {
        int safeCount = Math.max(0, taskCount);
        double raw = safeCount / (2.8D + 0.038D * safeCount);
        return Math.max(1, (int) Math.round(raw));
    }

    public int rerollsRemaining() {
        return rerollsRemaining;
    }

    public int rerollsUsed() {
        return Math.max(0, maxRerolls - rerollsRemaining);
    }


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
        return Math.max(0L, world.getLevelProperties().getTime() - startTime);
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
        nbt.putBoolean("runStarted", runStarted);
        nbt.putLong("countdownEndTick", countdownEndTick);
        nbt.putLong("challengeSeed", challengeSeed);
        nbt.putString("challengeSeedText", challengeSeedText);
        nbt.putBoolean("seedSelected", seedSelected);
        nbt.putBoolean("modifierEnabled", modifierEnabled);
        nbt.putString("modifier", modifier.name());
        nbt.putString("runDifficulty", runDifficulty.name());
        nbt.putInt("challengeCount", challengeCount);
        nbt.putInt("maxRerolls", maxRerolls);
        nbt.putInt("rerollsRemaining", rerollsRemaining);
        nbt.putDouble("distanceRemainder", distanceRemainder);
        nbt.putBoolean("trackingDistance", trackingDistance);
        nbt.putDouble("lastX", lastX);
        nbt.putDouble("lastY", lastY);
        nbt.putDouble("lastZ", lastZ);
        nbt.putDouble("appliedMovementSpeed", appliedMovementSpeed);
        nbt.putDouble("appliedMaxHealth", appliedMaxHealth);
        nbt.putLong("wearNextDamageTime", wearNextDamageTime);
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
        state.runStarted = nbt.getBoolean("runStarted", false);
        state.countdownEndTick = nbt.getLong("countdownEndTick", -1L);
        if (!state.runStarted && state.challenge != null) {
            state.runStarted = true;
            if (state.countdownEndTick < 0L) state.countdownEndTick = state.startTime;
        }
        state.challengeSeed = nbt.getLong("challengeSeed", 0L);
        state.challengeSeedText = nbt.getString("challengeSeedText", Long.toString(state.challengeSeed));
        if (state.challengeSeedText.isBlank()) state.challengeSeedText = Long.toString(state.challengeSeed);
        state.seedSelected = nbt.getBoolean("seedSelected", false);
        state.modifierEnabled = nbt.getBoolean("modifierEnabled", false);
        state.modifier = ModifierType.fromId(nbt.getString("modifier", ModifierType.NONE.name()));
        state.runDifficulty = RunDifficulty.fromId(nbt.getString("runDifficulty", RunDifficulty.NORMAL.name()));
        state.challengeCount = nbt.getInt("challengeCount", 10);
        state.maxRerolls = nbt.getInt("maxRerolls", calculateMaxRerolls(state.challengeCount));
        state.rerollsRemaining = nbt.getInt("rerollsRemaining", state.maxRerolls);
        state.distanceRemainder = nbt.getDouble("distanceRemainder", 0.0D);
        state.trackingDistance = nbt.getBoolean("trackingDistance", false);
        state.lastX = nbt.getDouble("lastX", Double.NaN);
        state.lastY = nbt.getDouble("lastY", Double.NaN);
        state.lastZ = nbt.getDouble("lastZ", Double.NaN);
        state.appliedMovementSpeed = nbt.getDouble("appliedMovementSpeed", Double.NaN);
        state.appliedMaxHealth = nbt.getDouble("appliedMaxHealth", Double.NaN);
        state.wearNextDamageTime = nbt.getLong("wearNextDamageTime", -1L);
        state.hangoverNextEffectTime = nbt.getLong("hangoverNextEffectTime", -1L);
        state.hangoverCurrentEffectId = nbt.getString("hangoverCurrentEffectId", "");
        state.hangoverCurrentEffectEndTime = nbt.getLong("hangoverCurrentEffectEndTime", -1L);
        state.cataclysmActiveUntil = nbt.getLong("cataclysmActiveUntil", -1L);
        state.cataclysmNextStart = nbt.getLong("cataclysmNextStart", -1L);
        if (state.rerollsRemaining < 0 || state.rerollsRemaining > state.maxRerolls) {
            state.rerollsRemaining = Math.max(0, Math.min(state.maxRerolls, state.rerollsRemaining));
        }
        if (state.challenge != null) state.challenge.setCompletedSteps(state.progress);
        return state;
    }
}
