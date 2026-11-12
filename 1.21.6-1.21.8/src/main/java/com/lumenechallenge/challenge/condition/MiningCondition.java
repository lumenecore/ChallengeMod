package com.lumenechallenge.challenge.condition;

import com.lumenechallenge.client.layout.ModI18n;

import com.lumenechallenge.challenge.ChallengeContext;
import com.lumenechallenge.challenge.TriggerSource;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class MiningCondition extends AbstractCondition {
    private final Block block;

    public MiningCondition(Block block) {
        this.block = block;
    }

    @Override
    public boolean check(ChallengeContext context) {
        BlockState state = context.minedBlockState();
        return state != null && state.isOf(block);
    }

    @Override
    public Text getDescription() {
        return ModI18n.text("condition.lumenechallenge.mining", block.getName());
    }

    @Override
    public ConditionKind getKind() {
        return ConditionKind.MINING;
    }

    @Override
    public TriggerSource trigger() {
        return TriggerSource.BLOCK_BREAK;
    }

    @Override
    protected void writeFields(NbtCompound tag) {
        tag.putString("block", Registries.BLOCK.getId(block).toString());
    }

    public static MiningCondition fromNbt(NbtCompound tag) {
        String blockId = tag.getString("block").orElse("minecraft:stone");
        return new MiningCondition(Registries.BLOCK.get(Identifier.of(blockId)));
    }
}
