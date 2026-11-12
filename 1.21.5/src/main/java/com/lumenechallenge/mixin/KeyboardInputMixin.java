package com.lumenechallenge.mixin;

import com.lumenechallenge.challenge.ChallengeState;
import com.lumenechallenge.challenge.ModifierType;
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

        InputAccessor accessor = (InputAccessor) this;
        if (menuOpen || countdownActive) {
            lumenechallenge$syncPhysicalMovementKeys(client);
            accessor.lumenechallenge$setPlayerInput(PlayerInput.DEFAULT);
            accessor.lumenechallenge$setMovementVector(net.minecraft.util.math.Vec2f.ZERO);
            return;
        }

        if (state != null && state.timerStarted() && !state.completed() && state.modifierEnabled()
                && state.modifier() == ModifierType.INVERSION) {
            PlayerInput input = accessor.lumenechallenge$getPlayerInput();
            if (input != null) {
                accessor.lumenechallenge$setPlayerInput(new PlayerInput(
                        input.backward(), input.forward(), input.right(), input.left(),
                        input.jump(), input.sneak(), input.sprint()));
            }
            net.minecraft.util.math.Vec2f vector = accessor.lumenechallenge$getMovementVector();
            if (vector != null) {
                accessor.lumenechallenge$setMovementVector(new net.minecraft.util.math.Vec2f(-vector.x, -vector.y));
            }
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
