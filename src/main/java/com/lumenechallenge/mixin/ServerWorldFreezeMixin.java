package com.lumenechallenge.mixin;

import com.lumenechallenge.challenge.ChallengeState;
import com.lumenechallenge.util.WorldMarkerUtil;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerWorld.class)
public abstract class ServerWorldFreezeMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void lumenechallenge$freezeWorld(java.util.function.BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        if (shouldFreeze()) ci.cancel();
    }

    @Inject(method = "tickTime", at = @At("HEAD"), cancellable = true)
    private void lumenechallenge$freezeTime(CallbackInfo ci) {
        if (shouldFreeze()) {
            ci.cancel();
        }
    }

    @Inject(method = "tickEntity", at = @At("HEAD"), cancellable = true)
    private void lumenechallenge$freezeEntities(Entity entity, CallbackInfo ci) {
        if (shouldFreeze()) {
            ci.cancel();
        }
    }

    @Inject(method = "tickChunk", at = @At("HEAD"), cancellable = true)
    private void lumenechallenge$freezeChunkTick(WorldChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        if (shouldFreeze()) {
            ci.cancel();
        }
    }

    @Inject(method = "tickSpawners", at = @At("HEAD"), cancellable = true)
    private void lumenechallenge$freezeSpawners(boolean spawnAnimals, boolean spawnMonsters, CallbackInfo ci) {
        if (shouldFreeze()) {
            ci.cancel();
        }
    }

    @Inject(method = "tickThunder", at = @At("HEAD"), cancellable = true)
    private void lumenechallenge$freezeThunder(WorldChunk chunk, CallbackInfo ci) {
        if (shouldFreeze()) {
            ci.cancel();
        }
    }

    @Inject(method = "tickIceAndSnow", at = @At("HEAD"), cancellable = true)
    private void lumenechallenge$freezeIceAndSnow(BlockPos pos, CallbackInfo ci) {
        if (shouldFreeze()) {
            ci.cancel();
        }
    }

    private boolean shouldFreeze() {
        ServerWorld world = (ServerWorld) (Object) this;
        if (world.getServer() == null || !WorldMarkerUtil.isMarked(world.getServer())) {
            return false;
        }
        ChallengeState state = ChallengeState.getServerState(world.getServer());
        return !state.runStarted();
    }
}
