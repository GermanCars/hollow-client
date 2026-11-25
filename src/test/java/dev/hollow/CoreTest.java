package dev.hollow;

import static org.junit.jupiter.api.Assertions.*;

import dev.hollow.action.Holes;
import dev.hollow.action.Priority;
import dev.hollow.action.Request;
import dev.hollow.action.Rot;
import dev.hollow.core.Category;
import dev.hollow.core.Commands;
import dev.hollow.core.Config;
import dev.hollow.core.Friends;
import dev.hollow.core.Setting;
import dev.hollow.core.Colors;
import dev.hollow.ui.Ease;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CoreTest {
    @TempDir Path dir;

    static class Fake extends dev.hollow.core.Module {
        final Setting.Num range = num("Range", 4, 1, 6, 0.5);
        final Setting.Choice mode = choice("Mode", "A", "B");
        final Setting.Bool flag = bool("Flag", false);

        Fake(String n) { super(Category.COMBAT, n, ""); }
    }

    @Test void configRoundTrip() throws Exception {
        Fake a = new Fake("One"), b = new Fake("One");
        Friends f = new Friends(), g = new Friends();
        a.set(true);
        a.key = 21;
        a.range.set(5.5);
        a.mode.cycle(1);
        a.flag.set(true);
        f.add("Steve");
        Config c = new Config(dir.resolve("sub/h.json"));
        c.save(List.of(a), f);
        assertTrue(c.load(List.of(b), g));
        assertTrue(b.on());
        assertEquals(21, b.key);
        assertEquals(5.5, b.range.get());
        assertEquals("B", b.mode.get());
        assertTrue(b.flag.get());
        assertTrue(g.has("steve"));
    }

    @Test void brokenConfigIsKeptAside() throws Exception {
        Path p = dir.resolve("h.json");
        Files.writeString(p, "{ not json");
        assertFalse(new Config(p).load(List.of(new Fake("One")), new Friends()));
        assertFalse(Files.exists(p));
        try (var s = Files.list(dir)) {
            assertEquals(1, s.filter(x -> x.toString().contains(".broken-")).count());
        }
    }

    @Test void wrongTypesKeepDefaults() throws Exception {
        Path p = dir.resolve("h.json");
        Files.writeString(p, "{\"modules\":{\"One\":{\"on\":\"yes\",\"key\":[],\"settings\":{\"Range\":\"far\",\"Mode\":\"Z\",\"Flag\":3}}},\"friends\":5}");
        Fake a = new Fake("One");
        assertTrue(new Config(p).load(List.of(a), new Friends()));
        assertFalse(a.on());
        assertEquals(0, a.key);
        assertEquals(4, a.range.get());
        assertEquals("A", a.mode.get());
    }

    @Test void numbersSnapAndClamp() {
        Fake a = new Fake("x");
        a.range.set(99);
        assertEquals(6, a.range.get());
        a.range.set(3.2);
        assertEquals(3, a.range.get());
        a.range.set(Double.NaN);
        assertEquals(3, a.range.get());
    }

    @Test void commands() {
        Fake a = new Fake("KillAura");
        Friends f = new Friends();
        Commands c = new Commands(List.of(a), f, k -> k.equals("r") ? 21 : -1, w -> "wp");
        assertTrue(c.run("hello").isEmpty());
        assertTrue(c.run("...").isEmpty());
        assertTrue(c.run(".").isEmpty());
        assertEquals("KillAura on", c.run(".toggle killaura").orElseThrow());
        c.run(".bind killaura r");
        assertEquals(21, a.key);
        assertEquals("No key called zz", c.run(".bind killaura zz").orElseThrow());
        assertEquals(21, a.key);
        c.run(".friend add Alex");
        assertTrue(f.has("alex"));
        c.run(".friend remove ALEX");
        assertFalse(f.has("alex"));
        assertTrue(c.run(".nope").orElseThrow().startsWith("Unknown"));
    }

    static Request req(Priority p, Rot r, boolean exact) { return new Request(p, r, exact, -1, () -> {}); }

    @Test void highestPriorityOwnsTheView() {
        Request scaffold = req(Priority.SCAFFOLD, new Rot(0, 80), false), crystal = req(Priority.CRYSTAL, new Rot(90, 0), false);
        Request near = req(Priority.KILL_AURA, new Rot(95, 3), false), free = req(Priority.PRINTER, null, false);
        List<Request> got = Request.choose(List.of(scaffold, near, free, crystal), true);
        assertEquals(List.of(crystal, near, free), got);
        assertEquals(new Rot(90, 0), Request.view(got, true));
    }

    @Test void rotationsOffStillHonoursExact() {
        Request a = req(Priority.CRYSTAL, new Rot(90, 0), false), b = req(Priority.PRINTER, new Rot(0, 0), true), c = req(Priority.PRINTER, new Rot(180, 0), true);
        List<Request> got = Request.choose(List.of(a, b, c), false);
        assertEquals(List.of(a, b), got);
        assertEquals(new Rot(0, 0), Request.view(got, false));
    }

    @Test void angles() {
        assertEquals(2, new Rot(179, 0).angleTo(new Rot(-179, 0)), 1e-3);
        assertEquals(90, Rot.between(0, 0, 0, 0, -1, 0).pitch(), 1e-3);
        assertEquals(0, Rot.between(0, 0, 0, 0, 0, 1).yaw(), 1e-3);
        assertEquals(-90, Rot.between(0, 0, 0, 1, 0, 0).yaw(), 1e-3);
    }

    @Test void holes() {
        BlockPos feet = new BlockPos(0, 64, 0);
        Set<BlockPos> obby = new HashSet<>(List.of(feet.below(), feet.north(), feet.south(), feet.east(), feet.west()));
        assertTrue(Holes.isHole(feet, obby::contains, p -> !obby.contains(p)));
        obby.add(feet.above(2));
        assertFalse(Holes.isHole(feet, obby::contains, p -> !obby.contains(p)));
        obby.remove(feet.above(2));
        obby.remove(feet.west());
        assertFalse(Holes.isHole(feet, obby::contains, p -> !obby.contains(p)));
    }

    @Test void easing() {
        for (float t : new float[] {-1, 0}) {
            assertEquals(0, Ease.outCubic(t), 1e-5);
            assertEquals(0, Ease.outBack(t), 1e-5);
            assertEquals(0, Ease.inOut(t), 1e-5);
        }
        for (float t : new float[] {1, 2}) {
            assertEquals(1, Ease.outCubic(t), 1e-5);
            assertEquals(1, Ease.outBack(t), 1e-5);
            assertEquals(1, Ease.inOut(t), 1e-5);
        }
        float one = Ease.approach(0, 1, 8, 0.1f), two = Ease.approach(Ease.approach(0, 1, 8, 0.05f), 1, 8, 0.05f);
        assertEquals(one, two, 1e-5);
        assertEquals(0xFF00FF00, Colors.mix(0xFFFF0000, 0xFF00FF00, 1));
    }
}
