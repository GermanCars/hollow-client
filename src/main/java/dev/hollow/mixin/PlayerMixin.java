package dev.hollow.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.hollow.Hollow;
import dev.hollow.module.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
abstract class PlayerMixin {
    private boolean hollow$me() { return (Object) this == Minecraft.getInstance().player; }

    @ModifyReturnValue(method = "isStayingOnGroundSurface", at = @At("RETURN"))
    private boolean hollow$safeWalk(boolean b) { return b || hollow$me() && (Hollow.module(MoveModules.SafeWalk.class).on() || Hollow.SCAFFOLD.safe()); }

    @ModifyReturnValue(method = "blockInteractionRange", at = @At("RETURN"))
    private double hollow$blocks(double r) {
        PlayerModules.Reach m = Hollow.module(PlayerModules.Reach.class);
        return hollow$me() && m.on() ? Math.max(r, m.blocks.get()) : r;
    }

    @ModifyReturnValue(method = "entityInteractionRange", at = @At("RETURN"))
    private double hollow$entities(double r) {
        PlayerModules.Reach m = Hollow.module(PlayerModules.Reach.class);
        return hollow$me() && m.on() ? Math.max(r, m.entities.get()) : r;
    }
}
