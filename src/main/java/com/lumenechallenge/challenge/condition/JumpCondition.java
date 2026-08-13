package com.lumenechallenge.challenge.condition;

import com.lumenechallenge.challenge.ChallengeContext;
import com.lumenechallenge.challenge.TriggerSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

public final class JumpCondition extends AbstractCondition {
    @Override
    public boolean check(ChallengeContext context) {
        return context.jumpedThisTick();
    }

    @Override
    public Text getDescription() {
        return Text.translatable("condition.lumenechallenge.jump");
    }

    @Override
    public ConditionKind getKind() {
        return ConditionKind.JUMP;
    }

    @Override
    public TriggerSource trigger() {
        return TriggerSource.TICK;
    }

    @Override
    protected void writeFields(NbtCompound tag) {
    }

    public static JumpCondition fromNbt(NbtCompound tag) {
        return new JumpCondition();
    }
}
