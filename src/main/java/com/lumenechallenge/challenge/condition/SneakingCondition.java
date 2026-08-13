package com.lumenechallenge.challenge.condition;

import com.lumenechallenge.challenge.ChallengeContext;
import com.lumenechallenge.challenge.TriggerSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

public final class SneakingCondition extends AbstractCondition {
    private final boolean sneaking;

    public boolean sneaking() {
        return sneaking;
    }

    public SneakingCondition(boolean sneaking) {
        this.sneaking = sneaking;
    }

    @Override
    public boolean check(ChallengeContext context) {
        return context.player().isSneaking() == sneaking;
    }

    @Override
    public Text getDescription() {
        return sneaking
                ? Text.translatable("condition.lumenechallenge.sneaking.on")
                : Text.translatable("condition.lumenechallenge.sneaking.off");
    }

    @Override
    public ConditionKind getKind() {
        return ConditionKind.SNEAKING;
    }

    @Override
    public TriggerSource trigger() {
        return TriggerSource.TICK;
    }

    @Override
    protected void writeFields(NbtCompound tag) {
        tag.putBoolean("sneaking", sneaking);
    }

    public static SneakingCondition fromNbt(NbtCompound tag) {
        return new SneakingCondition(tag.getBoolean("sneaking").orElse(true));
    }
}
