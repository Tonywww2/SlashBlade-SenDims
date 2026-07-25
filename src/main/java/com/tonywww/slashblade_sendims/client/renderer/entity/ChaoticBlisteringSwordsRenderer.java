package com.tonywww.slashblade_sendims.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tonywww.slashblade_sendims.entities.EntityChaoticBlisteringSwords;
import com.tonywww.slashblade_sendims.utils.ChaoticSlashArtEffects;
import mods.flammpfeil.slashblade.client.renderer.entity.SummonedSwordRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class ChaoticBlisteringSwordsRenderer extends SummonedSwordRenderer<EntityChaoticBlisteringSwords> {
    public ChaoticBlisteringSwordsRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(
            EntityChaoticBlisteringSwords entity,
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