#version 330
#extension GL_ARB_separate_shader_objects : require

// The ice cavern behind the menus. An endless tunnel of chambers; each has a hole in its roof that lets in
// light and snow, and clusters of glowing crystals.

layout(std140) uniform SceneInfo {
    vec4 Params;   // time, travel speed, aspect, zoom
    vec4 CamPos;   // xyz, detail (0..1)
    vec4 Forward;
    vec4 Right;
    vec4 Up;
    vec4 Accent;
    vec4 Second;
} scene;

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

#define TIME scene.Params.x
const float CH = 9.0;
const float FLOOR = -1.3;

float hash1(float n) { return fract(sin(n * 127.1) * 43758.5453); }
float hash2(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }

// Keep in step with Cavern.java.
vec2 path(float z) { return vec2(sin(z * 0.21) * 0.9 + sin(z * 0.083) * 0.7, sin(z * 0.13) * 0.25); }

float gyroid(vec3 p) { return dot(sin(p), cos(p.zxy)); }

float smin(float a, float b, float k) {
    float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);
    return mix(b, a, h) - k * h * (1.0 - h);
}

// The hole in a chamber's roof. The first is on the right, over the player.
vec3 shaftAt(float c) {
    float z = (c + 0.5) * CH;
    vec2 o = path(z);
    return vec3(o.x + (c == 0.0 || hash1(c + 3.0) > 0.5 ? 1.25 : -1.25), o.y, z);
}

float crystals(vec3 p, float c, out float tip) {
    float d = 1e3;
    tip = 0.0;
    for (int i = 0; i < 5; i++) {
        float k = c * 7.0 + float(i);
        float side = hash1(k) > 0.5 ? 1.0 : -1.0;
        float z = (c + 0.12 + 0.76 * hash1(k + 1.7)) * CH;
        vec2 o = path(z);
        vec3 base = vec3(o.x + side * (1.75 + 0.65 * hash1(k + 2.9)), o.y + FLOOR - 0.1, z);
        vec3 q = p - base;
        float lean = side * (0.15 + 0.35 * hash1(k + 4.1));
        q.x += q.y * lean;
        q.z += q.y * (hash1(k + 5.3) - 0.5) * 0.6;
        float h = 0.55 + 0.95 * hash1(k + 6.1), w = 0.1 + 0.1 * hash1(k + 7.7);
        float taper = w * (1.0 - smoothstep(h * 0.55, h, q.y));
        float hex = max(abs(q.x) * 0.866 + abs(q.z) * 0.5, abs(q.z));
        float e = max(hex - taper, max(q.y - h, -q.y)) * 0.75;
        if (e < d) { d = e; tip = clamp(q.y / h, 0.0, 1.0); }
    }
    return d;
}

// Distance to the ice. Positive in the open.
float cave(vec3 p) {
    vec2 o = path(p.z);
    vec3 q = vec3(p.xy - o, p.z);
    float cz = (fract(p.z / CH) - 0.5) * CH;
    float wide = 2.15 + 1.35 * exp(-cz * cz * 0.16);
    float d = wide - length(q.xy * vec2(0.8, 1.0));
    float g1 = gyroid(p * vec3(1.25, 1.05, 1.0)), g2 = gyroid(p * 2.9 + 4.0), g3 = gyroid(p * 6.3 + 1.7);
    d += g1 * 0.22 + abs(g2) * 0.10 + abs(g3) * 0.028 * scene.CamPos.w;

    // Icicles.
    vec2 cell = floor(p.xz / 0.62);
    vec2 jit = vec2(hash2(cell), hash2(cell + 7.3)) - 0.5;
    vec2 f = p.xz - (cell + 0.5 + jit * 0.55) * 0.62;
    float len = hash2(cell + 2.1);
    len = len > 0.45 ? 0.35 + 1.25 * len * len : 0.0;
    float roof = o.y + wide - 0.1 - abs((cell.x + 0.5) * 0.62 - o.x) * 0.35;
    float cone = (length(f) - 0.11 * clamp((p.y - (roof - len)) / max(len, 0.01), 0.0, 1.0)) * 0.6;
    cone = max(cone, (roof - len) - p.y);
    if (len > 0.0) d = smin(d, cone, 0.16);

    // Light comes in through the roof.
    vec3 s = shaftAt(floor(p.z / CH));
    if (p.y > s.y + 0.3) d = max(d, 0.78 + 0.1 * g2 - length(p.xz - s.xz));

    float fl = p.y - o.y - FLOOR + 0.02 * gyroid(p * vec3(3.1, 1.0, 2.7)) + max(0.0, abs(q.x) - 1.5) * -0.22;
    return smin(d, fl, 0.28);
}

