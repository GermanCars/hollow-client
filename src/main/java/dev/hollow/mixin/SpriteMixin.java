package dev.hollow.mixin;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dev.hollow.Hollow;
import dev.hollow.core.Colors;
import dev.hollow.module.Interface;
import dev.hollow.ui.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tabs, pop-ups, slots and the hotbar, which the game draws from sprites. */
@Mixin(GuiGraphicsExtractor.class)
abstract class SpriteMixin {
    @Unique private static final Anim HOLLOW_SLOT = new Anim(Float.NaN);

    @Inject(method = "blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V", at = @At("HEAD"), cancellable = true)
    private void hollow$sprite(RenderPipeline p, Identifier id, int x, int y, int w, int h, CallbackInfo ci) {
        String s = id.getPath();
        GuiGraphicsExtractor g = (GuiGraphicsExtractor) (Object) this;
        if (s.startsWith("hud/hotbar")) {
            if (Hollow.INTERFACE.hotbar.get() && hollow$hotbar(g, s, x, y, w, h)) ci.cancel();
            return;
        }
        if (s.startsWith("container/inventory/effect_background") || s.startsWith("recipe_book/button")) {
            if (!Interface.glassContainers()) return;
            boolean book = s.startsWith("recipe");
            Style.plate(g, x, y, w, h, 1);
            if (book) {
                // An open book: two leaves and a spine.
                int c = Colors.with(s.contains("highlighted") ? 0xFFFFFF : Theme.accent(), .95f);
                float mx = x + w / 2f, my = y + h / 2f;
                Draw d = new Draw(g);
                d.poly(new float[] {mx - .5f, my - 3.4f, mx - .5f, my + 4.6f, mx - 6, my + 3.2f, mx - 6, my - 4.8f}, c);
                d.poly(new float[] {mx + .5f, my - 3.4f, mx + 6, my - 4.8f, mx + 6, my + 3.2f, mx + .5f, my + 4.6f}, Colors.alpha(c, .72f));
                d.flush();
            }
            ci.cancel();
            return;
        }
        if (s.startsWith("container/slot")) {
            // The highlight is one picture behind the item and one in front; ours needs only the first.
            if (!Interface.glassContainers() || !s.contains("highlight")) return;
            if (s.contains("back")) {
                Draw d = new Draw(g);
                float[] slot = Draw.round(x + 3.5f, y + 3.5f, 17, 17, 3);
                d.poly(slot, Colors.with(Theme.accent(), .34f));
                d.stroke(Draw.offset(slot, -.4f), .9f, Colors.with(0xFFFFFF, .75f));
                d.flush();
            }
            ci.cancel();
            return;
        }
        if (!s.startsWith("widget/tab") && !s.startsWith("toast/") || !Interface.styled()) return;
        boolean tab = s.startsWith("widget/tab"), selected = s.contains("selected"), lit = s.contains("highlighted");
        if (!tab && !s.equals("toast/advancement") && !s.equals("toast/system") && !s.equals("toast/now_playing") && !s.equals("toast/recipe")) return;
        if (tab) {
            float top = selected ? y : y + 3;
            float[] shape = Draw.shard(x + 1, top, w - 2, y + h - top, 7);
            Ice.panel(g, shape, x + 1, top, w - 2, y + h - top, new Ice.Look(1, selected ? .35f : .75f, selected ? .5f : .2f, selected || lit ? 1.2f : .6f, 0));
            if (selected) {
                Draw d = new Draw(g);
                d.poly(shape, Theme.fill(x, x + w, .30f));
                d.flush();
            }
        } else {
            float[] shape = Draw.shard(x + 2, y + 2, w - 4, h - 4, 8);
            Ice.panel(g, shape, x + 2, y + 2, w - 4, h - 4, new Ice.Look(1, .8f, .8f, 1.1f, 8));
        }
        ci.cancel();
    }

    @Unique
    private static boolean hollow$hotbar(GuiGraphicsExtractor g, String s, int x, int y, int w, int h) {
        switch (s) {
            case "hud/hotbar" -> {
                Style.plate(g, x, y, w, h, 1);
                Draw d = new Draw(g);
                for (int i = 0; i < 9; i++) d.poly(Draw.round(x + 2.5f + i * 20, y + 2.5f, 17, 17, 3.5f), Colors.with(0x000000, .20f));
                d.flush();
            }
            case "hud/hotbar_selection" -> {
                if (Float.isNaN(HOLLOW_SLOT.v) || Math.abs(HOLLOW_SLOT.v - x) > 260) HOLLOW_SLOT.snap(x);
                float sx = HOLLOW_SLOT.spring(x, 620, 36);
                float[] box = Draw.round(sx + 2.5f, y + 2.5f, 19, 19, 4.5f);
                Draw d = new Draw(g);
                d.shadow(box, 8, Colors.with(Theme.accent(), .45f));
                d.poly(box, (a, b) -> Colors.alpha(Theme.at(Ease.clamp((b - y) / h)), .34f));
                Ice.rim(d, box, 1, 1.6f);
                d.flush();
            }
            case "hud/hotbar_offhand_left" -> Style.plate(g, x, y + 1, 22, 22, 1);
            case "hud/hotbar_offhand_right" -> Style.plate(g, x + 7, y + 1, 22, 22, 1);
            default -> {
                return false;
            }
        }
        return true;
    }
}
