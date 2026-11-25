package dev.hollow.ui;

import dev.hollow.Hollow;
import dev.hollow.module.Interface;
import java.util.BitSet;
import java.util.function.IntFunction;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.util.RandomSource;

/** Hollow's letters where its fonts have them, the game's everywhere else, so no language or symbol turns into a box. */
public final class MixedGlyphs implements GlyphSource {
    /** First and last of each run of characters that all of Hollow's fonts have. */
    private static final int[] RUNS = {0x20, 0x7E, 0xA0, 0xB4, 0xB6, 0xFF, 0x102, 0x102, 0x131, 0x131, 0x152, 0x153, 0x2BB, 0x2BC, 0x2C6, 0x2C6, 0x2DA, 0x2DA, 0x2DC, 0x2DC, 0x300, 0x301, 0x303, 0x304, 0x308, 0x308, 0x323, 0x323, 0x2013, 0x2014, 0x2018, 0x201A, 0x201C, 0x201E, 0x2022, 0x2022, 0x2026, 0x2026, 0x2039, 0x203A, 0x2044, 0x2044, 0x20AC, 0x20AC, 0x2122, 0x2122, 0x2191, 0x2191, 0x2193, 0x2193, 0x2212, 0x2212, 0x2215, 0x2215};
    private static final BitSet OURS = new BitSet();

    static {
        for (int i = 0; i < RUNS.length; i += 2) OURS.set(RUNS[i], RUNS[i + 1] + 1);
    }

    private final GlyphSource[] labels = new GlyphSource[9];
    private final IntFunction<GlyphSource> label;
    private final GlyphSource own, vanilla;

    private MixedGlyphs(GlyphSource own, IntFunction<GlyphSource> label, GlyphSource vanilla) {
        this.own = own;
        this.label = label;
        this.vanilla = vanilla;
    }

    /** One of Hollow's fonts. */
    public static GlyphSource own(GlyphSource own, GlyphSource vanilla) { return new MixedGlyphs(own, null, vanilla); }

    /** The game's font, which follows the settings and the GUI scale. */
    public static GlyphSource game(IntFunction<GlyphSource> label, GlyphSource vanilla) { return new MixedGlyphs(null, label, vanilla); }

    private GlyphSource ours() {
        if (own != null) return own;
        if (Hollow.INTERFACE == null || !Interface.everywhere()) return vanilla;
        int s = Math.clamp(Math.round(Frame.scale), 1, 8);
        return labels[s] == null ? labels[s] = label.apply(s) : labels[s];
    }

    @Override public BakedGlyph getGlyph(int codepoint) {
        // Narrow no-break space, which dates use.
        if (codepoint == 0x202F) codepoint = 0xA0;
        return OURS.get(codepoint) ? ours().getGlyph(codepoint) : vanilla.getGlyph(codepoint);
    }

    @Override public BakedGlyph getRandomGlyph(RandomSource random, int width) { return vanilla.getRandomGlyph(random, width); }
}
