#version 150

// 仪式地面图案的画布烘焙。坐标单位是方块，原点在仪式中心，x 指向东（+X），y 指向南（+Z）。
// 输出预乘颜色，供叠加材质（RenderType.eyes）使用。
uniform float Time;       // 仪式开始后的 tick
uniform float Progress;   // 蓄能进度 0..1
uniform float Phase;      // 0 蓄能 1 释放 2 中断 3 抵达
uniform float PhaseTime;  // 当前阶段开始后的 tick
uniform float Mode;       // 0 仪式法阵 1 冲击波 2 抵达符印 3 基岩裂隙
uniform float Kind;       // 0 召唤 1 传送 2 基岩
uniform float Style;      // 主题样式编号
uniform float Radius;     // 图案半径（方块）
uniform float Extent;     // 画布半边长（方块）
uniform float Heading;    // 罗盘方位（弧度）；没有时小于 -50
uniform float Glow;       // 烘焙光晕倍率
uniform float Intensity;
uniform float Inner;      // 1：画中心印记
uniform float Level;      // 基岩裂隙强度 0..1
uniform float Detail;     // 1 近处完整细节，0 远处省略符文
uniform vec3 ColorA;
uniform vec3 ColorB;

in vec2 texCoord;
out vec4 fragColor;

const float PI = 3.14159265;

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
float segT(vec2 p, vec2 a, vec2 b) {
    vec2 pa = p - a;
    vec2 ba = b - a;
    return clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
}
float ramp(float p, float a, float b) { return clamp((p - a) / (b - a), 0.0, 1.0); }
// (线条核心, 光晕)
vec2 line(float d, float w) { return vec2(smoothstep(w, w * 0.35, d), exp(-d * 9.0)); }
float ang01(vec2 p, float start) { return fract((atan(p.y, p.x) - start) / (2.0 * PI)); }

// 程序化符文：3 列 × 4 行点阵中按种子取 3–5 条线段；样式 3 为圆点，样式 2 只取斜线。
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

