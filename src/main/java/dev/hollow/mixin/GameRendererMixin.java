package dev.hollow.mixin;

import dev.hollow.Hollow;
import dev.hollow.module.RenderModules;
import dev.hollow.ui.Frame;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
    @Inject(method = "extract", at = @At("HEAD"))
    private void hollow$frame(DeltaTracker delta, boolean advance, CallbackInfo ci) {
        Frame.next();
        Hollow.worldRender(delta.getGameTimeDeltaPartialTick(false));
    }

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void hollow$hurt(CallbackInfo ci) {
        RenderModules.NoRender n = Hollow.module(RenderModules.NoRender.class);
        if (n.on() && n.hurt.get()) ci.cancel();
    }
}
