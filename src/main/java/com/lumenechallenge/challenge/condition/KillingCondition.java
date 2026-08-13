package com.lumenechallenge.challenge.condition;

import com.lumenechallenge.challenge.ChallengeContext;
import com.lumenechallenge.challenge.TriggerSource;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class KillingCondition extends AbstractCondition {
    private final EntityType<?> entityType;

    public KillingCondition(EntityType<?> entityType) {
        this.entityType = entityType;
    }

    @Override
    public boolean check(ChallengeContext context) {
        LivingEntity killed = context.killedEntity();
        return killed != null && killed.getType() == entityType;
    }

    @Override
    public Text getDescription() {
        return Text.translatable("condition.lumenechallenge.killing", entityType.getName());
    }

    @Override
    public ConditionKind getKind() {
        return ConditionKind.KILLING;
    }

    @Override
    public TriggerSource trigger() {
        return TriggerSource.KILL;
    }

    @Override
    protected void writeFields(NbtCompound tag) {
        tag.putString("entity", Registries.ENTITY_TYPE.getId(entityType).toString());
    }

    public static KillingCondition fromNbt(NbtCompound tag) {
        String entityId = tag.getString("entity").orElse("minecraft:zombie");
        return new KillingCondition(Registries.ENTITY_TYPE.get(Identifier.of(entityId)));
    }
}
