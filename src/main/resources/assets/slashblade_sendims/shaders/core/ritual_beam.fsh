#version 150

// 光柱与能量流的画布烘焙，输出预乘颜色，供叠加材质使用。
// Mode 0 光柱：u 横跨柱宽，v 从底到顶。Mode 1 能量流：u 从源头到核心，v 横跨光带。
uniform float Time;
uniform float Mode;
uniform float Style;
uniform float Glow;
uniform float Intensity;
uniform vec3 ColorA;
uniform vec3 ColorB;

in vec2 texCoord;
out vec4 fragColor;

float hash2(vec2 p) { return fract(sin(dot(p, vec2(41.3, 289.1))) * 43758.5453); }
float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash2(i), hash2(i + vec2(1.0, 0.0)), f.x), mix(hash2(i + vec2(0.0, 1.0)), hash2(i + vec2(1.0, 1.0)), f.x), f.y);
}

void main() {
    int s = int(Style + 0.5);
    float t = Time;
    vec3 col;
    if (Mode < 0.5) {
        float v = texCoord.y;
        float x = (texCoord.x - 0.5) * 2.0;
        if (s == 4 || s == 8) x += (noise(vec2(v * 14.0, t * 0.15)) - 0.5) * 0.06;
        float core = exp(-x * x / 0.012) * 1.4;
        float glow = exp(-x * x / 0.18) * 0.55 * Glow;
        if (s == 1) glow += exp(-x * x / 0.6) * 0.25 * Glow;
        float flow = 0.6 + 0.6 * noise(vec2(x * 3.0, v * 8.0 - t * 0.12));
        float pulse = exp(-pow((fract(v * 1.5 - t * 0.06) - 0.5) / 0.06, 2.0)) * 0.6;
        float ends = smoothstep(0.0, 0.04, v) * smoothstep(1.0, 0.75, v);
        vec3 coreCol = mix(ColorA, vec3(1.0), 0.7);
        vec3 glowCol = mix(ColorA, ColorB, v * 0.8);
        if (s == 8) { coreCol = mix(ColorB, vec3(1.0, 0.9, 0.6), 0.5); glowCol = mix(ColorB, ColorA, v); }
        col = (coreCol * core * (0.8 + pulse) + glowCol * glow * flow) * ends;
    } else {
        float u = texCoord.x;
        float y = (texCoord.y - 0.5) * 2.0;
        float core = exp(-y * y / 0.02);
        float glow = exp(-y * y / 0.25) * 0.5 * Glow;
        float dash = pow(fract(u * 5.0 - t * 0.15), 8.0) * 1.5 + 0.35;
        float ends = smoothstep(0.0, 0.1, u) * (0.6 + 0.4 * u);
        col = (mix(ColorA, vec3(1.0), 0.55) * core * dash + mix(ColorB, ColorA, u) * glow) * ends;
    }
    col = max(col * Intensity, vec3(0.0));
    fragColor = vec4(col, clamp(max(col.r, max(col.g, col.b)), 0.0, 1.0));
}
