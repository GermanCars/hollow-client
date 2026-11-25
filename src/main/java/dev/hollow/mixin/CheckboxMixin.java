package dev.hollow.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dev.hollow.module.Interface;
import dev.hollow.ui.Draw;
import dev.hollow.ui.Style;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Checkbox.class)
abstract class CheckboxMixin {
    @Shadow private boolean selected;

    @WrapOperation(method = "extractContents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V"))
    private void hollow$check(GuiGraphicsExtractor g, RenderPipeline p, Identifier id, int x, int y, int w, int h, int color, Operation<Void> original) {
        if (!Interface.styled()) {
            original.call(g, p, id, x, y, w, h, color);
            return;
        }
        Style.State s = Style.state(this);
        Draw d = new Draw(g);
        Style.check(d, x + w / 2f, y + h / 2f, w * .48f, s.press.to(selected, .2f), s.hover.to(((Checkbox) (Object) this).isHoveredOrFocused(), .2f), (color >>> 24) / 255f);
        d.flush();
    }
}
