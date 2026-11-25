package dev.hollow.mixin;

import dev.hollow.module.Interface;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Its text is dark, for the light background it no longer has. */
@Mixin(RecipeToast.class)
abstract class RecipeToastMixin {
    @ModifyConstant(method = "extractRenderState", constant = @Constant(intValue = -11534256))
    private int hollow$title(int c) { return Interface.styled() ? 0xFFEFF4F3 : c; }

    @ModifyConstant(method = "extractRenderState", constant = @Constant(intValue = -16777216))
    private int hollow$text(int c) { return Interface.styled() ? 0xFFB9C6C9 : c; }
}
