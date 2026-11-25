package dev.hollow.mixin;

import dev.hollow.Hollow;
import dev.hollow.module.RenderModules;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
abstract class ClientLevelMixin {
    @Inject(method = "destroyBlockProgress", at = @At("HEAD"))
    private void hollow$breaking(int id, BlockPos pos, int progress, CallbackInfo ci) { Hollow.module(RenderModules.BreakEsp.class).progress(id, pos, progress); }

    @Inject(method = "doAddParticle", at = @At("HEAD"), cancellable = true)
    private void hollow$particles(ParticleOptions p, boolean a, boolean b, double x, double y, double z, double dx, double dy, double dz, CallbackInfo ci) {
        RenderModules.NoRender n = Hollow.module(RenderModules.NoRender.class);
        if (n.on() && n.explosions.get() && (p.getType() == ParticleTypes.EXPLOSION || p.getType() == ParticleTypes.EXPLOSION_EMITTER)) ci.cancel();
    }
}
