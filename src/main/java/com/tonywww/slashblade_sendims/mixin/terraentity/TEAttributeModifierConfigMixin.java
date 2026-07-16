package com.tonywww.slashblade_sendims.mixin.terraentity;

import net.minecraft.world.entity.LivingEntity;
import org.confluence.terraentity.config.TEAttributeModifierConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TEAttributeModifierConfig.class, remap = false)
public class TEAttributeModifierConfigMixin {
    @Inject(
            method = "modify(Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At("HEAD"),
            remap = false,
            cancellable = true)
    private void sdbf$disableAttributeModifierConfig(LivingEntity mob, CallbackInfo ci) {
        ci.cancel();
    }
}

