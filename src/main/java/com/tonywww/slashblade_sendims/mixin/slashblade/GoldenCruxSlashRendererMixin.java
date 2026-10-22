package com.tonywww.slashblade_sendims.mixin.slashblade;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tonywww.slashblade_sendims.sa.GoldenCrux;
import mods.flammpfeil.slashblade.client.renderer.entity.SlashEffectRenderer;
import mods.flammpfeil.slashblade.entity.EntitySlashEffect;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** EX keeps native damage entities, while the shared client renderer draws their crown. */
@Mixin(value = SlashEffectRenderer.class, remap = false)
public abstract class GoldenCruxSlashRendererMixin {
    @Inject(method = "render(Lmods/flammpfeil/slashblade/entity/EntitySlashEffect;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void goldenCruxCrownOnly(EntitySlashEffect entity, float yaw, float partialTick,
            PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
        if ((entity.getColor() & 0xff000000) == GoldenCrux.HIDDEN_SLASH_MARKER) ci.cancel();
    }
}
