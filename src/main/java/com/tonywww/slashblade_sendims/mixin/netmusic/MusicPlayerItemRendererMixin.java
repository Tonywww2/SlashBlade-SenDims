package com.tonywww.slashblade_sendims.mixin.netmusic;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tonywww.slashblade_sendims.client.compat.netmusic.MusicPlayerItemRendering;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.tartaricacid.netmusic.client.renderer.MusicPlayerItemRenderer", remap = false)
public abstract class MusicPlayerItemRendererMixin {
    @Inject(method = {
            "renderByItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            "m_108829_(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V"
    }, at = @At("HEAD"), cancellable = true, remap = false)
    private void sendims$renderIndependentItemModel(ItemStack stack, ItemDisplayContext context,
                                                   PoseStack poses, MultiBufferSource buffers,
                                                   int light, int overlay, CallbackInfo ci) {
        MusicPlayerItemRendering.render(poses, buffers, light);
        ci.cancel();
    }
}
