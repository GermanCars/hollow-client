package dev.hollow.mixin;

import dev.hollow.Hollow;
import dev.hollow.module.RenderModules;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
abstract class KeyboardInputMixin extends ClientInput {
    /** The keys steer the camera instead. */
    @Inject(method = "tick", at = @At("TAIL"))
    private void hollow$freecam(CallbackInfo ci) {
        if (!Hollow.module(RenderModules.Freecam.class).on()) return;
        keyPresses = Input.EMPTY;
        moveVector = Vec2.ZERO;
    }
}
