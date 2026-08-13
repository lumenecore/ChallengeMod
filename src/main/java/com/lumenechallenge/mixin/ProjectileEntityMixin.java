package com.lumenechallenge.mixin;

import com.lumenechallenge.challenge.ChallengeHooks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ProjectileEntity.class)
public abstract class ProjectileEntityMixin {
    @Inject(method = "onEntityHit", at = @At("TAIL"))
    private void lumenechallenge$onEntityHit(EntityHitResult hitResult, CallbackInfo ci) {
        ProjectileEntity projectile = (ProjectileEntity) (Object) this;
        if (!(projectile instanceof PersistentProjectileEntity)) {
            return;
        }

        Entity owner = projectile.getOwner();
        if (!(owner instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        Entity hitEntity = hitResult.getEntity();
        if (hitEntity instanceof LivingEntity living && !(living instanceof PlayerEntity)) {
            double distance = serverPlayer.getEyePos().distanceTo(living.getPos());
            ChallengeHooks.onProjectileHit(serverPlayer, living, distance);
        }
    }
}
