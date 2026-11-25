package dev.hollow.mixin;

import dev.hollow.Hollow;
import dev.hollow.core.Setting;
import dev.hollow.hud.HudElement;
import dev.hollow.hud.Notices;
import dev.hollow.module.RenderModules;
import dev.hollow.ui.ClickGui;
import dev.hollow.ui.HudEditor;
import java.util.function.Function;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
abstract class HudMixin {
    @Shadow @Final private Minecraft minecraft;

    private static boolean hollow$hides(Function<RenderModules.NoRender, Setting.Bool> s) {
        RenderModules.NoRender n = Hollow.module(RenderModules.NoRender.class);
        return n.on() && s.apply(n).get();
    }

    /** The Effects element replaces the game's icons. */
    @Inject(method = "extractEffects", at = @At("HEAD"), cancellable = true)
    private void hollow$effects(GuiGraphicsExtractor g, DeltaTracker delta, CallbackInfo ci) { if (Hollow.HUD.on() && Hollow.EFFECTS.on()) ci.cancel(); }

    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
    private void hollow$crosshair(GuiGraphicsExtractor g, DeltaTracker delta, CallbackInfo ci) {
        RenderModules.Crosshair c = Hollow.module(RenderModules.Crosshair.class);
        if (!c.on() || !minecraft.options.getCameraType().isFirstPerson()) return;
        c.draw(g);
        ci.cancel();
    }

    @Inject(method = "extractTextureOverlay", at = @At("HEAD"), cancellable = true)
    private void hollow$pumpkin(CallbackInfo ci) { if (hollow$hides(n -> n.pumpkin)) ci.cancel(); }

    @Inject(method = {"extractPortalOverlay", "extractConfusionOverlay"}, at = @At("HEAD"), cancellable = true)
    private void hollow$nausea(CallbackInfo ci) { if (hollow$hides(n -> n.nausea)) ci.cancel(); }

    @Inject(method = "extractVignette", at = @At("HEAD"), cancellable = true)
    private void hollow$vignette(CallbackInfo ci) { if (hollow$hides(n -> n.vignette)) ci.cancel(); }

    @Inject(method = "extractBossOverlay", at = @At("HEAD"), cancellable = true)
    private void hollow$bosses(CallbackInfo ci) { if (hollow$hides(n -> n.bosses)) ci.cancel(); }

    @Inject(method = "extractScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void hollow$scoreboard(CallbackInfo ci) { if (hollow$hides(n -> n.scoreboard)) ci.cancel(); }

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void hollow$overlay(GuiGraphicsExtractor g, DeltaTracker delta, CallbackInfo ci) {
        if (!((Hud) (Object) this).isHidden()) Hollow.overlay(g, delta.getGameTimeDeltaPartialTick(false));
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void hollow$hud(GuiGraphicsExtractor g, DeltaTracker delta, CallbackInfo ci) {
        if (minecraft.player == null || minecraft.level == null || ((Hud) (Object) this).isHidden()) return;
        g.nextStratum();
        // The editor draws the elements itself.
        if (Hollow.HUD.on() && !(minecraft.gui.screen() instanceof HudEditor) && !(minecraft.gui.screen() instanceof ClickGui) && !minecraft.getDebugOverlay().showDebugScreen()) {
            for (HudElement e : Hollow.HUD_ELEMENTS) e.draw(g, g.guiWidth(), g.guiHeight(), false);
        }
        Notices.draw(g);
    }
}
