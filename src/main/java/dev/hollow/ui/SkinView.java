package dev.hollow.ui;

import dev.hollow.module.Interface;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

/** The player's own skin: breathes, watches the cursor, waves when clicked, turns when dragged. */
public final class SkinView {
    private final ModelPart wideRoot, slimRoot;
    private final Model.Simple wide, slim;
    private final Supplier<PlayerSkin> skin;
    private final Anim lookX = new Anim(), lookY = new Anim(), turn = new Anim(), wave = new Anim();
    private float dragged;
    private long waveUntil;

    public SkinView() {
        Minecraft mc = Minecraft.getInstance();
        wideRoot = mc.getEntityModels().bakeLayer(ModelLayers.PLAYER);
        slimRoot = mc.getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM);
        wide = new Model.Simple(wideRoot, RenderTypes::entityTranslucent);
        slim = new Model.Simple(slimRoot, RenderTypes::entityTranslucent);
        skin = mc.getSkinManager().createLookup(mc.getGameProfile(), false);
    }

    public void wave() { waveUntil = Util.getMillis() + 1900; }

    public void drag(double dx) { dragged += (float) dx * 1.6f; }

    /** Feet stand at (x, y). */
    public void draw(GuiGraphicsExtractor g, float x, float y, float tall, float mouseX, float mouseY) {
        PlayerSkin s = skin.get();
        boolean isSlim = s.model() == PlayerModelType.SLIM;
        boolean calm = Interface.calm();
        float headY = y - tall * .86f;
        lookX.glide(calm ? 0 : Math.clamp((mouseX - x) / (tall * 1.4f), -1, 1), 7);
        lookY.glide(calm ? 0 : Math.clamp((mouseY - headY) / (tall * 1.2f), -1, 1), 7);
        turn.glide(dragged, 8);
        wave.to(Util.getMillis() < waveUntil, .25f);
        pose(isSlim ? slimRoot : wideRoot, calm ? 0 : Frame.time);
        float scale = .97f * tall / 2.125f;
        int half = Math.round(tall * .55f);
        g.skin(isSlim ? slim : wide, s.body().texturePath(), scale, -3, -18 + turn.v + lookX.v * 14, -1.0625f, Math.round(x) - half, Math.round(y - tall), Math.round(x) + half, Math.round(y));
    }

    private void pose(ModelPart root, float t) {
        ModelPart head = root.getChild("head"), body = root.getChild("body"), ra = root.getChild("right_arm"), la = root.getChild("left_arm"), rl = root.getChild("right_leg"), ll = root.getChild("left_leg");
        head.yRot = -lookX.v * .45f;
        head.xRot = lookY.v * .07f;
        head.zRot = (float) Math.sin(t * .7) * .02f;
        float breath = (float) Math.sin(t * 1.7);
        body.yRot = (float) Math.sin(t * .45) * .03f;
        ra.zRot = .07f + breath * .035f;
        la.zRot = -.07f - breath * .035f;
        ra.xRot = (float) Math.sin(t * .9) * .05f;
        la.xRot = -(float) Math.sin(t * .9) * .05f;
        rl.zRot = .035f;
        ll.zRot = -.035f;
        rl.yRot = .1f;
        ll.yRot = -.1f;
        float w = Ease.inOut(wave.v);
        ra.zRot = Ease.lerp(ra.zRot, 2.5f + (float) Math.sin(t * 11) * .32f, w);
        ra.xRot = Ease.lerp(ra.xRot, -.15f, w);
    }
}
