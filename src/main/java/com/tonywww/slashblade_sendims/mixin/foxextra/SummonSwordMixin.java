package com.tonywww.slashblade_sendims.mixin.foxextra;

import com.dinzeer.foxextra.se.SummonSword;
import com.dinzeer.legendreliclib.lib.compat.slashblade.SwordRainGenerator;
import com.dinzeer.legendreliclib.lib.compat.slashblade.entity.swordrain.BaseSwordRainEntity;
import com.dinzeer.legendreliclib.lib.util.slashblade.AbstractSpecialEffect;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = SummonSword.class, remap = false)
public abstract class SummonSwordMixin extends AbstractSpecialEffect {

    public SummonSwordMixin(int requestLevel) {
        super(requestLevel);
    }

    @Inject(method = "handleDoSlash", at = @At("HEAD"), cancellable = true, remap = false)
    private void sbs$generateTwoPointSwordRain(SlashBladeEvent.DoSlashEvent event, LivingEntity entity, CallbackInfo ci) {
        if (this.isEffective(entity)) {
            List<BaseSwordRainEntity> swords = SwordRainGenerator.generateFivePointSwordRain(event.getUser(), event.getUser().level(), 5);
            int count = 0;
            for (BaseSwordRainEntity sword : swords) {
                count++;
                sword.setColor(0xFFFFFF);
                sword.setDelay(entity.getRandom().nextInt(10));
                if (count > 2) {
                    sword.discard();
                }
            }
        }
        ci.cancel();
    }
}
