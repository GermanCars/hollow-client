package dev.hollow.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.hollow.Hollow;
import dev.hollow.module.RenderModules;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Level.class)
abstract class LevelMixin {
    @ModifyReturnValue(method = "getRainLevel", at = @At("RETURN"))
    private float hollow$rain(float r) {
        RenderModules.NoRender n = Hollow.module(RenderModules.NoRender.class);
        return ((Level) (Object) this).isClientSide() && n.on() && n.weather.get() ? 0 : r;
    }
}
