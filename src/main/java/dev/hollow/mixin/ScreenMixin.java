package dev.hollow.mixin;

import dev.hollow.Hollow;
import dev.hollow.core.Colors;
import dev.hollow.module.Interface;
import dev.hollow.ui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
abstract class ScreenMixin {
    @Shadow @Final protected Minecraft minecraft;
    @Shadow public int width;
    @Shadow public int height;

    @Inject(method = "extractPanorama", at = @At("HEAD"), cancellable = true)
    private void hollow$scene(GuiGraphicsExtractor g, float a, CallbackInfo ci) {
        if (!Interface.snowyMenus()) return;
        Scene.get().begin(g);
        Scene.get().drawBack(g);
        Scene.get().drawFront(g);
        ci.cancel();
    }

    @Inject(method = "extractBlurredBackground", at = @At("HEAD"), cancellable = true)
    private void hollow$noBlur(GuiGraphicsExtractor g, CallbackInfo ci) { if (Interface.styled()) ci.cancel(); }

    /** One sheet of glass over the world or the cavern instead of the game's dirt-dark picture. */
    @Inject(method = "extractMenuBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIII)V", at = @At("HEAD"), cancellable = true)
    private void hollow$menuBackground(GuiGraphicsExtractor g, int x, int y, int w, int h, CallbackInfo ci) {
        if (!Interface.styled()) return;
        float t = Ease.outCubic(Style.state(this).glint.to(1, .5f));
        Draw d = new Draw(g);
        if (minecraft.level == null && w >= width && h >= height - 4) {
            // The cavern stays sharp; only its edges darken.
            d.rect(x, y, w, h, Colors.with(Theme.ink(), .16f * t));
            d.glow(width / 2f, height / 2f, width * .72f, height * .85f, Colors.with(Theme.ink(), .30f * t), Colors.with(Theme.ink(), .48f * t), 48);
        } else {
            float[] sheet = {x - 4, y - 4, x + w + 4, y - 4, x + w + 4, y + h + 4, x - 4, y + h + 4};
            Ice.glass(g, sheet, t, 1);
            d.rect(x, y, w, h, Colors.with(Colors.mix(Theme.ink(), Theme.second(), .16f), .60f * t));
        }
        d.flush();
        ci.cancel();
    }

    /** Containers draw this first and their own picture on top. */
    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void hollow$container(GuiGraphicsExtractor g, int mouseX, int mouseY, float a, CallbackInfo ci) {
        Object self = this;
        if (!Interface.glassContainers() || !(self instanceof ContainerAccess c) || self instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen) return;
        float t = Style.state(self).hover.to(1, .9f);
        Style.panel(g, c.hollow$left(), c.hollow$top(), c.hollow$width(), c.hollow$height(), Ease.clamp(t * 6), .4f + .6f * t);
    }

    /** Menus rise out of the glass. */
    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
    private void hollow$arrive(GuiGraphicsExtractor g, int mouseX, int mouseY, float a, CallbackInfo ci) {
        g.pose().pushMatrix();
        Object self = this;
        if (!Interface.styled() || self instanceof HollowTitleScreen || self instanceof ClickGui || self instanceof HudEditor || self instanceof net.minecraft.client.gui.screens.ChatScreen) return;
        float e = 1 - Ease.outQuint(Style.state(this).press.to(1, .55f));
        if (e <= .001f) return;
        g.pose().translate(width / 2f, height / 2f + e * 16);
        g.pose().scale(1 - e * .07f);
        g.pose().translate(-width / 2f, -height / 2f);
    }

    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V", shift = At.Shift.AFTER))
    private void hollow$arrived(GuiGraphicsExtractor g, int mouseX, int mouseY, float a, CallbackInfo ci) { g.pose().popMatrix(); }

    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("TAIL"))
    private void hollow$over(GuiGraphicsExtractor g, int mouseX, int mouseY, float a, CallbackInfo ci) {
        Object self = this;
        if (self instanceof PauseScreen && Hollow.INTERFACE.pauseBrand.get()) {
            float size = Math.min(26, height * .075f), w = Brand.lineWidth(size), in = Style.state(self).hover.to(1, 1.1f);
            Brand.line(g, (width - w) / 2, Math.max(6, height / 4f - 48 - size), size, in, .5f + .06f * (float) Math.sin(Frame.time * .8), 1);
        } else if (minecraft.level == null && !(self instanceof HollowTitleScreen) && !(self instanceof ClickGui) && Interface.snowyMenus()) {
            Brand.line(g, 8, height - 17, 9, Style.state(self).hover.to(1, .9f), .55f, .95f);
        }
        Ice.drawShards(g);
    }
}
