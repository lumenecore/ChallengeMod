package com.lumenechallenge.challenge.condition;

import com.lumenechallenge.challenge.ChallengeContext;
import com.lumenechallenge.challenge.TriggerSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

public final class BurningCondition extends AbstractCondition {
    @Override
    public boolean check(ChallengeContext context) {
        return context.onFire();
    }

    @Override
    public Text getDescription() {
        return Text.translatable("condition.lumenechallenge.burning");
    }

    @Override
    public ConditionKind getKind() {
        return ConditionKind.BURNING;
    }

    @Override
    public TriggerSource trigger() {
        return TriggerSource.TICK;
    }

    @Override
    protected void writeFields(NbtCompound tag) {
    }

    public static BurningCondition fromNbt(NbtCompound tag) {
        return new BurningCondition();
    }
}
