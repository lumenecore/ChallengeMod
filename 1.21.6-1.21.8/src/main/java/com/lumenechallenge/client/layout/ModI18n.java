package com.lumenechallenge.client.layout;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.text.MutableText;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.Reader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Independent ChallengeMod localization, decoupled from Minecraft's selected language. */
public final class ModI18n {
    public record Language(String code, String name) {}

    private static final Gson GSON = new Gson();
    private static final Map<String, String> LANGUAGES = new LinkedHashMap<>();
    private static final Map<String, String> DISPLAY_NAMES = new LinkedHashMap<>();
    private static Map<String, String> translations = Map.of();
    private static final Map<String, Map<String, String>> MINECRAFT_LANGUAGE_CACHE = new java.util.HashMap<>();

    static {
        register("en_us", "English");
        register("ru_ru", "Русский");
        register("es_es", "Español");
        register("pt_br", "Português");
        register("de_de", "Deutsch");
        register("fr_fr", "Français");
        register("zh_cn", "简体中文");
        register("pl_pl", "Polski");
        register("tr_tr", "Türkçe");
        register("ja_jp", "日本語");
        register("ko_kr", "한국어");
        reload();
    }

    private ModI18n() {}

    private static void register(String code, String name) {
        LANGUAGES.put(code, name);
        DISPLAY_NAMES.put(code, name);
    }

    public static List<Language> languages() {
        return LANGUAGES.entrySet().stream().map(e -> new Language(e.getKey(), e.getValue())).toList();
    }

    public static boolean isSupported(String code) {
        return code != null && LANGUAGES.containsKey(code);
    }

    public static String normalizeMinecraftLanguage(String minecraftLanguage) {
        return isSupported(minecraftLanguage) ? minecraftLanguage : "en_us";
    }

    public static String displayName(String code) {
        return DISPLAY_NAMES.getOrDefault(code, DISPLAY_NAMES.get("en_us"));
    }

    public static void reload() {
        MINECRAFT_LANGUAGE_CACHE.clear();
        String code = ChallengeModSettings.getModLanguage();
        Path path = FabricLoader.getInstance().getModContainer("lumenechallenge")
                .flatMap(container -> container.findPath("assets/lumenechallenge/lang/" + code + ".json"))
                .orElse(null);
        if (path == null) {
            translations = Map.of();
            return;
        }
        translations = read(path);
    }

    private static Map<String, String> read(Path path) {
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject object = GSON.fromJson(reader, JsonObject.class);
            Map<String, String> result = new LinkedHashMap<>();
            if (object != null) {
                object.entrySet().forEach(entry -> {
                    if (entry.getValue().isJsonPrimitive()) {
                        result.put(entry.getKey(), entry.getValue().getAsString());
                    }
                });
            }
            return result;
        } catch (IOException ignored) {
            return Map.of();
        }
    }

    /**
     * Resolves a vanilla Minecraft translation key using the language selected for ChallengeMod,
     * rather than the language currently selected in Minecraft's UI.
     */
    public static String minecraftText(String languageCode, String translationKey) {
        String normalized = normalizeMinecraftLanguage(languageCode);
        Map<String, String> language = MINECRAFT_LANGUAGE_CACHE.get(normalized);
        if (language == null) {
            language = loadMinecraftLanguage(normalized);
            // Do not permanently cache a failed load: the resource manager may not have
            // been ready yet (e.g. immediately after switching language/reloading packs).
            if (!language.isEmpty()) {
                MINECRAFT_LANGUAGE_CACHE.put(normalized, language);
            }
        }
        return language.getOrDefault(translationKey, translationKey);
    }

    private static Map<String, String> loadMinecraftLanguage(String languageCode) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.getResourceManager() == null) {
                return Map.of();
            }

            ResourceManager manager = client.getResourceManager();
            Identifier id = Identifier.of("minecraft", "lang/" + languageCode + ".json");

            // Vanilla language files are resource-pack data. Use all matching resources so
            // resource packs can extend/override Minecraft translations correctly.
            List<Resource> resources = manager.getAllResources(id);
            if (resources.isEmpty()) {
                return Map.of();
            }

            Map<String, String> result = new LinkedHashMap<>();
            for (Resource resource : resources) {
                try (Reader reader = resource.getReader()) {
                    JsonObject object = GSON.fromJson(reader, JsonObject.class);
                    if (object == null) continue;
                    object.entrySet().forEach(entry -> {
                        if (entry.getValue().isJsonPrimitive()) {
                            result.put(entry.getKey(), entry.getValue().getAsString());
                        }
                    });
                }
            }
            return result;
        } catch (Throwable ignored) {
            return Map.of();
        }
    }

    public static MutableText text(String key, Object... args) {
        String value = translations.get(key);
        if (value == null) {
            return Text.translatable(key, args);
        }
        return Text.literal(format(value, args));
    }

    public static Text textInLanguage(String languageCode, String key, Object... args) {
        String normalized = normalizeMinecraftLanguage(languageCode);
        Path path = FabricLoader.getInstance().getModContainer("lumenechallenge")
                .flatMap(container -> container.findPath("assets/lumenechallenge/lang/" + normalized + ".json"))
                .orElse(null);
        if (path == null) {
            return Text.literal(format(key, args));
        }
        String value = read(path).get(key);
        if (value == null) {
            return Text.literal(format(key, args));
        }
        return Text.literal(format(value, args));
    }

    private static String format(String template, Object... args) {
        if (args == null || args.length == 0) {
            return template;
        }
        Object[] strings = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            strings[i] = arg instanceof Text text ? text.getString() : arg;
        }
        try {
            return String.format(Locale.ROOT, template, strings);
        } catch (RuntimeException ignored) {
            return template;
        }
    }
}
