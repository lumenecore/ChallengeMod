package com.lumenechallenge.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PerspectivePayload(int perspective) implements CustomPayload {
    public static final CustomPayload.Id<PerspectivePayload> ID =
            new CustomPayload.Id<>(Identifier.of("lumenechallenge", "camera_perspective"));
    public static final PacketCodec<RegistryByteBuf, PerspectivePayload> CODEC =
            PacketCodec.tuple(PacketCodecs.INTEGER, PerspectivePayload::perspective, PerspectivePayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
