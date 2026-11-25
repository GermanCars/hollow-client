package dev.hollow.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hollow.Hollow;
import dev.hollow.hud.Stats;
import dev.hollow.module.PlayerModules;
import dev.hollow.module.RenderModules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
abstract class MouseHandlerMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "onButton", at = @At("HEAD"))
    private void hollow$button(long handle, MouseButtonInfo info, int action, CallbackInfo ci) {
        if (action != 1 || minecraft.player == null || minecraft.gui.screen() != null) return;
        if (info.button() == InputConstants.MOUSE_BUTTON_LEFT) Stats.clicked();
        if (info.button() == InputConstants.MOUSE_BUTTON_MIDDLE) Hollow.module(PlayerModules.MiddleClick.class).pressed();
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void hollow$scroll(long handle, double x, double y, CallbackInfo ci) {
        if (minecraft.player != null && minecraft.gui.screen() == null && y != 0 && Hollow.module(RenderModules.Zoom.class).scrolled(y)) ci.cancel();
    }
}
