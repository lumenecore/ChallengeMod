package com.lumenechallenge.mixin;

import com.lumenechallenge.challenge.ChallengeHooks;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractHorseEntity.class)
public abstract class AbstractHorseEntityMixin {
    @Unique
    private boolean lumenechallenge$wasTameBeforeInteraction;

    @Inject(
            method = "interactMob(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;",
            at = @At("HEAD")
    )
    private void lumenechallenge$rememberTameState(PlayerEntity player, Hand hand,
                                                    CallbackInfoReturnable<ActionResult> cir) {
        lumenechallenge$wasTameBeforeInteraction = ((AbstractHorseEntity) (Object) this).isTame();
    }

    @Inject(
            method = "interactMob(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;",
            at = @At("RETURN")
    )
    private void lumenechallenge$detectHorseTaming(PlayerEntity player, Hand hand,
                                                    CallbackInfoReturnable<ActionResult> cir) {
        AbstractHorseEntity horse = (AbstractHorseEntity) (Object) this;
        if (!lumenechallenge$wasTameBeforeInteraction && horse.isTame()
                && player instanceof ServerPlayerEntity serverPlayer) {
            ChallengeHooks.onTame(serverPlayer, horse);
        }
    }
}
