package com.lumenechallenge.challenge.condition;

import com.lumenechallenge.client.layout.ModI18n;

import com.lumenechallenge.challenge.ChallengeContext;
import com.lumenechallenge.challenge.TriggerSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

public final class VillageCondition extends AbstractCondition {
    @Override
    public boolean check(ChallengeContext context) {
        return context.world().isNearOccupiedPointOfInterest(context.position());
    }

    @Override
    public Text getDescription() {
        return ModI18n.text("condition.lumenechallenge.village");
    }

    @Override
    public ConditionKind getKind() {
        return ConditionKind.VILLAGE;
    }

    @Override
    public TriggerSource trigger() {
        return TriggerSource.TICK;
    }

    @Override
    protected void writeFields(NbtCompound tag) {
    }

    public static VillageCondition fromNbt(NbtCompound tag) {
        return new VillageCondition();
    }
}
