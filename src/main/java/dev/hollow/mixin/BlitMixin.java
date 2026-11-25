package dev.hollow.mixin;

import dev.hollow.module.Interface;
import dev.hollow.ui.GlassTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GuiGraphicsExtractor.class)
abstract class BlitMixin {
    /** Every kind of textured blit ends up here. */
    @ModifyVariable(method = "innerBlit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIIFFFFI)V", at = @At("HEAD"), argsOnly = true)
    private Identifier hollow$glass(Identifier id) { return GlassTextures.of(id); }

    /** The dark grey of container titles can't be read on glass. */
    @ModifyVariable(method = {"text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V", "text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V"}, at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private int hollow$label(int color) {
        if (color != 0xFF404040 || !Interface.glassContainers()) return color;
        var s = Minecraft.getInstance().gui.screen();
        return s instanceof AbstractContainerScreen<?> && !(s instanceof CreativeModeInventoryScreen) ? 0xFFEAF4FA : color;
    }
}
