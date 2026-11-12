package com.lumenechallenge.mixin;

import com.lumenechallenge.challenge.ChallengeHooks;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.EnchantmentScreenHandler;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    @Inject(method = "incrementStat(Lnet/minecraft/util/Identifier;)V", at = @At("TAIL"))
    private void lumenechallenge$recordEnchantStat(Identifier stat, CallbackInfo ci) {
        if (!Stats.ENCHANT_ITEM.equals(stat) || !((Object) this instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        if (serverPlayer.currentScreenHandler instanceof EnchantmentScreenHandler handler) {
            ItemStack enchanted = handler.getSlot(0).getStack();
            if (!enchanted.isEmpty() && enchanted.hasEnchantments()) {
                ChallengeHooks.onItemEnchanted(serverPlayer, enchanted);
            }
        }
    }

    @Inject(method = "wakeUp(ZZ)V", at = @At("HEAD"))
    private void lumenechallenge$wakeUp(boolean bl, boolean updateSleepingPlayers, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity serverPlayer && serverPlayer.getSleepTimer() >= 100) {
            ChallengeHooks.onPlayerSlept(serverPlayer);
        }
    }
}
