package dev.hollow.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.hollow.Hollow;
import dev.hollow.module.*;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
abstract class LocalPlayerMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void hollow$beforeMove(CallbackInfo ci) { Hollow.beforeMove(); }

    @Inject(method = "sendChanges", at = @At("HEAD"))
    private void hollow$beforeSend(CallbackInfo ci) { Hollow.beforeSend(); }

    @Inject(method = "sendChanges", at = @At("RETURN"))
    private void hollow$afterSend(CallbackInfo ci) { Hollow.afterSend(); }

    @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;onGround()Z"))
    private boolean hollow$noFall(boolean onGround) { return onGround || Hollow.NO_FALL.spoof(); }

    @Inject(method = "itemUseSpeedMultiplier", at = @At("HEAD"), cancellable = true)
    private void hollow$noSlow(CallbackInfoReturnable<Float> cir) { if (Hollow.NO_SLOW.on()) cir.setReturnValue(1f); }

    @Inject(method = "isSlowDueToUsingItem", at = @At("HEAD"), cancellable = true)
    private void hollow$noSlowSprint(CallbackInfoReturnable<Boolean> cir) { if (Hollow.NO_SLOW.on()) cir.setReturnValue(false); }

    @Inject(method = "moveTowardsClosestSpace", at = @At("HEAD"), cancellable = true)
    private void hollow$noBlockPush(double x, double z, CallbackInfo ci) {
        MoveModules.NoPush n = Hollow.module(MoveModules.NoPush.class);
        if (n.on() && n.blocks.get()) ci.cancel();
    }

    @Inject(method = "openTextEdit", at = @At("HEAD"), cancellable = true)
    private void hollow$sign(SignBlockEntity sign, SignTextSlot slot, CallbackInfo ci) {
        if (Hollow.module(WorldModules.AutoSign.class).write(sign.getBlockPos(), slot)) ci.cancel();
    }
}
