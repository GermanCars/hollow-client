package dev.hollow.hud;

import dev.hollow.Hollow;
import dev.hollow.core.Colors;
import dev.hollow.ui.*;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Util;

/** Short pop-ups in the corner. */
public final class Notices {
    private static final class Notice {
        final String title, detail;
        final int color;
        long until;
        final Anim in = new Anim(), y = new Anim(Float.NaN);

        Notice(String title, String detail, int color, long life) {
            this.title = title;
            this.detail = detail;
            this.color = color;
            until = Util.getMillis() + life;
        }
    }

    private static final List<Notice> SHOWN = new ArrayList<>();

    private Notices() {}

    public static void add(String title, String detail, boolean good) {
        SHOWN.removeIf(n -> n.title.equals(title));
        SHOWN.add(new Notice(title, detail, good ? 0 : 0xFFFF9A7A, 2200));
        for (int i = 0; i < SHOWN.size() - 4; i++) SHOWN.get(i).until = 0;
    }

    public static void draw(GuiGraphicsExtractor g) {
        long now = Util.getMillis();
        float w = g.guiWidth(), slot = g.guiHeight() - 62;
        for (int i = SHOWN.size() - 1; i >= 0; i--) {
            Notice n = SHOWN.get(i);
            float t = n.in.to(now < n.until, .3f);
            if (t <= 0 && now >= n.until) {
                SHOWN.remove(i);
                continue;
            }
            if (Float.isNaN(n.y.v)) n.y.snap(slot);
            float y = n.y.glide(slot, 14), e = Ease.outQuint(t), bw = Math.max(100, Math.max(Fonts.BOLD.width(n.title, 9.5f), Fonts.TEXT.width(n.detail, 8.5f)) + 32), x = w - (bw + 6) * e;
            int c = n.color == 0 ? Theme.accent() : n.color;
            float[] s = Draw.shard(x, y, bw, 26, 8);
            Ice.panel(g, s, x, y, bw, 26, new Ice.Look(e, .75f, .7f * t, 1.1f, 9));
            Draw d = new Draw(g);
            Ice.glint(d, s, x, bw, y, 26, Ease.clamp(t * .9f), 1);
            d.circle(x + 13, y + 13, 7 * e, Colors.with(c, .18f * e));
            d.circle(x + 13, y + 13, 4 * Ease.outBack(t), Colors.alpha(c, e));
            // The light along the bottom runs out as the time does.
            float left = Math.clamp((n.until - now) / 2200f, 0, 1);
            d.poly(Draw.round(x + 8, y + 23, Math.max(1, (bw - 16) * left), 1.4f, .7f), Colors.alpha(c, e * .9f));
            d.flush();
            Fonts.BOLD.draw(g, n.title, x + 24, y + 11.5f, 9.5f, Colors.alpha(Theme.text(), e));
            Fonts.TEXT.draw(g, n.detail, x + 24, y + 21, 8.5f, Colors.alpha(Theme.dim(), e));
            slot -= 30;
        }
    }
}
