package dev.hollow.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dev.hollow.Hollow;
import dev.hollow.core.Colors;
import dev.hollow.ui.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** While the game loads, the name fills with snow instead of a progress bar. */
@Mixin(LoadingOverlay.class)
abstract class LoadingOverlayMixin {
    @Shadow private float currentProgress;

    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIIIIII)V"))
    private void hollow$logo(GuiGraphicsExtractor g, RenderPipeline p, Identifier id, int x, int y, float u, float v, int w, int h, int uw, int vh, int tw, int th, int color, Operation<Void> original) {
        if (!Hollow.INTERFACE.loading.get()) {
            original.call(g, p, id, x, y, u, v, w, h, uw, vh, tw, th, color);
            return;
        }
        // The game draws its logo in two halves; ours is drawn once, with the first.
        if (u > 0) return;
        float alpha = (color >>> 24) / 255f, tall = Math.min(g.guiHeight() * .14f, g.guiWidth() * .075f), width = Brand.width("HOLLOW", tall), bx = (g.guiWidth() - width) / 2, by = g.guiHeight() / 2f - tall * .75f;
        Draw.Shade snow = (px, py) -> Colors.mix(0xFFFFFFFF, Theme.at(Ease.clamp((px - bx) / width)), Ease.clamp((py - by) / tall) * .8f);
        Brand.word(g, "HOLLOW", bx, by, tall, new Brand.Look(1, currentProgress, Theme.text(), tall * .045f, snow, alpha));
        float small = tall * .3f, letters = Brand.width("CLIENT", small), extra = (width - letters) / 5, pen = bx;
        for (int i = 0; i < 6; i++) {
            String ch = "CLIENT".substring(i, i + 1);
            int c = Colors.alpha(i < currentProgress * 6.2f ? Theme.at(i / 5f) : Theme.faint(), 1);
            Brand.word(g, ch, pen, by + tall * 1.2f, small, new Brand.Look(1, 1, c, .01f, (px, py) -> c, alpha));
            pen += Brand.width(ch, small) + extra + .13f * small;
        }
    }

    @Inject(method = "extractProgressBar", at = @At("HEAD"), cancellable = true)
    private void hollow$noBar(CallbackInfo ci) { if (Hollow.INTERFACE.loading.get()) ci.cancel(); }

    @ModifyArg(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/LoadingOverlay;replaceAlpha(II)I"), index = 0)
    private int hollow$fade(int color) { return Hollow.INTERFACE.loading.get() ? Theme.ink() : color; }

    @ModifyArg(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ARGB;setVector4fFromARGB32(Lorg/joml/Vector4f;I)Lorg/joml/Vector4f;"), index = 1)
    private int hollow$clear(int color) { return Hollow.INTERFACE.loading.get() ? Theme.ink() : color; }
}
