package com.tonywww.slashblade_sendims.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.client.leader.LeaderStatusCharacterRenderer;
import com.tonywww.slashblade_sendims.entities.boss.TelegraphKind;
import com.tonywww.slashblade_sendims.entities.boss.TwilightBladeWarden;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 暮刃守卫的占位渲染（外观方案 A）：一块悬浮旋转的终焉城堡符文砖核心，
 * 外圈环绕数把「符文终焉剑」。前摇时剑阵收紧并加速旋转；每进一个阶段多一把剑。
 */
public class TwilightBladeWardenRenderer extends EntityRenderer<TwilightBladeWarden> {
    public static final ResourceLocation SWORD_MODEL = SenDims.prefix("entity/rune_finale_sword");
    private static final ResourceLocation CORE_BLOCK =
            ResourceLocation.fromNamespaceAndPath("twilightforest", "violet_castle_rune_brick");
    private final BlockRenderDispatcher blockRenderer;
    private BlockState coreState;

    public TwilightBladeWardenRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 1.0F;
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(TwilightBladeWarden entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        float time = entity.tickCount + partialTick;
        int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0.0F);
        float scale = entity.deathTime > 0
                ? Math.max(0.0F, 1.0F - (entity.deathTime - 1 + partialTick) / 20.0F)
                : 1.0F;
        boolean telegraph = entity.getTelegraphKind() != TelegraphKind.NONE;

        poseStack.pushPose();
        poseStack.translate(0.0D, 1.4D + Mth.sin(time * 0.08F) * 0.12F, 0.0D);
        poseStack.scale(scale, scale, scale);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 1.2F));
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(time * 0.05F) * 8.0F));
        poseStack.scale(1.1F, 1.1F, 1.1F);
        poseStack.translate(-0.5D, -0.5D, -0.5D);
        this.blockRenderer.renderSingleBlock(this.coreState(), poseStack, buffers, packedLight, overlay);
        poseStack.popPose();

        BakedModel sword = Minecraft.getInstance().getModelManager().getModel(SWORD_MODEL);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        int count = 5 + entity.getPhase();
        float spin = time * (telegraph ? 9.0F : 3.0F);
        float radius = telegraph ? 1.3F : 1.8F;
        for (int i = 0; i < count; i++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(spin + i * 360.0F / count));
            poseStack.translate(radius, Mth.sin(time * 0.1F + i) * 0.15F, 0.0D);
            poseStack.mulPose(Axis.ZP.rotationDegrees(-135.0F));
            poseStack.scale(1.6F, 1.6F, 1.6F);
            poseStack.translate(-0.5D, -0.5D, -0.03125D);
            Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
                    poseStack.last(), consumer, null, sword, 1.0F, 1.0F, 1.0F,
                    LightTexture.FULL_BRIGHT, overlay);
            poseStack.popPose();
        }
        poseStack.popPose();
        // 不是 LivingEntityRenderer，不会触发 RenderLivingEvent，「危」「晕」要在这里自己画。
        LeaderStatusCharacterRenderer.renderStatusCharacter(entity, partialTick, poseStack, buffers);
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    private BlockState coreState() {
        if (this.coreState == null) {
            Block block = ForgeRegistries.BLOCKS.getValue(CORE_BLOCK);
            this.coreState = block == null || block == Blocks.AIR
                    ? Blocks.CRYING_OBSIDIAN.defaultBlockState()
                    : block.defaultBlockState();
        }
        return this.coreState;
    }

    @Override
    public ResourceLocation getTextureLocation(TwilightBladeWarden entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
