#version 150

// 天顶·裂空的发光层画布：每帧在世界绘制之外烘焙一次（RenderTickEvent 开头），
// 拔刀剑把 *_luminous 分组按普通纹理采样（叠加、全亮），所以开着光影包也照常显示。
// 画布 128×4096，按 u 分三块，块边缘留黑：
//   [0, 0.5)    刀身裂缝：横向为裂缝条带的宽度方向，纵向从护手一侧到刀尖
//   [0.5, 0.75) 刀鞘裂缝：横向为条带宽度，纵向从鞘口到鞘尾
//   [0.75, 1)   刀镡发光缝：横向为径向，纵向绕一周
uniform float Time;

in vec2 texCoord;
out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);
}

float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 4; i++) {
        value += amplitude * noise(p);
        p = p * 2.03 + vec2(17.1, 9.4);
        amplitude *= 0.5;
    }
    return value;
}

// 折线噪声（-0.5..0.5）：线性插值，折角是尖的，像裂缝
float zigzag(float x, float seed) {
    float i = floor(x);
    return mix(hash(vec2(i, seed)), hash(vec2(i + 1.0, seed)), fract(x)) - 0.5;
}

vec3 stars(vec2 p, float t) {
    vec2 cell = floor(p);
    vec2 f = fract(p) - 0.5;
    float h = hash(cell);
    vec2 offset = (vec2(hash(cell + 7.1), hash(cell + 3.7)) - 0.5) * 0.6;
    float twinkle = 0.55 + 0.45 * sin(t * (1.5 + 3.0 * h) + h * 40.0);
    return vec3(0.75, 0.85, 1.0) * step(0.82, h) * twinkle * smoothstep(0.17, 0.0, length(f - offset));
}

// 刀身裂缝：折线中线、宽窄变化的开口，开口里是缓慢流向刀尖的深蓝星空，
// 边缘一圈闪烁的亮边，每隔一会儿有一道能量脉冲从护手跑到刀尖。
vec3 rift(float across, float along, float t) {
    const float aspect = 110.0;  // 条带长度 ÷ 宽度（实际尺寸），让星点保持圆形
    float taper = smoothstep(0.0, 0.04, along) * smoothstep(1.0, 0.95, along);
    float center = 0.5 + 0.24 * zigzag(along * 34.0, 1.3) + 0.08 * zigzag(along * 110.0, 5.7);
    float width = (0.10 + 0.16 * fbm(vec2(along * 7.0, 2.0))) * taper;
    float d = abs(across - center);
    vec2 phys = vec2(across, along * aspect);
    float inside = smoothstep(width, width * 0.6, d);

    vec2 sp = phys * 0.9 + vec2(0.0, -t * 0.25);
    float nebula = fbm(sp * vec2(1.2, 0.35) + vec2(3.1, t * 0.04));
    vec3 space = mix(vec3(0.01, 0.02, 0.08), vec3(0.10, 0.20, 0.62), smoothstep(0.25, 0.8, nebula));
    space += vec3(0.30, 0.10, 0.45) * pow(fbm(sp * vec2(2.0, 0.6) + 11.0), 3.0) * 0.8;
    space += stars(phys * 4.0 + vec2(0.0, -t * 0.6), t);
    space += stars(phys * 7.0 + vec2(3.3, -t * 0.25), t * 1.3) * 0.6;

    float edge = d - width;
    float rim = exp(-edge * edge / 0.0012) * step(0.002, width);
    float pulse = exp(-pow(fract(along - t * 0.6) - 0.5, 2.0) / 0.0015);
    float flicker = 0.8 + 0.2 * sin(t * 13.0 + along * 90.0) * sin(t * 7.0 + along * 37.0);
    vec3 color = space * inside;
    color += vec3(0.55, 0.80, 1.0) * rim * flicker * (0.9 + 1.6 * pulse);
    color += vec3(0.05, 0.12, 0.35) * exp(-max(edge, 0.0) / 0.08) * (1.0 - inside) * 0.6;
    return color * taper;
}

// 刀鞘裂缝：细线缓慢呼吸，偶尔有亮点沿缝滑过。
vec3 sheathSeam(float across, float along, float t) {
    float taper = smoothstep(0.0, 0.05, along) * smoothstep(1.0, 0.95, along);
    float center = 0.5 + 0.18 * zigzag(along * 60.0, 9.1);
    float d = abs(across - center);
    float line = exp(-d * d / 0.008);
    float halo = exp(-d / 0.18) * 0.45;
    float breathe = 0.6 + 0.4 * sin(t * 1.6);
    float pulse = exp(-pow(fract(along - t * 0.35) - 0.5, 2.0) / 0.003);
    vec3 color = vec3(0.6, 0.82, 1.0) * line * (0.7 + 0.5 * breathe + 1.4 * pulse);
    color += vec3(0.08, 0.16, 0.45) * halo * breathe;
    return color * taper;
}

// 刀镡发光缝：细环，两颗亮点绕着转。
vec3 guardRing(float across, float around, float t) {
    float d = abs(across - 0.5);
    float line = exp(-d * d / 0.01);
    float comet = pow(fract(around - t * 0.18), 6.0) + pow(fract(around + 0.5 - t * 0.18), 6.0);
    return vec3(0.55, 0.80, 1.0) * line * (0.35 + 1.4 * comet)
            + vec3(0.06, 0.14, 0.40) * exp(-d / 0.25) * 0.5 * smoothstep(0.5, 0.35, d);
}

void main() {
    float u = texCoord.x;
    float v = texCoord.y;
    vec3 color;
    if (u < 0.5) {
        color = rift(clamp((u - 0.01) / 0.48, 0.0, 1.0), v, Time);
    } else if (u < 0.75) {
        color = sheathSeam(clamp((u - 0.51) / 0.23, 0.0, 1.0), v, Time);
    } else {
        color = guardRing(clamp((u - 0.76) / 0.23, 0.0, 1.0), v, Time);
    }
    fragColor = vec4(color, 1.0);
}
