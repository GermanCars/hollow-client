package dev.hollow.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.hollow.Hollow;
import dev.hollow.module.RenderModules;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntityRenderer.class)
abstract class NameMixin {
    @ModifyReturnValue(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z", at = @At("RETURN"))
    private boolean hollow$name(boolean b, LivingEntity e, double d) { return b && !Hollow.module(RenderModules.Nametags.class).replaces(e); }
}
