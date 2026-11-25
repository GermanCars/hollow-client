package dev.hollow.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.hollow.module.Interface;
import dev.hollow.ui.Fonts;
import net.minecraft.client.gui.components.tabs.MenuTabBar;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MenuTabBar.MenuTabButton.class)
abstract class TabLabelMixin {
    @ModifyExpressionValue(method = {"renderLabel", "renderFocusUnderline"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/tabs/MenuTabBar$MenuTabButton;getMessage()Lnet/minecraft/network/chat/Component;"))
    private Component hollow$label(Component c) { return Interface.styled() ? Fonts.label(c) : c; }
}