float map(vec3 p) {
    float t, c = floor(p.z / CH);
    return min(cave(p), crystals(p, c, t));
}

vec3 normalAt(vec3 p) {
    vec2 e = vec2(0.006, 0.0);
    return normalize(vec3(map(p + e.xyy) - map(p - e.xyy), map(p + e.yxy) - map(p - e.yxy), map(p + e.yyx) - map(p - e.yyx)));
}

float march(vec3 ro, vec3 rd, int steps, float far) {
    float t = 0.04;
    for (int i = 0; i < steps; i++) {
        float d = map(ro + rd * t);
        if (d < 0.0012 * (1.0 + t * 1.6)) return t;
        t += d * 0.82;
        if (t > far) break;
    }
    return far;
}

// Light hanging in the air: the shafts, and a glow around each crystal cluster.
vec3 air(vec3 ro, vec3 rd, float far, vec3 sky) {
    vec3 sum = vec3(0.0);
    float c0 = floor(ro.z / CH);
    for (int i = -1; i <= 3; i++) {
        float c = c0 + float(i);
        vec3 s = shaftAt(c);
        // Closest the ray comes to the shaft's axis.
        vec2 o = ro.xz - s.xz, d = rd.xz;
        float t = clamp(-dot(o, d) / max(dot(d, d), 1e-4), 0.0, far);
        vec3 p = ro + rd * t;
        float r = length(p.xz - s.xz);
        float up = smoothstep(s.y + FLOOR - 0.4, s.y + 3.2, p.y);
        float rays = 0.72 + 0.28 * sin(atan(p.z - s.z, p.x - s.x) * 5.0 + TIME * 0.22 + c);
        float inside = smoothstep(0.0, 1.4, far - t);
        sum += sky * exp(-r * r * 3.6) * (0.06 + 0.85 * up * up) * rays * exp(-t * 0.10) * mix(0.12, 1.0, inside);
        for (int j = 0; j < 2; j++) {
            float k = c * 7.0 + float(j * 2);
            float z = (c + 0.12 + 0.76 * hash1(k + 1.7)) * CH;
            vec2 q = path(z);
            vec3 l = vec3(q.x + (hash1(k) > 0.5 ? 1.0 : -1.0) * (1.75 + 0.65 * hash1(k + 2.9)), q.y + FLOOR + 0.45, z);
            float tl = clamp(dot(l - ro, rd), 0.0, far);
            float m = length(ro + rd * tl - l);
            float pulse = 0.85 + 0.15 * sin(TIME * 0.9 + k * 2.0);
            sum += mix(scene.Accent.rgb, scene.Second.rgb, float(j)) * 0.055 * pulse / (0.03 + m * m) * exp(-tl * 0.10) * 0.62;
        }
    }
    return sum;
}

vec3 shade(vec3 p, vec3 rd, vec3 n, float t, vec3 sky, vec3 deep) {
    float c = floor(p.z / CH), tip;
    float cr = crystals(p, c, tip);
    float fres = pow(1.0 - max(dot(n, -rd), 0.0), 3.0);
    vec3 crystalColor = mix(scene.Second.rgb, scene.Accent.rgb, tip);
    if (cr < 0.02) {
        float facet = 0.55 + 0.45 * abs(n.x * 0.7 + n.z * 0.5);
        return crystalColor * crystalColor * (0.75 + 1.7 * tip) * facet + vec3(1.0) * fres * 0.8 + crystalColor * 0.35;
    }
    float ao = clamp(map(p + n * 0.22) / 0.22, 0.0, 1.0) * 0.65 + clamp(map(p + n * 0.7) / 0.7, 0.0, 1.0) * 0.35;
    // Thin ice lets light through.
    float thin = clamp(1.0 + map(p - n * 0.35) / 0.35, 0.0, 1.0);
    // Ice swallows red first, so shade runs to blue.
    vec3 ice = mix(vec3(0.16, 0.5, 0.9), scene.Second.rgb, 0.45);
    vec3 col = deep * 1.1 + ice * (0.045 + 0.035 * n.y) + ice * 0.11 * thin;
    float bands = 0.5 + 0.5 * sin(p.y * 9.0 + gyroid(p * 1.6) * 2.4);
    col *= 0.86 + 0.2 * bands;

    for (int i = -1; i <= 1; i++) {
        vec3 s = shaftAt(c + float(i));
        vec3 l = vec3(s.x, s.y + 3.4, s.z) - p;
        float d2 = dot(l, l);
        l = normalize(l);
        float lit = max(dot(n, l), 0.0) * 0.75 + 0.25;
        col += mix(ice, sky, 0.35) * lit * lit * 6.2 / (2.2 + d2) * (0.2 + 0.8 * ao);
        vec3 h = normalize(l - rd);
        col += sky * pow(max(dot(n, h), 0.0), 70.0) * 9.0 / (2.0 + d2);
        // Where the shaft meets the floor.
        float spot = exp(-dot(p.xz - s.xz, p.xz - s.xz) * 1.5) * smoothstep(0.2, 0.9, n.y);
        col += sky * spot * 1.1;
        for (int j = 0; j < 3; j++) {
            float k = (c + float(i)) * 7.0 + float(j);
            float z = (c + float(i) + 0.12 + 0.76 * hash1(k + 1.7)) * CH;
            vec2 q = path(z);
            vec3 lp = vec3(q.x + (hash1(k) > 0.5 ? 1.0 : -1.0) * (1.75 + 0.65 * hash1(k + 2.9)), q.y + FLOOR + 0.4, z) - p;
            float e2 = dot(lp, lp);
            vec3 cc = mix(scene.Accent.rgb, scene.Second.rgb, hash1(k + 9.0));
            col += cc * (0.35 + 0.65 * max(dot(n, normalize(lp)), 0.0)) * 0.75 / (0.22 + e2 * 1.5);
        }
    }
    col *= 0.10 + 0.90 * ao;
    col += sky * fres * 0.22 * ao;
    // Glints.
    float g = hash2(floor(p.xz * 90.0) + floor(p.y * 90.0));
    col += sky * step(0.9965, g) * (0.5 + 0.5 * sin(TIME * 2.0 + g * 900.0)) * ao * 0.9 * exp(-t * 0.18);
    return col;
}

