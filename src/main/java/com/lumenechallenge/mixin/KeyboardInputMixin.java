package com.lumenechallenge.mixin;

import com.lumenechallenge.challenge.ChallengeState;
import com.lumenechallenge.client.ClientChallengeBridge;
import com.lumenechallenge.client.SeedSelectionScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void lumenechallenge$restrictGameplayInput(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            return;
        }

        ChallengeState state = ClientChallengeBridge.getVisibleState();
        boolean menuOpen = client.currentScreen instanceof SeedSelectionScreen;
        boolean countdownActive = client.world != null && state != null && state.isCountdownActive(client.getServer());

        if (!menuOpen && !countdownActive) {
            return;
        }

        lumenechallenge$syncPhysicalMovementKeys(client);
        ((InputAccessor) this).lumenechallenge$setPlayerInput(PlayerInput.DEFAULT);
        ((InputAccessor) this).lumenechallenge$setMovementVector(net.minecraft.util.math.Vec2f.ZERO);
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

        long windowHandle = client.getWindow().getHandle();
        for (KeyBinding binding : bindings) {
            InputUtil.Key key = ((KeyBindingAccessor) (Object) binding).lumenechallenge$getBoundKey();
            if (key.getCategory() == InputUtil.Type.MOUSE) {
                continue;
            }
            boolean pressed = InputUtil.isKeyPressed(windowHandle, key.getCode());
            KeyBinding.setKeyPressed(key, pressed);
        }
    }
}
