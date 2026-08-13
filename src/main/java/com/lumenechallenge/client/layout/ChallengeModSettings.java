package com.lumenechallenge.client.layout;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ChallengeModSettings {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("lumenechallenge-settings.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static boolean challengeModEnabled = true;
    private static boolean autoDeleteCompletedWorlds = false;
    private static String modLanguage;
    private static String lastMinecraftLanguage;
    private static boolean loaded;

    private ChallengeModSettings() {}

    public static synchronized void load() {
        if (loaded) return;
        loaded = true;
        if (!Files.isRegularFile(CONFIG_PATH)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root != null) {
                if (root.has("challengeModEnabled")) challengeModEnabled = root.get("challengeModEnabled").getAsBoolean();
                if (root.has("autoDeleteCompletedWorlds")) autoDeleteCompletedWorlds = root.get("autoDeleteCompletedWorlds").getAsBoolean();
                if (root.has("modLanguage")) modLanguage = root.get("modLanguage").getAsString();
                if (root.has("lastMinecraftLanguage")) lastMinecraftLanguage = root.get("lastMinecraftLanguage").getAsString();
            }
        } catch (Exception ignored) {
            challengeModEnabled = true;
            autoDeleteCompletedWorlds = false;
            modLanguage = null;
            lastMinecraftLanguage = null;
        }
    }

    public static synchronized void initializeLanguage(String minecraftLanguage) {
        load();
        if (modLanguage == null || !ModI18n.isSupported(modLanguage)) {
            modLanguage = ModI18n.normalizeMinecraftLanguage(minecraftLanguage);
        }
        if (lastMinecraftLanguage == null || lastMinecraftLanguage.isBlank()) {
            lastMinecraftLanguage = minecraftLanguage;
        }
        save();
        ModI18n.reload();
    }

    public static synchronized boolean consumeMinecraftLanguageChange(String minecraftLanguage) {
        load();
        if (lastMinecraftLanguage == null) {
            lastMinecraftLanguage = minecraftLanguage;
            save();
            return false;
        }
        if (lastMinecraftLanguage.equals(minecraftLanguage)) {
            return false;
        }
        lastMinecraftLanguage = minecraftLanguage;
        save();
        return true;
    }

    public static boolean isChallengeModEnabled() { load(); return challengeModEnabled; }
    public static void setChallengeModEnabled(boolean enabled) { load(); challengeModEnabled = enabled; save(); }
    public static boolean isAutoDeleteCompletedWorlds() { load(); return autoDeleteCompletedWorlds; }
    public static void setAutoDeleteCompletedWorlds(boolean enabled) { load(); autoDeleteCompletedWorlds = enabled; save(); }

    public static String getModLanguage() {
        load();
        return ModI18n.isSupported(modLanguage) ? modLanguage : "en_us";
    }

    public static void setModLanguage(String language) {
        load();
        modLanguage = ModI18n.isSupported(language) ? language : "en_us";
        save();
        ModI18n.reload();
    }

    public static String getLastMinecraftLanguage() {
        load();
        return lastMinecraftLanguage;
    }

    private static synchronized void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("challengeModEnabled", challengeModEnabled);
            root.addProperty("autoDeleteCompletedWorlds", autoDeleteCompletedWorlds);
            if (modLanguage != null) root.addProperty("modLanguage", modLanguage);
            if (lastMinecraftLanguage != null) root.addProperty("lastMinecraftLanguage", lastMinecraftLanguage);
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(root, writer);
            }
        } catch (Exception ignored) {
        }
    }
}
