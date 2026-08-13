package com.lumenechallenge.challenge.condition;

import com.lumenechallenge.challenge.ChallengeContext;
import com.lumenechallenge.challenge.TriggerSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

public final class InventoryCondition extends AbstractCondition {
    public enum Mode { EMPTY, FULL }

    private final Mode mode;

    public Mode mode() {
        return mode;
    }

    public InventoryCondition(Mode mode) {
        this.mode = mode;
    }

    @Override
    public boolean check(ChallengeContext context) {
        boolean empty = true;
        boolean full = true;
        for (int i = 0; i < context.inventory().size(); i++) {
            if (context.inventory().getStack(i).isEmpty()) {
                full = false;
            } else {
                empty = false;
            }
        }
        return mode == Mode.EMPTY ? empty : full;
    }

    @Override
    public Text getDescription() {
        return mode == Mode.EMPTY
                ? Text.translatable("condition.lumenechallenge.inventory.empty")
                : Text.translatable("condition.lumenechallenge.inventory.full");
    }

    @Override
    public ConditionKind getKind() {
        return ConditionKind.INVENTORY;
    }

    @Override
    public TriggerSource trigger() {
        return TriggerSource.TICK;
    }

    @Override
    protected void writeFields(NbtCompound tag) {
        tag.putString("mode", mode.name());
    }

    public static InventoryCondition fromNbt(NbtCompound tag) {
        return new InventoryCondition(Mode.valueOf(tag.getString("mode").orElse(Mode.EMPTY.name())));
    }
}
