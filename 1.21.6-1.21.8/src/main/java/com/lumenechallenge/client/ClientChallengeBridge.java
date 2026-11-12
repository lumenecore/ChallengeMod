package com.lumenechallenge.client;

import com.lumenechallenge.challenge.ChallengeState;
import com.lumenechallenge.util.WorldMarkerUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.MinecraftServer;

public final class ClientChallengeBridge {
    private static MinecraftServer cachedServer;
    private static ChallengeState cachedState;
    private static boolean markerConfirmed;

    private ClientChallengeBridge() {
    }

    public static ChallengeState getVisibleState() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return null;

        MinecraftServer server = client.getServer();
        if (server == null) {
            clearCache();
            return null;
        }
        if (server != cachedServer) {
            cachedServer = server;
            cachedState = null;
            markerConfirmed = false;
        }

        if (!markerConfirmed) {
            markerConfirmed = WorldMarkerUtil.isMarked(server);
            if (!markerConfirmed) return null;
        }

        if (cachedState == null) {
            cachedState = ChallengeState.getServerState(server);
        }
        return cachedState;
    }

    public static void clearCache() {
        cachedServer = null;
        cachedState = null;
        markerConfirmed = false;
    }
}
