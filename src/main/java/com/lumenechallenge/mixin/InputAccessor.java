package com.lumenechallenge.mixin;

import net.minecraft.client.input.Input;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Input.class)
public interface InputAccessor {

    @Accessor("playerInput")
    void lumenechallenge$setPlayerInput(PlayerInput playerInput);

    @Accessor("movementVector")
    void lumenechallenge$setMovementVector(Vec2f movementVector);
}
