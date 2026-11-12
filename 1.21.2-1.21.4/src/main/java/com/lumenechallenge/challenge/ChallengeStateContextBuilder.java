package com.lumenechallenge.challenge;

import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

public final class ChallengeStateContextBuilder {
    private ChallengeStateContextBuilder() {
    }

    public static ChallengeContext build(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        BlockPos pos = player.getBlockPos();
        PlayerInventory inventory = player.getInventory();
        RegistryEntry<Biome> biome = world.getBiome(pos);
        return new ChallengeContext(
                player,
                world,
                pos,
                inventory,
                player.getMainHandStack(),
                world.getLevelProperties().getTime(),
                biome,
                world.isRaining(),
                world.isThundering(),
                0,
                false,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                false,
                0.0D,
                null,
                false,
                player.isOnFire(),
                null,
                false,
                false,
                false,
                false,
                0.0D,
                null,
                false,
                false,
                false,
                false,
                false,
                false,
                java.util.List.of(),
                "",
                null,
                "",
                "",
                "",
                "",
                "",
                null,
                null,
                "",
                0,
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                0
        );
    }

    public static ChallengeContext build(ServerPlayerEntity player,
                                         BlockState mined,
                                         BlockState placed,
                                         LivingEntity killed,
                                         ItemStack crafted,
                                         ItemStack consumed,
                                         ItemStack enchanted,
                                         ItemStack smelted,
                                         boolean damaged,
                                         double damageAmount,
                                         LivingEntity damagedEntity,
                                         boolean jumped,
                                         boolean fished,
                                         boolean caughtSelfOnRod,
                                         boolean teleportedByPearl,
                                         LivingEntity tamedEntity,
                                         boolean slept,
                                         boolean traded) {
        ServerWorld world = player.getServerWorld();
        BlockPos pos = player.getBlockPos();
        PlayerInventory inventory = player.getInventory();
        RegistryEntry<Biome> biome = world.getBiome(pos);
        return new ChallengeContext(
                player,
                world,
                pos,
                inventory,
                player.getMainHandStack(),
                world.getLevelProperties().getTime(),
                biome,
                world.isRaining(),
                world.isThundering(),
                0,
                false,
                mined,
                placed,
                killed,
                crafted,
                consumed,
                false,
                enchanted,
                null,
                smelted,
                damaged,
                damageAmount,
                damagedEntity,
                jumped,
                player.isOnFire(),
                null,
                fished,
                caughtSelfOnRod,
                teleportedByPearl,
                false,
                0.0D,
                tamedEntity,
                slept,
                traded,
                false,
                false,
                false,
                false,
                java.util.List.of(),
                "",
                null,
                "",
                "",
                "",
                "",
                "",
                null,
                null,
                "",
                0,
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                0
        );
    }
}
