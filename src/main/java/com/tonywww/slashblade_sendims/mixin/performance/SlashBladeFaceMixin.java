package com.tonywww.slashblade_sendims.mixin.performance;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tonywww.slashblade_sendims.client.performance.ClientPerformanceOptions;
import com.tonywww.slashblade_sendims.client.performance.ObjFaceEmitter;
import mods.flammpfeil.slashblade.client.renderer.model.obj.Face;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Face.class, remap = false)
public abstract class SlashBladeFaceMixin {
    @Unique private static final ThreadLocal<ObjFaceEmitter> sdbf$emitter = ThreadLocal.withInitial(ObjFaceEmitter::new);

    @Inject(method = "addFaceForRender(Lcom/mojang/blaze3d/vertex/VertexConsumer;FLorg/joml/Matrix4f;II)V",
            at = @At("HEAD"), cancellable = true)
    private void sdbf$submit(VertexConsumer consumer, float offset, Matrix4f pose, int light, int color, CallbackInfo callback) {
        if (!ClientPerformanceOptions.slashbladeObj) return;
        if (sdbf$emitter.get().render((Face) (Object) this, consumer, offset, pose, light, color)) callback.cancel();
    }
}
