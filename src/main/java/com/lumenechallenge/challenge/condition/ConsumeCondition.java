package com.lumenechallenge.challenge.condition;

import com.lumenechallenge.challenge.ChallengeContext;
import com.lumenechallenge.challenge.TriggerSource;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class ConsumeCondition extends AbstractCondition {
    public enum Mode { EAT, DRINK }

    private final Mode mode;
    private final String itemId;

    public ConsumeCondition(Mode mode, String itemId) {
        this.mode = mode;
        this.itemId = itemId;
    }

    @Override
    public boolean check(ChallengeContext context) {
        ItemStack stack = context.consumedItem();
        if (stack == null) {
            return false;
        }
        boolean matches = itemId.isBlank() || Registries.ITEM.getId(stack.getItem()).toString().equals(itemId);
        return matches;
    }

    @Override
    public Text getDescription() {
        Text item = itemId.isBlank()
                ? Text.translatable("condition.lumenechallenge.consume.any_item")
                : Text.translatable(Registries.ITEM.get(Identifier.of(itemId)).getTranslationKey());
        return switch (mode) {
            case EAT -> Text.translatable("condition.lumenechallenge.eating", item);
            case DRINK -> Text.translatable("condition.lumenechallenge.drinking", item);
        };
    }

    @Override
    public ConditionKind getKind() {
        return mode == Mode.EAT ? ConditionKind.EATING : ConditionKind.DRINKING;
    }

    @Override
    public TriggerSource trigger() {
        return TriggerSource.TICK;
    }

    @Override
    protected void writeFields(NbtCompound tag) {
        tag.putString("mode", mode.name());
        tag.putString("itemId", itemId);
    }

    public static ConsumeCondition fromNbt(NbtCompound tag) {
        return new ConsumeCondition(
                Mode.valueOf(tag.getString("mode", Mode.EAT.name())),
                tag.getString("itemId", "")
        );
    }
}
