package com.lumenechallenge.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Small shared setting readable by both client UI and the integrated server. */
public final class DeveloperModeConfig {
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("lumenechallenge-developer-mode.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static boolean enabled;
    private static boolean loaded;

    private DeveloperModeConfig() {}

    public static synchronized void load() {
        if (loaded) return;
        loaded = true;
        if (!Files.isRegularFile(PATH)) { save(); return; }
        try (Reader reader = Files.newBufferedReader(PATH)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root != null && root.has("enabled")) enabled = root.get("enabled").getAsBoolean();
        } catch (Exception ignored) {
            enabled = false;
        }
    }

    public static synchronized boolean isEnabled() {
        load();
        return enabled;
    }

    public static synchronized void setEnabled(boolean value) {
        load();
        enabled = value;
        save();
    }

    private static synchronized void save() {
        try {
            Files.createDirectories(PATH.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("enabled", enabled);
            try (Writer writer = Files.newBufferedWriter(PATH)) { GSON.toJson(root, writer); }
        } catch (Exception ignored) {}
    }
}
