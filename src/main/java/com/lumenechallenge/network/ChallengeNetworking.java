
package com.lumenechallenge.network;

import com.lumenechallenge.challenge.ChallengeState;
import com.lumenechallenge.challenge.ModifierType;
import com.lumenechallenge.util.SeedUtil;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class ChallengeNetworking {
    public static final Identifier SELECT_SEED_PACKET = Identifier.of("lumenechallenge", "select_seed");

    private ChallengeNetworking() {
    }

    public static void registerServer() {
        PayloadTypeRegistry.playC2S().register(SelectSeedPayload.ID, SelectSeedPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SelectSeedPayload.ID, (payload, context) -> {
            long seed = SeedUtil.resolveSeed(payload.seed());
            ModifierType modifier = ModifierType.fromId(payload.modifierId());
            int challengeCount = Math.max(1, Math.min(50, payload.challengeCount()));

            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                ChallengeState state = ChallengeState.getServerState(player.getServer());
                if (state.challenge() != null) {
                    return;
                }
                state.selectSeed(player, seed, modifier, challengeCount);
                state.setChallengeSeedText(payload.seed());
                state.markDirty();
            });
        });
    }
}
