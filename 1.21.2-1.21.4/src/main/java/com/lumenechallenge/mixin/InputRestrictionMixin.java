package com.lumenechallenge.mixin;

import com.lumenechallenge.challenge.ChallengeState;
import com.lumenechallenge.challenge.ModifierType;
import com.lumenechallenge.client.ClientChallengeBridge;
import com.lumenechallenge.client.SeedSelectionScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Shared gameplay-input restriction for Minecraft 1.21.2-1.21.4.
 * The intermediary name method_3129 is stable across this range, while its
 * descriptor changes between the old (boolean, float) and new () signature.
 */
@Mixin(KeyboardInput.class)
public abstract class InputRestrictionMixin {

    @Inject(method = "method_3129", at = @At("TAIL"), remap = false, require = 1)
    private void lumenechallenge$restrictGameplayInput(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            return;
        }

        ChallengeState state = ClientChallengeBridge.getVisibleState();
        boolean menuOpen = client.currentScreen instanceof SeedSelectionScreen;
        boolean countdownActive = client.world != null && state != null
                && state.isCountdownActive(client.getServer());

        Input input = (Input) (Object) this;
        if (menuOpen || countdownActive) {
            lumenechallenge$syncPhysicalMovementKeys(client);
            input.playerInput = PlayerInput.DEFAULT;
            input.movementForward = 0.0F;
            input.movementSideways = 0.0F;
            return;
        }

        if (state != null && state.timerStarted() && !state.completed() && state.modifierEnabled()
                && state.modifier() == ModifierType.INVERSION) {
            PlayerInput playerInput = input.playerInput;
            if (playerInput != null) {
                input.playerInput = new PlayerInput(
                        playerInput.backward(), playerInput.forward(), playerInput.right(), playerInput.left(),
                        playerInput.jump(), playerInput.sneak(), playerInput.sprint());
            }
            input.movementForward = -input.movementForward;
            input.movementSideways = -input.movementSideways;
        }
    }

    private static void lumenechallenge$syncPhysicalMovementKeys(MinecraftClient client) {
        KeyBinding[] bindings = {
                client.options.forwardKey,
                client.options.backKey,
                client.options.leftKey,
                client.options.rightKey,
                client.options.jumpKey,
                client.options.sneakKey,
                client.options.sprintKey
        };

        for (KeyBinding binding : bindings) {
            InputUtil.Key key = ((KeyBindingAccessor) (Object) binding).lumenechallenge$getBoundKey();
            if (key.getCategory() == InputUtil.Type.MOUSE) {
                continue;
            }
            boolean pressed = InputUtil.isKeyPressed(client.getWindow().getHandle(), key.getCode());
            KeyBinding.setKeyPressed(key, pressed);
        }
    }
}
