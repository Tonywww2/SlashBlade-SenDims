#version 150

// 仪式画布烘焙与屏幕效果共用：顶点直接给出 NDC 坐标的全屏三角形。
in vec3 Position;

out vec2 texCoord;

void main() {
    gl_Position = vec4(Position.xy, 0.0, 1.0);
    texCoord = Position.xy * 0.5 + 0.5;
}
