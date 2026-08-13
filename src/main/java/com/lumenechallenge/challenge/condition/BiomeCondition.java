package com.lumenechallenge.challenge.condition;

import com.lumenechallenge.challenge.ChallengeContext;
import com.lumenechallenge.challenge.TriggerSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

public final class BiomeCondition extends AbstractCondition {
    private final RegistryKey<Biome> biomeKey;

    public BiomeCondition(RegistryKey<Biome> biomeKey) {
        this.biomeKey = biomeKey;
    }

    @Override
    public boolean check(ChallengeContext context) {
        return context.biomeEntry().matchesKey(biomeKey);
    }

    @Override
    public Text getDescription() {
        return Text.translatable("condition.lumenechallenge.biome", Text.translatable(biomeKey.getValue().toTranslationKey("biome")));
    }

    @Override
    public ConditionKind getKind() {
        return ConditionKind.BIOME;
    }

    @Override
    public TriggerSource trigger() {
        return TriggerSource.TICK;
    }

    @Override
    protected void writeFields(NbtCompound tag) {
        tag.putString("biome", biomeKey.getValue().toString());
    }

    public static BiomeCondition fromNbt(NbtCompound tag) {
        String biome = tag.getString("biome").orElse("minecraft:plains");
        RegistryKey<Biome> key = RegistryKey.of(net.minecraft.registry.RegistryKeys.BIOME, Identifier.of(biome));
        return new BiomeCondition(key);
    }
}
