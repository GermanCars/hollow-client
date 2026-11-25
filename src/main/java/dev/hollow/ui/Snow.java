package dev.hollow.ui;

import dev.hollow.core.Colors;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Falling snow that can settle on banks and be shaken off. One per screen that snows. */
public final class Snow {
    private static final class Flake {
        float x, y, depth, radius, speed, phase, wobble;
        boolean extra;
    }

    private static final class Bit {
        float x, y, vx, vy, radius, life = 1;
    }

    private final Random random = new Random();
    private final List<Flake> flakes = new ArrayList<>();
    private final List<Bit> bits = new ArrayList<>();
    public final List<SnowBank> banks = new ArrayList<>();
    public float wind;
    private float width, height;

    public void shake(SnowBank b) {
        if (b.shaken) return;
        b.shaken = true;
        for (int i = 0; i < b.depth.length; i += 2) if (b.depth[i] > .15f) puff(b.x + i * SnowBank.STEP, b.y - b.depth[i], .5f + random.nextFloat() * .6f);
        b.clear();
    }

    private void puff(float x, float y, float r) {
        Bit p = new Bit();
        p.x = x;
        p.y = y;
        p.vx = (random.nextFloat() - .4f) * 44;
        p.vy = -random.nextFloat() * 40;
        p.radius = r;
        bits.add(p);
    }

    /** A little burst, for clicks. */
    public void burst(float x, float y, int count) { for (int i = 0; i < count; i++) puff(x + (random.nextFloat() - .5f) * 10, y, .5f + random.nextFloat() * .7f); }

    private Flake spawn(boolean anywhere) {
        Flake f = new Flake();
        f.depth = (float) Math.pow(random.nextFloat(), 1.7);
        f.x = random.nextFloat() * width * 1.2f - width * .1f;
        f.y = anywhere ? random.nextFloat() * height : -4;
        f.radius = .28f + f.depth * 1.05f;
        f.speed = 9 + f.depth * 27;
        f.phase = random.nextFloat() * 6.3f;
        f.wobble = .4f + random.nextFloat();
        return f;
    }

    /** @param amount 1 is normal, 0 is none */
    public void update(float w, float h, float amount) {
        if (w != width || h != height) {
            width = w;
            height = h;
            flakes.clear();
        }
        int want = Math.round(Math.min(460, w * h / 620) * amount);
        while (flakes.size() < want) flakes.add(spawn(true));
        while (flakes.size() > want + 40) flakes.removeLast();
        if (Frame.dt > 1) return;
        float dt = Frame.realDt, t = Frame.time;
        wind *= (float) Math.pow(.12, dt);
        float sway = (float) Math.sin(t * .23) * 4;
        if (amount > 0 && !banks.isEmpty() && random.nextFloat() < dt * 26 * amount) {
            SnowBank b = banks.get(random.nextInt(banks.size()));
            Flake f = spawn(false);
            f.depth = .8f;
            f.radius = .9f;
            f.speed = 32;
            f.x = b.x + random.nextFloat() * b.width;
            f.y = Math.max(-4, b.y - 30 - random.nextFloat() * 60);
            f.extra = true;
            flakes.add(f);
        }
        for (int i = flakes.size() - 1; i >= 0; i--) {
            Flake f = flakes.get(i);
            f.y += f.speed * dt;
            f.x += ((f.extra ? 0 : sway) * f.depth + (float) Math.sin(t * f.wobble + f.phase) * 4 * (.4f + f.depth) + wind * 26 * (.3f + f.depth)) * dt;
            boolean gone = f.y > h + 5 || f.x < -w * .15f || f.x > w * 1.15f;
            if (!gone && f.depth > .35f) for (SnowBank b : banks) if (b.land(f.x, f.y, 2.5f, f.radius * .34f)) { gone = true; break; }
            if (!gone) continue;
            if (f.extra) flakes.remove(i);
            else flakes.set(i, spawn(false));
        }
        for (int i = bits.size() - 1; i >= 0; i--) {
            Bit b = bits.get(i);
            b.vy += 190 * dt;
            b.x += b.vx * dt;
            b.y += b.vy * dt;
            b.life -= dt * 1.2f;
            if (b.life <= 0) bits.remove(i);
        }
        for (SnowBank b : banks) b.settle(dt);
    }

    /** Flakes nearer or further than the given depth, so things can be drawn in between. */
    public void draw(Draw d, float from, float to, float shiftX, float alpha) {
        for (Flake f : flakes) {
            if (f.depth < from || f.depth >= to) continue;
            int c = Colors.with(0xFFFFFF, (.22f + f.depth * .7f) * alpha);
            float x = f.x - shiftX * f.depth;
            if (f.radius < .6f) d.rect(x - f.radius, f.y - f.radius, f.radius * 2, f.radius * 2, c);
            else d.circle(x, f.y, f.radius, c);
        }
    }

    public void drawSettled(Draw d, float alpha) {
        for (SnowBank b : banks) b.draw(d, alpha);
        for (Bit b : bits) d.circle(b.x, b.y, b.radius, Colors.with(0xFFFFFF, b.life * alpha));
    }
}
