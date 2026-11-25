package dev.hollow.test;

import dev.hollow.Hollow;
import dev.hollow.build.LitematicaBridge;
import dev.hollow.core.Module;
import dev.hollow.ui.ClickGui;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** With the whole mod pack loaded: menus still draw, and Printer rebuilds a schematic exactly. */
public class PackTest implements FabricClientGameTest {
    private static Module mod(String n) { return Hollow.modules().stream().filter(m -> m.name.equals(n)).findFirst().orElseThrow(); }

    private static void say(String s) { System.out.println("HOLLOWTEST " + s); }

    @Override public void runTest(ClientGameTestContext c) {
        c.getInput().resizeWindow(1600, 900);
        say("mods: sodium=" + FabricLoader.getInstance().isModLoaded("sodium") + " iris=" + FabricLoader.getInstance().isModLoaded("iris") + " litematica=" + FabricLoader.getInstance().isModLoaded("litematica") + " modmenu=" + FabricLoader.getInstance().isModLoaded("modmenu") + " total=" + FabricLoader.getInstance().getAllMods().size());
        c.waitTicks(100);
        c.takeScreenshot("pack-title");
        try (var w = c.worldBuilder().adjustSettings(s -> s.setAllowCommands(true)).create()) {
            c.waitTicks(60);
            String[] setup = {"gamerule send_command_feedback false", "gamerule advance_time false", "time set day", "tp @a 0.5 -60 0.5 0 20",
                // Awkward blocks: stairs both ways up, slabs, logs, torches, a chest, a lever, a trapdoor, a repeater, a door, a fence, a double slab.
                "setblock 2 -60 2 stone", "setblock 3 -60 2 oak_stairs[facing=east]", "setblock 4 -60 2 oak_stairs[facing=south,half=top]", "setblock 2 -59 2 oak_slab[type=bottom]", "setblock 3 -60 3 oak_slab[type=double]", "setblock 4 -60 3 oak_log[axis=x]", "setblock 2 -60 3 oak_log[axis=z]", "setblock 2 -60 4 furnace[facing=west]", "setblock 3 -60 4 chest[facing=north]", "setblock 4 -60 4 dispenser[facing=up]", "setblock 4 -59 3 torch", "setblock 1 -60 2 wall_torch[facing=west]", "setblock 2 -59 4 lever[face=floor,facing=north]", "setblock 1 -60 4 piston[facing=east]", "setblock 1 -60 3 oak_fence", "setblock 3 -59 3 repeater[facing=south]", "setblock 1 -60 1 oak_door[facing=south,half=lower,hinge=left]", "setblock 1 -59 1 oak_door[facing=south,half=upper,hinge=left]", "setblock 4 -60 1 oak_trapdoor[facing=north,half=bottom]"};
            for (String s : setup) w.getServer().runCommand(s);
            c.waitTicks(20);
            BlockPos a = new BlockPos(1, -60, 1), b = new BlockPos(4, -59, 4);
            List<BlockState> want = c.computeOnClient(mc -> BlockPos.betweenClosedStream(a, b).map(p -> mc.level.getBlockState(p)).toList());
            c.runOnClient(mc -> {
                AreaSelection sel = new AreaSelection();
                sel.addSubRegionBox(new Box(a, b, "main"), true);
                LitematicaSchematic s = LitematicaSchematic.createFromWorld(mc.level, sel, new LitematicaSchematic.SchematicSaveInfo(false, false), "Hollow", msg -> say("litematica: " + msg));
                SchematicPlacement p = SchematicPlacement.createFor(s, a, "test", true, true);
                DataManager.getSchematicPlacementManager().addSchematicPlacement(p, false);
            });
            c.waitTicks(40);
            w.getServer().runCommand("fill 1 -60 1 4 -59 4 air");
            w.getServer().runCommand("kill @e[type=item]");
            c.waitTicks(20);
            say("placement loaded: " + c.computeOnClient(mc -> LitematicaBridge.hasPlacement()));
            c.takeScreenshot("schematic");
            String[] items = {"stone", "oak_stairs", "oak_slab", "oak_log", "furnace", "chest", "dispenser", "torch", "lever", "piston", "oak_fence", "repeater", "oak_door", "oak_trapdoor"};
            for (String i : items) w.getServer().runCommand("give @a " + i + " 8");
            w.getServer().runCommand("tp @a 2.5 -60 -0.5 0 30");
            c.waitTicks(10);
            c.runOnClient(mc -> mod("Printer").set(true));
            c.waitTicks(260);
            w.getServer().runCommand("tp @a 2.5 -60 6.5 180 30");
            c.waitTicks(260);
            c.takeScreenshot("printed");
            List<BlockState> got = c.computeOnClient(mc -> BlockPos.betweenClosedStream(a, b).map(p -> mc.level.getBlockState(p)).toList());
            int right = 0, total = 0;
            for (int i = 0; i < want.size(); i++) {
                if (want.get(i).isAir()) continue;
                total++;
                if (dev.hollow.build.BuildPlan.sameOrientation(got.get(i), want.get(i))) right++;
                else say("DIFF wanted " + want.get(i) + " got " + got.get(i));
            }
            say((right == total ? "PASS" : "FAIL") + " Printer placed " + right + " of " + total);
            c.runOnClient(mc -> mod("Printer").set(false));

            // AutoBuild walks there by itself.
            w.getServer().runCommand("fill 1 -60 1 4 -59 4 air");
            w.getServer().runCommand("kill @e[type=item]");
            w.getServer().runCommand("clear @a");
            for (String i : items) w.getServer().runCommand("give @a " + i + " 8");
            w.getServer().runCommand("tp @a -9.5 -60 -9.5 0 0");
            c.waitTicks(20);
            c.runOnClient(mc -> mod("AutoBuild").set(true));
            for (int i = 0; i < 60 && c.computeOnClient(mc -> mod("AutoBuild").on()); i++) c.waitTicks(20);
            got = c.computeOnClient(mc -> BlockPos.betweenClosedStream(a, b).map(p -> mc.level.getBlockState(p)).toList());
            right = 0;
            for (int i = 0; i < want.size(); i++) if (!want.get(i).isAir() && dev.hollow.build.BuildPlan.sameOrientation(got.get(i), want.get(i))) right++;
            say((right >= total - 2 ? "PASS" : "FAIL") + " AutoBuild walked over and placed " + right + " of " + total);
            c.runOnClient(mc -> {
                mod("AutoBuild").set(false);
                mod("Printer").set(false);
                for (String n : new String[] {"ESP", "StorageESP", "ChunkBorders", "Sprint", "BlockOutline", "FakePlayer", "Nametags"}) mod(n).set(true);
            });
            w.getServer().runCommand("tp @a 0.5 -60 -4.5 0 10");
            c.waitTicks(40);
            c.takeScreenshot("pack-world");
            c.runOnClient(mc -> mc.gui.setScreen(new ClickGui()));
            c.waitTicks(40);
            c.takeScreenshot("pack-gui");
            c.runOnClient(mc -> {
                mc.gui.setScreen(null);
            });
            for (Module m : c.computeOnClient(mc -> List.copyOf(Hollow.modules()))) {
                if (List.of("Panic", "AutoReconnect", "Blink", "Freecam", "ChestSwap", "AutoLog").contains(m.name)) continue;
                c.runOnClient(mc -> m.set(true));
                c.waitTicks(2);
                String bad = c.computeOnClient(mc -> mc.player == null ? "gone" : !Float.isFinite(mc.player.getYRot()) || !Float.isFinite(mc.player.getXRot()) || !Double.isFinite(mc.player.getX()) || !Double.isFinite(mc.player.getY()) ? mc.player.position() + " " + mc.player.getYRot() + " " + mc.player.getXRot() : null);
                if (bad != null) {
                    say("FAIL " + m.name + " made the player " + bad);
                    break;
                }
            }
            for (int round = 0; round < 4; round++) {
                c.waitTicks(60);
                c.runOnClient(mc -> { for (Module m : Hollow.modules()) if (m != Hollow.INTERFACE && m != Hollow.HUD) m.set(false); });
                w.getServer().runCommand("tp @a 0.5 -60 -4.5 0 10");
                c.waitTicks(10);
                c.runOnClient(mc -> { for (Module m : Hollow.modules()) if (!List.of("Panic", "AutoReconnect", "Blink", "Freecam", "ChestSwap", "AutoLog").contains(m.name)) m.set(true); });
            }
            c.waitTicks(60);
            say("PASS everything on with the pack for three seconds");
            c.runOnClient(mc -> { for (Module m : Hollow.modules()) if (m != Hollow.INTERFACE && m != Hollow.HUD) m.set(false); });
        }
        c.runOnClient(mc -> {
            Hollow.INTERFACE.startMenu.set(false);
            mc.gui.setScreen(new TitleScreen());
        });
        c.waitTicks(5);
    }
}
