package com.lumenechallenge.challenge.condition;

import com.lumenechallenge.challenge.ChallengeContext;
import com.lumenechallenge.challenge.TriggerSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

public final class MovementCondition extends AbstractCondition {
    private final int stationaryTicks;

    public MovementCondition(int stationaryTicks) {
        this.stationaryTicks = stationaryTicks;
    }

    @Override
    public boolean check(ChallengeContext context) {
        return context.stationaryTicks() >= stationaryTicks;
    }

    @Override
    public Text getDescription() {
        int seconds = Math.max(1, (int) Math.ceil(stationaryTicks / 20.0));
        return Text.translatable("condition.lumenechallenge.still_for", seconds);
    }

    @Override
    public ConditionKind getKind() {
        return ConditionKind.MOVEMENT;
    }

    @Override
    public TriggerSource trigger() {
        return TriggerSource.TICK;
    }

    @Override
    protected void writeFields(NbtCompound tag) {
        tag.putInt("stationaryTicks", stationaryTicks);
    }

    public static MovementCondition fromNbt(NbtCompound tag) {
        return new MovementCondition(tag.getInt("stationaryTicks").orElse(200));
    }
}
