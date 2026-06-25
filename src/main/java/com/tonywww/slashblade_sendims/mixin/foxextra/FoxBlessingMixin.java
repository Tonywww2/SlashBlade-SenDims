package com.tonywww.slashblade_sendims.mixin.foxextra;

import com.dinzeer.foxextra.se.FoxBlessing;
import com.dinzeer.legendreliclib.lib.util.slashblade.AbstractSpecialEffect;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FoxBlessing.class, remap = false)
public abstract class FoxBlessingMixin extends AbstractSpecialEffect {

    public FoxBlessingMixin(int requestLevel) {
        super(requestLevel);
    }

    @Inject(method = "handleUpdate", at = @At("HEAD"), cancellable = true, remap = false)
    private void sbs$changeBlessingEffects(SlashBladeEvent.UpdateEvent event, LivingEntity entity, CallbackInfo ci) {
        if (this.isEffective(entity)) {
            entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 1));
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1));
            entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 1));
        }
        ci.cancel();
    }
}
