package com.lumenechallenge.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RerollChallengePayload(int nonce) implements CustomPayload {
    public static final CustomPayload.Id<RerollChallengePayload> ID =
            new CustomPayload.Id<>(Identifier.of("lumenechallenge", "reroll_challenge"));

    public static final PacketCodec<RegistryByteBuf, RerollChallengePayload> CODEC =
            PacketCodec.tuple(PacketCodecs.INTEGER, RerollChallengePayload::nonce, RerollChallengePayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
