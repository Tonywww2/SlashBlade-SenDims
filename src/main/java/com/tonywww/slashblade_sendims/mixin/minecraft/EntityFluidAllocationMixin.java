package com.tonywww.slashblade_sendims.mixin.minecraft;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keep Forge's full fluid scan/physics, but don't reserve space for every registered fluid type. */
@Mixin(Entity.class)
public abstract class EntityFluidAllocationMixin {
    @Redirect(method = "updateFluidHeightAndDoFluidPushing(Ljava/util/function/Predicate;)V", remap = false,
            at = @At(value = "NEW", target = "(I)Lit/unimi/dsi/fastutil/objects/Object2ObjectArrayMap;", remap = false))
    private Object2ObjectArrayMap<?, ?> sdbf$lazyFluidMap(int registeredFluidTypes) {
        // The default map has shared empty arrays and grows only when an intersecting fluid is found.
        // This is safe on both logical sides and retains insertion order and all fluid predicates.
        return new Object2ObjectArrayMap<>();
    }
}
