package com.tonywww.slashblade_sendims.mixin.minecraft;

import com.tonywww.slashblade_sendims.client.performance.RenderResourceEpoch;
import com.tonywww.slashblade_sendims.performance.ModelTopologyAccess;
import com.tonywww.slashblade_sendims.performance.TopologySnapshot;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Mixin(value = ModelPart.class, priority = 900)
public abstract class ModelPartTraversalMixin implements ModelTopologyAccess {
    @Shadow @Final private Map<String, ModelPart> children;
    @Unique private static final Function<ModelPart, Map<?, ModelPart>> sdbf$children =
            part -> ((ModelTopologyAccess) (Object) part).sdbf$getChildren();
    @Unique private static final Predicate<ModelPart> sdbf$vanilla = part -> part.getClass() == ModelPart.class;
    @Unique private TopologySnapshot<ModelPart> sdbf$topology;
    @Unique private boolean sdbf$dynamic;
    @Unique private long sdbf$epoch = -1;

    @Override
    public Map<String, ModelPart> sdbf$getChildren() {
        return children;
    }

    @Inject(method = "getAllParts", at = @At("HEAD"), cancellable = true)
    private void sdbf$traverse(CallbackInfoReturnable<Stream<ModelPart>> callback) {
        long epoch = RenderResourceEpoch.current();
        if (epoch != sdbf$epoch) {
            sdbf$epoch = epoch;
            sdbf$topology = null;
            sdbf$dynamic = false;
        }
        if (sdbf$dynamic) {
            return;
        }
        if (sdbf$topology != null && !sdbf$topology.matches(sdbf$children)) {
            sdbf$topology = null;
            sdbf$dynamic = true;
            return;
        }
        if (sdbf$topology == null) {
            sdbf$topology = TopologySnapshot.capture((ModelPart) (Object) this, sdbf$children, sdbf$vanilla);
            if (sdbf$topology == null) {
                sdbf$dynamic = true;
                return;
            }
        }
        callback.setReturnValue(sdbf$topology.stream());
    }
}
