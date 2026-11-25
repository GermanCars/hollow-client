package dev.hollow.mixin;

import dev.hollow.module.Interface;
import dev.hollow.ui.Ease;
import dev.hollow.ui.Fonts;
import dev.hollow.ui.Frame;
import dev.hollow.ui.Style;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSliderButton.class)
abstract class SliderMixin extends AbstractWidget.WithInactiveMessage {
    @Shadow protected double value;

    private SliderMixin() { super(0, 0, 0, 0, Component.empty()); }

    @Inject(method = "extractWidgetRenderState", at = @At("HEAD"), cancellable = true)
    private void hollow$slider(GuiGraphicsExtractor g, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (!Interface.styled()) return;
        Style.State s = Style.state(this);
        if (Float.isNaN(s.value.v)) s.value.snap((float) value);
        s.value.v = Ease.approach(s.value.v, (float) value, 22, Frame.dt);
        Style.slider(g, getX(), getY(), getWidth(), getHeight(), s.value.v, s.hover.to(isActive() && isHoveredOrFocused(), .22f), isActive(), alpha);
        // Sits a little high, clear of the track.
        g.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE).acceptScrollingWithDefaultCenter(Component.empty().append(Fonts.label(getMessage())).withColor(Style.buttonText(0, isActive()) & 0xFFFFFF), getX() + 4, getX() + getWidth() - 4, getY() - 1, getY() + getHeight() - 4);
        handleCursor(g);
        ci.cancel();
    }
}
