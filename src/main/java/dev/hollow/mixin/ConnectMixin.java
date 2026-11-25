package dev.hollow.mixin;

import dev.hollow.Hollow;
import dev.hollow.module.MiscModules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConnectScreen.class)
abstract class ConnectMixin {
    @Inject(method = "startConnecting", at = @At("HEAD"))
    private static void hollow$remember(Screen parent, Minecraft mc, ServerAddress address, ServerData data, boolean quick, TransferState transfer, CallbackInfo ci) { Hollow.module(MiscModules.AutoReconnect.class).connecting(data); }
}
