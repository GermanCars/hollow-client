package dev.hollow.module;

import dev.hollow.core.Category;
import dev.hollow.core.Module;

public final class Sprint extends Module {
    public Sprint() { super(Category.MOVEMENT, "Sprint", "Always sprints."); }

    @Override public void onMove() { if (mc.gui.screen() == null) mc.options.keySprint.setDown(true); }

    @Override protected void onDisable() { mc.options.keySprint.setDown(false); }
}
