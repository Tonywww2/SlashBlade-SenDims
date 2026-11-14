package com.tonywww.slashblade_sendims.client.ritual;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.config.SenDimsConfig;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * 仪式屏幕效果：传送旋涡、抵达闪光、召唤折射涟漪。
 * <p>
 * 由 GameRenderer Mixin 在 renderLevel 返回之后调用：这时整帧世界（含光影最终合成和手持物）已经在主帧缓冲里，
 * 界面还没画。先把主帧缓冲拷进同尺寸的缓冲，再用自写着色器采样拷贝写回。没有效果时直接返回。
 * 计时用现实时间（跨维度传送时游戏时间与加载画面不可靠）。
 */
public final class RitualScreenEffect {
    private static final double WARP_HOLD = 20.0;
    private static final double FLASH_TICKS = 20.0;
    private static final double RIPPLE_TICKS = 12.0;

    private static double warpStart = -1;
    private static double warpDuration = 15;
    private static int warpMode;
    private static double flashStart = -1;
    private static double rippleStart = -1;
    private static float rippleStrength;
    private static Vec3 ripplePos = Vec3.ZERO;
    private static final float[] TINT = {1.0F, 1.0F, 1.0F};
    private static final Matrix4f PROJECTION = new Matrix4f();
    private static final Matrix4f VIEW = new Matrix4f();
    private static final Vector4f CLIP = new Vector4f();
    private static Vec3 camera = Vec3.ZERO;
    private static TextureTarget copy;
    private static boolean failed;

    private RitualScreenEffect() {
    }

    private static double ticks() {
        return Util.getMillis() / 50.0;
    }

    private static void tint(int color) {
        TINT[0] = (color >> 16 & 255) / 255.0F;
        TINT[1] = (color >> 8 & 255) / 255.0F;
        TINT[2] = (color & 255) / 255.0F;
    }

    /** 传送前的旋涡（只发给传送者）。mode 0 为旋涡，1 为下坠。 */
    static void warp(int mode, int color, int duration) {
        warpMode = mode;
        warpDuration = Math.max(1, duration);
        warpStart = ticks();
        flashStart = -1;
        tint(color);
    }

    /** 本地玩家抵达：从闪光中淡出，旋涡回正。 */
    static void arrive(int color, boolean breach) {
        if (warpStart < 0) {
            tint(color);
            warpMode = breach ? 1 : 0;
        }
        flashStart = ticks();
    }

    static void ripple(Vec3 position, float strength, int color) {
        ripplePos = position;
        rippleStrength = strength;
        rippleStart = ticks();
        if (warpStart < 0 && flashStart < 0) tint(color);
    }

    static void captureCamera(Matrix4f projection, Matrix4f view, Vec3 position) {
        PROJECTION.set(projection);
        VIEW.set(view);
        camera = position;
    }

    private static void clear() {
        warpStart = -1;
        flashStart = -1;
        rippleStart = -1;
    }

