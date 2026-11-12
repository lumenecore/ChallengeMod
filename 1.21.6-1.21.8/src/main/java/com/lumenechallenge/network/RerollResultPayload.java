package com.lumenechallenge.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Server acknowledgement for a reroll request. */
public record RerollResultPayload(boolean success) implements CustomPayload {
    public static final CustomPayload.Id<RerollResultPayload> ID =
            new CustomPayload.Id<>(Identifier.of("lumenechallenge", "reroll_result"));

    public static final PacketCodec<RegistryByteBuf, RerollResultPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.BOOLEAN, RerollResultPayload::success, RerollResultPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
