package dev.hollow.render;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import dev.hollow.module.Interface;
import dev.hollow.ui.Ease;
import dev.hollow.ui.Frame;
import dev.hollow.ui.Theme;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import org.joml.Vector3f;

/** The ice cavern and the camera that flies through it. */
public final class Cavern {
    private static final float CH = 9, FLOOR = -1.3f, ZOOM = 1.25f;
    private static final int SIZE = new Std140SizeCalculator().putVec4().putVec4().putVec4().putVec4().putVec4().putVec4().putVec4().get();

    private static MappableRingBuffer uniforms;
    private static TextureTarget target;
    private static boolean wanted, drew;
    private static float from, to, flight = 1, speed, mx, my, zoomKick;

    private Cavern() {}

    /** Fly to a chamber. */
    public static void go(float chamber) {
        if (chamber == to) return;
        from = travel();
        to = chamber;
        flight = Interface.calm() ? 1 : 0;
    }

    /** The menus sit at fixed chambers; more distant screens are further in. */
    public static boolean flying() { return flight < 1; }

    private static float travel() {
        float t = Ease.clamp(flight);
        // Slow, fast, slow, with a long settle.
        float e = t < .5f ? 8 * t * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 4) / 2;
        return from + (to - from) * e;
    }

    private static float pathX(float z) { return (float) (Math.sin(z * .21) * .9 + Math.sin(z * .083) * .7); }

    private static float pathY(float z) { return (float) Math.sin(z * .13) * .25f; }

    private static final Vector3f pos = new Vector3f(), fwd = new Vector3f(), right = new Vector3f(), up = new Vector3f();

    /** Moves the camera on. Once per frame that shows the cavern. */
    public static void update() {
        if (wanted) return;
        wanted = true;
        Minecraft mc = Minecraft.getInstance();
        var w = mc.getWindow();
        boolean calm = Interface.calm();
        float tx = calm ? 0 : Math.clamp((float) mc.mouseHandler.getScaledXPos(w) / Math.max(1, w.getGuiScaledWidth()) - .5f, -.5f, .5f), ty = calm ? 0 : Math.clamp((float) mc.mouseHandler.getScaledYPos(w) / Math.max(1, w.getGuiScaledHeight()) - .5f, -.5f, .5f);
        mx = Ease.approach(mx, tx, 4, Frame.realDt);
        my = Ease.approach(my, ty, 4, Frame.realDt);
        float before = travel();
        flight = Math.min(1, flight + Frame.realDt * Interface.speed() / (1.15f + .16f * Math.min(5, Math.abs(to - from))));
        float z = travel() * CH + .9f, moved = Frame.realDt > 0 ? Math.abs(travel() - before) * CH / Frame.realDt : 0;
        speed = Ease.approach(speed, Ease.clamp(moved / 26), 9, Frame.realDt);
        zoomKick = Ease.approach(zoomKick, speed, 7, Frame.realDt);
        float bob = calm ? 0 : (float) Math.sin(Frame.time * .5) * .035f;
        pos.set(pathX(z) + mx * .42f, pathY(z) - .28f - my * .2f + bob, z);
        fwd.set(pathX(z + 3.2f) + mx * .16f, pathY(z + 3.2f) - .18f + my * .12f, z + 3.2f).sub(pos).normalize();
        float roll = (pathX(z + 2) - pathX(z)) * .05f * (1 + speed * 3) + (calm ? 0 : (float) Math.sin(Frame.time * .23) * .006f);
        right.set((float) Math.sin(roll), (float) Math.cos(roll), 0).cross(fwd).normalize();
        up.set(fwd).cross(right);
    }

    /** Where a spot on the first chamber's floor appears, in GUI units, with the size of one block there. Null out of view. */
    public static float[] floorSpot(float guiW, float guiH, float dx, float dz) {
        float z = .9f + dz, x = pathX(z) + dx, y = pathY(z) + FLOOR;
        Vector3f d = new Vector3f(x, y, z).sub(pos);
        float depth = d.dot(fwd);
        if (depth < .2f) return null;
        float k = zoom() / depth;
        return new float[] {guiW / 2 + d.dot(right) * k * guiH / 2, guiH / 2 - d.dot(up) * k * guiH / 2, k * guiH / 2};
    }

    private static float zoom() { return ZOOM * (1 - .3f * zoomKick); }

    public static boolean available() { return drew || RenderSystem.getCompiledPipelineNullable(Pipelines.CAVERN) != null && RenderSystem.getCompiledPipelineNullable(Pipelines.COPY) != null; }

    /** Render thread: draws into the main target, before any interface. */
    public static void render() {
        if (!wanted) return;
        wanted = false;
        CompiledRenderPipeline p = RenderSystem.getCompiledPipelineNullable(Pipelines.CAVERN);
        drew = p != null;
        if (p == null) return;
        Minecraft mc = Minecraft.getInstance();
        var main = mc.gameRenderer.mainRenderTarget();
        float q = Interface.sceneQuality();
        int w = Math.max(64, Math.round(main.width * q)), h = Math.max(36, Math.round(main.height * q));
        if (target == null || target.width != w || target.height != h) {
            if (target != null) target.destroyBuffers();
            target = Glass.target("cavern", w, h);
        }
        if (uniforms == null) uniforms = new MappableRingBuffer(() -> "Hollow cavern", 130, SIZE);
        try (GpuBufferSlice.MappedView v = uniforms.currentBuffer().map(false, true)) {
            Std140Builder.intoBuffer(v.data())
                .putVec4(Frame.time, speed, w / (float) h, zoom())
                .putVec4(pos.x, pos.y, pos.z, q >= .7f ? 1 : q >= .45f ? .6f : .1f)
                .putVec4(fwd.x, fwd.y, fwd.z, 0).putVec4(right.x, right.y, right.z, 0).putVec4(up.x, up.y, up.z, 0)
                .putVec4(r(Theme.accent()), g(Theme.accent()), b(Theme.accent()), 1).putVec4(r(Theme.second()), g(Theme.second()), b(Theme.second()), 1);
        }
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Hollow cavern", target.getColorTextureView(), Optional.empty())) {
            pass.setPipeline(p);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("SceneInfo", uniforms.currentBuffer());
            pass.draw(3, 1, 0, 0);
        }
        uniforms.rotate();
        Glass.pass(Pipelines.COPY, target.getColorTextureView(), main.getColorTextureView());
    }

    private static float r(int c) { return (c >> 16 & 255) / 255f; }

    private static float g(int c) { return (c >> 8 & 255) / 255f; }

    private static float b(int c) { return (c & 255) / 255f; }
}
