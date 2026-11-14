#version 150

// 仪式屏幕效果：在整帧世界（含光影最终合成和手持物）画完之后、界面之前处理最终画面。
// 传送旋涡（WarpMode 0）或下坠旋涡（WarpMode 1）、抵达闪光、召唤折射涟漪，一次全屏绘制完成。
uniform sampler2D DiffuseSampler;
uniform float WarpStrength;
uniform float WarpMode;
uniform float Flash;
uniform vec3 Tint;
uniform vec2 RippleCenter;
uniform float RippleRadius;
uniform float RippleStrength;
uniform float Aspect;
uniform float Time;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 c = vec2(0.5);
    vec2 d = texCoord - c;
    d.x *= Aspect;
    float r = length(d);
    float s = WarpStrength;

    if (s > 0.0) {
        float falloff = pow(max(0.0, 1.0 - r / 1.1), 2.0);
        float ang = (WarpMode < 0.5 ? 2.4 : 0.7) * s * falloff;
        float ca = cos(ang);
        float sa = sin(ang);
        d = vec2(ca * d.x - sa * d.y, sa * d.x + ca * d.y);
        if (WarpMode < 0.5) {
            d *= 1.0 - s * 0.35 * (1.0 - smoothstep(0.0, 0.9, r));
        } else {
            d.y = d.y * (1.0 + s * 0.45) + s * 0.06 * (1.0 - r);
            d.x *= 1.0 - s * 0.15;
        }
    }

    vec2 uv = c + vec2(d.x / Aspect, d.y);
    float wave = 0.0;
    if (RippleStrength > 0.0) {
        vec2 rd = texCoord - RippleCenter;
        rd.x *= Aspect;
        float rr = length(rd);
        wave = exp(-pow((rr - RippleRadius) / 0.035, 2.0)) * RippleStrength;
        if (rr > 1e-4) uv += vec2(rd.x / Aspect, rd.y) / rr * wave * 0.03;
    }

    float ab = s * 0.014 * r + wave * 0.008;
    vec2 uvR = clamp(c + (uv - c) * (1.0 + ab), 0.0, 1.0);
    vec2 uvG = clamp(uv, 0.0, 1.0);
    vec2 uvB = clamp(c + (uv - c) * (1.0 - ab), 0.0, 1.0);
    vec3 col = vec3(texture(DiffuseSampler, uvR).r, texture(DiffuseSampler, uvG).g, texture(DiffuseSampler, uvB).b);

    float vig = smoothstep(0.3, 1.0, r) * s;
    col = mix(col, col * 0.3 + Tint * 0.22, vig);
    col += Tint * wave * 0.08;
    col = mix(col, mix(Tint, vec3(1.0), 0.65), clamp(Flash, 0.0, 1.0));
    fragColor = vec4(col, 1.0);
}
