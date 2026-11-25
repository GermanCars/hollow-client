package dev.hollow.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.hollow.Hollow;
import dev.hollow.module.RenderModules;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
abstract class CameraMixin {
    @Shadow private boolean detached;

    @Shadow protected abstract void setRotation(float yRot, float xRot);

    @Shadow protected abstract void setPosition(double x, double y, double z);

    @Shadow protected abstract void move(float forwards, float up, float right);

    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void hollow$free(float partial, CallbackInfo ci) {
        RenderModules.Freecam f = Hollow.module(RenderModules.Freecam.class);
        if (f.on()) {
            setRotation(f.yaw, f.pitch);
            setPosition(f.pos.x, f.pos.y, f.pos.z);
            detached = true;
        }
    }

    /** Turned before the third person camera backs away, so it orbits. */
    @ModifyArg(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 1), index = 0)
    private float hollow$lookYaw(float yaw) {
        RenderModules.Freelook f = Hollow.module(RenderModules.Freelook.class);
        return f.on() ? f.yaw : yaw;
    }

    @ModifyArg(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 1), index = 1)
    private float hollow$lookPitch(float pitch) {
        RenderModules.Freelook f = Hollow.module(RenderModules.Freelook.class);
        return f.on() ? f.pitch : pitch;
    }

    @ModifyReturnValue(method = "calculateFov", at = @At("RETURN"))
    private float hollow$zoom(float fov) { return Hollow.module(RenderModules.Zoom.class).fov(fov); }

    @Inject(method = "getMaxZoom", at = @At("HEAD"), cancellable = true)
    private void hollow$clip(float dist, CallbackInfoReturnable<Float> cir) {
        RenderModules.CameraTweaks c = Hollow.module(RenderModules.CameraTweaks.class);
        if (c.on() && c.clip.get()) cir.setReturnValue(c.distance.getF());
    }
}
