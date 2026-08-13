
package com.lumenechallenge.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SelectSeedPayload(String seed, String modifierId, int challengeCount) implements CustomPayload {

    public static final CustomPayload.Id<SelectSeedPayload> ID =
            new CustomPayload.Id<>(Identifier.of("lumenechallenge", "select_seed"));

    public static final PacketCodec<RegistryByteBuf, SelectSeedPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.STRING,
                    SelectSeedPayload::seed,
                    PacketCodecs.STRING,
                    SelectSeedPayload::modifierId,
                    PacketCodecs.INTEGER,
                    SelectSeedPayload::challengeCount,
                    SelectSeedPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
