package com.tonywww.slashblade_sendims.mixin.slashblade;

import com.tonywww.slashblade_sendims.registeries.SBSDTags;
import mods.flammpfeil.slashblade.ability.ArrowReflector;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ArrowReflector.class, remap = false)
public class MixinArrowReflector {
    @Inject(method = "doReflect(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private static void cancelDoReflect(Entity arrow, Entity attacker, CallbackInfo ci) {
        if (arrow != null && !arrow.getType().is(SBSDTags.EntityTypes.REFLECT_WHITELIST)) {
            ci.cancel();
        }
    }
}
