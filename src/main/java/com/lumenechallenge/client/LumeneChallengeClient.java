package com.lumenechallenge.client;

import com.lumenechallenge.challenge.ChallengeState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class LumeneChallengeClient implements ClientModInitializer {
    private static boolean completionScreenOpened = false;
    private static boolean seedScreenOpened = false;
    private static boolean seedVisible = true;

    private static KeyBinding toggleSeedKey;

    @Override
    public void onInitializeClient() {
        toggleSeedKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.lumenechallenge.toggle_seed",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_T,
                "category.lumenechallenge"
        ));

        HudRenderCallback.EVENT.register(LumeneChallengeHud::render);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client == null || client.world == null) {
                completionScreenOpened = false;
                seedScreenOpened = false;
                seedVisible = true;
                return;
            }

            while (toggleSeedKey.wasPressed()) {
                seedVisible = !seedVisible;
            }

            ChallengeState state = ClientChallengeBridge.getVisibleState();
            if (state == null) {
                return;
            }

            if (state.challenge() == null && !state.seedSelected() && !state.completed()
                    && !seedScreenOpened && !(client.currentScreen instanceof SeedSelectionScreen)) {
                seedScreenOpened = true;
                client.execute(() -> client.setScreen(new SeedSelectionScreen()));
            }

            if (state.challenge() != null) {
                seedScreenOpened = false;
            }

            if (state.completed() && !completionScreenOpened) {
                completionScreenOpened = true;
                client.execute(() -> client.setScreen(new CompleteChallengeScreen(state)));
            }

            if (!state.completed()) {
                completionScreenOpened = false;
            }
        });
    }

    public static boolean isSeedVisible() {
        return seedVisible;
    }
}
