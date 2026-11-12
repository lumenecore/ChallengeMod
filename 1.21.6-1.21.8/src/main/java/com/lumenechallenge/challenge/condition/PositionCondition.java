package com.lumenechallenge.challenge.condition;

import com.lumenechallenge.client.layout.ModI18n;

import com.lumenechallenge.challenge.ChallengeContext;
import com.lumenechallenge.challenge.TriggerSource;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

public final class PositionCondition extends AbstractCondition {
    public enum Mode { ON_BLOCK, IN_AIR, STANDING_ON_BLOCK, Y_RANGE, ABOVE_Y, BELOW_Y, EXACT_Y }

    private final Mode mode;
    private final int a;
    private final int b;
    private final String blockId;

    public PositionCondition(Mode mode, int a, int b, String blockId) {
        this.mode = mode;
        this.a = a;
        this.b = b;
        this.blockId = blockId;
    }

    @Override
    public boolean check(ChallengeContext context) {
        int y = context.position().getY();
        return switch (mode) {
            case ON_BLOCK -> context.player().isOnGround();
            case IN_AIR -> !context.player().isOnGround();
            case STANDING_ON_BLOCK -> {
                BlockState at = context.world().getBlockState(context.position());
                BlockState below = context.world().getBlockState(context.position().down());
                yield net.minecraft.registry.Registries.BLOCK.getId(at.getBlock()).toString().equals(blockId)
                        || net.minecraft.registry.Registries.BLOCK.getId(below.getBlock()).toString().equals(blockId);
            }
            case Y_RANGE -> y >= Math.min(a, b) && y <= Math.max(a, b);
            case ABOVE_Y -> y > a;
            case BELOW_Y -> y < a;
            case EXACT_Y -> y == a;
        };
    }

    @Override
    public Text getDescription() {
        return switch (mode) {
            case ON_BLOCK -> ModI18n.text("condition.lumenechallenge.position.on_block");
            case IN_AIR -> ModI18n.text("condition.lumenechallenge.position.in_air");
            case STANDING_ON_BLOCK -> {
                var block = Registries.BLOCK.get(Identifier.of(blockId));
                yield ModI18n.text("condition.lumenechallenge.position.on_specific_block", ModI18n.text(block.getTranslationKey()));
            }
            case Y_RANGE -> ModI18n.text("condition.lumenechallenge.position.y_range", a, b);
            case ABOVE_Y -> ModI18n.text("condition.lumenechallenge.position.above_y", a);
            case BELOW_Y -> ModI18n.text("condition.lumenechallenge.position.below_y", a);
            case EXACT_Y -> ModI18n.text("condition.lumenechallenge.position.exact_y", a);
        };
    }

    @Override
    public ConditionKind getKind() {
        return ConditionKind.POSITION;
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
        tag.putString("blockId", blockId);
    }

    public static PositionCondition fromNbt(NbtCompound tag) {
        return new PositionCondition(
                Mode.valueOf(tag.getString("mode", Mode.ON_BLOCK.name())),
                tag.getInt("a", 0),
                tag.getInt("b", 0),
                tag.getString("blockId", "minecraft:stone")
        );
    }
}
