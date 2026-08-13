package com.lumenechallenge.challenge;

import com.lumenechallenge.challenge.condition.Condition;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Challenge {
    private final ChallengeType type;
    private final List<Condition> conditions;
    private int completedSteps;

    public Challenge(ChallengeType type, List<Condition> conditions, int completedSteps) {
        this.type = type;
        this.conditions = Collections.unmodifiableList(new ArrayList<>(conditions));
        this.completedSteps = completedSteps;
    }

    public ChallengeType type() {
        return type;
    }

    public List<Condition> conditions() {
        return conditions;
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

    public NbtCompound toNbt() {
        NbtCompound tag = new NbtCompound();
        tag.putString("type", type.id());
        tag.putInt("completedSteps", completedSteps);

        NbtList list = new NbtList();
        for (Condition condition : conditions) {
            NbtCompound conditionTag = condition.toNbt();
            conditionTag.putString("kind", condition.getKind().id());
            list.add(conditionTag);
        }

        tag.put("conditions", list);
        return tag;
    }

    public static Challenge fromNbt(NbtCompound tag) {
        String typeId = tag.getString("type").orElse(ChallengeType.DEATH.name());

        ChallengeType type = ChallengeType.valueOf(typeId);

        int completedSteps = tag.getInt("completedSteps").orElse(0);

        NbtList list = tag.getList("conditions").orElse(new NbtList());

        List<Condition> conditions = new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            NbtCompound conditionTag = list.getCompound(i).orElse(new NbtCompound());

            conditions.add(Condition.fromNbt(conditionTag));
        }

        return new Challenge(type, conditions, completedSteps);
    }
}
