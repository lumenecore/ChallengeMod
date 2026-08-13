package com.lumenechallenge.challenge.condition;

import com.lumenechallenge.challenge.ChallengeContext;
import com.lumenechallenge.challenge.TriggerSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

public final class DeathCondition extends AbstractCondition {
    @Override
    public boolean check(ChallengeContext context) {
        return context.diedThisTick();
    }

    @Override
    public Text getDescription() {
        return Text.translatable("condition.lumenechallenge.death");
    }

    @Override
    public ConditionKind getKind() {
        return ConditionKind.DEATH;
    }

    @Override
    public TriggerSource trigger() {
        return TriggerSource.DEATH;
    }

    @Override
    protected void writeFields(NbtCompound tag) {
    }

    public static DeathCondition fromNbt(NbtCompound tag) {
        return new DeathCondition();
    }
}
