package dev.hollow.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dev.hollow.core.Colors;
import dev.hollow.module.Interface;
import dev.hollow.ui.Draw;
import dev.hollow.ui.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractScrollArea.class)
abstract class ScrollAreaMixin {
    @WrapOperation(method = "extractScrollbar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private void hollow$bar(GuiGraphicsExtractor g, RenderPipeline p, Identifier id, int x, int y, int w, int h, Operation<Void> original) {
        if (!Interface.styled()) {
            original.call(g, p, id, x, y, w, h);
            return;
        }
        Draw d = new Draw(g);
        if (id.getPath().contains("background")) d.rect(x + w / 2f - .5f, y, 1, h, Colors.with(0xFFFFFF, .12f));
        else d.poly(Draw.round(x + w / 2f - 1.5f, y, 3, h, 1.5f), Draw.down(y, y + h, Theme.accent(), Theme.second()));
        d.flush();
    }
}
