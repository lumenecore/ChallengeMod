package com.lumenechallenge.challenge.condition;

import com.lumenechallenge.challenge.ChallengeContext;
import com.lumenechallenge.challenge.TriggerSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

public final class HungerCondition extends AbstractCondition {
    private final int minFoodLevel;
    private final int maxFoodLevel;

    public HungerCondition(int minFoodLevel, int maxFoodLevel) {
        this.minFoodLevel = Math.min(minFoodLevel, maxFoodLevel);
        this.maxFoodLevel = Math.max(minFoodLevel, maxFoodLevel);
    }

    @Override
    public boolean check(ChallengeContext context) {
        int food = context.player().getHungerManager().getFoodLevel();
        return food >= minFoodLevel && food <= maxFoodLevel;
    }

    @Override
    public Text getDescription() {
        if (minFoodLevel == maxFoodLevel) {
            return Text.translatable("condition.lumenechallenge.hunger.exact", minFoodLevel);
        }
        return Text.translatable("condition.lumenechallenge.hunger.range", minFoodLevel, maxFoodLevel);
    }

    @Override
    public ConditionKind getKind() {
        return ConditionKind.HUNGER;
    }

    @Override
    public TriggerSource trigger() {
        return TriggerSource.TICK;
    }

    @Override
    protected void writeFields(NbtCompound tag) {
        tag.putInt("minFoodLevel", minFoodLevel);
        tag.putInt("maxFoodLevel", maxFoodLevel);
    }

    public static HungerCondition fromNbt(NbtCompound tag) {
        return new HungerCondition(
                tag.getInt("minFoodLevel", 0),
                tag.getInt("maxFoodLevel", 20)
        );
    }
}
