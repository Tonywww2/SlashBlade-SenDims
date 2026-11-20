#version 150

// 真·次元斩屏幕层：在 HUD 画完之后、界面之前处理整帧画面（含光影最终合成、手持物和 HUD）。
// 采样实时画面或定格画面，沿线段两侧反向错位，加辉光、色差、褪色发冷和暗角。碎裂阶段不经过这里。
// 线段放在 64×2 的浮点纹理里（原版着色器 JSON 不支持数组 uniform）：
//   第 0 行：端点 a.xy、b.xy（屏幕 uv）；
//   第 1 行：完整度、亮度、错位量、宽度。
//   宽度 > 0 为投影到屏幕的世界斩痕：只在线段附近错位，宽度已按深度做了透视；
//   宽度 = 0 为碎裂裂纹：裂到全屏（完整度 1）后整个半平面错位，之前只在已裂开的线段范围内错位。
uniform sampler2D DiffuseSampler;
uniform sampler2D LineSampler;
uniform float LineCount;
uniform float Darken;
uniform float Desaturate;
uniform float Cold;
uniform float Freeze;
uniform float Aspect;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 scale = vec2(Aspect, 1.0);
    vec2 shift = vec2(0.0);
    float core = 0.0;
    float glow = 0.0;
    float warp = 0.0;
    int count = int(LineCount + 0.5);
    for (int i = 0; i < 64; i++) {
        if (i >= count) break;
        vec4 seg = texelFetch(LineSampler, ivec2(i, 0), 0);
        vec4 prm = texelFetch(LineSampler, ivec2(i, 1), 0);
        vec2 ba = (seg.zw - seg.xy) * scale;
        float len = length(ba);
        if (len < 1e-4 || prm.x <= 0.0) continue;
        vec2 dir = ba / len;
        vec2 pa = texCoord * scale - seg.xy * scale;
        float along = dot(pa, dir);
        float side = dot(pa, vec2(-dir.y, dir.x));
        float d = abs(side);
        bool world = prm.w > 0.0;
        float span = (!world && prm.x >= 0.999) ? 1.0 : step(0.0, along) * step(along, len);
        float local = 1.0;
        float width = 0.0016;
        if (world) {
            width = prm.w;
            local = exp(-d / (width * 4.0)) * smoothstep(0.0, width * 6.0, along) * (1.0 - smoothstep(len - width * 6.0, len, along));
        }
        shift += vec2(dir.x / Aspect, dir.y) * sign(side) * prm.z * span * local;
        warp += abs(prm.z) * span * local;
        core += span * prm.y * exp(-d * d / (width * width));
        glow += span * prm.y * exp(-d / (width * 7.0)) * 0.3;
    }
    vec2 uv = clamp(texCoord + shift, 0.0, 1.0);
    float ca = min(glow, 1.0) * 0.0035 + min(warp, 0.02) * 0.4 + Freeze * 0.0012;
    vec3 col;
    col.r = texture(DiffuseSampler, clamp(uv + vec2(ca, 0.0), 0.0, 1.0)).r;
    col.g = texture(DiffuseSampler, uv).g;
    col.b = texture(DiffuseSampler, clamp(uv - vec2(ca, 0.0), 0.0, 1.0)).b;

    float lum = dot(col, vec3(0.299, 0.587, 0.114));
    col = mix(col, vec3(lum), Desaturate);
    col = mix(col, col * vec3(0.72, 0.86, 1.18) + vec3(0.0, 0.008, 0.03), Cold);
    vec2 v = (texCoord - 0.5) * scale;
    col *= 1.0 - Darken * smoothstep(0.3, 1.05, length(v));

    float boost = 1.0 + Freeze;
    col += vec3(0.55, 0.75, 1.0) * glow * boost;
    col += vec3(0.92, 0.96, 1.0) * min(core, 3.0) * boost * 0.85;
    fragColor = vec4(col, 1.0);
}
