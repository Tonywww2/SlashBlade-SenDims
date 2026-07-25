package com.tonywww.slashblade_sendims.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tonywww.slashblade_sendims.entities.EntityChaoticJudgementCut;
import com.tonywww.slashblade_sendims.utils.ChaoticSlashArtEffects;
import mods.flammpfeil.slashblade.client.renderer.entity.JudgementCutRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class ChaoticJudgementCutRenderer extends JudgementCutRenderer<EntityChaoticJudgementCut> {
    public ChaoticJudgementCutRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(
            EntityChaoticJudgementCut entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        poseStack.pushPose();
        try {
            poseStack.scale(
                    ChaoticSlashArtEffects.VISUAL_SCALE,
                    ChaoticSlashArtEffects.VISUAL_SCALE,
                    ChaoticSlashArtEffects.VISUAL_SCALE
            );
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        } finally {
            poseStack.popPose();
        }
    }
}