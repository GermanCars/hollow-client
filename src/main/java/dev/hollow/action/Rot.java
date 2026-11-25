package dev.hollow.action;

public record Rot(float yaw, float pitch) {
    public static Rot between(double ex, double ey, double ez, double tx, double ty, double tz) {
        double dx = tx - ex, dy = ty - ey, dz = tz - ez;
        return new Rot((float) Math.toDegrees(Math.atan2(-dx, dz)), (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz))));
    }

    public float angleTo(Rot o) {
        float y = Math.abs(yaw - o.yaw) % 360f;
        if (y > 180) y = 360 - y;
        return Math.max(y, Math.abs(pitch - o.pitch));
    }
}
