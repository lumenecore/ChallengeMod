package com.lumenechallenge.mixin;

import com.lumenechallenge.challenge.ChallengeHooks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.registry.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ProjectileEntity.class)
public abstract class ProjectileEntityMixin {
    @Inject(method = "onEntityHit", at = @At("TAIL"))
    private void lumenechallenge$onEntityHit(EntityHitResult hitResult, CallbackInfo ci) {
        ProjectileEntity projectile = (ProjectileEntity) (Object) this;
        Entity owner = projectile.getOwner();
        Entity hitEntity = hitResult.getEntity();
        if (owner instanceof SkeletonEntity skeleton
                && skeleton.getTarget() instanceof ServerPlayerEntity targetedPlayer
                && hitEntity instanceof LivingEntity skeletonVictim
                && !(skeletonVictim instanceof PlayerEntity)) {
            ChallengeHooks.onSkeletonShotMob(targetedPlayer, skeletonVictim);
        }
        if (!(owner instanceof ServerPlayerEntity serverPlayer)) return;
        if (projectile instanceof PersistentProjectileEntity && hitEntity instanceof LivingEntity living && !(living instanceof PlayerEntity)) {
            double distance = serverPlayer.getEyePos().distanceTo(living.getEntityPos());
            ChallengeHooks.onProjectileHit(serverPlayer, living, distance);
        }
        if (hitEntity instanceof LivingEntity living && !(living instanceof PlayerEntity)) {
            ChallengeHooks.onProjectileHitTarget(serverPlayer, projectile, "mob", Registries.ENTITY_TYPE.getId(living.getType()).toString());
        }
    }

    @Inject(method = "onBlockHit", at = @At("TAIL"))
    private void lumenechallenge$onBlockHit(BlockHitResult hitResult, CallbackInfo ci) {
        ProjectileEntity projectile = (ProjectileEntity) (Object) this;
        if (!(projectile.getOwner() instanceof ServerPlayerEntity serverPlayer)) return;
        var state = projectile.getEntityWorld().getBlockState(hitResult.getBlockPos());
        String blockId = Registries.BLOCK.getId(state.getBlock()).toString();
        ChallengeHooks.onProjectileHitTarget(serverPlayer, projectile, "block", blockId);
    }
}
