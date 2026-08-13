package com.lumenechallenge.challenge;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import com.lumenechallenge.util.WorldMarkerUtil;

import java.util.List;
import java.util.Random;

public final class ChallengeHooks {
    private ChallengeHooks() {
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (!WorldMarkerUtil.isMarked(server)) return;
            ChallengeState state = ChallengeState.getServerState(server);
            state.onPlayerJoin(handler.player);
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (!WorldMarkerUtil.isMarked(newPlayer.getServer())) return;
            ChallengeState state = ChallengeState.getServerState(newPlayer.getServer());
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
                onPlayerDeath(player);
            }
        });

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, blockState, blockEntity) -> {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                if (!WorldMarkerUtil.isMarked(serverPlayer.getServer())) return;
                ChallengeState challengeState = ChallengeState.getServerState(serverPlayer.getServer());
                challengeState.onBlockBreak(serverPlayer, blockState);
            }
        });

        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killedEntity) -> {
            if (entity instanceof ServerPlayerEntity player) {
                if (!WorldMarkerUtil.isMarked(player.getServer())) return;
                ChallengeState state = ChallengeState.getServerState(player.getServer());
                state.onKill(player, killedEntity);
            }
        });
    }

    public static void onCraftedByPlayer(ServerPlayerEntity player, ItemStack stack) {
        if (!WorldMarkerUtil.isMarked(player.getServer())) return;
        ChallengeState state = ChallengeState.getServerState(player.getServer());
        state.onCraft(player, stack);
    }

    public static ItemStack createChaosCraftReplacement(ServerPlayerEntity player, ItemStack crafted) {
        if (!WorldMarkerUtil.isMarked(player.getServer())) return ItemStack.EMPTY;
        if (crafted == null || crafted.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ChallengeState state = ChallengeState.getServerState(player.getServer());
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
        List<Item> items = Registries.ITEM.stream()
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

    public static void onBlockPlaced(ServerPlayerEntity player, BlockState blockState) {
        if (!WorldMarkerUtil.isMarked(player.getServer())) return;
        ChallengeState state = ChallengeState.getServerState(player.getServer());
        state.onBlockPlace(player, blockState);
    }

    public static void onItemConsumed(ServerPlayerEntity player, ItemStack stack) {
        if (!WorldMarkerUtil.isMarked(player.getServer())) return;
        ChallengeState state = ChallengeState.getServerState(player.getServer());
        state.onConsume(player, stack);
    }

    public static void onItemBroken(ServerPlayerEntity player, ItemStack stack) {
        if (!WorldMarkerUtil.isMarked(player.getServer())) return;
        ChallengeState state = ChallengeState.getServerState(player.getServer());
        state.onItemBroken(player, stack);
    }

    public static void onItemEnchanted(ServerPlayerEntity player, ItemStack stack) {
        if (!WorldMarkerUtil.isMarked(player.getServer())) return;
        ChallengeState state = ChallengeState.getServerState(player.getServer());
        state.onEnchant(player, stack);
    }

    public static void onPlayerSlept(ServerPlayerEntity player) {
        if (!WorldMarkerUtil.isMarked(player.getServer())) return;
        ChallengeState state = ChallengeState.getServerState(player.getServer());
        state.onSleep(player);
    }

    public static void onPlayerDeath(ServerPlayerEntity player) {
        if (!WorldMarkerUtil.isMarked(player.getServer())) return;
        ChallengeState state = ChallengeState.getServerState(player.getServer());
        state.onDeath(player);
    }

    public static void onDamage(ServerPlayerEntity player, LivingEntity damaged, net.minecraft.entity.damage.DamageSource source, float amount) {
        if (!WorldMarkerUtil.isMarked(player.getServer())) return;
        ChallengeState state = ChallengeState.getServerState(player.getServer());
        state.onDamage(player, damaged, source, amount);
    }

    public static void onFishing(ServerPlayerEntity player, boolean caughtItem, boolean selfCatch) {
        if (!WorldMarkerUtil.isMarked(player.getServer())) return;
        ChallengeState state = ChallengeState.getServerState(player.getServer());
        state.onFish(player, caughtItem, selfCatch);
    }

    public static void onPearlTeleport(ServerPlayerEntity player) {
        if (!WorldMarkerUtil.isMarked(player.getServer())) return;
        ChallengeState state = ChallengeState.getServerState(player.getServer());
        state.onPearlTeleport(player);
    }

    public static void onTame(ServerPlayerEntity player, LivingEntity entity) {
        if (!WorldMarkerUtil.isMarked(player.getServer())) return;
        ChallengeState state = ChallengeState.getServerState(player.getServer());
        state.onTame(player, entity);
    }

    public static void onTrade(ServerPlayerEntity player) {
        if (!WorldMarkerUtil.isMarked(player.getServer())) return;
        ChallengeState state = ChallengeState.getServerState(player.getServer());
        state.onTrade(player);
    }

    public static void onProjectileHit(ServerPlayerEntity player, LivingEntity victim, double distance) {
        if (!WorldMarkerUtil.isMarked(player.getServer())) return;
        ChallengeState state = ChallengeState.getServerState(player.getServer());
        state.onProjectileHit(player, victim, distance);
    }
}
