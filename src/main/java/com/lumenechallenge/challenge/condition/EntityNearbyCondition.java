package com.lumenechallenge.challenge.condition;

import com.lumenechallenge.challenge.ChallengeContext;
import com.lumenechallenge.challenge.TriggerSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class EntityNearbyCondition extends AbstractCondition {
    private final EntityType<?> entityType;
    private final int radius;

    public EntityNearbyCondition(EntityType<?> entityType, int radius) {
        this.entityType = entityType;
        this.radius = radius;
    }

    @Override
    public boolean check(ChallengeContext context) {
        for (Entity entity : context.world().getOtherEntities(context.player(), context.player().getBoundingBox().expand(radius))) {
            if (entity.getType() == entityType) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Text getDescription() {
        return Text.translatable("condition.lumenechallenge.entity_nearby", entityType.getName(), radius);
    }

    @Override
    public ConditionKind getKind() {
        return ConditionKind.ENTITY_NEARBY;
    }

    @Override
    public TriggerSource trigger() {
        return TriggerSource.TICK;
    }

    @Override
    protected void writeFields(NbtCompound tag) {
        tag.putString("entity", Registries.ENTITY_TYPE.getId(entityType).toString());
        tag.putInt("radius", radius);
    }

    public static EntityNearbyCondition fromNbt(NbtCompound tag) {
        String entityId = tag.getString("entity").orElse("minecraft:zombie");
        int radius = tag.getInt("radius").orElse(10);
        return new EntityNearbyCondition(Registries.ENTITY_TYPE.get(Identifier.of(entityId)), radius);
    }
}
