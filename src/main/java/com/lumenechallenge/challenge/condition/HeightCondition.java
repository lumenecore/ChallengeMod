package com.lumenechallenge.challenge.condition;

import com.lumenechallenge.challenge.ChallengeContext;
import com.lumenechallenge.challenge.TriggerSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

public final class HeightCondition extends AbstractCondition {
    public enum Mode { RANGE, EXACT, ABOVE, BELOW, AIR, ON_BLOCK }

    private final Mode mode;
    private final int a;
    private final int b;

    public HeightCondition(Mode mode, int a, int b) {
        this.mode = mode;
        this.a = a;
        this.b = b;
    }

    @Override
    public boolean check(ChallengeContext context) {
        int y = context.position().getY();
        return switch (mode) {
            case RANGE -> y >= Math.min(a, b) && y <= Math.max(a, b);
            case EXACT -> y == a;
            case ABOVE -> y > a;
            case BELOW -> y < a;
            case AIR -> !context.player().isOnGround();
            case ON_BLOCK -> context.player().isOnGround();
        };
    }

    @Override
    public Text getDescription() {
        return switch (mode) {
            case RANGE -> Text.translatable("condition.lumenechallenge.height.range", a, b);
            case EXACT -> Text.translatable("condition.lumenechallenge.height.exact", a);
            case ABOVE -> Text.translatable("condition.lumenechallenge.height.above", a);
            case BELOW -> Text.translatable("condition.lumenechallenge.height.below", a);
            case AIR -> Text.translatable("condition.lumenechallenge.height.air");
            case ON_BLOCK -> Text.translatable("condition.lumenechallenge.height.on_block");
        };
    }

    @Override
    public ConditionKind getKind() {
        return ConditionKind.HEIGHT;
    }

    @Override
    public TriggerSource trigger() {
        return TriggerSource.TICK;
    }

    @Override
    protected void writeFields(NbtCompound tag) {
        tag.putString("mode", mode.name());
        tag.putInt("a", a);
        tag.putInt("b", b);
    }

    public static HeightCondition fromNbt(NbtCompound tag) {
        Mode mode = Mode.valueOf(tag.getString("mode", Mode.RANGE.name()));
        return new HeightCondition(mode, tag.getInt("a", 0), tag.getInt("b", 0));
    }
}
