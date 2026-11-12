package com.lumenechallenge.challenge;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.SweetBerryBushBlock;
import net.minecraft.block.CaveVines;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import com.lumenechallenge.util.WorldMarkerUtil;

import java.util.List;
import java.util.Random;

public final class ChallengeHooks {
    private ChallengeHooks() {
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            NaturalLootTracker.clearServer(server);
            ChallengeState.clearServerCache(server);
            WorldMarkerUtil.clearServerCache(server);
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (!WorldMarkerUtil.isMarked(server)) return;
            ChallengeState state = ChallengeState.getServerState(server);
            state.onPlayerJoin(handler.player);
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (!WorldMarkerUtil.isMarked(newPlayer.getWorld().getServer())) return;
            ChallengeState state = ChallengeState.getServerState(newPlayer.getWorld().getServer());
            state.onPlayerRespawn(newPlayer);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!WorldMarkerUtil.isMarked(server)) return;
            ChallengeState state = ChallengeState.getServerState(server);
            if (state.challenge() == null || state.playerId() == null) {
                return;
            }
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (state.playerId().equals(player.getUuid())) {
                    state.tick(player);
                    break;
                }
            }
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof ServerPlayerEntity player) {
                onPlayerDeath(player, damageSource);
            }
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient() || !(player instanceof ServerPlayerEntity serverPlayer)) {
                return net.minecraft.util.ActionResult.PASS;
            }
            BlockState state = world.getBlockState(hitResult.getBlockPos());
            // Bone meal is processed after UseBlockCallback. Without this guard, a ripe
            // age-2 sweet berry bush is counted before bone meal advances it to age 3.
            boolean boneMealOnAgeTwoSweetBerryBush = state.getBlock() instanceof SweetBerryBushBlock
                    && state.get(SweetBerryBushBlock.AGE) == 2
                    && player.getStackInHand(hand).isOf(Items.BONE_MEAL);
            if (boneMealOnAgeTwoSweetBerryBush) {
                return net.minecraft.util.ActionResult.PASS;
            }

            boolean ripeSweetBerryBush = state.getBlock() instanceof SweetBerryBushBlock
                    && state.get(SweetBerryBushBlock.AGE) > 1;
            boolean ripeGlowBerryVines = state.getBlock() instanceof CaveVines
                    && CaveVines.hasBerries(state);
            if (ripeSweetBerryBush || ripeGlowBerryVines) {
                onBerryCropHarvest(serverPlayer, state);
            }
            return net.minecraft.util.ActionResult.PASS;
        });

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, blockState, blockEntity) -> {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                if (!WorldMarkerUtil.isMarked(serverPlayer.getWorld().getServer())) return;
                ChallengeState challengeState = ChallengeState.getServerState(serverPlayer.getWorld().getServer());
                challengeState.onBlockBreak(serverPlayer, blockState);
            }
        });

        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killedEntity) -> {
            if (entity instanceof ServerPlayerEntity player) {
                if (!WorldMarkerUtil.isMarked(player.getWorld().getServer())) return;
                ChallengeState state = ChallengeState.getServerState(player.getWorld().getServer());
                state.onKill(player, killedEntity);
            }
        });
    }

    public static void onCraftedByPlayer(ServerPlayerEntity player, ItemStack stack) {
        if (!WorldMarkerUtil.isMarked(player.getWorld().getServer())) return;
        ChallengeState state = ChallengeState.getServerState(player.getWorld().getServer());
        state.onCraft(player, stack);
    }

    public static ItemStack createChaosCraftReplacement(ServerPlayerEntity player, ItemStack crafted) {
        if (!WorldMarkerUtil.isMarked(player.getWorld().getServer())) return ItemStack.EMPTY;
        if (crafted == null || crafted.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ChallengeState state = ChallengeState.getServerState(player.getWorld().getServer());
        if (!state.modifierEnabled() || state.modifier() != ModifierType.CHAOS) {
            return ItemStack.EMPTY;
        }

        Item replacement = pickChaosCraftReplacement(state, crafted);
        if (replacement == null) {
            return ItemStack.EMPTY;
        }

        return new ItemStack(replacement, Math.max(1, crafted.getCount()));
    }

    private static Item pickChaosCraftReplacement(ChallengeState state, ItemStack crafted) {
        List<Item> items = ChallengeCategoryManager.get().allChallengeItems().stream()
                .filter(ChallengeHooks::isSurvivalReplacementItem)
                .filter(item -> !crafted.isOf(item))
                .toList();

        if (items.isEmpty()) {
            return null;
        }

        long seed = state.challengeSeed() ^ Registries.ITEM.getId(crafted.getItem()).toString().hashCode();
        Random random = new Random(seed);
        return items.get(Math.floorMod(random.nextInt(), items.size()));
    }

    private static boolean isSurvivalReplacementItem(Item item) {
        String path = Registries.ITEM.getId(item).getPath();
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
                && !path.contains("debug_stick")
                && !path.contains("bundle");
    }

    public static void onItemStored(ServerPlayerEntity player, ItemStack stack, String storageId) {
        if (player == null || stack == null || stack.isEmpty() || storageId == null) return;
        var server = player.getWorld().getServer();
        if (server == null || !WorldMarkerUtil.isMarked(server)) return;
        ChallengeState.getServerState(server).onItemStored(player, Registries.ITEM.getId(stack.getItem()).toString(), storageId);
    }

    public static void onEntityStartedRiding(net.minecraft.entity.Entity passenger, net.minecraft.entity.Entity vehicle) {
        if (passenger == null || vehicle == null || passenger.getWorld().getServer() == null) return;
        var server = passenger.getWorld().getServer();
        if (!WorldMarkerUtil.isMarked(server)) return;
        ChallengeState state = ChallengeState.getServerState(server);
        if (passenger instanceof ServerPlayerEntity player) {
            state.onPlayerRodeMob(player, vehicle);
        } else if (vehicle instanceof net.minecraft.entity.vehicle.AbstractBoatEntity
                && passenger instanceof LivingEntity living) {
            state.onMobEnteredBoat(living);
        }
    }

    public static void onPerspectiveChanged(ServerPlayerEntity player, int perspective) {
        if (player == null || player.getWorld().getServer() == null) return;
        var server = player.getWorld().getServer();
        if (!WorldMarkerUtil.isMarked(server)) return;
        ChallengeState.getServerState(server).setCameraPerspective(player, perspective);
    }

    public static void onVillagerLevelUp(net.minecraft.entity.passive.VillagerEntity villager) {
        if (villager == null || villager.getWorld().getServer() == null) return;
        var server = villager.getWorld().getServer();
        if (!WorldMarkerUtil.isMarked(server)) return;
        ChallengeState.getServerState(server).onVillagerLevelUp(villager);
    }

    public static void onProjectileHitTarget(ServerPlayerEntity player, net.minecraft.entity.Entity projectile, String targetKind, String targetId) {
        if (player == null || projectile == null || player.getWorld().getServer() == null) return;
        var server = player.getWorld().getServer();
        if (!WorldMarkerUtil.isMarked(server)) return;
        ChallengeState.getServerState(server).onProjectileHitTarget(player, projectile, targetKind, targetId);
    }

    public static void onSaplingGrown(net.minecraft.server.world.ServerWorld world, net.minecraft.util.math.BlockPos pos, BlockState originalState) {
        if (world == null || world.getServer() == null || !WorldMarkerUtil.isMarked(world.getServer())) return;
        ChallengeState.getServerState(world.getServer()).onSaplingGrown(world, pos, originalState);
    }

    public static void onBerryCropHarvest(ServerPlayerEntity player, BlockState blockState) {
        if (player == null || blockState == null || player.getWorld().getServer() == null) return;
        var server = player.getWorld().getServer();
        if (!WorldMarkerUtil.isMarked(server)) return;
        ChallengeState.getServerState(server).onBerryCropHarvest(player, blockState);
    }

    public static void onBlockPlaced(ServerPlayerEntity player, net.minecraft.util.math.BlockPos pos, BlockState blockState) {
        if (!WorldMarkerUtil.isMarked(player.getWorld().getServer())) return;
        ChallengeState state = ChallengeState.getServerState(player.getWorld().getServer());
        state.onBlockPlace(player, pos, blockState);
    }

    public static void onItemConsumed(ServerPlayerEntity player, ItemStack stack, boolean underwater) {
        if (player == null || stack == null || stack.isEmpty() || player.getWorld().getServer() == null) return;
        if (!WorldMarkerUtil.isMarked(player.getWorld().getServer())) return;
        ChallengeState state = ChallengeState.getServerState(player.getWorld().getServer());
        state.onConsume(player, stack, underwater);
    }

    public static void onItemBroken(ServerPlayerEntity player, ItemStack stack) {
        if (!WorldMarkerUtil.isMarked(player.getWorld().getServer())) return;
        ChallengeState state = ChallengeState.getServerState(player.getWorld().getServer());
        state.onItemBroken(player, stack);
    }

    public static void onSmelted(ServerPlayerEntity player, ItemStack stack) {
        if (!WorldMarkerUtil.isMarked(player.getWorld().getServer())) return;
        ChallengeState state = ChallengeState.getServerState(player.getWorld().getServer());
        state.onSmelted(player, stack);
    }

    public static void onItemRenamed(ServerPlayerEntity player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty() || player.getWorld().getServer() == null) return;
        if (!WorldMarkerUtil.isMarked(player.getWorld().getServer())) return;
        ChallengeState state = ChallengeState.getServerState(player.getWorld().getServer());
        state.onItemRenamed(player, stack);
    }

    public static void onItemEnchanted(ServerPlayerEntity player, ItemStack stack) {
        if (!WorldMarkerUtil.isMarked(player.getWorld().getServer())) return;
        ChallengeState state = ChallengeState.getServerState(player.getWorld().getServer());
        state.onEnchant(player, stack);
    }

    public static void onPlayerSlept(ServerPlayerEntity player) {
        if (!WorldMarkerUtil.isMarked(player.getWorld().getServer())) return;
        ChallengeState state = ChallengeState.getServerState(player.getWorld().getServer());
        state.onSleep(player);
    }

    public static void onPlayerDeath(ServerPlayerEntity player, net.minecraft.entity.damage.DamageSource source) {
        if (!WorldMarkerUtil.isMarked(player.getWorld().getServer())) return;
        ChallengeState state = ChallengeState.getServerState(player.getWorld().getServer());
        state.onDeath(player, source);
    }

    public static void onPlayerEffectApplied(ServerPlayerEntity player, net.minecraft.entity.effect.StatusEffectInstance effect) {
        if (player == null || player.getWorld().getServer() == null || effect == null) return;
        if (!WorldMarkerUtil.isMarked(player.getWorld().getServer())) return;
        ChallengeState.getServerState(player.getWorld().getServer()).onPlayerEffectApplied(player, effect);
    }

    public static void onPlayerReceivedDamage(ServerPlayerEntity player, net.minecraft.entity.damage.DamageSource source) {
        if (player == null || player.getWorld().getServer() == null || source == null) return;
        if (!WorldMarkerUtil.isMarked(player.getWorld().getServer())) return;
        ChallengeState.getServerState(player.getWorld().getServer()).onPlayerReceivedDamage(player, source);
    }

    public static void onDamage(ServerPlayerEntity player, LivingEntity damaged, net.minecraft.entity.damage.DamageSource source, float amount) {
        if (!WorldMarkerUtil.isMarked(player.getWorld().getServer())) return;
        ChallengeState state = ChallengeState.getServerState(player.getWorld().getServer());
        state.onDamage(player, damaged, source, amount);
    }

    public static void onFishing(ServerPlayerEntity player, boolean caughtItem, boolean selfCatch) {
        if (!WorldMarkerUtil.isMarked(player.getWorld().getServer())) return;
        ChallengeState state = ChallengeState.getServerState(player.getWorld().getServer());
        state.onFish(player, caughtItem, selfCatch);
    }

    public static void onPearlTeleport(ServerPlayerEntity player) {
        if (!WorldMarkerUtil.isMarked(player.getWorld().getServer())) return;
        ChallengeState state = ChallengeState.getServerState(player.getWorld().getServer());
        state.onPearlTeleport(player);
    }

    public static void onTame(ServerPlayerEntity player, LivingEntity entity) {
        if (!WorldMarkerUtil.isMarked(player.getWorld().getServer())) return;
        ChallengeState state = ChallengeState.getServerState(player.getWorld().getServer());
        state.onTame(player, entity);
    }

    public static void onTrade(ServerPlayerEntity player) {
        if (!WorldMarkerUtil.isMarked(player.getWorld().getServer())) return;
        ChallengeState state = ChallengeState.getServerState(player.getWorld().getServer());
        state.onTrade(player);
    }

    public static void onProjectileHit(ServerPlayerEntity player, LivingEntity victim, double distance) {
        if (!WorldMarkerUtil.isMarked(player.getWorld().getServer())) return;
        ChallengeState state = ChallengeState.getServerState(player.getWorld().getServer());
        state.onProjectileHit(player, victim, distance);
    }

    public static void onEffectApplied(LivingEntity entity, net.minecraft.entity.effect.StatusEffectInstance effect) {
        if (entity == null || entity instanceof net.minecraft.entity.player.PlayerEntity
                || entity.getWorld().getServer() == null) return;
        var server = entity.getWorld().getServer();
        if (!WorldMarkerUtil.isMarked(server)) return;
        ChallengeState.getServerState(server).onEffectApplied(entity, effect);
    }

    public static void onMobBurned(LivingEntity entity) {
        if (entity == null || entity instanceof net.minecraft.entity.player.PlayerEntity
                || entity.getWorld().getServer() == null) return;
        var server = entity.getWorld().getServer();
        if (!WorldMarkerUtil.isMarked(server)) return;
        ChallengeState.getServerState(server).onMobBurned(entity);
    }

    public static void onSkeletonShotMob(ServerPlayerEntity player, LivingEntity victim) {
        if (player == null || victim == null || player.getWorld().getServer() == null) return;
        var server = player.getWorld().getServer();
        if (!WorldMarkerUtil.isMarked(server)) return;
        ChallengeState.getServerState(server).onSkeletonShotMob(victim);
    }

    public static void onCreeperExploded(ServerPlayerEntity player) {
        if (player == null || player.getWorld().getServer() == null) return;
        if (!WorldMarkerUtil.isMarked(player.getWorld().getServer())) return;
        ChallengeState.getServerState(player.getWorld().getServer()).onCreeperExploded(player);
    }

    public static void onCowMilked(ServerPlayerEntity player) {
        if (player == null || player.getWorld().getServer() == null) return;
        if (!WorldMarkerUtil.isMarked(player.getWorld().getServer())) return;
        ChallengeState.getServerState(player.getWorld().getServer()).onCowMilked(player);
    }

    public static void onSoilTilled(ServerPlayerEntity player) {
        if (player == null || player.getWorld().getServer() == null) return;
        if (!WorldMarkerUtil.isMarked(player.getWorld().getServer())) return;
        ChallengeState.getServerState(player.getWorld().getServer()).onSoilTilled(player);
    }

    public static void onRuinedPortalRestored(ServerPlayerEntity player) {
        if (player == null || player.getWorld().getServer() == null) return;
        if (!WorldMarkerUtil.isMarked(player.getWorld().getServer())) return;
        ChallengeState.getServerState(player.getWorld().getServer()).onRuinedPortalRestored(player);
    }

    public static void onNaturalDungeonLootGenerated(ServerPlayerEntity player, Identifier lootTableId, List<ItemStack> generatedItems) {
        if (player == null || lootTableId == null || player.getWorld().getServer() == null) return;
        if (!WorldMarkerUtil.isMarked(player.getWorld().getServer())) return;
        if (!"minecraft".equals(lootTableId.getNamespace())) return;
        String path = lootTableId.getPath();
        // Only vanilla structure/chest loot is accepted; bonus chests and non-chest loot are excluded.
        if (!path.startsWith("chests/") || path.equals("chests/spawn_bonus_chest")) return;
        ChallengeState state = ChallengeState.getServerState(player.getWorld().getServer());
        state.onNaturalDungeonLoot(player, lootTableId, generatedItems);
    }
}
