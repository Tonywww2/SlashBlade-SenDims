#version 150

// 符文光幕的画布烘焙：u 绕圆筒一圈（可平铺），v 从底到顶。输出预乘颜色，供叠加材质使用。
uniform float Time;
uniform float Progress;
uniform float Phase;      // 0 蓄能 1 释放 2 中断
uniform float PhaseTime;
uniform float Style;
uniform float Glow;
uniform float Intensity;
uniform float Detail;
uniform vec3 ColorA;
uniform vec3 ColorB;

in vec2 texCoord;
out vec4 fragColor;

float hash1(float n) { return fract(sin(n * 127.1) * 43758.5453); }
float hash2(vec2 p) { return fract(sin(dot(p, vec2(41.3, 289.1))) * 43758.5453); }
float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash2(i), hash2(i + vec2(1.0, 0.0)), f.x), mix(hash2(i + vec2(0.0, 1.0)), hash2(i + vec2(1.0, 1.0)), f.x), f.y);
}
float segD(vec2 p, vec2 a, vec2 b) {
    vec2 pa = p - a;
    vec2 ba = b - a;
    float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
    return length(pa - ba * h);
}
float glyph(vec2 q, float seed, int style) {
    float d = 1e3;
    for (int i = 0; i < 5; i++) {
        float h = hash1(seed * 13.7 + float(i) * 3.1);
        if (i >= 3 && h < 0.45) continue;
        vec2 a = vec2(floor(hash1(seed + float(i) * 1.7) * 3.0), floor(hash1(seed * 2.3 + float(i)) * 4.0));
        float dir = floor(h * 4.0);
        vec2 b = a + (dir < 1.0 ? vec2(1.0, 0.0) : dir < 2.0 ? vec2(0.0, 1.0) : dir < 3.0 ? vec2(1.0, 1.0) : vec2(-1.0, 1.0));
        if (style == 2) b = a + (dir < 2.0 ? vec2(1.0, 1.0) : vec2(-1.0, 1.0));
        b = clamp(b, vec2(0.0), vec2(2.0, 3.0));
        vec2 A = (a - vec2(1.0, 1.5)) * vec2(0.30, 0.27);
        vec2 B = (b - vec2(1.0, 1.5)) * vec2(0.30, 0.27);
        if (style == 3) d = min(d, length(q - A) - 0.035);
        else d = min(d, segD(q, A, B));
    }
    return d;
}

void main() {
    float u = texCoord.x;
    float v = texCoord.y;
    int s = int(Style + 0.5);
    int phase = int(Phase + 0.5);
    float t = Time;
    float rows = 3.0;
    float cells = 24.0;

    float glyphs = 0.0;
    if (Detail > 0.5) {
        float vv = v * rows - t * 0.015;
        float row = floor(vv);
        float uu = u * cells + row * 0.5;
        float cell = floor(uu);
        vec2 local = vec2(fract(uu), fract(vv));
        vec2 q = vec2((local.x - 0.5) * 0.78, (local.y - 0.5) * 1.05);
        float id = mod(cell, cells) + row * 57.0;
        float show = step(hash1(id * 3.3), Progress * 0.8 - 0.1) * (1.0 - smoothstep(0.55, 0.95, v));
        float flick = 0.7 + 0.3 * sin(t * 0.25 + id * 1.7);
        float d = glyph(q, id + float(s) * 31.0, s);
        glyphs = (smoothstep(0.055, 0.02, d) * 1.4 + exp(-d * 14.0) * 0.5) * show * flick;
    }

    // 竖直光纹：向上流动的帘状光
    float streak = pow(noise(vec2(u * 64.0, v * 3.0 - t * 0.04)), 3.0) * 0.9;
    if (s == 2) streak = pow(noise(vec2(u * 48.0, v * 5.0 - t * 0.09)), 2.0) * 1.2;
    if (s == 5) {
        float c = abs(sin(noise(vec2(u * 30.0, v * 4.0 + t * 0.02)) * 9.0));
        streak = smoothstep(0.85, 1.0, c) * 0.9;
    }
    float bottom = exp(-v * 10.0) * 0.8;
    float topFade = (s == 1) ? smoothstep(1.0, 0.45 + 0.25 * noise(vec2(u * 12.0, t * 0.02)), v) : pow(1.0 - v, 1.3);
    float alpha = smoothstep(0.0, 0.06, v) * topFade;

    float fade = 1.0;
    float desat = 0.0;
    if (phase == 1) fade = 1.0 - smoothstep(2.0, 10.0, PhaseTime);
    else if (phase == 2) {
        float k = clamp(PhaseTime / 15.0, 0.0, 1.0);
        fade = (1.0 - k) * step(hash1(floor(t * 1.7) + 3.0), 1.0 - k * 0.8);
        desat = k;
    }

    vec3 tint = mix(ColorA, ColorB, v);
    vec3 core = mix(ColorA, vec3(1.0), 0.5);
    vec3 col = core * glyphs + tint * (streak * 0.55 + bottom * 0.7) * Glow;
    col *= alpha * fade * Intensity * (0.35 + 0.65 * smoothstep(0.0, 0.3, Progress));
    float grey = dot(col, vec3(0.299, 0.587, 0.114));
    col = mix(col, vec3(grey) * 0.8, desat);
    col = max(col, vec3(0.0));
    fragColor = vec4(col, clamp(max(col.r, max(col.g, col.b)), 0.0, 1.0));
}
