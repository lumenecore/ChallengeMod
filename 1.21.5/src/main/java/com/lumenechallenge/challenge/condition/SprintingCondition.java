package com.lumenechallenge.challenge.condition;

import com.lumenechallenge.client.layout.ModI18n;

import com.lumenechallenge.challenge.ChallengeContext;
import com.lumenechallenge.challenge.TriggerSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

public final class SprintingCondition extends AbstractCondition {
    private final boolean sprinting;

    public boolean sprinting() {
        return sprinting;
    }

    public SprintingCondition(boolean sprinting) {
        this.sprinting = sprinting;
    }

    @Override
    public boolean check(ChallengeContext context) {
        return context.player().isSprinting() == sprinting;
    }

    @Override
    public Text getDescription() {
        return sprinting
                ? ModI18n.text("condition.lumenechallenge.sprinting.on")
                : ModI18n.text("condition.lumenechallenge.sprinting.off");
    }

    @Override
    public ConditionKind getKind() {
        return ConditionKind.SPRINTING;
    }

    @Override
    public TriggerSource trigger() {
        return TriggerSource.TICK;
    }

    @Override
    protected void writeFields(NbtCompound tag) {
        tag.putBoolean("sprinting", sprinting);
    }

    public static SprintingCondition fromNbt(NbtCompound tag) {
        return new SprintingCondition(tag.getBoolean("sprinting").orElse(true));
    }
}
