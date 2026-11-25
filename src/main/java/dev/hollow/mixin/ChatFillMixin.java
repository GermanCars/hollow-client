package dev.hollow.mixin;

import dev.hollow.Hollow;
import dev.hollow.core.Colors;
import dev.hollow.ui.Draw;
import dev.hollow.ui.Ice;
import dev.hollow.ui.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Chat lines on glass. Both ways the game draws chat. */
@Mixin(targets = {"net.minecraft.client.gui.components.ChatComponent$DrawingBackgroundGraphicsAccess", "net.minecraft.client.gui.components.ChatComponent$DrawingFocusedGraphicsAccess"})
abstract class ChatFillMixin {
    @Shadow(remap = false) @Final private GuiGraphicsExtractor graphics;

    @Inject(method = "fill", at = @At("HEAD"), cancellable = true)
    private void hollow$glass(int x0, int y0, int x1, int y1, int color, CallbackInfo ci) {
        // The scroll bar has a colour; line backgrounds are black and start left of the text.
        if (!Hollow.INTERFACE.chat.get() || (color & 0xFFFFFF) != 0 || x0 >= 0) return;
        float a = Math.min(1, (color >>> 24) / 255f * 1.9f);
        ci.cancel();
        if (a < .01f) return;
        Ice.glass(graphics, new float[] {x0, y0, x1, y0, x1, y1, x0, y1}, a, .01f);
        Draw d = new Draw(graphics);
        d.gradientX(x0, y0, x1 - x0, y1 - y0, Colors.with(Colors.mix(Theme.ink(), Theme.second(), .14f), .62f * a), Colors.with(Theme.ink(), .38f * a));
        d.rect(x0, y0, 1, y1 - y0, Colors.with(Theme.accent(), .55f * a));
        d.flush();
    }
}
