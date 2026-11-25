package dev.hollow.ui;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/** World positions to GUI positions. */
public final class Project {
    private static final Matrix4f M = new Matrix4f();

    private Project() {}

    /** Null when behind the camera. */
    public static float[] at(Vec3 p) {
        Minecraft mc = Minecraft.getInstance();
        Camera c = mc.gameRenderer.mainCamera();
        Vec3 d = p.subtract(c.position());
        Vector4f v = c.getViewRotationProjectionMatrix(M).transform(new Vector4f((float) d.x, (float) d.y, (float) d.z, 1));
        if (v.w <= .05f) return null;
        float w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
        return new float[] {(v.x / v.w * .5f + .5f) * w, (.5f - v.y / v.w * .5f) * h};
    }
}
