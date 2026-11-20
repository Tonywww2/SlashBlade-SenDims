package com.tonywww.slashblade_sendims.client.zenith;

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
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

/**
 * 天顶·裂空刀身上的发光特效：刀身裂缝、刀镡发光缝、刀鞘裂缝都由着色器 {@code zenith_rift_blade} 实时绘制，不用贴图。
 * <p>
 * 模型的 {@code *_luminous} 分组 UV 指向一张 128×4096 的画布。每帧开头（世界绘制之外）用着色器把画布画一遍，
 * 拔刀剑取发光材质时把这把刀的贴图换成画布（{@code BladeRenderStateRiftMixin}），
 * 于是手持、腰间、物品栏、掉落物都按普通纹理采样到同一帧动画，开着光影包时也由光影包照常接管。
 * 最近 3 秒内没有画过这把刀时不烘焙。
 */
@Mod.EventBusSubscriber(modid = SenDims.MOD_ID, value = Dist.CLIENT)
public final class ZenithRiftBlade {
    /** 天顶·裂空模型的贴图；只有它的发光分组会被换成画布。 */
    public static final ResourceLocation ATLAS = SenDims.prefix("model/zenith_final/zenith_final.png");
    public static final ResourceLocation CANVAS = SenDims.prefix("dynamic/zenith_rift/blade");
    private static final int WIDTH = 128;
    private static final int HEIGHT = 4096;
    private static final long IDLE_MS = 3000L;
    /** 关闭动画时烘焙的那一帧。 */
    private static final float STILL_TIME = 12.0F;

    private static TextureTarget target;
    private static boolean registered;
    private static boolean failed;
    private static boolean stillBaked;
    private static long lastUse = Long.MIN_VALUE / 2;

    private ZenithRiftBlade() {
    }

    /** 拔刀剑取发光材质时调用（渲染线程）：这把刀的贴图换成画布，其他刀原样返回。 */
    public static ResourceLocation luminous(ResourceLocation texture) {
        if (!ATLAS.equals(texture)) return texture;
        lastUse = Util.getMillis();
        if (!registered) {
            registered = true;
            Minecraft.getInstance().getTextureManager().register(CANVAS, new CanvasTexture());
        }
        return CANVAS;
    }

    @SubscribeEvent
    public static void bake(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !registered || failed) return;
        if (Util.getMillis() - lastUse > IDLE_MS) return;
        ShaderInstance shader = ZenithRiftShaders.blade;
        if (shader == null) return;
        boolean animated = SenDimsConfig.ZENITH_BLADE_ANIMATION.get();
        if (!animated && stillBaked) return;
        try {
            if (target == null) create();
            float time = animated ? (Util.getMillis() % 3_600_000L) / 1000.0F : STILL_TIME;
            shader.safeGetUniform("Time").set(time);
            target.bindWrite(true);
            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(() -> shader);
            BufferBuilder builder = Tesselator.getInstance().getBuilder();
            builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION);
            builder.vertex(-1.0D, -1.0D, 0.0D).endVertex();
            builder.vertex(3.0D, -1.0D, 0.0D).endVertex();
            builder.vertex(-1.0D, 3.0D, 0.0D).endVertex();
            BufferUploader.drawWithShader(builder.end());
            GlStateManager._bindTexture(target.getColorTextureId());
            GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
            GlStateManager._bindTexture(0);
            stillBaked = !animated;
        } catch (RuntimeException error) {
            failed = true;
            SenDims.LOGGER.warn("Zenith Rift blade canvas failed; its glow stays dark for this session", error);
        } finally {
            Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
        }
    }

    /** 着色器重新加载（F3+T）后重画一次静止帧。 */
    static void shadersReloaded() {
        stillBaked = false;
        failed = false;
    }

    private static void create() {
        target = new TextureTarget(WIDTH, HEIGHT, false, Minecraft.ON_OSX);
        target.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        target.clear(Minecraft.ON_OSX);
        GlStateManager._bindTexture(target.getColorTextureId());
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 4);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
        GlStateManager._bindTexture(0);
    }

    /** 把画布交给纹理管理器；过滤方式由画布自己定（线性加 mipmap），忽略材质的设置。还没画过时是 0（采样为黑，叠加后不显示）。 */
    private static final class CanvasTexture extends AbstractTexture {
        @Override
        public void load(ResourceManager manager) {
        }

        @Override
        public int getId() {
            return target == null ? 0 : target.getColorTextureId();
        }

        @Override
        public void setFilter(boolean blur, boolean mipmap) {
        }

        @Override
        public void releaseId() {
        }
    }
}
