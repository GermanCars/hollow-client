package dev.hollow.mixin;

import dev.hollow.Hollow;
import dev.hollow.core.Colors;
import dev.hollow.module.MiscModules;
import dev.hollow.ui.Fonts;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerTabOverlay.class)
abstract class TabMixin {
    @ModifyConstant(method = "getPlayerInfos", constant = @Constant(longValue = 80))
    private long hollow$most(long n) {
        MiscModules.BetterTab t = Hollow.module(MiscModules.BetterTab.class);
        return t.on() ? t.most.getInt() : n;
    }

    @Inject(method = "extractPingIcon", at = @At("HEAD"), cancellable = true)
    private void hollow$ping(GuiGraphicsExtractor g, int width, int x, int y, PlayerInfo info, CallbackInfo ci) {
        MiscModules.BetterTab t = Hollow.module(MiscModules.BetterTab.class);
        if (!t.on() || !t.ping.get()) return;
        Fonts.BOLD.right(g, Integer.toString(info.getLatency()), x + width - 1, y + 7, 7.5f, Colors.health(1 - Math.clamp(info.getLatency() / 400f, 0, 1)));
        ci.cancel();
    }
}
