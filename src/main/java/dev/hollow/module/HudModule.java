package dev.hollow.module;

import dev.hollow.core.Category;
import dev.hollow.core.Module;

public final class HudModule extends Module {
    public HudModule() {
        super(Category.RENDER, "HUD", "Shows the HUD. Arrange it on the HUD page.");
        hidden = true;
        set(true);
    }
}
