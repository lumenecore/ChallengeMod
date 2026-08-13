package com.lumenechallenge.challenge.condition;

import com.lumenechallenge.client.layout.ModI18n;

import com.lumenechallenge.challenge.ChallengeContext;
import com.lumenechallenge.challenge.TriggerSource;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class CraftCondition extends AbstractCondition {
    private final Item item;

    public CraftCondition(Item item) {
        this.item = item;
    }

    @Override
    public boolean check(ChallengeContext context) {
        ItemStack crafted = context.craftedItem();
        return crafted != null && crafted.isOf(item);
    }

    @Override
    public Text getDescription() {
        return ModI18n.text("condition.lumenechallenge.craft", item.getName());
    }

    @Override
    public ConditionKind getKind() {
        return ConditionKind.CRAFT;
    }

    @Override
    public TriggerSource trigger() {
        return TriggerSource.CRAFT;
    }

    @Override
    protected void writeFields(NbtCompound tag) {
        tag.putString("item", Registries.ITEM.getId(item).toString());
    }

    public static CraftCondition fromNbt(NbtCompound tag) {
        String itemId = tag.getString("item").orElse("minecraft:air");
        return new CraftCondition(Registries.ITEM.get(Identifier.of(itemId)));
    }
}
