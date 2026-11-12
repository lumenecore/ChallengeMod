package com.lumenechallenge.challenge.condition;

import com.lumenechallenge.challenge.ChallengeContext;
import com.lumenechallenge.challenge.TriggerSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CompositeCondition extends AbstractCondition {
    private final List<Condition> conditions;

    public CompositeCondition(List<Condition> conditions) {
        if (conditions.isEmpty()) {
            throw new IllegalArgumentException("CompositeCondition requires at least one condition");
        }

        List<Condition> flattened = new ArrayList<>();
        for (Condition condition : conditions) {
            if (condition instanceof CompositeCondition composite) {
                flattened.addAll(composite.conditions);
            } else {
                flattened.add(condition);
            }
        }

        if (flattened.isEmpty()) {
            throw new IllegalArgumentException("CompositeCondition requires at least one non-composite condition");
        }

        this.conditions = Collections.unmodifiableList(flattened);
    }

    public static CompositeCondition of(Condition first, Condition second) {
        List<Condition> list = new ArrayList<>();
        if (first instanceof CompositeCondition compositeFirst) {
            list.addAll(compositeFirst.conditions);
        } else {
            list.add(first);
        }
        if (second instanceof CompositeCondition compositeSecond) {
            list.addAll(compositeSecond.conditions);
        } else {
            list.add(second);
        }
        return new CompositeCondition(list);
    }

    @Override
    public boolean check(ChallengeContext context) {
        for (Condition condition : conditions) {
            if (!condition.check(context)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Text getDescription() {
        MutableText result = Text.empty();
        for (int i = 0; i < conditions.size(); i++) {
            if (i > 0) {
                result.append(Text.literal(" + "));
            }
            result.append(conditions.get(i).getDescription());
        }
        return result;
    }

    @Override
    public ConditionKind getKind() {
        return ConditionKind.COMPOSITE;
    }

    @Override
    public TriggerSource trigger() {
        return conditions.get(0).trigger();
    }

    @Override
    protected void writeFields(NbtCompound tag) {
        NbtList list = new NbtList();
        for (Condition condition : conditions) {
            list.add(condition.toNbt());
        }
        tag.put("conditions", list);
    }

    public static CompositeCondition fromNbt(NbtCompound tag) {
        NbtList list = tag.getList("conditions").orElse(new NbtList());
        List<Condition> conditions = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            conditions.add(Condition.fromNbt(list.getCompound(i).orElse(new NbtCompound())));
        }
        return new CompositeCondition(conditions);
    }
}
