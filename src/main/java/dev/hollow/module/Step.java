package dev.hollow.module;

import dev.hollow.core.Category;
import dev.hollow.core.Module;
import dev.hollow.core.Setting;

public final class Step extends Module {
    private final Setting.Num height = num("Height", 1, 1, 2.5, .5);

    public Step() { super(Category.MOVEMENT, "Step", "Walks straight up full blocks."); }

    public float raise(float normal) { return on() && mc.player.onGround() && !mc.player.isShiftKeyDown() ? Math.max(normal, (float) height.get()) : normal; }

    @Override public String info() { return height.text(); }
}
