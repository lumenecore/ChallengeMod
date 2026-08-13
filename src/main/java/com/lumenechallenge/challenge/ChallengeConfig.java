package com.lumenechallenge.challenge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ChallengeConfig {
    public int maxRerolls = 5;
    public double rerollMultiplier = 0.95D;
    public List<RankBand> ranks = new ArrayList<>();
    public List<ChallengeTaskDefinition> tasks = new ArrayList<>();
    public List<ModifierRule> modifiers = new ArrayList<>();

    public ChallengeConfig() {
    }

    public int maxRerolls() {
        return maxRerolls;
    }

    public double rerollMultiplier() {
        return rerollMultiplier;
    }

    public List<RankBand> ranks() {
        return ranks;
    }

    public List<ChallengeTaskDefinition> tasks() {
        return tasks;
    }

    public List<ModifierRule> modifiers() {
        return modifiers;
    }

    public double difficultyMultiplier(RunDifficulty difficulty) {
        RunDifficulty safe = difficulty == null ? RunDifficulty.NORMAL : difficulty;
        return switch (safe) {
            case NORMAL -> 1.0D;
            case HARD -> 0.95D;
            case INSANE -> 0.90D;
        };
    }

    public List<ChallengeTaskDefinition> allowedTasks(RunDifficulty runDifficulty) {
        List<ChallengeTaskDefinition> result = new ArrayList<>();
        for (ChallengeTaskDefinition task : tasks) {
            if (task != null && task.allowedIn(runDifficulty)) {
                result.add(task.copy());
            }
        }
        return result;
    }

    public RankBand rankForAccuracy(double accuracyPercent) {
        RankBand best = null;
        for (RankBand rank : ranks) {
            if (rank != null && accuracyPercent >= rank.threshold()) {
                if (best == null || rank.threshold() > best.threshold()) {
                    best = rank;
                }
            }
        }
        return best == null ? new RankBand("C", 0.0D) : best;
    }

    public ModifierRule modifierRuleById(int id) {
        for (ModifierRule rule : modifiers) {
            if (rule != null && rule.id == id) {
                return rule;
            }
        }
        return null;
    }

    public void sanitize() {
        if (ranks == null) {
            ranks = new ArrayList<>();
        }
        if (tasks == null) {
            tasks = new ArrayList<>();
        }
        if (modifiers == null) {
            modifiers = new ArrayList<>();
        }

        if (maxRerolls < 0) {
            maxRerolls = 0;
        }
        if (rerollMultiplier <= 0.0D || rerollMultiplier > 1.0D) {
            rerollMultiplier = 0.95D;
        }

        ChallengeConfig bundled = ChallengeConfigManager.loadBundledDefaultsRaw();
        if (bundled != null) {
            if (ranks.isEmpty() && bundled.ranks != null) {
                ranks = copyRanks(bundled.ranks);
            }
            if (modifiers.isEmpty() && bundled.modifiers != null) {
                modifiers = copyModifiers(bundled.modifiers);
            }
            if (tasks.isEmpty() && bundled.tasks != null) {
                tasks = copyTasks(bundled.tasks);
            }
        }

        if (ranks.isEmpty()) {
            ranks.add(new RankBand("SS", 0.95D));
            ranks.add(new RankBand("S", 0.80D));
            ranks.add(new RankBand("A", 0.70D));
            ranks.add(new RankBand("B", 0.50D));
            ranks.add(new RankBand("C", 0.0D));
        }
        ranks.sort(Comparator.comparingDouble(RankBand::threshold).reversed());

        for (RankBand band : ranks) {
            if (band == null) {
                continue;
            }
            if (band.name == null || band.name.isBlank()) {
                band.name = "C";
            }
            if (band.threshold < 0.0D) {
                band.threshold = 0.0D;
            }
        }

        for (ModifierRule rule : modifiers) {
            if (rule == null) {
                continue;
            }
            if (rule.id <= 0) {
                rule.id = 1;
            }
            if (rule.difficulty == null || rule.difficulty.isBlank()) {
                rule.difficulty = RunDifficulty.NORMAL.name();
            }
            if (rule.weight <= 0) {
                rule.weight = 1;
            }
        }
        modifiers.sort(Comparator.comparingInt(ModifierRule::id));

        for (ChallengeTaskDefinition task : tasks) {
            if (task == null) {
                continue;
            }
            if (task.type == null || task.type.isBlank()) {
                task.type = "JUMP";
            }
            if (task.difficulty == null || task.difficulty.isBlank()) {
                task.difficulty = RunDifficulty.NORMAL.name();
            }
            if (task.recommendedSecondsNormal <= 0) {
                task.recommendedSecondsNormal = task.recommendedSeconds > 0 ? task.recommendedSeconds : 60;
            }
            if (task.recommendedSecondsHard <= 0) {
                task.recommendedSecondsHard = task.recommendedSecondsNormal;
            }
            if (task.recommendedSecondsInsane <= 0) {
                task.recommendedSecondsInsane = task.recommendedSecondsHard;
            }
            if (task.frequency <= 0) {
                task.frequency = 1;
            }
            if (task.modifiers == null) {
                task.modifiers = new ArrayList<>();
            }
        }
    }

    public static ChallengeConfig defaults() {
        ChallengeConfig config = ChallengeConfigManager.loadBundledDefaultsRaw();
        if (config == null) {
            config = new ChallengeConfig();
        }
        config.sanitize();
        return config;
    }

    private static List<RankBand> copyRanks(List<RankBand> source) {
        List<RankBand> result = new ArrayList<>();
        for (RankBand band : source) {
            if (band != null) {
                result.add(new RankBand(band.name, band.threshold));
            }
        }
        return result;
    }

    private static List<ModifierRule> copyModifiers(List<ModifierRule> source) {
        List<ModifierRule> result = new ArrayList<>();
        for (ModifierRule rule : source) {
            if (rule != null) {
                result.add(new ModifierRule(rule.id, rule.difficulty, rule.weight, rule.enabled));
            }
        }
        return result;
    }

    private static List<ChallengeTaskDefinition> copyTasks(List<ChallengeTaskDefinition> source) {
        List<ChallengeTaskDefinition> result = new ArrayList<>();
        for (ChallengeTaskDefinition definition : source) {
            if (definition != null) {
                result.add(definition.copy());
            }
        }
        return result;
    }


    public static final class RankBand {
        public String name = "C";
        public double threshold = 0.0D;

        public RankBand() {
        }

        public RankBand(String name, double threshold) {
            this.name = name;
            this.threshold = threshold;
        }

        public String name() {
            return name;
        }

        public double threshold() {
            return threshold;
        }
    }

    public static final class ModifierRule {
        public int id = 1;
        public String difficulty = RunDifficulty.NORMAL.name();
        public int weight = 1;
        public boolean enabled = true;

        public ModifierRule() {
        }

        public ModifierRule(int id, String difficulty, int weight, boolean enabled) {
            this.id = id;
            this.difficulty = difficulty;
            this.weight = weight;
            this.enabled = enabled;
        }

        public int id() {
            return id;
        }

        public String difficulty() {
            return difficulty;
        }

        public int weight() {
            return weight;
        }

        public boolean enabled() {
            return enabled;
        }

        public RunDifficulty difficultyEnum() {
            return RunDifficulty.fromId(difficulty);
        }

        public boolean allowedIn(RunDifficulty runDifficulty) {
            RunDifficulty safe = runDifficulty == null ? RunDifficulty.NORMAL : runDifficulty;
            return enabled && safe.allows(difficultyEnum());
        }
    }
}
