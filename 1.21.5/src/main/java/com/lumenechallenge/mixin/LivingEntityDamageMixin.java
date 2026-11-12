package com.lumenechallenge.mixin;

import com.lumenechallenge.challenge.ChallengeHooks;
import com.lumenechallenge.challenge.ChallengeState;
import com.lumenechallenge.challenge.ModifierType;
import com.lumenechallenge.util.WorldMarkerUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void lumenechallenge$onDamage(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity target = (LivingEntity) (Object) this;
        if (target instanceof PlayerEntity playerTarget) {
            if (playerTarget.getWorld().getServer() != null && WorldMarkerUtil.isMarked(playerTarget.getWorld().getServer())) {
                ChallengeState state = ChallengeState.getServerState(playerTarget.getWorld().getServer());
                if (state.challenge() == null && !state.seedSelected()) {
                    cir.setReturnValue(false);
                    return;
                }
            }
        }
        if (source.getAttacker() instanceof ServerPlayerEntity player) {
            if (!(target instanceof PlayerEntity) && WorldMarkerUtil.isMarked(player.getWorld().getServer())) {
                ChallengeState state = ChallengeState.getServerState(player.getWorld().getServer());
                if (state.modifierEnabled() && state.modifier() == ModifierType.PACIFIST && !state.allowsMobDamage()) {
                    cir.setReturnValue(false);
                }
            }
        }
    }

    @Inject(method = "damage", at = @At("RETURN"))
    private void lumenechallenge$afterDamage(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(cir.getReturnValue())) {
            return;
        }
        LivingEntity target = (LivingEntity) (Object) this;
        if (target instanceof ServerPlayerEntity hurtPlayer) {
            ChallengeHooks.onPlayerReceivedDamage(hurtPlayer, source);
        }
        if (!(source.getAttacker() instanceof ServerPlayerEntity player)) {
            return;
        }
        if (target instanceof PlayerEntity) {
            return;
        }
        ChallengeHooks.onDamage(player, target, source, amount);
    }
}
