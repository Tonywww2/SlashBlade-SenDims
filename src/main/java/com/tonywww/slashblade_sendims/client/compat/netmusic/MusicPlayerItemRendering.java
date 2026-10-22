package com.tonywww.slashblade_sendims.client.compat.netmusic;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/** JEI, inventories and held items all use the same independently baked model. */
public final class MusicPlayerItemRendering {
    private static final ResourceLocation TEXTURE = new ResourceLocation("netmusic", "textures/block/music_player.png");

    private MusicPlayerItemRendering() {
    }

    public static void render(PoseStack poses, MultiBufferSource buffers, int light) {
        Model baked = NetMusicItemModels.MUSIC_PLAYER.model();
        poses.pushPose();
        try {
            applyItemPose(poses, Direction.WEST);
            baked.renderToBuffer(poses, buffers.getBuffer(RenderType.entityTranslucent(TEXTURE)),
                    light, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
        } finally {
            poses.popPose();
        }
    }

    static void applyItemPose(PoseStack poses, Direction direction) {
        // Preserve both MusicPlayerItemRenderer's outer transform and the body
        // transform from MusicPlayerRenderer.renderMusicPlayer, in that order.
        poses.scale(1.3333334f, 1.3333334f, 1.3333334f);
        poses.translate(-0.16666666666666663, 0, -0.16666666666666663);
        poses.scale(.75f, .75f, .75f);
        poses.translate(2.0 / 3.0, 1.5, 2.0 / 3.0);
        poses.mulPose(Axis.YP.rotationDegrees(180 - direction.get2DDataValue() * 90));
        poses.mulPose(Axis.ZP.rotationDegrees(180));
    }
}
