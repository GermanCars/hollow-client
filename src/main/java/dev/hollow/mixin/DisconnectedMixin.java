package dev.hollow.mixin;

import dev.hollow.Hollow;
import dev.hollow.module.Interface;
import dev.hollow.module.MiscModules;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisconnectedScreen.class)
abstract class DisconnectedMixin extends Screen {
    @Unique private Button hollow$button;
    @Unique private long hollow$since;
    @Unique private boolean hollow$stopped;

    private DisconnectedMixin() { super(Component.empty()); }

    @Inject(method = "init", at = @At("TAIL"))
    private void hollow$add(CallbackInfo ci) {
        MiscModules.AutoReconnect r = Hollow.module(MiscModules.AutoReconnect.class);
        if (!r.ready()) return;
        if (hollow$since == 0) hollow$since = Util.getMillis();
        hollow$button = addRenderableWidget(Button.builder(Component.literal("Reconnect"), b -> r.reconnect()).bounds(width / 2 - 100, Math.min(height - 30, height / 2 + 70), 200, 20).build());
        if (r.on()) addRenderableWidget(Button.builder(Component.literal("Stop reconnecting"), b -> hollow$stopped = true).bounds(width / 2 - 100, Math.min(height - 8, height / 2 + 94), 200, 20).build());
    }

    @Override public void tick() {
        super.tick();
        MiscModules.AutoReconnect r = Hollow.module(MiscModules.AutoReconnect.class);
        if (hollow$button == null || !r.on()) return;
        if (hollow$stopped) {
            hollow$button.setMessage(Component.literal("Reconnect"));
            return;
        }
        long left = (long) (r.seconds.get() * 1000) - (Util.getMillis() - hollow$since);
        hollow$button.setMessage(Component.literal("Reconnecting in " + Math.max(0, (left + 999) / 1000)));
        if (left <= 0) {
            hollow$stopped = true;
            r.reconnect();
        }
    }
}
