package com.lumenechallenge.challenge;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ChallengeConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ChallengeConfig cached;

    private ChallengeConfigManager() {
    }

    public static ChallengeConfig get() {
        if (cached == null) {
            cached = load();
        }
        return cached;
    }

    public static void reload() {
        cached = load();
    }

    private static ChallengeConfig load() {
        ChallengeConfig config = loadBundledDefaults();
        if (config == null) {
            config = new ChallengeConfig();
        }
        config.sanitize();
        return config;
    }

    static ChallengeConfig loadBundledDefaultsRaw() {
        try (InputStream stream = defaultResourceStream()) {
            if (stream == null) {
                return null;
            }
            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return GSON.fromJson(json, ChallengeConfig.class);
        } catch (IOException | JsonParseException ex) {
            return null;
        }
    }

    static ChallengeConfig loadBundledDefaults() {
        ChallengeConfig config = loadBundledDefaultsRaw();
        if (config == null) {
            config = new ChallengeConfig();
        }
        config.sanitize();
        return config;
    }

    public static String dumpDefaultJson() {
        return GSON.toJson(loadBundledDefaults());
    }

    public static void writeDefaultTo(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, GSON.toJson(loadBundledDefaults()), StandardCharsets.UTF_8);
    }

    public static InputStream defaultResourceStream() {
        return ChallengeConfigManager.class.getResourceAsStream("/assets/lumenechallenge/challenge/default-config.json");
    }
}
