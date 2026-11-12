package com.lumenechallenge.challenge.condition;

import com.lumenechallenge.client.layout.ModI18n;

import com.lumenechallenge.challenge.ChallengeContext;
import com.lumenechallenge.challenge.TriggerSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

public final class TimeCondition extends AbstractCondition {
    public enum Mode { DAY, NIGHT }

    private final Mode mode;

    public TimeCondition(Mode mode) {
        this.mode = mode == null ? Mode.DAY : mode;
    }

    @Override
    public boolean check(ChallengeContext context) {
        long timeOfDay = context.world().getTimeOfDay() % 24000L;
        return mode == Mode.DAY ? timeOfDay < 12000L : timeOfDay >= 12000L;
    }

    @Override
    public Text getDescription() {
        return ModI18n.text(mode == Mode.DAY ? "condition.lumenechallenge.day" : "condition.lumenechallenge.night");
    }

    @Override
    public ConditionKind getKind() {
        return ConditionKind.TIME;
    }

    @Override
    public TriggerSource trigger() {
        return TriggerSource.TICK;
    }

    @Override
    protected void writeFields(NbtCompound tag) {
        tag.putString("mode", mode.name());
    }

    public static TimeCondition fromNbt(NbtCompound tag) {
        String modeName = tag.getString("mode").orElse(Mode.DAY.name());
        Mode mode = "NIGHT".equalsIgnoreCase(modeName) ? Mode.NIGHT : Mode.DAY;
        return new TimeCondition(mode);
    }
}
