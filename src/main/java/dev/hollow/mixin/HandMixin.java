package dev.hollow.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.hollow.Hollow;
import dev.hollow.module.RenderModules;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
abstract class HandMixin {
    @Inject(method = "submitArmWithItem", at = @At("HEAD"))
    private void hollow$before(PlayerRenderState ps, FirstPersonHandsAndItemsRenderState st, float partial, float xRot, InteractionHand hand, float attack, ItemStack item, float height, PoseStack pose, SubmitNodeCollector out, int light, CallbackInfo ci) {
        RenderModules.HandView h = Hollow.module(RenderModules.HandView.class);
        pose.pushPose();
        if (!h.on()) return;
        float side = hand == InteractionHand.MAIN_HAND ? 1 : -1;
        pose.translate(h.x.getF() * side, h.y.getF(), -h.z.getF());
        pose.translate(.56f * side, -.52f, -.72f);
        pose.rotateDegrees(Axis.ZP, h.tilt.getF() * side);
        pose.rotateDegrees(Axis.YP, h.yaw.getF() * side);
        pose.scale(h.scale.getF(), h.scale.getF(), h.scale.getF());
        pose.translate(-.56f * side, .52f, .72f);
    }

    @Inject(method = "submitArmWithItem", at = @At("RETURN"))
    private void hollow$after(PlayerRenderState ps, FirstPersonHandsAndItemsRenderState st, float partial, float xRot, InteractionHand hand, float attack, ItemStack item, float height, PoseStack pose, SubmitNodeCollector out, int light, CallbackInfo ci) { pose.popPose(); }
}
