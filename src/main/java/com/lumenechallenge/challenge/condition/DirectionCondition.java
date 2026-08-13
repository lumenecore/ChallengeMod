package com.lumenechallenge.challenge.condition;

import com.lumenechallenge.client.layout.ModI18n;

import com.lumenechallenge.challenge.ChallengeContext;
import com.lumenechallenge.challenge.TriggerSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;

public final class DirectionCondition extends AbstractCondition {
    public enum Mode { NORTH, SOUTH, EAST, WEST, UP, DOWN }

    private final Mode mode;

    public DirectionCondition(Mode mode) {
        this.mode = mode;
    }

    @Override
    public boolean check(ChallengeContext context) {
        Direction facing = context.player().getHorizontalFacing();
        return switch (mode) {
            case NORTH -> facing == Direction.NORTH;
            case SOUTH -> facing == Direction.SOUTH;
            case EAST -> facing == Direction.EAST;
            case WEST -> facing == Direction.WEST;
            case UP -> context.player().getPitch() <= -60.0f;
            case DOWN -> context.player().getPitch() >= 60.0f;
        };
    }

    @Override
    public Text getDescription() {
        return switch (mode) {
            case NORTH -> ModI18n.text("condition.lumenechallenge.direction.north");
            case SOUTH -> ModI18n.text("condition.lumenechallenge.direction.south");
            case EAST -> ModI18n.text("condition.lumenechallenge.direction.east");
            case WEST -> ModI18n.text("condition.lumenechallenge.direction.west");
            case UP -> ModI18n.text("condition.lumenechallenge.direction.up");
            case DOWN -> ModI18n.text("condition.lumenechallenge.direction.down");
        };
    }

    @Override
    public ConditionKind getKind() {
        return ConditionKind.DIRECTION;
    }

    @Override
    public TriggerSource trigger() {
        return TriggerSource.TICK;
    }

    @Override
    protected void writeFields(NbtCompound tag) {
        tag.putString("mode", mode.name());
    }

    public static DirectionCondition fromNbt(NbtCompound tag) {
        return new DirectionCondition(Mode.valueOf(tag.getString("mode").orElse(Mode.NORTH.name())));
    }
}
