package com.lumenechallenge.challenge.condition;

import com.lumenechallenge.challenge.ChallengeContext;
import com.lumenechallenge.challenge.TriggerSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

public final class DamageCondition extends AbstractCondition {
    @Override
    public boolean check(ChallengeContext context) {
        return context.damagedThisTick();
    }

    @Override
    public Text getDescription() {
        return Text.translatable("condition.lumenechallenge.damage");
    }

    @Override
    public ConditionKind getKind() {
        return ConditionKind.DAMAGE;
    }

    @Override
    public TriggerSource trigger() {
        return TriggerSource.TICK;
    }

    @Override
    protected void writeFields(NbtCompound tag) {
    }

    public static DamageCondition fromNbt(NbtCompound tag) {
        return new DamageCondition();
    }
}
