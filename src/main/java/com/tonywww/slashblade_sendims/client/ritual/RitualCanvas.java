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
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

/**
 * 离屏画布：每帧开头用自写着色器把当前帧的图案画进来，世界里再用原版材质把它当普通纹理贴到面片上。
 * 纹理以 {@link #location} 注册进纹理管理器，原版材质（以及光影包接管后的程序）按普通纹理采样。
 */
final class RitualCanvas {
    final ResourceLocation location;
    final RenderType glow;
    private final boolean wrapU;
    private TextureTarget target;
    private int width;
    private int height;

    RitualCanvas(String name, int width, int height, boolean wrapU) {
        this.location = SenDims.prefix("dynamic/ritual/" + name);
        this.wrapU = wrapU;
        this.width = width;
        this.height = height;
        Minecraft.getInstance().getTextureManager().register(location, new CanvasTexture(this));
        this.glow = RitualMaterials.glow(location);
    }

    int textureId() {
        return target == null ? 0 : target.getColorTextureId();
    }

    void resize(int width, int height) {
        if (this.width == width && this.height == height) return;
        this.width = width;
        this.height = height;
        destroy();
    }

    /** 用 shader 画满整张画布（全屏三角形），之后生成 mipmap。调用前设置好 uniform。 */
    void bake(ShaderInstance shader) {
        RenderSystem.assertOnRenderThread();
        if (target == null) create();
        target.bindWrite(true);
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
    }

    private void create() {
        target = new TextureTarget(width, height, false, Minecraft.ON_OSX);
        target.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        target.clear(Minecraft.ON_OSX);
        GlStateManager._bindTexture(target.getColorTextureId());
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 4);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, wrapU ? GL11.GL_REPEAT : GL12.GL_CLAMP_TO_EDGE);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
        GlStateManager._bindTexture(0);
    }

    void destroy() {
        if (target != null) {
            target.destroyBuffers();
            target = null;
        }
    }

    static void restoreMainTarget() {
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        main.bindWrite(true);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }

    /** 把画布的颜色纹理交给纹理管理器；过滤方式由画布自己定（线性加 mipmap），忽略材质的设置。 */
    private static final class CanvasTexture extends AbstractTexture {
        private final RitualCanvas canvas;

        CanvasTexture(RitualCanvas canvas) {
            this.canvas = canvas;
        }

        @Override
        public void load(ResourceManager manager) {
        }

        @Override
        public int getId() {
            return canvas.textureId();
        }

        @Override
        public void setFilter(boolean blur, boolean mipmap) {
        }

        @Override
        public void releaseId() {
        }
    }
}