    /** GameRenderer Mixin 调用。 */
    public static void render(float partialTick) {
        if (warpStart < 0 && flashStart < 0 && rippleStart < 0) return;
        Minecraft mc = Minecraft.getInstance();
        ShaderInstance shader = RitualShaders.screen;
        if (mc.level == null || failed || shader == null || !SenDimsConfig.RITUAL_ENABLED.get()
                || !SenDimsConfig.RITUAL_SCREEN_EFFECTS.get()) {
            clear();
            return;
        }
        double now = ticks();
        if (mc.screen instanceof ReceivingLevelScreen) {
            // 跨维度传送的加载画面期间保持旋涡和闪光，画面关闭后才开始淡出
            if (flashStart >= 0) flashStart = now;
            else if (warpStart >= 0) warpStart = Math.min(warpStart, now - warpDuration);
        }
        float warp = 0.0F;
        float flash = 0.0F;
        if (flashStart >= 0) {
            double k = (now - flashStart) / FLASH_TICKS;
            if (k >= 1.0) {
                flashStart = -1;
                warpStart = -1;
            } else {
                double relax = 1.0 - k;
                flash = (float) (Math.pow(relax, 1.5) * 0.9);
                warp = warpStart >= 0 ? (float) (0.6 * relax) : 0.0F;
            }
        } else if (warpStart >= 0) {
            double elapsed = now - warpStart;
            if (elapsed < warpDuration) {
                double k = elapsed / warpDuration;
                warp = (float) (k * k);
                flash = (float) (smooth((elapsed - (warpDuration - 3.0)) / 3.0) * 0.85);
            } else if (elapsed < warpDuration + WARP_HOLD) {
                warp = 1.0F;
                flash = 0.85F;
            } else {
                // 没有收到抵达（传送失败或玩家被拦下）：10 tick 内淡出。
                double k = (elapsed - warpDuration - WARP_HOLD) / 10.0;
                if (k >= 1.0) {
                    warpStart = -1;
                } else {
                    warp = (float) (1.0 - k);
                    flash = (float) (0.85 * (1.0 - k));
                }
            }
        }
        float rippleRadius = 0.0F;
        float ripple = 0.0F;
        float centerX = 0.5F;
        float centerY = 0.5F;
        if (rippleStart >= 0) {
            double k = (now - rippleStart) / RIPPLE_TICKS;
            if (k >= 1.0) {
                rippleStart = -1;
            } else {
                CLIP.set((float) (ripplePos.x - camera.x), (float) (ripplePos.y - camera.y), (float) (ripplePos.z - camera.z), 1.0F);
                VIEW.transform(CLIP);
                PROJECTION.transform(CLIP);
                if (CLIP.w > 0.05F) {
                    centerX = CLIP.x / CLIP.w * 0.5F + 0.5F;
                    centerY = CLIP.y / CLIP.w * 0.5F + 0.5F;
                    rippleRadius = (float) (0.05 + 0.95 * (1.0 - (1.0 - k) * (1.0 - k)));
                    ripple = (float) (rippleStrength * (1.0 - k));
                }
            }
        }
        float intensity = SenDimsConfig.RITUAL_INTENSITY.get().floatValue();
        warp = Math.min(1.0F, warp * Math.min(1.0F, intensity));
        flash = Math.min(1.0F, flash * intensity);
        ripple *= intensity;
        if (warp <= 0.001F && flash <= 0.001F && ripple <= 0.001F) return;
        try {
            draw(mc, shader, warp, flash, ripple, rippleRadius, centerX, centerY, (float) now);
        } catch (RuntimeException error) {
            failed = true;
            clear();
            SenDims.LOGGER.warn("Ritual screen effect failed; screen effects are disabled for this session", error);
        }
    }

    private static void draw(Minecraft mc, ShaderInstance shader, float warp, float flash, float ripple,
            float rippleRadius, float centerX, float centerY, float time) {
        RenderTarget main = mc.getMainRenderTarget();
        int width = main.width;
        int height = main.height;
        if (copy == null) {
            copy = new TextureTarget(width, height, false, Minecraft.ON_OSX);
            copy.setFilterMode(GL11.GL_LINEAR);
        } else if (copy.width != width || copy.height != height) {
            copy.resize(width, height, Minecraft.ON_OSX);
            copy.setFilterMode(GL11.GL_LINEAR);
        }
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, copy.frameBufferId);
        GlStateManager._glBlitFrameBuffer(0, 0, width, height, 0, 0, width, height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        main.bindWrite(true);

        shader.setSampler("DiffuseSampler", copy.getColorTextureId());
        shader.safeGetUniform("WarpStrength").set(warp);
        shader.safeGetUniform("WarpMode").set((float) warpMode);
        shader.safeGetUniform("Flash").set(flash);
        shader.safeGetUniform("Tint").set(TINT[0], TINT[1], TINT[2]);
        shader.safeGetUniform("RippleCenter").set(centerX, centerY);
        shader.safeGetUniform("RippleRadius").set(rippleRadius);
        shader.safeGetUniform("RippleStrength").set(ripple);
        shader.safeGetUniform("Aspect").set((float) width / Math.max(1, height));
        shader.safeGetUniform("Time").set(time);

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(() -> shader);
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION);
        builder.vertex(-1.0D, -1.0D, 0.0D).endVertex();
        builder.vertex(3.0D, -1.0D, 0.0D).endVertex();
        builder.vertex(-1.0D, 3.0D, 0.0D).endVertex();
        BufferUploader.drawWithShader(builder.end());
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    private static double smooth(double k) {
        k = Math.max(0.0, Math.min(1.0, k));
        return k * k * (3.0 - 2.0 * k);
    }
}
