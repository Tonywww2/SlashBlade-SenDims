package com.tonywww.slashblade_sendims.mixin.foxextra;

import com.dinzeer.foxextra.regsiter.FEXcomboRegsitry;
import mods.flammpfeil.slashblade.slasharts.Drive;
import mods.flammpfeil.slashblade.util.AttackManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FEXcomboRegsitry.class, remap = false)
public class FEXComboRegistryMixin {

    @Inject(method = "lambda$static$10", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sbs$changeSakuraEndLeftSlashDamage(LivingEntity entity, CallbackInfo ci) {
        AttackManager.doSlash(entity, 22.5F, Vec3.ZERO, false, false, 1.0D);
        ci.cancel();
    }

    @Inject(method = "lambda$static$14", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sbs$changeSakuraEndRightSlashDamage(LivingEntity entity, CallbackInfo ci) {
        AttackManager.doSlash(entity, 157.5F, Vec3.ZERO, false, true, 2.0D);
        ci.cancel();
    }

    @Inject(method = "lambda$static$21", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sbs$changeComboB1EndFirstSlashDamage(LivingEntity entity, CallbackInfo ci) {
        Drive.doSlash(entity, 180.0F, 10, Vec3.ZERO, false, 1.0D, 8.0F);
        ci.cancel();
    }

    @Inject(method = "lambda$static$19", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sbs$changeComboB1EndSecondSlashDamage(LivingEntity entity, CallbackInfo ci) {
        AttackManager.doSlash(entity, 0.0F, new Vec3(entity.getRandom().nextFloat() - 0.5F, 0.8d, 0.0D), false, true, 1.5D);
        ci.cancel();
    }

    @Inject(method = "lambda$static$20", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sbs$changeComboB1EndThirdSlashDamage(LivingEntity entity, CallbackInfo ci) {
        AttackManager.doSlash(entity, 5.0F, new Vec3(entity.getRandom().nextFloat() - 0.5F, 0.8d, 0.0D), true, false, 1.5D);
        ci.cancel();
    }
}
