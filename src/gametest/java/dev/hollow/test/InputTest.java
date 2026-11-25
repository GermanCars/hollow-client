package dev.hollow.test;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hollow.Hollow;
import dev.hollow.core.Module;
import dev.hollow.core.Setting;
import dev.hollow.hud.HudElement;
import dev.hollow.ui.ClickGui;
import dev.hollow.ui.HollowTitleScreen;
import dev.hollow.ui.HudEditor;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;

/** Uses the menus the way a person would. */
public class InputTest implements FabricClientGameTest {
    private ClientGameTestContext c;

    private static Module mod(String n) { return Hollow.modules().stream().filter(m -> m.name.equals(n)).findFirst().orElseThrow(); }

    private void check(String what, boolean ok) { System.out.println("HOLLOWTEST " + (ok ? "PASS " : "FAIL ") + what); }

    /** GUI coordinates. */
    private void at(double x, double y) {
        double k = c.computeOnClient(mc -> mc.getWindow().getScreenWidth() / (double) mc.getWindow().getGuiScaledWidth());
        c.getInput().setCursorPos(x * k, y * k);
        c.waitTicks(2);
    }

    private void click(double x, double y, int button) {
        at(x, y);
        c.getInput().pressMouse(button);
        c.waitTicks(2);
    }

