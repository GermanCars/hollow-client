package dev.hollow.ui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hollow.core.Colors;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.util.Util;

/** One line of text being typed. */
public final class TextField {
    public String text = "";
    public boolean focused;
    private final String hint;
    private final int max;
    private final Consumer<String> changed;
    private final Anim focus = new Anim(), caret = new Anim();
    private long typedAt;

    public TextField(String hint, int max, Consumer<String> changed) {
        this.hint = hint;
        this.max = max;
        this.changed = changed;
    }

    public void set(String s) {
        text = s;
        changed.accept(s);
    }

    public void draw(GuiGraphicsExtractor g, float x, float y, float w, float h, float size, float alpha) {
        Style.field(g, x, y, w, h, focus.to(focused, .25f), alpha);
        float base = Fonts.TEXT.centre(y, h, size), room = w - 14;
        String shown = text;
        while (Fonts.TEXT.width(shown, size) > room) shown = shown.substring(1);
        float tw = Fonts.TEXT.width(shown, size);
        if (text.isEmpty()) Fonts.TEXT.draw(g, hint, x + 7, base, size, Colors.alpha(Theme.dim(), alpha * .75f));
        Fonts.TEXT.draw(g, shown, x + 7, base, size, Colors.alpha(Theme.text(), alpha));
        caret.glide(tw, 30);
        if (focused && (Util.getMillis() - typedAt) % 1000 < 560) {
            Draw d = new Draw(g);
            d.rect(x + 7.5f + caret.v, y + h * .24f, 1, h * .52f, Colors.alpha(Theme.accent(), alpha));
            d.flush();
        }
    }

    public boolean type(String s) {
        if (!focused) return false;
        for (char c : s.toCharArray()) if (c >= ' ' && c != 127 && text.length() < max) text += c;
        typedAt = Util.getMillis();
        changed.accept(text);
        return true;
    }

    public boolean key(KeyEvent e) {
        if (!focused) return false;
        typedAt = Util.getMillis();
        boolean ctrl = e.hasControlDownWithQuirk();
        if (e.key() == InputConstants.KEY_BACKSPACE && !text.isEmpty()) {
            set(ctrl ? text.substring(0, Math.max(0, text.stripTrailing().lastIndexOf(' ') + 1)) : text.substring(0, text.length() - 1));
            return true;
        }
        if (ctrl && e.key() == InputConstants.KEY_V) {
            type(Minecraft.getInstance().keyboardHandler.getClipboard());
            return true;
        }
        return e.key() == InputConstants.KEY_BACKSPACE;
    }
}
