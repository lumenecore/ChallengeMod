package com.lumenechallenge.challenge.condition;

import com.lumenechallenge.client.layout.ModI18n;

import com.lumenechallenge.challenge.ChallengeContext;
import com.lumenechallenge.challenge.TriggerSource;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class BlockPlacementCondition extends AbstractCondition {
    private final String blockId;

    public BlockPlacementCondition(String blockId) {
        this.blockId = blockId;
    }

    @Override
    public boolean check(ChallengeContext context) {
        BlockState state = context.placedBlockState();
        if (state == null) {
            return false;
        }

        String actualId = Registries.BLOCK.getId(state.getBlock()).toString();
        if (actualId.equals(blockId)) {
            return true;
        }

        return normalizeHangingSignId(actualId).equals(normalizeHangingSignId(blockId));
    }

    private static String normalizeHangingSignId(String id) {
        return id.contains("_wall_hanging_sign") ? id.replace("_wall_hanging_sign", "_hanging_sign") : id;
    }

    @Override
    public Text getDescription() {
        return ModI18n.text("condition.lumenechallenge.placing", ModI18n.text(Registries.BLOCK.get(Identifier.of(blockId)).getTranslationKey()));
    }

    @Override
    public ConditionKind getKind() {
        return ConditionKind.PLACING;
    }

    @Override
    public TriggerSource trigger() {
        return TriggerSource.TICK;
    }

    @Override
    protected void writeFields(NbtCompound tag) {
        tag.putString("blockId", blockId);
    }

    public static BlockPlacementCondition fromNbt(NbtCompound tag) {
        return new BlockPlacementCondition(tag.getString("blockId").orElse("minecraft:stone"));
    }
}
