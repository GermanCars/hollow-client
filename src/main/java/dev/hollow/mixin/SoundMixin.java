package dev.hollow.mixin;

import dev.hollow.Hollow;
import dev.hollow.module.MiscModules;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundManager.class)
abstract class SoundMixin {
    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void hollow$mute(SoundInstance s, CallbackInfoReturnable<SoundEngine.PlayResult> cir) { if (Hollow.module(MiscModules.SoundFilter.class).mutes(s)) cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED); }
}
