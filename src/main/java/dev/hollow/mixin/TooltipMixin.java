package dev.hollow.mixin;

import dev.hollow.Hollow;
import dev.hollow.core.Colors;
import dev.hollow.ui.Draw;
import dev.hollow.ui.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TooltipRenderUtil.class)
abstract class TooltipMixin {
    @Inject(method = "extractTooltipBackground", at = @At("HEAD"), cancellable = true)
    private static void hollow$tooltip(GuiGraphicsExtractor g, int x, int y, int w, int h, Identifier style, CallbackInfo ci) {
        // Items with their own tooltip frame keep it.
        if (!Hollow.INTERFACE.tooltips.get() || style != null) return;
        float[] s = Draw.shard(x - 6, y - 6, w + 12, h + 12, 6);
        dev.hollow.ui.Ice.panel(g, s, x - 6, y - 6, w + 12, h + 12, new dev.hollow.ui.Ice.Look(1, 1.05f, .6f, 1.2f, 8));
        ci.cancel();
    }
}
