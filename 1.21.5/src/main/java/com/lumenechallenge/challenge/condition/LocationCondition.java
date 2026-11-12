package com.lumenechallenge.challenge.condition;

import com.lumenechallenge.client.layout.ModI18n;

import com.lumenechallenge.challenge.ChallengeContext;
import com.lumenechallenge.challenge.TriggerSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.world.World;

public final class LocationCondition extends AbstractCondition {
    public enum Mode { OVERWORLD, NETHER, END, UNDERWATER }

    private final Mode mode;

    public LocationCondition(Mode mode) {
        this.mode = mode;
    }

    public Mode mode() {
        return mode;
    }

    @Override
    public boolean check(ChallengeContext context) {
        return switch (mode) {
            case OVERWORLD -> context.world().getRegistryKey().equals(World.OVERWORLD);
            case NETHER -> context.world().getRegistryKey().equals(World.NETHER);
            case END -> context.world().getRegistryKey().equals(World.END);
            case UNDERWATER -> context.player().isSubmergedInWater();
        };
    }

    @Override
    public Text getDescription() {
        return switch (mode) {
            case OVERWORLD -> ModI18n.text("condition.lumenechallenge.location.overworld");
            case NETHER -> ModI18n.text("condition.lumenechallenge.location.nether");
            case END -> ModI18n.text("condition.lumenechallenge.location.end");
            case UNDERWATER -> ModI18n.text("condition.lumenechallenge.location.underwater");
        };
    }

    @Override
    public ConditionKind getKind() {
        return ConditionKind.LOCATION;
    }

    @Override
    public TriggerSource trigger() {
        return TriggerSource.TICK;
    }

    @Override
    protected void writeFields(NbtCompound tag) {
        tag.putString("mode", mode.name());
    }

    public static LocationCondition fromNbt(NbtCompound tag) {
        return new LocationCondition(Mode.valueOf(tag.getString("mode").orElse(Mode.OVERWORLD.name())));
    }
}