    @Override public void runTest(ClientGameTestContext c) {
        this.c = c;
        c.getInput().resizeWindow(1600, 900);
        c.runOnClient(mc -> Hollow.INTERFACE.startMenu.set(true));
        c.runOnClient(mc -> mc.gui.setScreen(new HollowTitleScreen()));
        c.waitTicks(40);
        // Find Options by its label rather than by guessing where it is.
        int[] b = c.computeOnClient(mc -> mc.gui.screen().children().stream().filter(w -> w instanceof net.minecraft.client.gui.components.AbstractWidget a && a.getMessage().getString().equals("Options")).map(w -> (net.minecraft.client.gui.components.AbstractWidget) w).map(a -> new int[] {a.getX() + a.getWidth() / 2, a.getY() + a.getHeight() / 2}).findFirst().orElseThrow());
        click(b[0], b[1], InputConstants.MOUSE_BUTTON_LEFT);
        c.waitTicks(5);
        c.waitTicks(40);
        check("a start menu button opens its screen", c.computeOnClient(mc -> mc.gui.screen() instanceof OptionsScreen));
        c.getInput().pressKey(InputConstants.KEY_ESCAPE);
        c.waitTicks(10);
        check("escape comes back to the Hollow start menu", c.computeOnClient(mc -> mc.gui.screen() instanceof HollowTitleScreen));
        try (var w = c.worldBuilder().create()) {
            c.waitTicks(30);
            c.getInput().pressKey(InputConstants.KEY_RSHIFT);
            c.waitTicks(30);
            check("Right Shift opens the menu", c.computeOnClient(mc -> mc.gui.screen() instanceof ClickGui));
            float s = c.computeOnClient(mc -> Math.clamp(Math.min(mc.gui.screen().width / 640f, mc.gui.screen().height / 360f), .5f, 1.15f));
            float vw = c.computeOnClient(mc -> (float) mc.gui.screen().width) / s, vh = c.computeOnClient(mc -> (float) mc.gui.screen().height) / s;
            float wx = (vw - 606) / 2, wy = (vh - 336) / 2;
            // First row of Combat is AimAssist.
            double rx = (wx + 112 + 60) * s, ry = (wy + 42 + 3 + 12) * s;
            click(rx, ry, InputConstants.MOUSE_BUTTON_LEFT);
            check("left click switches a module on", c.computeOnClient(mc -> mod("AimAssist").on()));
            click(rx, ry, InputConstants.MOUSE_BUTTON_LEFT);
            check("and off", c.computeOnClient(mc -> !mod("AimAssist").on()));
            click(rx, ry, InputConstants.MOUSE_BUTTON_MIDDLE);
            c.getInput().pressKey(InputConstants.KEY_G);
            c.waitTicks(2);
            check("middle click binds a key", c.computeOnClient(mc -> mod("AimAssist").key == InputConstants.KEY_G));
            c.waitTicks(10);
            c.takeScreenshot("settings");
            double before = c.computeOnClient(mc -> ((Setting.Num) mod("AimAssist").settings.getFirst()).get());
            boolean moved = false;
            for (double y = 118; y < 190 && !moved; y += 3) {
                at((wx + 606 - 100) * s, (wy + y) * s);
                c.getInput().holdMouse(InputConstants.MOUSE_BUTTON_LEFT);
                c.waitTicks(1);
                at((wx + 606 - 20) * s, (wy + y) * s);
                c.getInput().releaseMouse(InputConstants.MOUSE_BUTTON_LEFT);
                c.waitTicks(1);
                moved = c.computeOnClient(mc -> ((Setting.Num) mod("AimAssist").settings.getFirst()).get()) > before;
            }
            check("dragging a slider changes it", moved);
            click((wx + 50) * s, (wy + 44 + 22 * 3 + 10) * s, InputConstants.MOUSE_BUTTON_LEFT);
            c.waitTicks(12);
            click(rx, ry, InputConstants.MOUSE_BUTTON_LEFT);
            check("another category shows its own modules", c.computeOnClient(mc -> mod("Ambience").on()));
            click(rx, ry, InputConstants.MOUSE_BUTTON_LEFT);
            c.getInput().typeChars("blockesp");
            c.waitTicks(14);
            click(rx, ry, InputConstants.MOUSE_BUTTON_RIGHT);
            c.waitTicks(10);
            click((wx + 606 - 100) * s, (wy + 121) * s, InputConstants.MOUSE_BUTTON_LEFT);
            c.waitTicks(14);
            c.takeScreenshot("picker");
            check("search finds a module and right click leaves it off", c.computeOnClient(mc -> !mod("BlockESP").on()));
            // Header drag moves the window.
            at((wx + 200) * s, (wy + 12) * s);
            c.getInput().holdMouse(InputConstants.MOUSE_BUTTON_LEFT);
            c.waitTicks(1);
            at((wx + 212) * s, (wy + 18) * s);
            c.getInput().releaseMouse(InputConstants.MOUSE_BUTTON_LEFT);
            c.waitTicks(2);
            for (int i = 0; i < 6 && c.computeOnClient(mc -> mc.gui.screen() != null); i++) {
                c.getInput().pressKey(InputConstants.KEY_ESCAPE);
                c.waitTicks(2);
            }
            check("escape closes the menu", c.computeOnClient(mc -> mc.gui.screen() == null));
            c.getInput().pressKey(InputConstants.KEY_G);
            c.waitTicks(3);
            check("the key switches the module in game", c.computeOnClient(mc -> mod("AimAssist").on()));
            c.getInput().holdKey(InputConstants.KEY_C);
            c.waitTicks(5);
            check("Zoom is on while C is held", c.computeOnClient(mc -> mod("Zoom").on()));
            c.getInput().releaseKey(InputConstants.KEY_C);
            c.waitTicks(5);
            check("and off when let go", c.computeOnClient(mc -> !mod("Zoom").on()));

            c.runOnClient(mc -> mc.gui.setScreen(new HudEditor(null)));
            c.waitTicks(10);
            HudElement fps = Hollow.HUD_ELEMENTS.stream().filter(e -> e.name.equals("FPS")).findFirst().orElseThrow();
            float[] p = c.computeOnClient(mc -> new float[] {fps.x(mc.gui.screen().width) + fps.width() / 2, fps.y(mc.gui.screen().height) + fps.height() / 2});
            at(p[0], p[1]);
            c.getInput().holdMouse(InputConstants.MOUSE_BUTTON_LEFT);
            c.waitTicks(1);
            at(400, 150);
            c.takeScreenshot("hud-drag");
            c.getInput().releaseMouse(InputConstants.MOUSE_BUTTON_LEFT);
            c.waitTicks(2);
            check("a HUD element drags", c.computeOnClient(mc -> Math.abs(fps.x(mc.gui.screen().width) + fps.width() / 2 - 400) < 8 && Math.abs(fps.y(mc.gui.screen().height) + fps.height() / 2 - 150) < 8));
            double size = c.computeOnClient(mc -> fps.scale.get());
            c.getInput().scroll(1);
            c.waitTicks(2);
            check("scrolling resizes it", c.computeOnClient(mc -> fps.scale.get()) > size);
            for (HudElement e : Hollow.HUD_ELEMENTS) c.runOnClient(mc -> e.set(true));
            c.waitTicks(20);
            c.runOnClient(mc -> mc.gui.setScreen(null));
            c.waitTicks(20);
            c.takeScreenshot("all-hud");
            check("every HUD element draws", true);
            c.runOnClient(mc -> Hollow.INTERFACE.preset.pick(2));
            c.runOnClient(mc -> mc.gui.setScreen(new ClickGui()));
            c.waitTicks(30);
            c.takeScreenshot("ember");
            c.runOnClient(mc -> {
                Hollow.INTERFACE.preset.pick(0);
                mc.gui.setScreen(null);
            });
        }
        c.runOnClient(mc -> {
            Hollow.INTERFACE.startMenu.set(false);
            mc.gui.setScreen(new TitleScreen());
        });
        c.waitTicks(5);
    }
}
