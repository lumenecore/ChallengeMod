package com.lumenechallenge.challenge.condition;

import com.lumenechallenge.challenge.ChallengeContext;
import com.lumenechallenge.challenge.TriggerSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

public final class WeatherCondition extends AbstractCondition {
    public enum Mode { CLEAR, RAIN, THUNDER }

    private final Mode mode;

    public WeatherCondition(Mode mode) {
        this.mode = mode;
    }

    @Override
    public boolean check(ChallengeContext context) {
        return switch (mode) {
            case CLEAR -> !context.raining() && !context.thundering();
            case RAIN -> context.raining() && !context.thundering();
            case THUNDER -> context.thundering();
        };
    }

    @Override
    public Text getDescription() {
        return switch (mode) {
            case CLEAR -> Text.translatable("condition.lumenechallenge.clear_weather");
            case RAIN -> Text.translatable("condition.lumenechallenge.rain");
            case THUNDER -> Text.translatable("condition.lumenechallenge.thunder");
        };
    }

    @Override
    public ConditionKind getKind() {
        return ConditionKind.WEATHER;
    }

    @Override
    public TriggerSource trigger() {
        return TriggerSource.TICK;
    }

    @Override
    protected void writeFields(NbtCompound tag) {
        tag.putString("mode", mode.name());
    }

    public static WeatherCondition fromNbt(NbtCompound tag) {
        String mode = tag.getString("mode").orElse(Mode.CLEAR.name());
        return new WeatherCondition(Mode.valueOf(mode));
    }
}
