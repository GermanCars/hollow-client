package dev.hollow.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.hollow.Hollow;
import dev.hollow.module.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
abstract class EntityMixin {
    private boolean hollow$me() { return (Object) this == Minecraft.getInstance().player; }

    @ModifyVariable(method = "pushFromExplosion", at = @At("HEAD"), argsOnly = true)
    private Vec3 hollow$blast(Vec3 push) { return hollow$me() && Hollow.VELOCITY.on() ? Hollow.VELOCITY.scale(push) : push; }

    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void hollow$turn(double x, double y, CallbackInfo ci) {
        if (!hollow$me()) return;
        RenderModules.Freecam f = Hollow.module(RenderModules.Freecam.class);
        RenderModules.Freelook l = Hollow.module(RenderModules.Freelook.class);
        if (f.on()) f.turn(x, y);
        else if (l.on()) l.turn(x, y);
        else return;
        ci.cancel();
    }

    @ModifyReturnValue(method = "getPickRadius", at = @At("RETURN"))
    private float hollow$hitbox(float r) {
        CombatModules.Hitboxes h = Hollow.module(CombatModules.Hitboxes.class);
        return h.grows((Entity) (Object) this) ? r + h.size.getF() : r;
    }

    @Inject(method = "makeStuckInBlock", at = @At("HEAD"), cancellable = true)
    private void hollow$web(BlockState s, Vec3 v, CallbackInfo ci) { if (hollow$me() && Hollow.module(MoveModules.NoWeb.class).on()) ci.cancel(); }

    @ModifyReturnValue(method = "isPushedByFluid", at = @At("RETURN"))
    private boolean hollow$water(boolean b) {
        MoveModules.NoPush n = Hollow.module(MoveModules.NoPush.class);
        return b && !(hollow$me() && n.on() && n.water.get());
    }

    @ModifyReturnValue(method = "getTeamColor", at = @At("RETURN"))
    private int hollow$glowColor(int c) {
        RenderModules.Glow g = Hollow.module(RenderModules.Glow.class);
        Entity e = (Entity) (Object) this;
        return e.level().isClientSide() && g.glows(e) ? g.color(e) : c;
    }
}
