package dev.hollow.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.hollow.module.Interface;
import dev.hollow.ui.Fonts;
import net.minecraft.client.gui.components.AbstractStringWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Headings on the game's own screens. */
@Mixin(StringWidget.class)
abstract class StringWidgetMixin extends AbstractStringWidget {
    private StringWidgetMixin() { super(0, 0, 0, 0, Component.empty(), null); }

    @Override public Component getMessage() { return Interface.styled() ? Fonts.label(super.getMessage()) : super.getMessage(); }
}
