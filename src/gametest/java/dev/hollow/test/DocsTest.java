package dev.hollow.test;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hollow.Hollow;
import dev.hollow.core.Category;
import dev.hollow.core.Module;
import dev.hollow.core.Setting;
import dev.hollow.ui.Brand;
import dev.hollow.ui.ClickGui;
import dev.hollow.ui.HollowTitleScreen;
import dev.hollow.ui.HudEditor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;

/** Takes the pictures and writes the module list that the documentation uses. */
public class DocsTest implements FabricClientGameTest {
    private ClientGameTestContext c;

    private static Module mod(String n) { return Hollow.modules().stream().filter(m -> m.name.equals(n)).findFirst().orElseThrow(); }

    private void at(double x, double y) {
        double k = c.computeOnClient(mc -> mc.getWindow().getScreenWidth() / (double) mc.getWindow().getGuiScaledWidth());
        c.getInput().setCursorPos(x * k, y * k);
        c.waitTicks(1);
    }

    private void menu(double x, double y) {
        float s = c.computeOnClient(mc -> Math.clamp(Math.min(mc.gui.screen().width / 640f, mc.gui.screen().height / 360f), .5f, 1.15f));
        at(x * s, y * s);
    }

    private void click() { c.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT); }

    private void shot(String n, int wait) {
        c.waitTicks(wait);
        c.takeScreenshot(n);
    }

    /** Pictures take a while, so the animations are slowed down for them. */
    private void slow(boolean on) { c.runOnClient(mc -> Hollow.INTERFACE.settings.stream().filter(v -> v.name.equals("Animation speed")).forEach(v -> ((Setting.Num) v).set(on ? .3 : 1))); }

    private void film(String n, int frames) {
        for (int i = 0; i < frames; i++) {
            System.out.println("HOLLOWFRAME " + n + " " + i + " " + System.nanoTime() / 1000000);
            c.takeScreenshot(String.format(Locale.ROOT, "%s-%03d", n, i));
            c.waitTick();
        }
    }

    /** Empty, so only the cavern and the name show. */
    private static final class Banner extends Screen {
        Banner() { super(Component.empty()); }

        @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
            float h = height * .13f;
            Brand.line(g, (width - Brand.lineWidth(h)) / 2, height * .44f, h, 1, .5f, 1);
        }
    }

    private static String describe(Setting s) {
        return switch (s) {
            case Setting.Bool b -> b.get() ? "on" : "off";
            case Setting.Num n -> n.text() + " (" + trim(n.min) + " to " + trim(n.max) + ")";
            case Setting.Choice o -> String.join(", ", o.options.stream().map(v -> v.equals(o.get()) ? "**" + v + "**" : v).toList());
            case Setting.Color x -> x.theme ? "theme colour" : "colour";
            case Setting.Text t -> t.get().isEmpty() ? "text" : "`" + t.get() + "`";
            case Setting.Ids ids -> "list of " + switch (ids.kind) { case BLOCK -> "blocks"; case ITEM -> "items"; case ENTITY -> "entity types"; } + ", " + ids.all().size() + " picked";
        };
    }

    private static String trim(double v) { return v == Math.rint(v) ? Integer.toString((int) v) : Double.toString(v); }

    private static void list(StringBuilder b, List<? extends Module> all) {
        for (Module m : all.stream().sorted(Comparator.comparing((Module m) -> m.name)).toList()) {
            b.append("### ").append(m.name).append("\n\n").append(m.description).append("\n\n");
            List<Setting> shown = m.settings.stream().filter(s -> !s.name.startsWith("anchor") && !s.name.startsWith("off")).toList();
            if (shown.isEmpty()) continue;
            b.append("<details><summary>").append(shown.size()).append(shown.size() == 1 ? " setting" : " settings").append("</summary>\n\n| Setting | Default and range |\n| --- | --- |\n");
            for (Setting s : shown) b.append("| ").append(s.name).append(" | ").append(describe(s)).append(" |\n");
            b.append("\n</details>\n\n");
        }
    }

    private static void write() {
        StringBuilder b = new StringBuilder(), t = new StringBuilder();
        for (Category k : Category.COLUMNS) {
            List<Module> in = Hollow.modules().stream().filter(m -> m.category == k).toList();
            b.append("## ").append(k.title).append("\n\n");
            list(b, in);
            for (Module m : in) t.append(k.title).append('\t').append(m.name).append('\t').append(m.description).append('\n');
        }
        b.append("## HUD elements\n\n");
        list(b, Hollow.HUD_ELEMENTS);
        for (Module m : Hollow.HUD_ELEMENTS) t.append("HUD\t").append(m.name).append('\t').append(m.description).append('\n');
        b.append("## Client settings\n\n");
        list(b, Hollow.modules().stream().filter(m -> m.category == Category.CLIENT).toList());
        try {
            Files.writeString(Path.of("modules.md"), b);
            Files.writeString(Path.of("modules.tsv"), t);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    @Override public void runTest(ClientGameTestContext c) {
        this.c = c;
        c.runOnClient(mc -> write());
        c.getInput().resizeWindow(1600, 900);
        c.runOnClient(mc -> {
            Hollow.INTERFACE.startMenu.set(true);
            mc.options.renderDistance().set(12);
            mc.gui.setScreen(new HollowTitleScreen());
        });
        at(300, 100);
        shot("title", 110);
        c.runOnClient(mc -> mc.gui.setScreen(new Banner()));
        shot("banner", 60);
        c.runOnClient(mc -> mc.gui.setScreen(new HollowTitleScreen()));
        c.waitTicks(50);
        int[] b = c.computeOnClient(mc -> mc.gui.screen().children().stream().filter(w -> w instanceof AbstractWidget a && a.getMessage().getString().equals("Options")).map(w -> (AbstractWidget) w).map(a -> new int[] {a.getX() + a.getWidth() / 2, a.getY() + a.getHeight() / 2}).findFirst().orElseThrow());
        at(b[0], b[1]);
        c.waitTicks(6);
        slow(true);
        film("fly-a", 4);
        click();
        film("fly", 90);
        slow(false);
        shot("options", 30);
        try (var world = c.worldBuilder().setUseConsistentSettings(false).adjustSettings(s -> {
            s.setSeed("hollow");
            s.setAllowCommands(true);
        }).create()) {
            BlockPos p = world.getServer().computeOnServer(s -> {
                var found = s.overworld().findClosestBiome3d(h -> h.is(Biomes.ICE_SPIKES), BlockPos.ZERO, 6400, 64, 64);
                return found == null ? BlockPos.ZERO : found.getFirst();
            });
            System.out.println("HOLLOWTEST biome at " + p);
            for (String s : new String[] {"gamerule send_command_feedback false", "gamerule advance_time false", "gamerule advance_weather false", "gamerule show_advancement_messages false", "weather clear", "difficulty peaceful", "time set 1000", "effect give @a minecraft:resistance 600 4 true", "tp @a " + p.getX() + " 200 " + p.getZ()}) world.getServer().runCommand(s);
            c.waitTicks(100);
            int y = world.getServer().computeOnServer(s -> s.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, p.getX(), p.getZ()));
            for (String s : new String[] {"give @a minecraft:totem_of_undying 4", "give @a end_crystal 64", "give @a obsidian 64", "give @a netherite_sword", "give @a enchanted_golden_apple 12", "give @a experience_bottle 64", "item replace entity @a weapon.offhand with totem_of_undying", "item replace entity @a armor.head with minecraft:netherite_helmet[damage=120]", "item replace entity @a armor.chest with minecraft:netherite_chestplate[damage=260]", "item replace entity @a armor.legs with minecraft:netherite_leggings[damage=90]", "item replace entity @a armor.feet with minecraft:netherite_boots", "effect give @a minecraft:speed 600 1 true", "effect give @a minecraft:strength 600 1 true", "effect clear @a minecraft:resistance", "tp @a " + (p.getX() + .5) + " " + (y + .2) + " " + (p.getZ() + .5) + " 0 -4"}) world.getServer().runCommand(s);
            for (String n : new String[] {"Sprint", "Step", "NoSlow", "AutoTotem", "Velocity", "ElytraFly", "Fullbright", "ESP"}) c.runOnClient(mc -> mod(n).set(true));
            c.waitTicks(220);
            c.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(6));
            for (int yaw = 0; yaw < 360; yaw += 60) {
                world.getServer().runCommand("tp @a " + (p.getX() + .5) + " " + (y + .2) + " " + (p.getZ() + .5) + " " + yaw + " -4");
                shot("hud-" + yaw, 60);
            }
            c.runOnClient(mc -> {
                mod("KillAura").set(true);
                mod("CrystalAura").set(true);
            });
            slow(true);
            c.runOnClient(mc -> mc.gui.setScreen(new ClickGui()));
            film("menu", 60);
            slow(false);
            menu(250, 150);
            shot("menu-combat", 30);
            String[] names = {"player", "movement", "render", "world", "misc"};
            for (int i = 0; i < names.length; i++) {
                menu(60, 56 + 22 * (i + 1) + 10);
                click();
                menu(250, 170);
                shot("menu-" + names[i], 20);
            }
            menu(60, 230);
            click();
            shot("menu-theme", 25);
            for (int i = 1; i < 6; i++) {
                int k = i;
                c.runOnClient(mc -> Hollow.INTERFACE.preset.pick(k));
                shot("theme-" + i, 20);
            }
            c.runOnClient(mc -> Hollow.INTERFACE.preset.pick(0));
            c.runOnClient(mc -> mc.gui.setScreen(new HudEditor(null)));
            at(266, 150);
            shot("hud-editor", 25);
            c.runOnClient(mc -> mc.gui.setScreen(new PauseScreen(true)));
            shot("pause", 35);
            c.runOnClient(mc -> mc.gui.setScreen(new InventoryScreen(mc.player)));
            shot("inventory", 30);
            c.runOnClient(mc -> mc.gui.setScreen(null));
        }
        c.runOnClient(mc -> {
            Hollow.INTERFACE.startMenu.set(false);
            mc.gui.setScreen(new TitleScreen());
        });
        c.waitTicks(5);
    }
}
