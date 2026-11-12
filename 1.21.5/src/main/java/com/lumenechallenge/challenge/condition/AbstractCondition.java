package com.lumenechallenge.challenge.condition;

import net.minecraft.nbt.NbtCompound;

public abstract class AbstractCondition implements Condition {
    @Override
    public NbtCompound toNbt() {
        NbtCompound tag = new NbtCompound();
        tag.putString("kind", getKind().id());
        writeFields(tag);
        return tag;
    }

    protected abstract void writeFields(NbtCompound tag);
}
