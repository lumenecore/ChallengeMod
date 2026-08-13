package com.lumenechallenge.challenge;

public enum RunDifficulty {
    NORMAL("difficulty.lumenechallenge.normal", 1.0D),
    HARD("difficulty.lumenechallenge.hard", 0.95D),
    INSANE("difficulty.lumenechallenge.insane", 0.90D);

    private final String translationKey;
    private final double multiplier;

    RunDifficulty(String translationKey, double multiplier) {
        this.translationKey = translationKey;
        this.multiplier = multiplier;
    }

    public String translationKey() {
        return translationKey;
    }

    public double multiplier() {
        return multiplier;
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
