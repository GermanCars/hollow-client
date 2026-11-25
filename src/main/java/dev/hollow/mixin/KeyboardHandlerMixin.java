package dev.hollow.mixin;

import dev.hollow.Hollow;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
abstract class KeyboardHandlerMixin {
    @Shadow @Final private Minecraft minecraft;

    /** Before anything else, so a key let go over an open menu still counts. */
    @Inject(method = "keyPress", at = @At("HEAD"))
    private void hollow$released(long handle, int action, KeyEvent event, CallbackInfo ci) {
        if (action == 0 && minecraft.player != null) Hollow.keyUp(event.key());
    }

    @Inject(method = "keyPress", at = @At("TAIL"))
    private void hollow$pressed(long handle, int action, KeyEvent event, CallbackInfo ci) {
        if (action == 1 && minecraft.player != null && minecraft.gui.screen() == null && minecraft.gui.overlay() == null) Hollow.keyDown(event.key());
    }
}
