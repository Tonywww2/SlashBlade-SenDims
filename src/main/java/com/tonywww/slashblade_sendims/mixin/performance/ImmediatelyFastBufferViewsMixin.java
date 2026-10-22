package com.tonywww.slashblade_sendims.mixin.performance;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.tonywww.slashblade_sendims.client.performance.ClientPerformanceOptions;
import com.tonywww.slashblade_sendims.client.performance.RenderResourceEpoch;
import com.tonywww.slashblade_sendims.performance.FixedBufferViewCache;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Map;
import java.util.Set;

@Pseudo
@Mixin(targets = "net.raphimc.immediatelyfast.feature.core.BatchableBufferSource", remap = false)
public abstract class ImmediatelyFastBufferViewsMixin {
    @Shadow @Final protected Map<RenderType, ?> fallbackBuffers;
    @Unique private final FixedBufferViewCache<BufferBuilder> sdbf$views = new FixedBufferViewCache<>();
    @Unique private long sdbf$epoch = -1;

    @Inject(method = "getBufferBuilder", at = @At("HEAD"), cancellable = true)
    private void sdbf$view(RenderType layer, CallbackInfoReturnable<Set<BufferBuilder>> callback) {
        long epoch = RenderResourceEpoch.current();
        if (epoch != sdbf$epoch || !ClientPerformanceOptions.fixedBufferViews) {
            sdbf$views.clear(); sdbf$epoch = epoch;
        }
        if (!ClientPerformanceOptions.fixedBufferViews || fallbackBuffers.containsKey(layer)) return;
        BufferBuilder fixed = ((BufferSourceAccessor) this).sdbf$fixedBuffers().get(layer);
        if (fixed != null) {
            if (ClientPerformanceOptions.diagnostics) com.tonywww.slashblade_sendims.client.performance.RenderOptimizationCounters.fixedBufferViews++;
            callback.setReturnValue(sdbf$views.view(fixed));
        }
    }

    @Inject(method = "close", at = @At("RETURN"))
    private void sdbf$release(CallbackInfo callback) { sdbf$views.clear(); }
}
