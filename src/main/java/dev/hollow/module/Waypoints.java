package dev.hollow.module;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import dev.hollow.Hollow;
import dev.hollow.core.Category;
import dev.hollow.core.Colors;
import dev.hollow.core.Module;
import dev.hollow.core.Setting;
import dev.hollow.ui.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Named places, kept per server. Added with .waypoint add <name>. */
public final class Waypoints extends Module {
    public record Point(String name, String server, String dimension, int x, int y, int z) {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Setting.Num size = num("Size", 1, .5, 2, .1), fade = num("Hide within", 6, 0, 50, 1);
    private final Setting.Bool convert = bool("Show across Nether and Overworld", true);
    private final List<Point> points = new ArrayList<>();
    private boolean loaded;

    public Waypoints() { super(Category.RENDER, "Waypoints", "Shows saved places. Type .waypoint add and a name to save one."); }

    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("hollow").resolve("waypoints.json"); }

    private List<Point> points() {
        if (!loaded) {
            loaded = true;
            try {
                if (Files.exists(file())) points.addAll(GSON.fromJson(Files.readString(file()), new TypeToken<List<Point>>() {}));
            } catch (IOException | JsonParseException e) {
                Hollow.LOG.error("Couldn't read waypoints.json", e);
            }
        }
        return points;
    }

    private void save() {
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), GSON.toJson(points));
        } catch (IOException e) {
            Hollow.LOG.error("Couldn't save waypoints.json", e);
        }
    }

    private static String server() { return mc.getCurrentServer() == null ? "singleplayer" : mc.getCurrentServer().ip; }

    private static String dimension() { return mc.level.dimension().identifier().toString(); }

    public List<Point> here() { return points().stream().filter(p -> p.server.equals(server())).toList(); }

    public void add(String name) {
        points().removeIf(p -> p.server.equals(server()) && p.name.equalsIgnoreCase(name));
        points().add(new Point(name, server(), dimension(), mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ()));
        save();
    }

    public boolean remove(String name) {
        boolean gone = points().removeIf(p -> p.server.equals(server()) && p.name.equalsIgnoreCase(name));
        if (gone) save();
        return gone;
    }

    @Override public void onOverlay(GuiGraphicsExtractor g, float partial) {
        String dim = dimension();
        for (Point p : here()) {
            double k = 1;
            if (!p.dimension.equals(dim)) {
                boolean toNether = dim.equals(Level.NETHER.identifier().toString()) && p.dimension.equals(Level.OVERWORLD.identifier().toString()), toOver = dim.equals(Level.OVERWORLD.identifier().toString()) && p.dimension.equals(Level.NETHER.identifier().toString());
                if (!convert.get() || !toNether && !toOver) continue;
                k = toNether ? .125 : 8;
            }
            Vec3 at = new Vec3(p.x * k + .5, k == 1 ? p.y + 1.2 : mc.player.getY() + 1, p.z * k + .5);
            double dist = at.distanceTo(mc.player.position());
            if (dist < fade.get()) continue;
            // Far ones are pulled in along the line of sight so they project cleanly.
            Vec3 eye = mc.gameRenderer.mainCamera().position();
            float[] s = Project.at(dist > 200 ? eye.add(at.subtract(eye).normalize().scale(200)) : at);
            if (s == null) continue;
            g.pose().pushMatrix();
            g.pose().translate(s[0], s[1]);
            g.pose().scale(size.getF());
            String d = dist >= 1000 ? String.format(java.util.Locale.ROOT, "%.1f km", dist / 1000) : Math.round(dist) + " m";
            float nw = Fonts.BOLD.width(p.name, 9), dw = Fonts.TEXT.width(d, 8.5f), w = nw + dw + 14;
            Draw dr = new Draw(g);
            dr.poly(Draw.diamond(0, 0, 5), Theme.accent());
            dr.poly(Draw.diamond(0, 0, 2.6f), Theme.ink());
            dr.poly(Draw.shard(-w / 2, 8, w, 13, 4), Colors.with(Theme.ink(), .75f));
            dr.flush();
            Fonts.BOLD.draw(g, p.name, -w / 2 + 5, Fonts.BOLD.centre(8, 13, 9), 9, Theme.text());
            Fonts.TEXT.draw(g, d, -w / 2 + 9 + nw, Fonts.TEXT.centre(8, 13, 8.5f), 8.5f, Theme.dim());
            g.pose().popMatrix();
        }
    }
}
