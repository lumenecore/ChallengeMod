package com.lumenechallenge.challenge;

import com.lumenechallenge.challenge.condition.Condition;
import com.lumenechallenge.challenge.condition.ObjectiveCondition;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Challenge {
    private final ChallengeType type;
    private final List<Condition> conditions;
    private final List<ChallengeTaskDefinition> taskDefinitions;
    private int completedSteps;

    public Challenge(ChallengeType type, List<Condition> conditions, List<ChallengeTaskDefinition> taskDefinitions, int completedSteps) {
        this.type = type;
        this.conditions = Collections.unmodifiableList(new ArrayList<>(conditions));
        this.taskDefinitions = Collections.unmodifiableList(new ArrayList<>(taskDefinitions));
        this.completedSteps = completedSteps;
    }

    public Challenge(ChallengeType type, List<Condition> conditions, int completedSteps) {
        this(type, conditions, defaultDefinitions(conditions), completedSteps);
    }

    public ChallengeType type() {
        return type;
    }

    public List<Condition> conditions() {
        return conditions;
    }

    public List<ChallengeTaskDefinition> taskDefinitions() {
        return taskDefinitions;
    }

    public ChallengeTaskDefinition currentTaskDefinition() {
        if (isComplete() || completedSteps < 0 || completedSteps >= taskDefinitions.size()) {
            return null;
        }
        return taskDefinitions.get(completedSteps);
    }

    public int totalTasks() {
        return taskDefinitions.size();
    }

    public int completedTaskCount() {
        return Math.min(Math.max(completedSteps, 0), taskDefinitions.size());
    }

    public int completedSteps() {
        return completedSteps;
    }

    public void setCompletedSteps(int completedSteps) {
        this.completedSteps = completedSteps;
    }

    public boolean isComplete() {
        return completedSteps >= conditions.size();
    }

    public Condition currentCondition() {
        if (isComplete()) {
            return null;
        }
        return conditions.get(completedSteps);
    }

    public Challenge withReplacement(int index, Condition condition, ChallengeTaskDefinition definition) {
        List<Condition> updatedConditions = new ArrayList<>(conditions);
        List<ChallengeTaskDefinition> updatedDefinitions = new ArrayList<>(taskDefinitions);
        if (index >= 0 && index < updatedConditions.size()) {
            updatedConditions.set(index, condition);
        }
        if (index >= 0 && index < updatedDefinitions.size()) {
            updatedDefinitions.set(index, definition);
        }
        return new Challenge(type, updatedConditions, updatedDefinitions, completedSteps);
    }

    public NbtCompound toNbt() {
        NbtCompound tag = new NbtCompound();
        tag.putString("type", type.id());
        tag.putInt("completedSteps", completedSteps);

        NbtList conditionList = new NbtList();
        for (Condition condition : conditions) {
            NbtCompound conditionTag = condition.toNbt();
            conditionTag.putString("kind", condition.getKind().id());
            conditionList.add(conditionTag);
        }
        tag.put("conditions", conditionList);

        NbtList structuredTaskList = new NbtList();
        for (ChallengeTaskDefinition definition : taskDefinitions) {
            structuredTaskList.add(definition == null ? new ChallengeTaskDefinition().toNbt() : definition.toNbt());
        }
        tag.put("tasks", structuredTaskList);
        return tag;
    }

    public static Challenge fromNbt(NbtCompound tag) {
        String typeId = tag.getString("type").orElse(ChallengeType.DEATH.name());
        ChallengeType type = ChallengeType.valueOf(typeId);

        int completedSteps = tag.getInt("completedSteps").orElse(0);
        NbtList conditionList = tag.getList("conditions").orElse(new NbtList());
        List<Condition> conditions = new ArrayList<>();
        for (int i = 0; i < conditionList.size(); i++) {
            NbtCompound conditionTag = conditionList.getCompound(i).orElse(new NbtCompound());
            conditions.add(Condition.fromNbt(conditionTag));
        }

        NbtList taskList = tag.getList("tasks").orElse(new NbtList());
        List<ChallengeTaskDefinition> definitions = new ArrayList<>();
        for (int i = 0; i < taskList.size(); i++) {
            definitions.add(ChallengeTaskDefinition.fromNbt(taskList.getCompound(i).orElse(new NbtCompound())));
        }
        if (definitions.isEmpty()) {
            definitions.addAll(defaultDefinitions(conditions));
        }
        if (definitions.size() < conditions.size()) {
            List<ChallengeTaskDefinition> fallback = defaultDefinitions(conditions);
            for (int i = definitions.size(); i < conditions.size(); i++) {
                if (i < fallback.size()) {
                    definitions.add(fallback.get(i));
                } else {
                    definitions.add(new ChallengeTaskDefinition());
                }
            }
        }
        if (definitions.size() > conditions.size()) {
            definitions = new ArrayList<>(definitions.subList(0, conditions.size()));
        }

        return new Challenge(type, conditions, definitions, completedSteps);
    }

    private static List<ChallengeTaskDefinition> defaultDefinitions(List<Condition> conditions) {
        List<ChallengeTaskDefinition> definitions = new ArrayList<>();
        ChallengeConfig defaults = ChallengeConfig.defaults();
        for (Condition condition : conditions) {
            if (condition instanceof ObjectiveCondition objectiveCondition) {
                ChallengeTaskDefinition match = null;
                for (ChallengeTaskDefinition definition : defaults.tasks()) {
                    if (definition.typeEnum() == objectiveCondition.type()) {
                        match = definition.copy();
                        break;
                    }
                }
                if (match == null) {
                    match = new ChallengeTaskDefinition(objectiveCondition.type().name(), RunDifficulty.NORMAL.name(), 60, true);
                }
                definitions.add(match);
            } else {
                definitions.add(new ChallengeTaskDefinition());
            }
        }
        return definitions;
    }
}
