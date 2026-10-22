package com.tonywww.slashblade_sendims.mixin.sjap_adder;

import com.dinzeer.sjapadder.se.nihil.EndSakuraRiseUp;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = EndSakuraRiseUp.class, remap = false)
public class EndSakuraRiseUpMixin {

    @ModifyArg(method = "handleDoSlash", at = @At(value = "INVOKE", target = "Lcom/dinzeer/legendreliclib/lib/compat/slashblade/SlashEffect$SakuraEnd;doSlash(Lnet/minecraft/world/entity/LivingEntity;FLnet/minecraft/world/phys/Vec3;ZZD)Lmods/flammpfeil/slashblade/entity/EntitySlashEffect;"), index = 5, require = 2)
    private double onGetSlashDamage(double damage) {
        return 0.5D;
    }
}
