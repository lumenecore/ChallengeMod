package com.lumenechallenge.challenge.condition;

import com.lumenechallenge.client.layout.ModI18n;

import com.lumenechallenge.challenge.ChallengeContext;
import com.lumenechallenge.challenge.TriggerSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

public final class HealthCondition extends AbstractCondition {
    private final float minHealth;
    private final float maxHealth;

    public HealthCondition(float minHealth, float maxHealth) {
        this.minHealth = Math.min(minHealth, maxHealth);
        this.maxHealth = Math.max(minHealth, maxHealth);
    }

    @Override
    public boolean check(ChallengeContext context) {
        float health = context.player().getHealth();
        return health >= minHealth && health <= maxHealth;
    }

    @Override
    public Text getDescription() {
        if (Math.abs(minHealth - maxHealth) < 0.001f) {
            return ModI18n.text("condition.lumenechallenge.health.exact", hearts(minHealth));
        }
        return ModI18n.text("condition.lumenechallenge.health.range", hearts(minHealth), hearts(maxHealth));
    }

    private static String hearts(float health) {
        float hearts = health / 2.0f;
        if (Math.abs(hearts - Math.round(hearts)) < 0.001f) {
            return Integer.toString(Math.round(hearts));
        }
        return String.format(java.util.Locale.ROOT, "%.1f", hearts);
    }

    @Override
    public ConditionKind getKind() {
        return ConditionKind.HEALTH;
    }

    @Override
    public TriggerSource trigger() {
        return TriggerSource.TICK;
    }

    @Override
    protected void writeFields(NbtCompound tag) {
        tag.putFloat("minHealth", minHealth);
        tag.putFloat("maxHealth", maxHealth);
    }

    public static HealthCondition fromNbt(NbtCompound tag) {
        return new HealthCondition(
                tag.getFloat("minHealth", 0.0f),
                tag.getFloat("maxHealth", 20.0f)
        );
    }
}