void main() {
    vec2 uv = (texCoord * 2.0 - 1.0) * vec2(scene.Params.z, 1.0);
    float speed = scene.Params.y;
    vec3 ro = scene.CamPos.xyz;
    vec3 rd = normalize(scene.Forward.xyz * scene.Params.w + scene.Right.xyz * uv.x + scene.Up.xyz * uv.y);

    vec3 sky = mix(vec3(0.56, 0.84, 1.0), scene.Accent.rgb, 0.4);
    vec3 deep = mix(vec3(0.004, 0.028, 0.085), scene.Second.rgb * scene.Second.rgb * 0.16, 0.5);
    float far = 34.0;

    float t = march(ro, rd, 96, far);
    vec3 col;
    vec3 fog = deep * 0.9 + scene.Second.rgb * 0.03;
    if (t < far) {
        vec3 p = ro + rd * t, n = normalAt(p);
        col = shade(p, rd, n, t, sky, deep);
        float floorness = smoothstep(0.72, 0.95, n.y) * smoothstep(0.5, 0.0, abs(p.y - path(p.z).y - FLOOR));
        if (floorness > 0.01 && scene.CamPos.w > 0.2) {
            vec3 rr = reflect(rd, normalize(n + vec3(0.0, 1.4, 0.0)));
            float rt = march(p + n * 0.03, rr, 40, 14.0);
            vec3 rc = fog;
            if (rt < 14.0) {
                vec3 rp = p + rr * rt;
                rc = mix(shade(rp, rr, normalAt(rp), rt, sky, deep), fog, 1.0 - exp(-rt * 0.1));
            }
            rc += air(p, rr, rt, sky) * 0.7;
            float k = 0.16 + 0.6 * pow(1.0 - max(dot(n, -rd), 0.0), 3.0);
            col = mix(col, rc, k * floorness);
        }
    } else col = fog;
    col = mix(col, fog, 1.0 - exp(-t * t * 0.0042));
    col += air(ro, rd, t, sky);

    // Rushing through: light streaks from the middle.
    if (speed > 0.002) {
        float a = atan(uv.y, uv.x), r = length(uv);
        float streak = pow(hash1(floor(a * 70.0)), 22.0) * smoothstep(0.15, 1.1, r);
        col += sky * streak * speed * 0.8;
        col = mix(col, scene.Second.rgb * 0.5, smoothstep(0.0, 1.0, speed) * 0.12);
    }

    col = col * 1.28 / (1.0 + col * 0.72);
    col = min(pow(max(col, 0.0), vec3(0.74)), vec3(1.0));
    col = mix(col, col * col * (3.0 - 2.0 * col), 0.45);
    col = clamp(mix(vec3(dot(col, vec3(0.3, 0.55, 0.15))), col, 1.22), 0.0, 1.0);
    float v = length(uv * vec2(0.56, 0.72));
    col *= 1.0 - 0.34 * smoothstep(0.45, 1.25, v);
    col += (hash2(gl_FragCoord.xy + fract(TIME) * 61.0) - 0.5) / 255.0 * 1.5;
    fragColor = vec4(col, 1.0);
}
