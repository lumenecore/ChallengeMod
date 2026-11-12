package com.lumenechallenge.challenge;

import com.lumenechallenge.challenge.condition.ObjectiveCondition;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChallengeTaskDefinition {
    public String type = ObjectiveCondition.Type.JUMP.name();
    public String difficulty = RunDifficulty.NORMAL.name();
    public int recommendedSeconds = 60;
    public int recommendedSecondsNormal = 60;
    public int recommendedSecondsHard = 60;
    public int recommendedSecondsInsane = 60;
    public boolean enabled = true;
    public int frequency = 1;
    public List<Integer> modifiers = new ArrayList<>();

    public ChallengeTaskDefinition() {
    }

    public ChallengeTaskDefinition(String type, String difficulty, int recommendedSecondsNormal, boolean enabled) {
        this(type, difficulty, recommendedSecondsNormal, recommendedSecondsNormal, recommendedSecondsNormal, enabled, 1, List.of());
    }

    public ChallengeTaskDefinition(String type, String difficulty, int recommendedSecondsNormal, int recommendedSecondsHard, int recommendedSecondsInsane, boolean enabled, int frequency, List<Integer> modifiers) {
        this.type = type;
        this.difficulty = difficulty;
        this.recommendedSeconds = recommendedSecondsNormal;
        this.recommendedSecondsNormal = recommendedSecondsNormal;
        this.recommendedSecondsHard = recommendedSecondsHard;
        this.recommendedSecondsInsane = recommendedSecondsInsane;
        this.enabled = enabled;
        this.frequency = frequency;
        this.modifiers = modifiers == null ? new ArrayList<>() : new ArrayList<>(modifiers);
    }

    public String type() {
        return type;
    }

    public String difficulty() {
        return difficulty;
    }

    public int recommendedSecondsNormal() {
        return recommendedSecondsNormal;
    }

    public int recommendedSecondsHard() {
        return recommendedSecondsHard;
    }

    public int recommendedSecondsInsane() {
        return recommendedSecondsInsane;
    }

    public int recommendedSeconds(RunDifficulty runDifficulty) {
        int normal = recommendedSecondsNormal > 0 ? recommendedSecondsNormal : recommendedSeconds;
        int hard = recommendedSecondsHard > 0 ? recommendedSecondsHard : normal;
        int insane = recommendedSecondsInsane > 0 ? recommendedSecondsInsane : hard;
        return switch (runDifficulty == null ? RunDifficulty.NORMAL : runDifficulty) {
            case NORMAL -> normal;
            case HARD -> hard;
            case INSANE -> insane;
        };
    }

    public boolean enabled() {
        return enabled;
    }

    public int frequency() {
        return frequency;
    }

    public List<Integer> modifierIds() {
        if (modifiers == null) {
            return List.of();
        }
        return Collections.unmodifiableList(modifiers);
    }

    public ObjectiveCondition.Type typeEnum() {
        try {
            return ObjectiveCondition.Type.valueOf(type);
        } catch (IllegalArgumentException ex) {
            return ObjectiveCondition.Type.JUMP;
        }
    }

    public RunDifficulty difficultyEnum() {
        return RunDifficulty.fromId(difficulty);
    }

    public boolean allowedIn(RunDifficulty runDifficulty) {
        return enabled && runDifficulty.allows(difficultyEnum());
    }

    public ChallengeTaskDefinition copy() {
        ChallengeTaskDefinition copy = new ChallengeTaskDefinition(type, difficulty, recommendedSecondsNormal, recommendedSecondsHard, recommendedSecondsInsane, enabled, frequency, modifiers);
        copy.recommendedSeconds = recommendedSeconds;
        return copy;
    }

    public NbtCompound toNbt() {
        NbtCompound tag = new NbtCompound();
        tag.putString("type", type);
        tag.putString("difficulty", difficulty);
        tag.putInt("recommendedSeconds", recommendedSeconds);
        tag.putInt("recommendedSecondsNormal", recommendedSecondsNormal);
        tag.putInt("recommendedSecondsHard", recommendedSecondsHard);
        tag.putInt("recommendedSecondsInsane", recommendedSecondsInsane);
        tag.putBoolean("enabled", enabled);
        tag.putInt("frequency", frequency);

        NbtList modifierList = new NbtList();
        if (modifiers != null) {
            for (Integer id : modifiers) {
                modifierList.add(NbtInt.of(id == null ? 0 : id));
            }
        }
        tag.put("modifiers", modifierList);
        return tag;
    }

    public static ChallengeTaskDefinition fromNbt(NbtCompound tag) {
        ChallengeTaskDefinition definition = new ChallengeTaskDefinition();
        definition.type = tag.getString("type", ObjectiveCondition.Type.JUMP.name());
        definition.difficulty = tag.getString("difficulty", RunDifficulty.NORMAL.name());
        definition.recommendedSeconds = tag.getInt("recommendedSeconds", 60);
        definition.recommendedSecondsNormal = tag.getInt("recommendedSecondsNormal", definition.recommendedSeconds);
        definition.recommendedSecondsHard = tag.getInt("recommendedSecondsHard", definition.recommendedSecondsNormal);
        definition.recommendedSecondsInsane = tag.getInt("recommendedSecondsInsane", definition.recommendedSecondsHard);
        definition.enabled = tag.getBoolean("enabled", true);
        definition.frequency = Math.max(1, tag.getInt("frequency", 1));
        NbtList modifierList = tag.getList("modifiers").orElse(new NbtList());
        definition.modifiers = new ArrayList<>();
        for (int i = 0; i < modifierList.size(); i++) {
            definition.modifiers.add(modifierList.getInt(i).orElse(0));
        }
        return definition;
    }
}
