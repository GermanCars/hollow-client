package dev.hollow.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.hollow.Hollow;
import dev.hollow.module.WorldModules;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DeltaTracker.Timer.class)
abstract class DeltaTrackerMixin {
    @ModifyExpressionValue(method = "advanceGameTime", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/floats/FloatUnaryOperator;apply(F)F"))
    private float hollow$timer(float ms) { return ms / Hollow.module(WorldModules.Timer.class).factor(); }
}
