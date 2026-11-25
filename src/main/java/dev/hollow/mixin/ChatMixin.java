package dev.hollow.mixin;

import dev.hollow.Hollow;
import dev.hollow.module.MiscModules;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatComponent.class)
abstract class ChatMixin {
    @ModifyVariable(method = "addMessage", at = @At("HEAD"), argsOnly = true)
    private Component hollow$message(Component c) {
        Hollow.module(MiscModules.Mentions.class).chat(c.getString());
        return Hollow.module(MiscModules.ChatTimes.class).stamp(c);
    }
}
