package com.tonywww.slashblade_sendims.mixin.netherman;

import com.benji.netherman.entity.AzazelEntity;
import com.github.L_Ender.cataclysm.init.ModEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AzazelEntity.class, remap = false)
public abstract class AzazelEntityMixin {

    @Inject(method = "turnRandomItemToGold", at = @At("HEAD"), cancellable = true, remap = false)
    private void slashblade_sendims$replaceMidasConversionWithStun(Player player, CallbackInfo ci) {
        player.addEffect(
                new MobEffectInstance(ModEffect.EFFECTSTUN.get(), 60, 0),
                (AzazelEntity) (Object) this
        );
        ci.cancel();
    }
}