package com.tonywww.slashblade_sendims.client.compat.netmusic;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/** Item rendering must not depend on a block renderer having been constructed. */
public final class BigMegaphoneItemRendering {
    private static final ResourceLocation TEXTURE = new ResourceLocation("netmusic", "textures/block/big_megaphone_off.png");

    private BigMegaphoneItemRendering() {
    }

    public static void render(PoseStack poses, MultiBufferSource buffers, int light) {
        Model baked = NetMusicItemModels.BIG_MEGAPHONE.model();
        poses.pushPose();
        try {
            applyBodyPose(poses, Direction.WEST);
            baked.renderToBuffer(poses, buffers.getBuffer(RenderType.entityTranslucent(TEXTURE)),
                    light, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
        } finally {
            poses.popPose();
        }
    }

    static void applyBodyPose(PoseStack poses, Direction direction) {
        // Matches BigMegaphoneRenderer.renderBody, including its item orientation.
        poses.translate(0.5, 1.5, 0.5);
        poses.mulPose(Axis.YP.rotationDegrees(direction.get2DDataValue() * 90));
        poses.mulPose(Axis.ZP.rotationDegrees(180));
    }
}
