package dev.hollow.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.hollow.module.Interface;
import dev.hollow.ui.Fonts;
import dev.hollow.ui.Style;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractButton.class)
abstract class AbstractButtonMixin {
    @Inject(method = "extractDefaultSprite", at = @At("HEAD"), cancellable = true)
    private void hollow$button(GuiGraphicsExtractor g, CallbackInfo ci) {
        if (!Interface.styled()) return;
        AbstractWidget w = (AbstractWidget) (Object) this;
        float hover = Style.state(w).hover.to(w.isActive() && w.isHoveredOrFocused(), .24f);
        Style.button(g, w, w.getX(), w.getY(), w.getWidth(), w.getHeight(), hover, w.isActive(), w.getAlpha());
        ci.cancel();
    }

    @ModifyExpressionValue(method = "extractDefaultLabel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractButton;getMessage()Lnet/minecraft/network/chat/Component;"))
    private Component hollow$label(Component text) {
        if (!Interface.styled()) return text;
        AbstractWidget w = (AbstractWidget) (Object) this;
        int c = Style.buttonText(Style.state(w).hover.v, w.isActive());
        return Component.empty().append(Fonts.label(text)).withColor(c & 0xFFFFFF);
    }
}
