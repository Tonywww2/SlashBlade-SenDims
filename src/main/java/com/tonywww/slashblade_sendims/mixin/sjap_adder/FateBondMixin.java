package com.tonywww.slashblade_sendims.mixin.sjap_adder;

import com.dinzeer.legendreliclib.lib.util.FastMakeEntityUtil;
import com.dinzeer.legendreliclib.lib.util.slashblade.AbstractSpecialEffect;
import com.dinzeer.sjapadder.se.slashblade.FateBond;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FateBond.class, remap = false)
public abstract class FateBondMixin extends AbstractSpecialEffect {

    public FateBondMixin(int questionLevel) {
        super(questionLevel);
    }

    @Inject(method = "handleUpdate", at = @At("HEAD"), cancellable = true)
    public void onHandleUpdate(SlashBladeEvent.UpdateEvent event, LivingEntity player, CallbackInfo ci) {
        if (player.tickCount % 20 == 0) {
            if (this.isEffective(player)) {
                FastMakeEntityUtil.FastEffect(player, MobEffects.DAMAGE_BOOST, 2);
                FastMakeEntityUtil.FastEffect(player, MobEffects.REGENERATION, 1);
                FastMakeEntityUtil.FastEffect(player, MobEffects.MOVEMENT_SPEED, 1);
                FastMakeEntityUtil.FastEffect(player, MobEffects.JUMP, 1);
            }

        }
        ci.cancel();
    }
}

