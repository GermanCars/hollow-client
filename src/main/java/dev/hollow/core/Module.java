package dev.hollow.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;

public abstract class Module {
    /** Null in unit tests. */
    protected static final Minecraft mc = Minecraft.getInstance();
    /** Told about every switch, for notifications. */
    public static java.util.function.Consumer<Module> listener = m -> {};

    public final String name, description;
    public final Category category;
    public final List<Setting> settings = new ArrayList<>();
    /** Key code from InputConstants, or 0 when unbound. */
    public int key;
    /** On only while the key is held. */
    public boolean hold;
    /** Left out of the HUD's module list. */
    public boolean hidden;
    private boolean enabled;
    private String section = "";

    protected Module(Category category, String name, String description) {
        this.category = category;
        this.name = name;
        this.description = description;
    }

    protected final void section(String title) { section = title; }

    protected final Setting.Bool bool(String n, boolean v) { return add(new Setting.Bool(n, v)); }

    protected final Setting.Num num(String n, double v, double min, double max, double step) { return add(new Setting.Num(n, v, min, max, step)); }

    protected final Setting.Choice choice(String n, String... o) { return add(new Setting.Choice(n, o)); }

    /** Follows the theme until the user picks their own. */
    protected final Setting.Color color(String n, int argb) { return add(new Setting.Color(n, argb, true)); }

    protected final Setting.Color fixedColor(String n, int argb) { return add(new Setting.Color(n, argb, false)); }

    protected final Setting.Text text(String n, String v) { return add(new Setting.Text(n, v)); }

    protected final Setting.Ids ids(String n, Setting.Ids.Kind kind, String... v) { return add(new Setting.Ids(n, kind, v)); }

    protected final <T extends Setting> T when(T s, BooleanSupplier visible) {
        s.showWhen(visible);
        return s;
    }

    private <T extends Setting> T add(T s) {
        s.section = section;
        settings.add(s);
        return s;
    }

    public final boolean on() { return enabled; }

    public final void toggle() { set(!enabled); }

    public final void set(boolean value) {
        if (enabled == value) return;
        enabled = value;
        listener.accept(this);
        if (mc == null || mc.player == null || mc.level == null) return;
        if (value) onEnable();
        else onDisable();
    }

    /** Restores the saved state without running the enable hook; there is no world yet. */
    protected final void restore(boolean value) { enabled = value; }

    /** Only called with a world loaded. */
    protected void onEnable() {}

    /** Only called with a world loaded. */
    protected void onDisable() {}

    /** A world was joined while this was on. */
    public void onWorldJoined() {}

    /** The world was left while this was on. Drop anything remembered about it. */
    public void onWorldLeft() {}

    /** Before the player moves. */
    public void onMove() {}

    /** After the player moved, before the position is sent. Submit actions here. */
    public void onTick() {}

    /** Per frame, with gizmos available. */
    public void onWorldRender(float partial) {}

    /** Per frame, drawn flat over the world and under the HUD. */
    public void onOverlay(net.minecraft.client.gui.GuiGraphicsExtractor g, float partial) {}

    /** A packet from the server, on the network thread. Return true to drop it. */
    public boolean onReceive(Packet<?> packet) { return false; }

    /** A packet about to be sent. Return true to drop it. */
    public boolean onSend(Packet<?> packet) { return false; }

    /** Short text shown next to the name on the HUD. */
    public String info() { return ""; }
}
