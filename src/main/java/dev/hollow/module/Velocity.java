package dev.hollow.module;

import dev.hollow.core.Category;
import dev.hollow.core.Module;
import dev.hollow.core.Setting;
import net.minecraft.world.phys.Vec3;

public final class Velocity extends Module {
    private final Setting.Num horizontal = num("Horizontal %", 0, 0, 100, 5), vertical = num("Vertical %", 0, 0, 100, 5);

    public Velocity() { super(Category.COMBAT, "Velocity", "Reduces knockback from hits and explosions."); }

    /** What to move with instead of the knockback the server sent. */
    public Vec3 hit(Vec3 current, Vec3 sent) { return current.add(scale(sent.subtract(current))); }

    public Vec3 scale(Vec3 push) { return push.multiply(horizontal.get() / 100, vertical.get() / 100, horizontal.get() / 100); }

    @Override public String info() { return horizontal.text() + "% " + vertical.text() + "%"; }
}
