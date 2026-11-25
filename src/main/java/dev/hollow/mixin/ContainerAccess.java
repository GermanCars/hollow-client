package dev.hollow.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface ContainerAccess {
    @Accessor("leftPos") int hollow$left();

    @Accessor("topPos") int hollow$top();

    @Accessor("imageWidth") int hollow$width();

    @Accessor("imageHeight") int hollow$height();
}
