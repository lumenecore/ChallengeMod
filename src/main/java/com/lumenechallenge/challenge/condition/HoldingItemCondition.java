package com.lumenechallenge.challenge.condition;

import com.lumenechallenge.challenge.ChallengeContext;
import com.lumenechallenge.challenge.TriggerSource;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class HoldingItemCondition extends AbstractCondition {
    private final Item item;

    public HoldingItemCondition(Item item) {
        this.item = item;
    }

    @Override
    public boolean check(ChallengeContext context) {
        ItemStack hand = context.handStack();
        return hand != null && hand.isOf(item);
    }

    @Override
    public Text getDescription() {
        return Text.translatable("condition.lumenechallenge.holding_item", item.getName());
    }

    @Override
    public ConditionKind getKind() {
        return ConditionKind.HOLDING_ITEM;
    }

    @Override
    public TriggerSource trigger() {
        return TriggerSource.TICK;
    }

    @Override
    protected void writeFields(NbtCompound tag) {
        tag.putString("item", Registries.ITEM.getId(item).toString());
    }

    public static HoldingItemCondition fromNbt(NbtCompound tag) {
        String itemId = tag.getString("item").orElse("minecraft:air");
        return new HoldingItemCondition(Registries.ITEM.get(Identifier.of(itemId)));
    }
}
