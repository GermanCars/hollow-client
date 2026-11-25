package dev.hollow.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.hollow.Hollow;
import dev.hollow.module.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class MinecraftMixin {
    @Shadow private int rightClickDelay;
    @Shadow public LocalPlayer player;

    @Inject(method = "tick", at = @At("HEAD"))
    private void hollow$tick(CallbackInfo ci) { Hollow.clientTick(); }

    @Inject(method = "startUseItem", at = @At("TAIL"))
    private void hollow$fastUse(CallbackInfo ci) {
        PlayerModules.FastUse f = Hollow.module(PlayerModules.FastUse.class);
        if (f.on() && player != null && f.applies()) rightClickDelay = f.delay();
    }

    @ModifyReturnValue(method = "shouldEntityAppearGlowing", at = @At("RETURN"))
    private boolean hollow$glow(boolean b, Entity e) { return b || Hollow.module(RenderModules.Glow.class).glows(e); }

    @ModifyReturnValue(method = "createTitle", at = @At("RETURN"))
    private String hollow$title(String t) { return Hollow.INTERFACE.windowTitle.get() ? "Hollow Client | " + t.replace("*", "") : t; }

    /** Mining goes on while an item is being used. */
    @ModifyExpressionValue(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z", ordinal = 0))
    private boolean hollow$multitask(boolean using) { return using && !(Hollow.module(PlayerModules.Multitask.class).on() && ((Minecraft) (Object) this).options.keyAttack.isDown()); }
}
