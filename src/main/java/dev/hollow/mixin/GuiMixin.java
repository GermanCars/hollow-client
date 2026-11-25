package dev.hollow.mixin;

import dev.hollow.module.Interface;
import dev.hollow.render.Cavern;
import dev.hollow.ui.ClickGui;
import dev.hollow.ui.HollowTitleScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Gui.class)
abstract class GuiMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow private boolean clientLevelTeardownInProgress;

    /** The game turns "no screen, no world" into a title screen further down, so that case is replaced here too. */
    @ModifyVariable(method = "setScreen", argsOnly = true, at = @At("HEAD"))
    private Screen hollow$title(Screen screen) {
        boolean title = screen instanceof TitleScreen || screen == null && minecraft.level == null && !clientLevelTeardownInProgress;
        Screen out = title && Interface.snowyMenus() ? new HollowTitleScreen() : screen;
        // Each menu lies in its own chamber of the cavern; their sub-pages a little further in.
        if (out instanceof HollowTitleScreen) Cavern.go(0);
        else if (out instanceof SelectWorldScreen) Cavern.go(1);
        else if (out instanceof JoinMultiplayerScreen) Cavern.go(2);
        else if (out instanceof OptionsScreen) Cavern.go(3);
        else if (out instanceof OptionsSubScreen) Cavern.go(3.55f);
        else if (out instanceof CreateWorldScreen) Cavern.go(1.55f);
        else if (out instanceof ClickGui) Cavern.go(6);
        return out;
    }
}
