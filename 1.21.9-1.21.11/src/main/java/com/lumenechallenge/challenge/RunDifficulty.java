package com.lumenechallenge.challenge;

public enum RunDifficulty {
    NORMAL("difficulty.lumenechallenge.normal"),
    HARD("difficulty.lumenechallenge.hard"),
    INSANE("difficulty.lumenechallenge.insane");

    private final String translationKey;

    RunDifficulty(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }

    public boolean allows(RunDifficulty taskDifficulty) {
        return taskDifficulty.ordinal() <= this.ordinal();
    }

    public static RunDifficulty byIndex(int index) {
        RunDifficulty[] values = values();
        return values[Math.floorMod(index, values.length)];
    }

    public static RunDifficulty fromId(String id) {
        if (id == null || id.isBlank()) {
            return NORMAL;
        }
        for (RunDifficulty difficulty : values()) {
            if (difficulty.name().equalsIgnoreCase(id)) {
                return difficulty;
            }
        }
        return NORMAL;
    }
}