// 仪式法阵（也用于抵达符印）。返回 (核心, 光晕)，并通过 pool 输出光池强度。
vec2 ritualCircle(vec2 p, float R, float P, float spin, int s, float hot, float w, out float pool) {
    float r = length(p);
    float t = Time;
    vec2 acc = vec2(0.0);

    // 外圈双环：从两个起点沿圆周描线
    float outerP = ramp(P, 0.0, 0.25);
    for (int k = 0; k < 2; k++) {
        float rr = R - 0.15 - float(k) * 0.23;
        float th = ang01(p, PI * 0.5 + float(k) * PI);
        float vis = outerP > 0.0 ? step(th, outerP) : 0.0;
        float head = exp(-abs(th - outerP) * 60.0) * step(outerP, 0.999) * 2.0;
        acc += line(abs(r - rr), w * (k == 0 ? 1.3 : 0.8)) * vis * (1.0 + head);
    }

    // 符文带（与内层反向旋转）
    float runeP = ramp(P, 0.15, 0.55);
    float rIn = R - 1.15;
    float rOut = R - 0.55;
    if (R < 3.0) { rIn = R - 0.8; rOut = R - 0.4; }
    if (r > rIn - 0.1 && r < rOut + 0.1) {
        if (Detail > 0.5) {
            float N = (s == 7) ? 32.0 : (s == 3 ? 24.0 : 28.0);
            if (R < 3.0) N = floor(N * 0.6);
            float a = atan(p.y, p.x) - spin * 1.6;
            float cell = floor(fract(a / (2.0 * PI)) * N);
            float ca = (cell + 0.5) / N * 2.0 * PI + spin * 1.6;
            vec2 tang = vec2(-sin(ca), cos(ca));
            vec2 rad = vec2(cos(ca), sin(ca));
            vec2 c = rad * (rIn + rOut) * 0.5;
            float scale = (rOut - rIn) * 0.95;
            vec2 q = vec2(dot(p - c, tang), dot(p - c, rad)) / scale;
            float order = hash1(cell * 7.3);
            float vis = step(order * 0.8, runeP * 1.05 - 0.02);
            float flick = 0.75 + 0.25 * sin(t * 0.3 + cell * 2.1);
            acc += line(glyph(q, cell + float(s) * 31.0, s) * scale, w * 0.9) * vis * flick;
        } else {
            float band = smoothstep(0.12, 0.0, abs(r - (rIn + rOut) * 0.5) - (rOut - rIn) * 0.35);
            acc += vec2(band * 0.25, band * 0.2) * runeP;
        }
    }
    acc += line(abs(r - rIn), w * 0.6) * ramp(P, 0.12, 0.3) * 0.8;

    // 刻度环
    float rT = rIn - 0.2;
    acc += line(abs(r - rT), w * 0.8) * ramp(P, 0.40, 0.55);
    {
        float n = (s == 7) ? 32.0 : 60.0;
        float a = fract((atan(p.y, p.x) + spin * 0.5) / (2.0 * PI)) * n;
        float major = (s == 7) ? step(fract(floor(a) / 8.0), 0.01) : step(fract(floor(a) / 5.0), 0.01);
        float len = 0.12 + major * 0.14;
        float dd = abs(fract(a) - 0.5) / n * 2.0 * PI * r;
        float inBand = step(rT - len, r) * step(r, rT);
        acc += line(dd, w * 0.6) * inBand * ramp(P, 0.45, 0.65) * 0.9;
    }

    // 星形连线或主题主形
    float Rs = rT - 0.4;
    float starP = ramp(P, 0.33, 0.7);
    float rot = (s == 7) ? Heading : -spin * 0.8;
    if (s == 5) {
        // 深境二层：两条相位相反的六瓣藤蔓环，加叶脉
        for (int k = 0; k < 2; k++) {
            float th = atan(p.y, p.x);
            float rr = Rs * 0.78 + 0.28 * sin(6.0 * th + float(k) * PI + t * 0.02);
            float vis = starP > 0.0 ? step(ang01(p, 0.0), starP) : 0.0;
            acc += line(abs(r - rr), w) * vis;
        }
        float th6 = atan(p.y, p.x) * 6.0 / (2.0 * PI);
        float leaf = abs(fract(th6 + 0.5) - 0.5) * 2.0 * PI * r / 6.0;
        acc += line(leaf, w * 0.6) * step(r, Rs * 1.05) * step(Rs * 0.35, r) * ramp(P, 0.55, 0.8) * 0.6;
    } else if (s == 7) {
        // 地图传送：罗盘玫瑰（四长四短），北向标记指向目标
        for (int i = 0; i < 8; i++) {
            float a = rot + float(i) * PI / 4.0;
            float len = (i % 2 == 0) ? Rs : Rs * 0.55;
            vec2 tip = vec2(cos(a), sin(a)) * len;
            vec2 l1 = vec2(cos(a - PI / 8.0), sin(a - PI / 8.0)) * Rs * 0.22;
            vec2 l2 = vec2(cos(a + PI / 8.0), sin(a + PI / 8.0)) * Rs * 0.22;
            float pr = ramp(P, 0.33 + float(i) * 0.03, 0.5 + float(i) * 0.03);
            float d = min(segD(p, l1, tip), segD(p, l2, tip));
            d = min(d, segD(p, vec2(0.0), tip) * 1.6);
            acc += line(d, w * (i % 2 == 0 ? 1.1 : 0.8)) * pr * (i == 0 && Heading > -50.0 ? 1.6 : 1.0);
        }
        if (Heading > -50.0) {
            vec2 nd = vec2(cos(rot), sin(rot));
            vec2 sd = vec2(-nd.y, nd.x);
            vec2 np = nd * (R - 0.85);
            float tri = segD(p, np + nd * 0.25, np - sd * 0.18);
            tri = min(tri, segD(p, np + nd * 0.25, np + sd * 0.18));
            tri = min(tri, segD(p, np - sd * 0.18, np + sd * 0.18));
            acc += line(tri, w * 1.2) * ramp(P, 0.6, 0.75) * 1.5;
        }
        vec2 g = abs(fract(p / 0.8) - 0.5) * 0.8;
        acc += line(min(g.x, g.y), w * 0.4) * step(r, Rs * 0.95) * ramp(P, 0.6, 0.85) * 0.25;
        acc += line(abs(r - Rs * 0.22), w * 0.7) * ramp(P, 0.3, 0.45);
    } else {
        int n = s == 1 ? 8 : s == 2 ? 5 : s == 3 ? 6 : s == 4 ? 9 : 7;
        int kk = s == 1 ? 3 : s == 2 ? 2 : s == 3 ? 2 : s == 4 ? 4 : s == 6 ? 2 : 3;
        for (int i = 0; i < 9; i++) {
            if (i >= n) break;
            float a0 = rot + float(i) * 2.0 * PI / float(n) + PI * 0.5;
            float a1 = rot + float(i + kk) * 2.0 * PI / float(n) + PI * 0.5;
            vec2 A = vec2(cos(a0), sin(a0)) * Rs;
            vec2 B = vec2(cos(a1), sin(a1)) * Rs;
            float local = clamp(starP * float(n) - float(i), 0.0, 1.0);
            float tt = segT(p, A, B);
            float vis = local > 0.0 ? step(tt, local) : 0.0;
            float head = exp(-abs(tt - local) * 25.0) * step(local, 0.999) * 2.0;
            acc += line(segD(p, A, B), w) * vis * (1.0 + head);
            if (R >= 3.0) acc += line(abs(length(p - A) - 0.17), w * 0.7) * ramp(P, 0.55, 0.7);
        }
        acc += line(abs(r - Rs), w * 0.7) * ramp(P, 0.5, 0.65);
        float rInner = Rs * cos(PI * float(kk) / float(n));
        acc += line(abs(r - rInner), w * 0.7) * ramp(P, 0.6, 0.75);
        if (s == 1) {
            // 白金地牢：16 道日芒
            float a = fract((atan(p.y, p.x) - rot) / (2.0 * PI)) * 16.0;
            float dd = abs(fract(a) - 0.5) / 16.0 * 2.0 * PI * r;
            float band = step(Rs + 0.08, r) * step(r, rT - 0.32);
            acc += line(dd, w * 0.7) * band * ramp(P, 0.5, 0.7) * 0.9;
        }
        if (s == 4) {
            // 深境一层：缓慢流动的熔岩裂纹
            float cr = abs(noise(p * 1.6 + vec2(t * 0.01, 0.0)) - 0.5);
            float vein = smoothstep(0.06, 0.0, cr) * step(r, Rs * 1.02);
            acc += vec2(vein * 0.55, vein * 0.4) * ramp(P, 0.4, 0.8);
        }
    }

    if (Inner > 0.5) {
        acc += line(abs(r - 0.55), w) * ramp(P, 0.05, 0.2);
        acc += line(abs(r - 0.85), w * 0.7) * ramp(P, 0.1, 0.25);
    }

    pool = exp(-r * r / (R * R * 0.30)) * (R < 3.0 ? 0.14 : 0.32) * smoothstep(0.0, 0.3, P);
    if (s == 6) pool *= smoothstep(0.6, 2.2, r);
    return acc;
}

