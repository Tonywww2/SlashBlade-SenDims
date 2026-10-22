package com.tonywww.slashblade_sendims.mixin.slashblade;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tonywww.slashblade_sendims.client.YakumanVisualClient;
import com.tonywww.slashblade_sendims.entities.EntityYakumanDrive;
import mods.flammpfeil.slashblade.client.renderer.entity.DriveRenderer;
import mods.flammpfeil.slashblade.client.renderer.util.BladeRenderState;
import mods.flammpfeil.slashblade.entity.EntityDrive;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value=DriveRenderer.class,remap=false)
public abstract class YakumanDriveRendererMixin {
    @Redirect(method="render(Lmods/flammpfeil/slashblade/entity/EntityDrive;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at=@At(value="INVOKE",target="Lmods/flammpfeil/slashblade/client/renderer/util/BladeRenderState;setCol(I)V"),remap=false)
    private void yakumanOpacity(int argb,EntityDrive entity,float yaw,float partialTick,
                                PoseStack pose,MultiBufferSource buffers,int light) {
        if(entity instanceof EntityYakumanDrive)
            argb=(argb&0xffffff)|(Math.round((argb>>>24)*YakumanVisualClient.EFFECT_OPACITY)<<24);
        BladeRenderState.setCol(argb);
    }
}
