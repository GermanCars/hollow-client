package dev.hollow.mixin;

import dev.hollow.Hollow;
import dev.hollow.module.RenderModules;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelExtractor.class)
abstract class LevelExtractorMixin {
    @Inject(method = "extractBlockOutline", at = @At("HEAD"), cancellable = true)
    private void hollow$outline(Camera camera, LevelRenderState state, CallbackInfo ci) {
        if (!Hollow.module(RenderModules.BlockOutline.class).on()) return;
        state.blockOutlineRenderState = null;
        ci.cancel();
    }
}
