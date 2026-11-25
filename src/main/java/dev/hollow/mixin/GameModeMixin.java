package dev.hollow.mixin;

import dev.hollow.Hollow;
import dev.hollow.hud.Stats;
import dev.hollow.module.*;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
abstract class GameModeMixin {
    @Shadow private int destroyDelay;

    @Inject(method = "attack", at = @At("HEAD"))
    private void hollow$attack(Player p, Entity e, CallbackInfo ci) {
        CombatModules.AutoWeapon w = Hollow.module(CombatModules.AutoWeapon.class);
        if (w.on()) w.beforeAttack(e);
        Stats.hit();
        Hollow.module(RenderModules.HitMarker.class).hit();
    }

    @Inject(method = "startDestroyBlock", at = @At("HEAD"))
    private void hollow$start(BlockPos pos, Direction d, CallbackInfoReturnable<Boolean> cir) {
        if (Hollow.module(PlayerModules.AutoTool.class).on()) PlayerModules.AutoTool.pick(pos);
        Hollow.module(WorldModules.VeinMiner.class).started(pos);
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"))
    private void hollow$continue(BlockPos pos, Direction d, CallbackInfoReturnable<Boolean> cir) { if (Hollow.module(PlayerModules.FastBreak.class).on()) destroyDelay = 0; }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void hollow$use(LocalPlayer p, InteractionHand h, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (!p.isShiftKeyDown() && Hollow.module(PlayerModules.NoInteract.class).blocks(hit.getBlockPos())) cir.setReturnValue(InteractionResult.PASS);
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void hollow$interact(Player p, Entity e, EntityHitResult hit, InteractionHand h, CallbackInfoReturnable<InteractionResult> cir) { if (Hollow.module(PlayerModules.NoInteract.class).blocks(e)) cir.setReturnValue(InteractionResult.PASS); }
}
