package com.lumenechallenge.challenge.condition;

import com.lumenechallenge.challenge.ChallengeContext;
import com.lumenechallenge.challenge.TriggerSource;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

public final class ArmorCondition extends AbstractCondition {
    public enum Mode { NONE, FULL, HELMET_ONLY, CHEST_ONLY, LEGS_ONLY, BOOTS_ONLY }

    private final Mode mode;

    public Mode mode() {
        return mode;
    }

    public ArmorCondition(Mode mode) {
        this.mode = mode;
    }

    @Override
    public boolean check(ChallengeContext context) {
        boolean head = !context.player().getEquippedStack(EquipmentSlot.HEAD).isEmpty();
        boolean chest = !context.player().getEquippedStack(EquipmentSlot.CHEST).isEmpty();
        boolean legs = !context.player().getEquippedStack(EquipmentSlot.LEGS).isEmpty();
        boolean feet = !context.player().getEquippedStack(EquipmentSlot.FEET).isEmpty();
        return switch (mode) {
            case NONE -> !head && !chest && !legs && !feet;
            case FULL -> head && chest && legs && feet;
            case HELMET_ONLY -> head && !chest && !legs && !feet;
            case CHEST_ONLY -> !head && chest && !legs && !feet;
            case LEGS_ONLY -> !head && !chest && legs && !feet;
            case BOOTS_ONLY -> !head && !chest && !legs && feet;
        };
    }

    @Override
    public Text getDescription() {
        return switch (mode) {
            case NONE -> Text.translatable("condition.lumenechallenge.no_armor");
            case FULL -> Text.translatable("condition.lumenechallenge.full_armor");
            case HELMET_ONLY -> Text.translatable("condition.lumenechallenge.armor.helmet_only");
            case CHEST_ONLY -> Text.translatable("condition.lumenechallenge.armor.chest_only");
            case LEGS_ONLY -> Text.translatable("condition.lumenechallenge.armor.legs_only");
            case BOOTS_ONLY -> Text.translatable("condition.lumenechallenge.armor.boots_only");
        };
    }

    @Override
    public ConditionKind getKind() {
        return ConditionKind.ARMOR;
    }

    @Override
    public TriggerSource trigger() {
        return TriggerSource.TICK;
    }

    @Override
    protected void writeFields(NbtCompound tag) {
        tag.putString("mode", mode.name());
    }

    public static ArmorCondition fromNbt(NbtCompound tag) {
        return new ArmorCondition(Mode.valueOf(tag.getString("mode").orElse(Mode.NONE.name())));
    }
}
