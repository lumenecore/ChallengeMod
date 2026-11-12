package com.lumenechallenge.challenge.condition;

import com.lumenechallenge.client.layout.ModI18n;

import com.lumenechallenge.challenge.ChallengeContext;
import com.lumenechallenge.challenge.TriggerSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

public final class WaterCondition extends AbstractCondition {
    public enum Mode { IN_WATER, OUT_OF_WATER }

    private final Mode mode;

    public Mode mode() {
        return mode;
    }

    public WaterCondition(Mode mode) {
        this.mode = mode;
    }

    @Override
    public boolean check(ChallengeContext context) {
        boolean inWater = context.player().isTouchingWater() || context.player().isSubmergedInWater();
        return mode == Mode.IN_WATER ? inWater : !inWater;
    }

    @Override
    public Text getDescription() {
        return switch (mode) {
            case IN_WATER -> ModI18n.text("condition.lumenechallenge.water.in");
            case OUT_OF_WATER -> ModI18n.text("condition.lumenechallenge.water.out");
        };
    }

    @Override
    public ConditionKind getKind() {
        return ConditionKind.WATER;
    }

    @Override
    public TriggerSource trigger() {
        return TriggerSource.TICK;
    }

    @Override
    protected void writeFields(NbtCompound tag) {
        tag.putString("mode", mode.name());
    }

    public static WaterCondition fromNbt(NbtCompound tag) {
        String mode = tag.getString("mode").orElse(Mode.OUT_OF_WATER.name());
        return new WaterCondition(Mode.valueOf(mode));
    }
}
