package com.lumenechallenge.client;

import com.lumenechallenge.challenge.ChallengeState;
import com.lumenechallenge.client.layout.ModI18n;
import com.lumenechallenge.client.layout.NotificationManager;
import com.lumenechallenge.client.layout.ModSettingsScreen;
import com.lumenechallenge.client.layout.ChallengeModSettings;
import com.lumenechallenge.client.layout.WorldStorageUtil;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.MinecraftClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class LumeneChallengeClient implements ClientModInitializer {
    private static boolean completionScreenOpened = false;
    private static boolean seedScreenOpened = false;
    private static boolean seedVisible = true;
    private static int lastAnnouncedCompletedSteps = -1;
    private static boolean rerollKeyWasDown = false;
    private static int lastCountdownNumber = -1;
    private static int rerollKeyCode = -2;
    private static java.nio.file.Path pendingAutoDeleteWorld;
    private static boolean languageInitialized = false;
    private static boolean languagePromptOpen = false;

    private static KeyBinding toggleSeedKey;
    private static KeyBinding rerollKey;

    @Override
    public void onInitializeClient() {
        ChallengeModSettings.load();
        toggleSeedKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.lumenechallenge.toggle_seed", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_T, "category.lumenechallenge"));
        rerollKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.lumenechallenge.reroll", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "category.lumenechallenge"));

        HudRenderCallback.EVENT.register(LumeneChallengeHud::render);

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client == null || client.world == null) return;
            ChallengeState state = ClientChallengeBridge.getVisibleState();
            if (state == null) return;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client == null) return;

            String minecraftLanguage = client.getLanguageManager().getLanguage();
            if (languagePromptOpen && !(client.currentScreen instanceof com.lumenechallenge.client.layout.LanguageChangeConfirmScreen)) {
                languagePromptOpen = false;
            }
            if (!languageInitialized) {
                ChallengeModSettings.initializeLanguage(minecraftLanguage);
                languageInitialized = true;
            } else if (!languagePromptOpen
                    && !(client.currentScreen instanceof net.minecraft.client.gui.screen.option.LanguageOptionsScreen)
                    && ChallengeModSettings.consumeMinecraftLanguageChange(minecraftLanguage)) {
                languagePromptOpen = true;
                net.minecraft.client.gui.screen.Screen parent = client.currentScreen;
                client.setScreen(new com.lumenechallenge.client.layout.LanguageChangeConfirmScreen(parent, minecraftLanguage));
            }

            if (client.world == null) {
                if (client != null && client.getServer() == null && pendingAutoDeleteWorld != null) {
                    com.lumenechallenge.util.WorldMarkerUtil.deleteWorld(pendingAutoDeleteWorld);
                    pendingAutoDeleteWorld = null;
                    client.setScreen(new SelectWorldScreen(new TitleScreen()));
                }
                completionScreenOpened = false;
                seedScreenOpened = false;
                seedVisible = true;
                lastAnnouncedCompletedSteps = -1;
                rerollKeyWasDown = false;
                lastCountdownNumber = -1;
                rerollKeyCode = -2;
                return;
            }

            while (toggleSeedKey.wasPressed()) seedVisible = !seedVisible;

            ChallengeState state = ClientChallengeBridge.getVisibleState();
            if (state == null) {
                rerollKeyWasDown = false;
                lastCountdownNumber = -1;
                rerollKeyCode = -2;
                return;
            }

            if (state.isCountdownActive(client.getServer())) {
                int number = Math.max(1, (int) Math.ceil(state.countdownRemainingTicks(client.getServer()) / 20.0D));
                if (number != lastCountdownNumber) {
                    lastCountdownNumber = number;
                    client.inGameHud.setTitleTicks(0, 20, 10);
                    client.inGameHud.setTitle(Text.literal(Integer.toString(number)));
                }
            } else {
                lastCountdownNumber = -1;
            }

            boolean rerollKeyDown = isRerollKeyDown(client);
            if (rerollKeyDown && !rerollKeyWasDown
                    && state.challenge() != null && !state.completed() && state.rerollsRemaining() > 0
                    && client.currentScreen == null) {
                client.setScreen(new RerollConfirmScreen());
            }
            rerollKeyWasDown = rerollKeyDown;

            if (!state.runStarted() && !state.completed()
                    && !seedScreenOpened && !(client.currentScreen instanceof SeedSelectionScreen)) {
                seedScreenOpened = true;
                client.setScreen(new SeedSelectionScreen());
            }

            if (state.runStarted() || state.completed()) seedScreenOpened = false;

            int completedSteps = state.completedSteps();
            if (state.challenge() == null || completedSteps < lastAnnouncedCompletedSteps) {
                lastAnnouncedCompletedSteps = completedSteps;
            } else if (completedSteps > lastAnnouncedCompletedSteps && completedSteps > 0) {
                lastAnnouncedCompletedSteps = completedSteps;
                NotificationManager.show(ModI18n.text("notification.lumenechallenge.objective_completed"));
            }

            if (state.completed() && !completionScreenOpened) {
                completionScreenOpened = true;
                client.setScreen(new CompleteChallengeScreen(state));
            }
            if (!state.completed()) completionScreenOpened = false;
        });
    }

    private static boolean isRerollKeyDown(net.minecraft.client.MinecraftClient client) {
        if (rerollKey == null || client == null || client.getWindow() == null) return false;
        if (rerollKeyCode == -2) {
            rerollKeyCode = -1;
            for (int keyCode = 0; keyCode <= GLFW.GLFW_KEY_LAST; keyCode++) {
                if (rerollKey.matchesKey(keyCode, 0)) {
                    rerollKeyCode = keyCode;
                    break;
                }
            }
        }
        return rerollKeyCode >= 0
                && GLFW.glfwGetKey(client.getWindow().getHandle(), rerollKeyCode) == GLFW.GLFW_PRESS;
    }


    public static String getKeyName(String id) {
        KeyBinding key = "toggle_seed".equals(id) ? toggleSeedKey : rerollKey;
        return key == null ? "?" : key.getBoundKeyLocalizedText().getString();
    }

    public static void rebindSeedKey(int keyCode, int scanCode) {
        if (toggleSeedKey == null) return;
        toggleSeedKey.setBoundKey(InputUtil.fromKeyCode(keyCode, scanCode));
        saveKeybindings();
    }

    public static void rebindRerollKey(int keyCode, int scanCode) {
        if (rerollKey == null) return;
        rerollKey.setBoundKey(InputUtil.fromKeyCode(keyCode, scanCode));
        rerollKeyCode = keyCode;
        saveKeybindings();
    }

    private static void saveKeybindings() {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client != null && client.options != null) client.options.write();
    }

    public static void finishCompletedWorldAndReturnToWorldList() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || !client.isInSingleplayer() || client.getServer() == null) {
            return;
        }
        pendingAutoDeleteWorld = client.getServer().getSavePath(net.minecraft.util.WorldSavePath.ROOT);
        client.disconnect();
    }

    public static boolean shouldAutoDeleteCompletedWorlds() {
        return ChallengeModSettings.isAutoDeleteCompletedWorlds();
    }

    public static boolean isSeedVisible() { return seedVisible; }
}