// 基岩裂隙：从中心向外的锯齿裂缝，缝里透出熔岩光。lvl 为 0..1 的裂开程度。
vec2 fissure(vec2 p, float R, float lvl, out float pool) {
    float r = length(p);
    float th = atan(p.y, p.x);
    float N = 9.0;
    vec2 acc = vec2(0.0);
    float len = R * (0.25 + 0.75 * lvl);
    for (int i = 0; i < 9; i++) {
        float base = (float(i) + hash1(float(i) * 3.7) * 0.6) / N * 2.0 * PI;
        float wig = (noise(vec2(r * 2.2, float(i) * 7.1)) - 0.5) * 0.55 + (noise(vec2(r * 6.0, float(i) * 3.3)) - 0.5) * 0.18;
        float da = th - base - wig / max(r, 0.3);
        da = mod(da + PI, 2.0 * PI) - PI;
        float d = abs(da) * r;
        float l = len * (0.6 + 0.4 * hash1(float(i) * 9.1));
        float taper = 1.0 - smoothstep(l * 0.6, l, r);
        acc += line(d, 0.03 + 0.03 * taper) * taper;
        // 分叉
        float fb = base + 0.35 * (hash1(float(i) * 5.3) - 0.5);
        float da2 = mod(th - fb - wig * 1.3 / max(r, 0.3) + PI, 2.0 * PI) - PI;
        float t2 = (1.0 - smoothstep(l * 0.35, l * 0.7, r)) * step(l * 0.3, r);
        acc += line(abs(da2) * r, 0.025) * t2 * 0.7;
    }
    float ember = noise(p * 3.0 + vec2(Time * 0.05, 0.0));
    acc += vec2(0.0, smoothstep(0.55, 1.0, ember) * (1.0 - smoothstep(len * 0.5, len, r)) * 0.4);
    acc += line(abs(r - len * 0.95), 0.02) * 0.35 * lvl;
    pool = exp(-r * r / (len * len * 0.5 + 0.01)) * 0.45 * lvl;
    return acc;
}

