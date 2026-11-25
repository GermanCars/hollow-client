package dev.hollow.module;

import dev.hollow.core.Category;
import dev.hollow.core.Module;
import dev.hollow.core.Setting;
import net.minecraft.world.phys.Vec3;

/** Steer an open elytra with the movement keys. Jump climbs, sneak drops. */
public final class ElytraFly extends Module {
    private final Setting.Num speed = num("Speed", 1.6, .1, 3, .1), climb = num("Climb speed", .8, .1, 2, .1);

    public ElytraFly() { super(Category.MOVEMENT, "ElytraFly", "Steers an open elytra with the movement keys. Jump climbs, sneak drops."); }

    public Vec3 steer() {
        var in = mc.player.input.keyPresses;
        double f = (in.forward() ? 1 : 0) - (in.backward() ? 1 : 0), s = (in.left() ? 1 : 0) - (in.right() ? 1 : 0), len = Math.hypot(f, s);
        double y = in.jump() ? climb.get() : in.shift() ? -climb.get() : -.0001, yaw = Math.toRadians(mc.player.getYRot());
        if (len == 0) return new Vec3(0, y, 0);
        double k = speed.get() / len, sin = Math.sin(yaw), cos = Math.cos(yaw);
        return new Vec3((s * cos - f * sin) * k, y, (f * cos + s * sin) * k);
    }

    @Override public String info() { return speed.text(); }
}
