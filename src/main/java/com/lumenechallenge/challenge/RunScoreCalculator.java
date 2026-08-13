package com.lumenechallenge.challenge;

public final class RunScoreCalculator {
    private RunScoreCalculator() {
    }

    public static RunScoreResult calculate(Challenge challenge, RunDifficulty difficulty, int rerollsUsed, ChallengeConfig config, double earnedScore, ModifierType modifier) {
        if (challenge == null || config == null) {
            return new RunScoreResult(0.0D, 0.0D, 0, "C");
        }

        RunDifficulty safeDifficulty = difficulty == null ? RunDifficulty.NORMAL : difficulty;
        int totalTasks = challenge.totalTasks();
        if (totalTasks <= 0) {
            return new RunScoreResult(0.0D, 0.0D, 0, "C");
        }

        double safeEarnedScore = Math.max(0.0D, earnedScore);
        double basePercent = safeEarnedScore / (double) totalTasks;
        double adjusted = basePercent
                * rerollMultiplier(rerollsUsed)
                * modifierMultiplier(modifier);
        double accuracyPercent = Math.max(0.0D, Math.min(100.0D, adjusted * 100.0D));
        String rank = config.rankForAccuracy(accuracyPercent).name();
        return new RunScoreResult(accuracyPercent, safeEarnedScore, totalTasks, rank);
    }

    private static double rerollMultiplier(int rerollsUsed) {
        return switch (Math.max(0, rerollsUsed)) {
            case 0 -> 1.0D;
            case 1 -> 0.975D;
            case 2 -> 0.95D;
            default -> 0.90D;
        };
    }

    private static double modifierMultiplier(ModifierType modifier) {
        if (modifier == null) {
            return 1.0D;
        }
        return switch (modifier) {
            case GIANT, DWARF, SPEEDRUN, NONE, RANDOM -> 1.0D;
            case CHAOS -> 1.40D;
            case HANGOVER -> 1.10D;
            case POCKETS -> 1.30D;
            case PACIFIST -> 1.15D;
            case OBESITY -> 1.05D;
            case CATACLYSM -> 1.30D;
        };
    }

    public record RunScoreResult(double accuracyPercent, double completedScore, int totalTasks, String rank) {
    }
}
