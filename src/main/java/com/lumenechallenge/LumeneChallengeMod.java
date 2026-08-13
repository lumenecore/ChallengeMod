package com.lumenechallenge;

import com.lumenechallenge.challenge.ChallengeHooks;
import com.lumenechallenge.network.ChallengeNetworking;
import net.fabricmc.api.ModInitializer;

public class LumeneChallengeMod implements ModInitializer {
    public static final String MOD_ID = "lumenechallenge";

    @Override
    public void onInitialize() {
        ChallengeHooks.register();
        ChallengeNetworking.registerServer();
    }
}
