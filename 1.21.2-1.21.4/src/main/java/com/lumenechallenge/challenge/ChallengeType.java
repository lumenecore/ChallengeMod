package com.lumenechallenge.challenge;

public enum ChallengeType {
    MINING("MINING"),
    PLACING("PLACING"),
    CRAFT("CRAFT"),
    EATING("EATING"),
    DRINKING("DRINKING"),
    KILLING("KILLING"),
    DEATH("DEATH"),
    DAMAGE("DAMAGE"),
    JUMP("JUMP");

    private final String id;

    ChallengeType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static ChallengeType byIndex(int index) {
        ChallengeType[] values = values();
        return values[Math.floorMod(index, values.length)];
    }
}
