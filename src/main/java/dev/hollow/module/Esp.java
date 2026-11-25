package dev.hollow.module;

import dev.hollow.action.Damage;
import dev.hollow.core.Category;
import dev.hollow.core.Colors;
import dev.hollow.core.Module;
import dev.hollow.core.Setting;
import dev.hollow.ui.Draw;
import dev.hollow.ui.Ease;
import dev.hollow.ui.Project;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class Esp extends Module {
    private final Setting.Choice style = choice("Style", "Box", "Corners", "Frame");
    private final Setting.Bool players = bool("Players", true), monsters = bool("Monsters", false), animals = bool("Animals", false), crystals = bool("Crystals", true), health = bool("Health bar", true);
    private final Setting.Num width = num("Line width", 2, .5, 5, .5), fill = num("Fill", .15, 0, .6, .05);

    public Esp() { super(Category.RENDER, "ESP", "Draws boxes around players and more, through walls."); }

    private boolean wants(Entity e) {
        if (e == mc.player) return false;
        if (e instanceof Player) return players.get();
        if (e instanceof EndCrystal) return crystals.get();
        return e instanceof LivingEntity && (e instanceof Enemy ? monsters.get() : animals.get());
    }

    private static int color(Entity e) { return e instanceof EndCrystal ? 0xFFD9A8FF : RenderModules.entityColor(e); }

    @Override public void onWorldRender(float partial) {
        if (!style.is("Box")) return;
        for (Entity e : mc.level.entitiesForRendering()) if (wants(e)) Gizmos.cuboid(e.getInterpolatedBoundingBox(partial), GizmoStyle.strokeAndFill(Colors.with(color(e), .9f), width.getF(), Colors.with(color(e), fill.getF()))).setAlwaysOnTop();
    }

    @Override public void onOverlay(GuiGraphicsExtractor g, float partial) {
        if (style.is("Box") && !health.get()) return;
        Draw d = new Draw(g);
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!wants(e)) continue;
            AABB b = e.getInterpolatedBoundingBox(partial);
            float x0 = Float.MAX_VALUE, y0 = x0, x1 = -x0, y1 = -x0;
            boolean seen = true;
            for (int i = 0; i < 8 && seen; i++) {
                float[] s = Project.at(new Vec3((i & 1) == 0 ? b.minX : b.maxX, (i & 2) == 0 ? b.minY : b.maxY, (i & 4) == 0 ? b.minZ : b.maxZ));
                if (s == null) seen = false;
                else {
                    x0 = Math.min(x0, s[0]);
                    x1 = Math.max(x1, s[0]);
                    y0 = Math.min(y0, s[1]);
                    y1 = Math.max(y1, s[1]);
                }
            }
            if (!seen || x1 < 0 || x0 > g.guiWidth() || y1 < 0 || y0 > g.guiHeight()) continue;
            int c = color(e);
            float w = x1 - x0, h = y1 - y0, t = width.getF() * .5f;
            if (style.is("Frame")) d.stroke(Draw.shard(x0, y0, w, h, Math.min(w, h) * .18f), t, c);
            if (style.is("Corners")) {
                float k = Math.min(w, h) * .28f;
                for (int sx = 0; sx < 2; sx++) for (int sy = 0; sy < 2; sy++) {
                    float cx = sx == 0 ? x0 : x1, cy = sy == 0 ? y0 : y1, ux = sx == 0 ? k : -k, uy = sy == 0 ? k : -k;
                    d.line(cx, cy, cx + ux, cy, t, c);
                    d.line(cx, cy, cx, cy + uy, t, c);
                }
            }
            if (health.get() && e instanceof LivingEntity l) {
                float v = Ease.clamp(Damage.health(l) / Math.max(1, l.getMaxHealth())), bw = Math.clamp(w * .05f, .8f, 2);
                d.rect(x0 - bw * 2.2f, y0, bw, h, 0x90000000);
                d.rect(x0 - bw * 2.2f, y0 + h * (1 - v), bw, h * v, Colors.health(v));
            }
        }
        d.flush();
    }
}
