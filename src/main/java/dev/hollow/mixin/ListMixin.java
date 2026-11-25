package dev.hollow.mixin;

import dev.hollow.core.Colors;
import dev.hollow.module.Interface;
import dev.hollow.ui.Draw;
import dev.hollow.ui.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.layouts.LayoutElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSelectionList.class)
abstract class ListMixin {
    @Inject(method = "extractListBackground", at = @At("HEAD"), cancellable = true)
    private void hollow$background(GuiGraphicsExtractor g, CallbackInfo ci) {
        if (!Interface.styled()) return;
        AbstractWidget w = (AbstractWidget) (Object) this;
        float[] p = {w.getX(), w.getY(), w.getRight(), w.getY(), w.getRight(), w.getBottom(), w.getX(), w.getBottom()};
        dev.hollow.ui.Ice.glass(g, p, 1, 1);
        g.fill(w.getX(), w.getY(), w.getRight(), w.getBottom(), Colors.with(Theme.ink(), .46f));
        ci.cancel();
    }

    @Inject(method = "extractListSeparators", at = @At("HEAD"), cancellable = true)
    private void hollow$separators(GuiGraphicsExtractor g, CallbackInfo ci) {
        if (!Interface.styled()) return;
        AbstractWidget w = (AbstractWidget) (Object) this;
        Draw d = new Draw(g);
        float mid = w.getX() + w.getWidth() / 2f;
        for (float y : new float[] {w.getY() - 1, w.getBottom()}) {
            d.gradientX(w.getX(), y, w.getWidth() / 2f, 1, Theme.accent() & 0xFFFFFF, Colors.with(Theme.accent(), .7f));
            d.gradientX(mid, y, w.getWidth() / 2f, 1, Colors.with(Theme.second(), .7f), Theme.second() & 0xFFFFFF);
        }
        d.flush();
        ci.cancel();
    }

    @Inject(method = "extractSelection", at = @At("HEAD"), cancellable = true)
    private void hollow$selection(GuiGraphicsExtractor g, @Coerce LayoutElement e, int color, CallbackInfo ci) {
        if (!Interface.styled()) return;
        Draw d = new Draw(g);
        float[] s = Draw.shard(e.getX(), e.getY(), e.getWidth(), e.getHeight(), Theme.cut() * .7f);
        d.poly(s, Theme.fill(e.getX(), e.getX() + e.getWidth(), .22f));
        dev.hollow.ui.Ice.rim(d, s, 1, 1.3f);
        d.flush();
        ci.cancel();
    }
}
