package dev.hollow.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dev.hollow.module.Interface;
import dev.hollow.ui.Style;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EditBox.class)
abstract class EditBoxMixin {
    @WrapOperation(method = "extractWidgetRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private void hollow$field(GuiGraphicsExtractor g, RenderPipeline p, Identifier id, int x, int y, int w, int h, Operation<Void> original) {
        if (Interface.styled()) Style.field(g, x, y, w, h, Style.state(this).hover.to(((EditBox) (Object) this).isFocused(), .25f), 1);
        else original.call(g, p, id, x, y, w, h);
    }
}
