package com.lumenechallenge.challenge.condition;

import com.lumenechallenge.challenge.ChallengeContext;
import com.lumenechallenge.challenge.TriggerSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

public final class StepCondition implements Condition {
    private final Condition displayCondition;
    private final Condition logicCondition;

    public StepCondition(Condition displayCondition, Condition logicCondition) {
        this.displayCondition = displayCondition;
        this.logicCondition = logicCondition;
    }

    public Condition displayCondition() {
        return displayCondition;
    }

    public Condition logicCondition() {
        return logicCondition;
    }

    @Override
    public boolean check(ChallengeContext context) {
        return logicCondition.check(context);
    }

    @Override
    public Text getDescription() {
        return displayCondition.getDescription();
    }

    @Override
    public ConditionKind getKind() {
        return ConditionKind.STEP;
    }

    @Override
    public TriggerSource trigger() {
        return logicCondition.trigger();
    }

    @Override
    public NbtCompound toNbt() {
        NbtCompound tag = new NbtCompound();
        tag.putString("kind", getKind().id());
        tag.put("display", displayCondition.toNbt());
        tag.put("logic", logicCondition.toNbt());
        return tag;
    }

    public static StepCondition fromNbt(NbtCompound tag) {
        Condition display = Condition.fromNbt(tag.getCompound("display").orElse(new NbtCompound()));
        Condition logic = Condition.fromNbt(tag.getCompound("logic").orElse(new NbtCompound()));
        return new StepCondition(display, logic);
    }
}
