package com.tonywww.slashblade_sendims.mixin;

import com.tonywww.slashblade_sendims.utils.SBSDValues;
import mods.flammpfeil.slashblade.ability.Untouchable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tracen.umapyoi.api.UmapyoiAPI;
import net.tracen.umapyoi.utils.UmaSoulUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Untouchable.class)
public class UntouchableMixin {

    @Inject(method = "doUntouchable(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/Entity;)Z", at = @At("RETURN"), remap = false)
    private void injectDoUntouchable(LivingEntity self, Entity other, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            if (self instanceof ServerPlayer serverPlayer) {
                ItemStack soul = UmapyoiAPI.getUmaSoul(serverPlayer);
                if (soul == null || soul.isEmpty()) return;
                serverPlayer.playNotifySound(SoundEvents.FIREWORK_ROCKET_LAUNCH, serverPlayer.getSoundSource(), 10.0f, 1.05f);
                serverPlayer.serverLevel().sendParticles(ParticleTypes.END_ROD,
                        serverPlayer.getX(), serverPlayer.getY() + 0.5d, serverPlayer.getZ(),
                        12, 0.1d, 0.25d, 0.1d, 0.01d);

                CompoundTag persistentData = serverPlayer.getPersistentData();
                if (!persistentData.contains(SBSDValues.SPRINT_SUCCESSED_PATH) || !persistentData.getBoolean(SBSDValues.SPRINT_SUCCESSED_PATH)) {
                    UmaSoulUtils.addActionPoint(soul, SBSDValues.SPRINT_SUCCESS_AP);
                    persistentData.putBoolean(SBSDValues.SPRINT_SUCCESSED_PATH, true);

                }

            }
        }
    }

}
