package com.lumenechallenge.client.layout;

import com.lumenechallenge.util.WorldMarkerUtil;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.util.List;

public final class WorldStorageUtil {
    private WorldStorageUtil() {}

    public static Path getSavesDirectory() {
        return FabricLoader.getInstance().getGameDir().resolve("saves");
    }

    public static List<Path> getChallengeWorlds() {
        return WorldMarkerUtil.findMarkedWorlds(getSavesDirectory());
    }

    public static int getChallengeWorldCount() {
        return getChallengeWorlds().size();
    }

    public static long getChallengeWorldsSize() {
        return getChallengeWorlds().stream().mapToLong(WorldMarkerUtil::directorySize).sum();
    }

    public static long sizeInMegabytes(long bytes) {
        return Math.round(bytes / 1_048_576.0D);
    }


}
