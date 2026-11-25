package dev.hollow.mixin;

import dev.hollow.Hollow;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapRenderStateExtractor.class)
abstract class LightmapMixin {
    @Inject(method = "extract", at = @At("RETURN"))
    private void hollow$bright(LightmapRenderState state, float partial, CallbackInfo ci) {
        if (!state.needsUpdate || !Hollow.FULLBRIGHT.on()) return;
        state.nightVisionEffectIntensity = 1;
        state.nightVisionColor = new Vector3f(1, 1, 1);
        state.darknessEffectScale = 0;
    }
}
