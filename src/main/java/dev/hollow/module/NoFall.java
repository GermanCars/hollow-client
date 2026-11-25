package dev.hollow.module;

import dev.hollow.core.Category;
import dev.hollow.core.Module;

/** Tells the server the player is on the ground while falling far enough to be hurt. */
public final class NoFall extends Module {
    public NoFall() { super(Category.MOVEMENT, "NoFall", "Prevents fall damage."); }

    public boolean spoof() { return on() && mc.player.fallDistance > 2.5 && !mc.player.isFallFlying(); }
}
