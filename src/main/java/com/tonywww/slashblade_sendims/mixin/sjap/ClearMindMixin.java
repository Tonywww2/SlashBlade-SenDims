package com.tonywww.slashblade_sendims.mixin.sjap;

import com.dinzeer.legendreliclib.lib.util.slashblade.AbstractSpecialEffect;
import com.dinzeer.sjapadder.register.SjaStacksReg;
import com.dinzeer.sjapadder.se.slashblade.ClearMind;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClearMind.class, remap = false)
public abstract class ClearMindMixin extends AbstractSpecialEffect {

    public ClearMindMixin(int questionLevel) {
        super(questionLevel);
    }

    @Inject(method = "handleLivingDeath", at = @At("HEAD"), cancellable = true)
    public void onHandleLivingDeath(LivingDeathEvent event, LivingEntity attacker, CallbackInfo ci) {
        LivingEntity player = event.getEntity();
        if (this.isEffective(attacker)) {
            if (SjaStacksReg.ClearMindCD.getCurrentStacks(player) <= 0) {
                event.setCanceled(true);
                player.setHealth(player.getMaxHealth() * 0.15f);
                if (player instanceof Player truePlayer) {
                    truePlayer.getFoodData().setFoodLevel(20);
                    truePlayer.getFoodData().setSaturation(20.0F);
                }

                player.removeAllEffects();
                player.clearFire();
                SjaStacksReg.ClearMindCD.addStacks(player, SjaStacksReg.ClearMindCD.getMaxStacks() * 2);
            }

        }
        ci.cancel();
    }
}

