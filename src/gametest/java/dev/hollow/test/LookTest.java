package dev.hollow.test;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hollow.Hollow;
import dev.hollow.ui.ClickGui;
import dev.hollow.ui.HollowTitleScreen;
import dev.hollow.ui.HudEditor;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Photographs every screen. */
public class LookTest implements FabricClientGameTest {
    private ClientGameTestContext c;

    /** GUI coordinates. */
    private void at(double x, double y) {
        double k = c.computeOnClient(mc -> mc.getWindow().getScreenWidth() / (double) mc.getWindow().getGuiScaledWidth());
        c.getInput().setCursorPos(x * k, y * k);
        c.waitTicks(1);
    }

    /** Coordinates of the menu's own layout, 640 by 360 with the window in the middle. */
    private void menu(double x, double y) {
        float s = c.computeOnClient(mc -> Math.clamp(Math.min(mc.gui.screen().width / 640f, mc.gui.screen().height / 360f), .5f, 1.15f));
        at(x * s, y * s);
    }

    private void shot(String n, int wait) {
        c.waitTicks(wait);
        c.takeScreenshot(n);
    }

    private void use(BlockPos p) { c.runOnClient(mc -> mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(p), net.minecraft.core.Direction.UP, p, false))); }

    @Override public void runTest(ClientGameTestContext c) {
        this.c = c;
        c.getInput().resizeWindow(1600, 900);
        c.runOnClient(mc -> Hollow.INTERFACE.startMenu.set(true));
        c.runOnClient(mc -> mc.gui.setScreen(new HollowTitleScreen()));
        at(300, 100);
        shot("title-early", 10);
        shot("title", 90);
        int[] b = c.computeOnClient(mc -> mc.gui.screen().children().stream().filter(w -> w instanceof net.minecraft.client.gui.components.AbstractWidget a && a.getMessage().getString().equals("Options")).map(w -> (net.minecraft.client.gui.components.AbstractWidget) w).map(a -> new int[] {a.getX() + a.getWidth() / 2, a.getY() + a.getHeight() / 2}).findFirst().orElseThrow());
        at(b[0], b[1]);
        shot("title-hover", 7);
        c.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        shot("shatter", 3);
        shot("flight", 5);
        shot("options", 50);
        c.runOnClient(mc -> mc.gui.setScreen(new VideoSettingsScreen(new TitleScreen(), mc, mc.options)));
        at(160, 88);
        shot("video", 30);
        c.runOnClient(mc -> mc.gui.setScreen(new SelectWorldScreen(new TitleScreen())));
        shot("worlds", 40);
        try (var world = c.worldBuilder().create()) {
            c.waitTicks(40);
            for (String s : new String[] {"gamerule send_command_feedback false", "time set day", "give @a minecraft:totem_of_undying 3", "give @a obsidian 43", "give @a netherite_sword", "item replace entity @a armor.head with minecraft:netherite_helmet[damage=120]", "item replace entity @a armor.chest with minecraft:netherite_chestplate[damage=560]", "item replace entity @a armor.legs with minecraft:diamond_leggings[damage=300]", "item replace entity @a armor.feet with minecraft:netherite_boots", "effect give @a minecraft:speed 90 1", "summon husk 3 -60 6 {NoAI:1b}", "fill -8 -61 4 8 -55 14 spruce_planks hollow", "setblock 1 -60 6 chest{Items:[{Slot:0b,id:\"minecraft:diamond\",count:5},{Slot:11b,id:\"minecraft:end_crystal\",count:30}]}", "setblock -1 -60 2 furnace", "setblock 1 -60 2 crafting_table", "tp @a 0.5 -60 0.5 0 8"}) world.getServer().runCommand(s);
            for (String n : new String[] {"Sprint", "Step", "NoSlow", "AutoTotem", "KillAura", "Velocity", "ESP", "StorageESP"}) c.runOnClient(mc -> Hollow.modules().stream().filter(m -> m.name.equals(n)).findFirst().ifPresent(m -> m.set(true)));
            shot("hud", 25);
            c.waitTicks(50);
            c.runOnClient(mc -> mc.gui.setScreen(new ClickGui()));
            menu(250, 150);
            shot("gui-early", 5);
            shot("gui", 45);
            menu(60, 56 + 22 * 3 + 10);
            c.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
            menu(250, 170);
            c.waitTicks(10);
            c.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
            shot("gui-click", 2);
            menu(60, 230);
            c.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
            shot("gui-theme", 25);
            menu(60, 252);
            c.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
            shot("gui-friends", 25);
            menu(60, 66);
            c.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
            c.runOnClient(mc -> mc.gui.setScreen(new HudEditor(null)));
            at(266, 150);
            shot("hud-editor", 25);
            c.runOnClient(mc -> mc.gui.setScreen(new PauseScreen(true)));
            shot("pause", 35);
            c.runOnClient(mc -> mc.gui.setScreen(new InventoryScreen(mc.player)));
            shot("inventory", 30);
            c.runOnClient(mc -> mc.player.closeContainer());
            use(new BlockPos(-1, -60, 2));
            shot("furnace", 25);
            c.runOnClient(mc -> mc.player.closeContainer());
            world.getServer().runCommand("tp @a 1.5 -60 3.5 0 30");
            world.getServer().runCommand("fill 0 -60 4 2 -58 5 air");
            c.waitTicks(5);
            use(new BlockPos(1, -60, 6));
            c.waitTicks(10);
            at(266 - 40, 118);
            shot("chest", 20);
            c.runOnClient(mc -> mc.player.closeContainer());
            c.runOnClient(mc -> {
                for (int i = 0; i < 6; i++) mc.gui.hud.getChat().addClientSystemMessage(net.minecraft.network.chat.Component.literal("<Player" + i + "> the quick brown fox jumps over line " + i));
                mc.gui.setScreen(new ChatScreen("", false));
            });
            shot("chat", 15);
            c.runOnClient(mc -> mc.gui.setScreen(null));
            c.getInput().resizeWindow(854, 480);
            c.runOnClient(mc -> mc.gui.setScreen(new ClickGui()));
            shot("small-gui", 30);
            c.getInput().resizeWindow(1600, 900);
            c.runOnClient(mc -> mc.gui.setScreen(null));
        }
        c.runOnClient(mc -> {
            Hollow.INTERFACE.startMenu.set(false);
            mc.gui.setScreen(new TitleScreen());
        });
        c.waitTicks(5);
    }
}
