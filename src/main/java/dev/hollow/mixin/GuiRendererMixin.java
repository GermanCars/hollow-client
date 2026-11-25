package dev.hollow.mixin;

import dev.hollow.render.Cavern;
import dev.hollow.render.Glass;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
abstract class GuiRendererMixin {
    @Inject(method = "render()V", at = @At("HEAD"))
    private void hollow$behind(CallbackInfo ci) {
        Cavern.render();
        Glass.capture();
    }
}
