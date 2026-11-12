package com.lumenechallenge.challenge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ChallengeConfig {
    public List<ChallengeTaskDefinition> tasks = new ArrayList<>();
    public List<ModifierRule> modifiers = new ArrayList<>();

    public ChallengeConfig() {
    }



    public List<ChallengeTaskDefinition> tasks() {
        return tasks;
    }

    public List<ModifierRule> modifiers() {
        return modifiers;
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


    public ModifierRule modifierRuleById(int id) {
        for (ModifierRule rule : modifiers) {
            if (rule != null && rule.id == id) {
                return rule;
            }
        }
        return null;
    }

    public void sanitize() {
        if (tasks == null) {
            tasks = new ArrayList<>();
        }
        if (modifiers == null) {
            modifiers = new ArrayList<>();
        }


        ChallengeConfig bundled = ChallengeConfigManager.loadBundledDefaultsRaw();
        if (bundled != null) {
            if (modifiers.isEmpty() && bundled.modifiers != null) {
                modifiers = copyModifiers(bundled.modifiers);
            }
            if (tasks.isEmpty() && bundled.tasks != null) {
                tasks = copyTasks(bundled.tasks);
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
