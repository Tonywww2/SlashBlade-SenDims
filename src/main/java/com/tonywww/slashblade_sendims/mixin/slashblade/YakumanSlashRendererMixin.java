package com.tonywww.slashblade_sendims.mixin.slashblade;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tonywww.slashblade_sendims.client.YakumanVisualClient;
import com.tonywww.slashblade_sendims.yakuman.YakumanArts;
import mods.flammpfeil.slashblade.client.renderer.entity.SlashEffectRenderer;
import mods.flammpfeil.slashblade.client.renderer.util.BladeRenderState;
import mods.flammpfeil.slashblade.entity.EntitySlashEffect;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Apply the series opacity to every native slash layer, preserving its own fade and RGB. */
@Mixin(value=SlashEffectRenderer.class,remap=false)
public abstract class YakumanSlashRendererMixin {
    @Redirect(method="render(Lmods/flammpfeil/slashblade/entity/EntitySlashEffect;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at=@At(value="INVOKE",target="Lmods/flammpfeil/slashblade/client/renderer/util/BladeRenderState;setCol(I)V"),remap=false)
    private void yakumanOpacity(int argb,EntitySlashEffect entity,float yaw,float partialTick,
                                PoseStack pose,MultiBufferSource buffers,int light) {
        if((entity.getColor()&0xff000000)==YakumanArts.NATIVE_COLOR_MARKER) {
            int alpha=Math.round((argb>>>24)*YakumanVisualClient.EFFECT_OPACITY);
            argb=(argb&0xffffff)|(alpha<<24);
        }
        BladeRenderState.setCol(argb);
    }
}
