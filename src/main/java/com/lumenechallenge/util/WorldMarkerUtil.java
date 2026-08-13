package com.lumenechallenge.util;

import com.lumenechallenge.LumeneChallengeMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class WorldMarkerUtil {
    private static final String FILE = ".lumenechallenge_moded";

    private WorldMarkerUtil() {}

    public static void mark(MinecraftServer server) {
        mark(server.getSavePath(WorldSavePath.ROOT));
    }

    public static void mark(Path worldRoot) {
        try {
            Files.createDirectories(worldRoot);
            Files.writeString(worldRoot.resolve(FILE), LumeneChallengeMod.VERSION + "\n", StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    public static boolean isMarked(MinecraftServer server) {
        return isMarkedRoot(server.getSavePath(WorldSavePath.ROOT));
    }

    public static boolean isMarked(Path iconPath) {
        if (iconPath == null || iconPath.getParent() == null) return false;
        return isMarkedRoot(iconPath.getParent());
    }

    public static boolean isMarkedRoot(Path root) {
        if (root == null) return false;
        return Files.isRegularFile(root.resolve(FILE))
                || Files.isRegularFile(root.resolve("data").resolve("lumene_challenge.dat"));
    }

    public static List<Path> findMarkedWorlds(Path savesDirectory) {
        List<Path> worlds = new ArrayList<>();
        if (savesDirectory == null || !Files.isDirectory(savesDirectory)) return worlds;

        try (var stream = Files.list(savesDirectory)) {
            stream.filter(Files::isDirectory)
                    .filter(WorldMarkerUtil::isMarkedRoot)
                    .forEach(worlds::add);
        } catch (IOException ignored) {
        }
        return worlds;
    }

    public static long directorySize(Path root) {
        if (root == null || !Files.exists(root)) return 0L;
        try (var stream = Files.walk(root)) {
            return stream.filter(Files::isRegularFile).mapToLong(path -> {
                try {
                    return Files.size(path);
                } catch (IOException ignored) {
                    return 0L;
                }
            }).sum();
        } catch (IOException ignored) {
            return 0L;
        }
    }

    public static long markedWorldsSize(Path savesDirectory) {
        return findMarkedWorlds(savesDirectory).stream().mapToLong(WorldMarkerUtil::directorySize).sum();
    }

    public static int markedWorldCount(Path savesDirectory) {
        return findMarkedWorlds(savesDirectory).size();
    }

    public static boolean deleteWorld(Path worldRoot) {
        if (worldRoot == null || !Files.exists(worldRoot)) return true;
        try (var stream = Files.walk(worldRoot)) {
            stream.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
            return false;
        }
        return !Files.exists(worldRoot);
    }

    public static boolean deleteAllMarkedWorlds(Path savesDirectory) {
        boolean success = true;
        for (Path world : findMarkedWorlds(savesDirectory)) {
            if (!deleteWorld(world)) success = false;
        }
        return success;
    }

    public static Path getSavesDirectory(Path gameDirectory) {
        return gameDirectory.resolve("saves");
    }
}
