package dev.hollow.hud;

import dev.hollow.core.Setting;
import dev.hollow.ui.*;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** One label and one value. */
public final class InfoLine extends HudElement {
    private final Supplier<String> value;
    private final Setting.Color labelColor = color("Label colour", 0xFF8CF0CB), valueColor = fixedColor("Value colour", 0xFFEFF4F3);
    private final Setting.Bool showLabel = bool("Show label", true);
    private final String label;

    public InfoLine(String name, String label, String description, boolean on, int ax, int ay, double ox, double oy, Supplier<String> value) {
        super(name, description, on, ax, ay, ox, oy);
        this.value = value;
        this.label = label;
    }

    @Override protected void paint(GuiGraphicsExtractor g, boolean editing) {
        String v = mc.player == null ? "0" : value.get(), l = showLabel.get() ? label : "";
        float lw = l.isEmpty() ? 0 : Fonts.BOLD.width(l, 9) + 5;
        w = lw + Fonts.BOLD.width(v, 9) + 10;
        h = 14;
        text(g, l, 5, Fonts.BOLD.centre(0, h, 9), Theme.of(labelColor));
        text(g, v, 5 + lw, Fonts.BOLD.centre(0, h, 9), Theme.of(valueColor));
    }
}
