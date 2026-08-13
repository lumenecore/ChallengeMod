package com.lumenechallenge;

import com.lumenechallenge.challenge.ChallengeHooks;
import com.lumenechallenge.network.ChallengeNetworking;
import net.fabricmc.api.ModInitializer;

public class LumeneChallengeMod implements ModInitializer {
    public static final String MOD_ID = "lumenechallenge";
    public static final String VERSION = "1.1";

    @Override
    public void onInitialize() {
        ChallengeHooks.register();
        ChallengeNetworking.registerServer();
    }
}
