package dev.hollow.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.hollow.Hollow;
import dev.hollow.module.RenderModules;
import net.minecraft.client.ClientClockManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientClockManager.ClientClockInstance.class)
abstract class ClockMixin {
    @ModifyReturnValue(method = "totalTicks", at = @At("RETURN"))
    private long hollow$time(long t) {
        RenderModules.Ambience a = Hollow.module(RenderModules.Ambience.class);
        return a.on() && a.time.get() ? a.hour.getInt() : t;
    }
}
