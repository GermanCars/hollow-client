package dev.hollow.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.hollow.Hollow;
import dev.hollow.module.MoveModules;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
    private boolean hollow$me() { return (Object) this == Minecraft.getInstance().player; }

    @ModifyReturnValue(method = "maxUpStep", at = @At("RETURN"))
    private float hollow$step(float height) { return hollow$me() ? Hollow.STEP.raise(height) : height; }

    @ModifyReturnValue(method = "updateFallFlyingMovement", at = @At("RETURN"))
    private Vec3 hollow$elytra(Vec3 move) { return hollow$me() && Hollow.ELYTRA_FLY.on() ? Hollow.ELYTRA_FLY.steer() : move; }

    @ModifyReturnValue(method = "getJumpPower(F)F", at = @At("RETURN"))
    private float hollow$jump(float power) { return hollow$me() ? Hollow.module(MoveModules.HighJump.class).power(power) : power; }

    @ModifyReturnValue(method = "canStandOnFluid", at = @At("RETURN"))
    private boolean hollow$jesus(boolean b, FluidState fluid) { return b || hollow$me() && Hollow.module(MoveModules.Jesus.class).stands(fluid.is(net.minecraft.tags.FluidTags.LAVA)); }

    @Inject(method = "push", at = @At("HEAD"), cancellable = true)
    private void hollow$push(Entity e, CallbackInfo ci) {
        MoveModules.NoPush n = Hollow.module(MoveModules.NoPush.class);
        if (n.on() && n.entities.get() && (hollow$me() || e == Minecraft.getInstance().player)) ci.cancel();
    }
}
