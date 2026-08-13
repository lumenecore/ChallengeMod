package com.lumenechallenge.client;

import com.lumenechallenge.challenge.ChallengeState;
import com.lumenechallenge.util.WorldMarkerUtil;
import net.minecraft.client.MinecraftClient;

public final class ClientChallengeBridge {
    private ClientChallengeBridge() {
    }

    public static ChallengeState getVisibleState() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getServer() == null) {
            return null;
        }
        if (!WorldMarkerUtil.isMarked(client.getServer())) {
            return null;
        }
        return ChallengeState.getServerState(client.getServer());
    }
}
