package com.tonywww.slashblade_sendims.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tonywww.slashblade_sendims.entities.EntityYakumanDrive;
import mods.flammpfeil.slashblade.client.renderer.entity.DriveRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** Retain the native phantom-blade mesh/shader and scale it for each existing SA. */
public final class YakumanDriveRenderer extends DriveRenderer<EntityYakumanDrive> {
    public YakumanDriveRenderer(EntityRendererProvider.Context context) {super(context);}
    @Override public void render(EntityYakumanDrive entity,float yaw,float partialTick,
                                 PoseStack pose,MultiBufferSource buffers,int light) {
        pose.pushPose();
        float size=entity.getBaseSize();
        var direction=entity.getDeltaMovement().normalize();
        float center=size*EntityYakumanDrive.MODEL_CENTER_DEPTH;
        pose.translate(-direction.x*center,-direction.y*center,-direction.z*center);
        pose.scale(size,size*entity.getHeightScale(),size);
        super.render(entity,yaw,partialTick,pose,buffers,light);
        pose.popPose();
    }
}
