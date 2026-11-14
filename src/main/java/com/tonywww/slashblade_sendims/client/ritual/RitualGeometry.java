package com.tonywww.slashblade_sendims.client.ritual;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 仪式几何：地面图案按格贴地（只在收到阶段包时采样一次），光柱、光幕、能量流每帧按镜头朝向生成。
 * 全部顶点相对镜头位置，直接写进原版材质的缓冲，不分配新对象。
 */
final class RitualGeometry {
    /** 贴地面片抬高的距离，避免和地面深度冲突。 */
    private static final float LIFT = 0.015F;

    private RitualGeometry() {
    }

    /** 不画的格子：召唤结构占用的 3×3、传送核心方块所在的一格。 */
    enum Footprint { NONE, CENTER, THREE_BY_THREE }

    /**
     * 贴地图案：每格一块面片，花纹按世界水平坐标计算，台阶上下连续，悬空和被遮挡的格子不画。
     *
     * @param cells 每格 3 个数：西北角 x、表面 y、西北角 z
     */
    record Decal(double centerX, double centerZ, float extent, float[] cells, int count, double minY, double maxY) {
    }

    static Decal sample(ClientLevel level, double centerX, double centerZ, int baseY, double radius, float extent,
            Footprint footprint) {
        int reach = Mth.ceil(radius);
        int coreX = Mth.floor(centerX);
        int coreZ = Mth.floor(centerZ);
        float[] cells = new float[(reach * 2 + 1) * (reach * 2 + 1) * 3];
        int count = 0;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos();
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                if (footprint == Footprint.CENTER && dx == 0 && dz == 0) continue;
                if (footprint == Footprint.THREE_BY_THREE && Math.abs(dx) <= 1 && Math.abs(dz) <= 1) continue;
                int x = coreX + dx;
                int z = coreZ + dz;
                double nx = Mth.clamp(centerX, x, x + 1) - centerX;
                double nz = Mth.clamp(centerZ, z, z + 1) - centerZ;
                if (nx * nx + nz * nz > radius * radius) continue;
                if (!level.hasChunkAt(pos.set(x, baseY, z))) continue;
                for (int y = baseY; y >= baseY - 6; y--) {
                    BlockState state = level.getBlockState(pos.set(x, y, z));
                    if (state.isAir()) continue;
                    VoxelShape shape = state.getCollisionShape(level, pos);
                    if (shape.isEmpty()) continue;
                    double top;
                    if (state.isFaceSturdy(level, pos, Direction.UP)) {
                        BlockState cover = level.getBlockState(above.set(x, y + 1, z));
                        if (Block.isShapeFullBlock(cover.getCollisionShape(level, above))) break;
                        top = y + 1;
                    } else {
                        top = y + shape.max(Direction.Axis.Y);
                    }
                    cells[count * 3] = x;
                    cells[count * 3 + 1] = (float) top;
                    cells[count * 3 + 2] = z;
                    count++;
                    minY = Math.min(minY, top);
                    maxY = Math.max(maxY, top);
                    break;
                }
            }
        }
        if (count == 0) {
            minY = maxY = baseY;
        }
        return new Decal(centerX, centerZ, extent, cells, count, minY, maxY);
    }

    static void decal(VertexConsumer out, Matrix4f pose, Matrix3f normal, Decal decal,
            double camX, double camY, double camZ, float k) {
        if (decal.count == 0 || k <= 0.003F) return;
        float inv = 0.5F / decal.extent;
        float[] cells = decal.cells;
        for (int i = 0; i < decal.count; i++) {
            float x0 = cells[i * 3];
            float y = cells[i * 3 + 1] + LIFT;
            float z0 = cells[i * 3 + 2];
            float x1 = x0 + 1.0F;
            float z1 = z0 + 1.0F;
            float u0 = (float) ((x0 - decal.centerX) * inv + 0.5);
            float u1 = (float) ((x1 - decal.centerX) * inv + 0.5);
            float v0 = (float) ((z0 - decal.centerZ) * inv + 0.5);
            float v1 = (float) ((z1 - decal.centerZ) * inv + 0.5);
            float px0 = (float) (x0 - camX);
            float px1 = (float) (x1 - camX);
            float py = (float) (y - camY);
            float pz0 = (float) (z0 - camZ);
            float pz1 = (float) (z1 - camZ);
            vertex(out, pose, normal, px0, py, pz0, u0, v0, k, 0, 1, 0);
            vertex(out, pose, normal, px0, py, pz1, u0, v1, k, 0, 1, 0);
            vertex(out, pose, normal, px1, py, pz1, u1, v1, k, 0, 1, 0);
            vertex(out, pose, normal, px1, py, pz0, u1, v0, k, 0, 1, 0);
        }
    }

    /** 绕竖直轴朝向镜头的柱状光；u 横跨柱宽，v 从底到顶。 */
    static void beam(VertexConsumer out, Matrix4f pose, Matrix3f normal, double x, double y, double z,
            double height, double width, double camX, double camY, double camZ, float k) {
        if (height <= 0.01 || width <= 0.005 || k <= 0.003F) return;
        double tx = camX - x;
        double tz = camZ - z;
        double length = Math.sqrt(tx * tx + tz * tz);
        if (length < 1.0E-4) {
            tx = 1.0;
            tz = 0.0;
            length = 1.0;
        }
        float sx = (float) (tz / length * width * 0.5);
        float sz = (float) (-tx / length * width * 0.5);
        float bx = (float) (x - camX);
        float by = (float) (y - camY);
        float bz = (float) (z - camZ);
        float top = by + (float) height;
        float nx = (float) (tx / length);
        float nz = (float) (tz / length);
        vertex(out, pose, normal, bx - sx, by, bz - sz, 0, 0, k, nx, 0, nz);
        vertex(out, pose, normal, bx + sx, by, bz + sz, 1, 0, k, nx, 0, nz);
        vertex(out, pose, normal, bx + sx, top, bz + sz, 1, 1, k, nx, 0, nz);
        vertex(out, pose, normal, bx - sx, top, bz - sz, 0, 1, k, nx, 0, nz);
    }

    /** 圆筒光幕，内外两面各画一次（叠加材质会剔除背面）。u 绕一圈，v 从底到顶。 */
    static void wall(VertexConsumer out, Matrix4f pose, Matrix3f normal, double x, double y, double z,
            double radius, double height, double camX, double camY, double camZ, float k) {
        if (height <= 0.02 || k <= 0.003F) return;
        int segments = 48;
        float by = (float) (y - camY);
        float top = by + (float) height;
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2.0 * i / segments;
            double a1 = Math.PI * 2.0 * (i + 1) / segments;
            float c0 = (float) Math.cos(a0);
            float s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1);
            float s1 = (float) Math.sin(a1);
            float x0 = (float) (x + c0 * radius - camX);
            float z0 = (float) (z + s0 * radius - camZ);
            float x1 = (float) (x + c1 * radius - camX);
            float z1 = (float) (z + s1 * radius - camZ);
            float u0 = (float) i / segments;
            float u1 = (float) (i + 1) / segments;
            float mx = (c0 + c1) * 0.5F;
            float mz = (s0 + s1) * 0.5F;
            vertex(out, pose, normal, x0, by, z0, u0, 0, k, mx, 0, mz);
            vertex(out, pose, normal, x0, top, z0, u0, 1, k, mx, 0, mz);
            vertex(out, pose, normal, x1, top, z1, u1, 1, k, mx, 0, mz);
            vertex(out, pose, normal, x1, by, z1, u1, 0, k, mx, 0, mz);
            vertex(out, pose, normal, x1, by, z1, u1, 0, k, -mx, 0, -mz);
            vertex(out, pose, normal, x1, top, z1, u1, 1, k, -mx, 0, -mz);
            vertex(out, pose, normal, x0, top, z0, u0, 1, k, -mx, 0, -mz);
            vertex(out, pose, normal, x0, by, z0, u0, 0, k, -mx, 0, -mz);
        }
    }

    /** 从源头到目标的弧形光带（二次贝塞尔，中点抬高 lift），每段朝向镜头；两面都画。u 从源头到目标。 */
    static void stream(VertexConsumer out, Matrix4f pose, Matrix3f normal, double sx, double sy, double sz,
            double tx, double ty, double tz, double lift, double width,
            double camX, double camY, double camZ, float k) {
        if (k <= 0.003F) return;
        int segments = 12;
        double cx = (sx + tx) * 0.5;
        double cy = Math.max(sy, ty) + lift;
        double cz = (sz + tz) * 0.5;
        double px = sx;
        double py = sy;
        double pz = sz;
        for (int j = 1; j <= segments; j++) {
            double t = (double) j / segments;
            double m = 1.0 - t;
            double qx = m * m * sx + 2 * m * t * cx + t * t * tx;
            double qy = m * m * sy + 2 * m * t * cy + t * t * ty;
            double qz = m * m * sz + 2 * m * t * cz + t * t * tz;
            double dx = qx - px;
            double dy = qy - py;
            double dz = qz - pz;
            double vx = camX - (px + qx) * 0.5;
            double vy = camY - (py + qy) * 0.5;
            double vz = camZ - (pz + qz) * 0.5;
            double ox = dy * vz - dz * vy;
            double oy = dz * vx - dx * vz;
            double oz = dx * vy - dy * vx;
            double ol = Math.sqrt(ox * ox + oy * oy + oz * oz);
            if (ol > 1.0E-6) {
                double scale = width * 0.5 / ol;
                float wx = (float) (ox * scale);
                float wy = (float) (oy * scale);
                float wz = (float) (oz * scale);
                float ax = (float) (px - camX);
                float ay = (float) (py - camY);
                float az = (float) (pz - camZ);
                float bx = (float) (qx - camX);
                float by = (float) (qy - camY);
                float bz = (float) (qz - camZ);
                float u0 = (float) (j - 1) / segments;
                float u1 = (float) j / segments;
                vertex(out, pose, normal, ax - wx, ay - wy, az - wz, u0, 0, k, 0, 1, 0);
                vertex(out, pose, normal, bx - wx, by - wy, bz - wz, u1, 0, k, 0, 1, 0);
                vertex(out, pose, normal, bx + wx, by + wy, bz + wz, u1, 1, k, 0, 1, 0);
                vertex(out, pose, normal, ax + wx, ay + wy, az + wz, u0, 1, k, 0, 1, 0);
                vertex(out, pose, normal, ax + wx, ay + wy, az + wz, u0, 1, k, 0, 1, 0);
                vertex(out, pose, normal, bx + wx, by + wy, bz + wz, u1, 1, k, 0, 1, 0);
                vertex(out, pose, normal, bx - wx, by - wy, bz - wz, u1, 0, k, 0, 1, 0);
                vertex(out, pose, normal, ax - wx, ay - wy, az - wz, u0, 0, k, 0, 1, 0);
            }
            px = qx;
            py = qy;
            pz = qz;
        }
    }

    private static void vertex(VertexConsumer out, Matrix4f pose, Matrix3f normal, float x, float y, float z,
            float u, float v, float k, float nx, float ny, float nz) {
        int c = Mth.clamp((int) (k * 255.0F), 0, 255);
        out.vertex(pose, x, y, z).color(c, c, c, c).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT).normal(normal, nx, ny, nz).endVertex();
    }
}
