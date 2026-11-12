package com.lumenechallenge.challenge.condition;

import com.lumenechallenge.client.layout.ModI18n;

import com.lumenechallenge.challenge.ChallengeContext;
import com.lumenechallenge.challenge.TriggerSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.world.World;

public final class DimensionCondition extends AbstractCondition {
    public enum Mode { OVERWORLD, NETHER, END }

    private final Mode mode;

    public DimensionCondition(Mode mode) {
        this.mode = mode;
    }

    @Override
    public boolean check(ChallengeContext context) {
        return switch (mode) {
            case OVERWORLD -> context.world().getRegistryKey().equals(World.OVERWORLD);
            case NETHER -> context.world().getRegistryKey().equals(World.NETHER);
            case END -> context.world().getRegistryKey().equals(World.END);
        };
    }

    @Override
    public Text getDescription() {
        return switch (mode) {
            case OVERWORLD -> ModI18n.text("condition.lumenechallenge.dimension.overworld");
            case NETHER -> ModI18n.text("condition.lumenechallenge.dimension.nether");
            case END -> ModI18n.text("condition.lumenechallenge.dimension.end");
        };
    }

    @Override
    public ConditionKind getKind() {
        return ConditionKind.DIMENSION;
    }

    @Override
    public TriggerSource trigger() {
        return TriggerSource.TICK;
    }

    @Override
    protected void writeFields(NbtCompound tag) {
        tag.putString("mode", mode.name());
    }

    public static DimensionCondition fromNbt(NbtCompound tag) {
        String mode = tag.getString("mode").orElse(Mode.OVERWORLD.name());
        return new DimensionCondition(Mode.valueOf(mode));
    }
}
