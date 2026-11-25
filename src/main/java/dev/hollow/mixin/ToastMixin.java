package dev.hollow.mixin;

import dev.hollow.Hollow;
import dev.hollow.module.MiscModules;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ToastManager.class)
abstract class ToastMixin {
    @Inject(method = "addToast", at = @At("HEAD"), cancellable = true)
    private void hollow$none(Toast t, CallbackInfo ci) { if (Hollow.module(MiscModules.NoToasts.class).on()) ci.cancel(); }
}
