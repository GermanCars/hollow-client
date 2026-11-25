package dev.hollow.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.hollow.Hollow;
import dev.hollow.hud.Stats;
import dev.hollow.module.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
abstract class ClientPacketListenerMixin {
    @Unique private float hollow$yaw, hollow$pitch;

    @Inject(method = "sendChat", at = @At("HEAD"), cancellable = true)
    private void hollow$command(String text, CallbackInfo ci) { if (Hollow.INTERFACE.chatPrefix.get() && Hollow.command(text)) ci.cancel(); }

    @ModifyVariable(method = "sendChat", at = @At("HEAD"), argsOnly = true)
    private String hollow$tag(String text) { return text.startsWith(".") ? text : Hollow.module(MiscModules.ChatTag.class).add(text); }

    @WrapOperation(method = "handleSetEntityMotion", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;lerpMotion(Lnet/minecraft/world/phys/Vec3;)V"))
    private void hollow$velocity(Entity entity, Vec3 motion, Operation<Void> original) {
        boolean mine = entity == Minecraft.getInstance().player && Hollow.VELOCITY.on();
        original.call(entity, mine ? Hollow.VELOCITY.hit(entity.getDeltaMovement(), motion) : motion);
    }

    @Inject(method = "handleEntityEvent", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ClientboundEntityEventPacket;getEventId()B"))
    private void hollow$event(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        if (!(packet.getEntity(Minecraft.getInstance().level) instanceof Player p)) return;
        CombatModules.PopCounter c = Hollow.module(CombatModules.PopCounter.class);
        if (packet.getEventId() == 35) {
            c.popped(p, Stats.popped(p));
            Hollow.module(RenderModules.PopEffects.class).popped(p);
        } else if (packet.getEventId() == 3) c.died(p, Stats.died(p));
    }

    @Inject(method = "handleDamageEvent", at = @At("TAIL"))
    private void hollow$damage(ClientboundDamageEventPacket packet, CallbackInfo ci) { if (Minecraft.getInstance().player != null && packet.entityId() == Minecraft.getInstance().player.getId()) Stats.gotHit(); }

    @Inject(method = "handleMovePlayer", at = @At("HEAD"))
    private void hollow$beforeTeleport(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        Player p = Minecraft.getInstance().player;
        if (p == null || !Minecraft.getInstance().isSameThread()) return;
        hollow$yaw = p.getYRot();
        hollow$pitch = p.getXRot();
    }

    @Inject(method = "handleMovePlayer", at = @At("TAIL"))
    private void hollow$afterTeleport(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        Player p = Minecraft.getInstance().player;
        // The first one in a world sets where you look; leave it.
        if (!Hollow.module(PlayerModules.NoRotate.class).on() || p.tickCount < 40) return;
        p.setYRot(hollow$yaw);
        p.setXRot(hollow$pitch);
    }
}
