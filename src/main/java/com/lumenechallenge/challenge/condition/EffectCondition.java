package com.lumenechallenge.challenge.condition;

import com.lumenechallenge.challenge.ChallengeContext;
import com.lumenechallenge.challenge.TriggerSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class EffectCondition extends AbstractCondition {
    private final String effectId;

    public EffectCondition(String effectId) {
        this.effectId = effectId;
    }

    @Override
    public boolean check(ChallengeContext context) {
        if (effectId.isBlank()) {
            return !context.player().getActiveStatusEffects().isEmpty();
        }
        var entry = Registries.STATUS_EFFECT.getEntry(Identifier.of(effectId));

        return entry.isPresent() &&
                context.player().hasStatusEffect(entry.get());
    }

    @Override
    public Text getDescription() {
        return effectId.isBlank()
                ? Text.translatable("condition.lumenechallenge.effect.any")
                : Text.translatable("condition.lumenechallenge.effect.specific", Text.translatable(Registries.STATUS_EFFECT.get(Identifier.of(effectId)).getTranslationKey()));
    }

    @Override
    public ConditionKind getKind() {
        return ConditionKind.EFFECT;
    }

    @Override
    public TriggerSource trigger() {
        return TriggerSource.TICK;
    }

    @Override
    protected void writeFields(NbtCompound tag) {
        tag.putString("effectId", effectId);
    }

    public static EffectCondition fromNbt(NbtCompound tag) {
        return new EffectCondition(tag.getString("effectId").orElse(""));
    }
}
