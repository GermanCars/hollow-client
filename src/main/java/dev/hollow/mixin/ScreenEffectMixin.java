package dev.hollow.mixin;

import dev.hollow.Hollow;
import dev.hollow.core.Setting;
import dev.hollow.module.RenderModules;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
abstract class ScreenEffectMixin {
    private static boolean hollow$hides(java.util.function.Function<RenderModules.NoRender, Setting.Bool> s) {
        RenderModules.NoRender n = Hollow.module(RenderModules.NoRender.class);
        return n.on() && s.apply(n).get();
    }

    @Inject(method = "submitFire", at = @At("HEAD"), cancellable = true)
    private static void hollow$fire(CallbackInfo ci) { if (hollow$hides(n -> n.fire)) ci.cancel(); }

    @Inject(method = "submitWater", at = @At("HEAD"), cancellable = true)
    private static void hollow$water(CallbackInfo ci) { if (hollow$hides(n -> n.water)) ci.cancel(); }

    @Inject(method = "submitBlockSprite", at = @At("HEAD"), cancellable = true)
    private static void hollow$block(CallbackInfo ci) { if (hollow$hides(n -> n.block)) ci.cancel(); }

    @Inject(method = "renderItemActivationAnimation", at = @At("HEAD"), cancellable = true)
    private void hollow$totem(CallbackInfo ci) { if (hollow$hides(n -> n.totem)) ci.cancel(); }
}