void main() {
    vec2 p = (texCoord * 2.0 - 1.0) * Extent;
    int s = int(Style + 0.5);
    int mode = int(Mode + 0.5);
    int phase = int(Phase + 0.5);
    float R = Radius;
    float pt = PhaseTime;
    float dirSign = (s == 6) ? -1.0 : 1.0;
    float speed = (s == 3) ? 0.5 : 1.0;
    float P = Progress;
    float hot = ramp(P, 0.75, 1.0);
    float spin = Time * 0.010 * dirSign * speed * (1.0 + hot * 1.5);
    float w = 0.022 + hot * 0.010;
    float fade = 1.0;
    float flash = 1.0;
    float desat = 0.0;

    // 三段蓄能各脉冲一次；武士X 主题按心跳双拍脉动
    float pulse = 0.0;
    for (int k = 0; k < 3; k++) {
        float at = float(k) * 13.0 / 40.0;
        pulse += exp(-max(P - at, 0.0) * 40.0) * step(at, P);
    }
    if (s == 2) {
        float hb = fract(Time / 24.0);
        pulse += exp(-hb * 30.0) + 0.7 * exp(-abs(hb - 0.18) * 30.0);
    }

    if (phase == 1) {
        if (Kind < 0.5) {
            // 召唤释放：闪亮后向内坍缩
            flash = 1.0 + 1.5 * exp(-pt / 3.0);
            float sc = 1.0 - 0.8 * smoothstep(3.0, 23.0, pt);
            p /= sc;
            spin += pt * pt * 0.002;
            fade = 1.0 - smoothstep(10.0, 23.0, pt);
        } else if (Kind < 1.5) {
            flash = 1.0 + 1.2 * exp(-pt / 3.0);
            fade = 1.0 - smoothstep(10.0, 20.0, pt);
        } else {
            flash = 1.0 + 2.0 * exp(-pt / 3.0);
            fade = 1.0 - smoothstep(10.0, 30.0, pt);
        }
    } else if (phase == 2) {
        // 中断：随机闪断、褪色、熄灭
        float k = clamp(pt / 15.0, 0.0, 1.0);
        fade = (1.0 - k) * step(hash1(floor(Time * 1.7)), 1.0 - k * 0.8);
        desat = k;
        w *= 1.0 - 0.4 * k;
    } else if (phase == 3) {
        flash = 1.0 + 0.7 * exp(-pt / 4.0);
        spin += 0.6 * exp(-pt / 10.0);
        fade = 1.0 - smoothstep(12.0, 30.0, pt);
    }

    vec2 acc = vec2(0.0);
    float pool = 0.0;
    float r = length(p);
    if (mode == 1) {
        // 冲击波环
        float k = clamp(pt / 14.0, 0.0, 1.0);
        float ease = 1.0 - (1.0 - k) * (1.0 - k);
        float rr = 1.5 + (R - 1.5) * ease;
        float thick = mix(0.6, 0.12, k);
        float ring = exp(-pow(abs(r - rr) / thick, 2.0) * 2.0);
        float trail = smoothstep(rr, rr - 2.0, r) * step(r, rr) * 0.15;
        acc = vec2(ring * 0.9, ring * 0.8 + trail);
        fade = pow(1.0 - k, 1.5);
        pool = 0.0;
    } else if (mode == 3) {
        float lvl = phase == 1 ? 1.0 : Level;
        acc = fissure(p, R, lvl, pool);
        if (phase != 1) flash = 1.0 + 0.5 * exp(-pt / 3.0);
    } else {
        acc = ritualCircle(p, R, phase == 0 || phase == 2 ? P : 1.0, spin, s, hot, w, pool);
    }

    float intensity = (0.85 + pulse * 0.5) * (0.4 + 0.6 * smoothstep(0.0, 0.15, max(P, phase == 0 ? 0.0 : 1.0)));
    intensity *= flash * fade * Intensity;
    vec3 tint = mix(ColorA, ColorB, clamp(r / max(R, 0.1), 0.0, 1.0) * 0.85);
    vec3 coreCol = mix(ColorA, vec3(1.0), 0.45 + hot * 0.3);
    if (mode == 3 || (mode != 0 && Kind > 1.5)) {
        // 基岩：以橙红为主色
        coreCol = mix(ColorB, vec3(1.0, 0.85, 0.5), 0.4);
        tint = mix(ColorB, ColorA, clamp(r / max(R, 0.1), 0.0, 1.0) * (mode == 1 ? 0.3 : 1.0));
    }
    vec3 col = coreCol * acc.x + tint * acc.y * Glow * 0.55 + tint * pool * Glow;
    col *= intensity;
    float edge = (mode == 0 || mode == 2) ? smoothstep(R + 0.15, R - 0.05, r) : smoothstep(Extent, Extent - 0.25, r);
    col *= edge;
    float grey = dot(col, vec3(0.299, 0.587, 0.114));
    col = mix(col, vec3(grey) * 0.8, desat);
    col = max(col, vec3(0.0));
    fragColor = vec4(col, clamp(max(col.r, max(col.g, col.b)), 0.0, 1.0));
}
