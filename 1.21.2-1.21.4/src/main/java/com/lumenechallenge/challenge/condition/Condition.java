package com.lumenechallenge.challenge.condition;

import com.lumenechallenge.challenge.ChallengeContext;
import com.lumenechallenge.challenge.TriggerSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

public interface Condition {
    boolean check(ChallengeContext context);
    Text getDescription();
    ConditionKind getKind();
    TriggerSource trigger();
    NbtCompound toNbt();

    static Condition fromNbt(NbtCompound tag) {
        return ConditionKind.fromNbt(tag);
    }
}
