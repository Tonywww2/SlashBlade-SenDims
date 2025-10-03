package com.tonywww.slashblade_sendims.mixin;

import com.tonywww.slashblade_sendims.SBSDValues;
import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.registeries.SBSDAttributes;
import mods.flammpfeil.slashblade.ability.Untouchable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
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
                if (other == null || other == self) return;

                ItemStack soul = UmapyoiAPI.getUmaSoul(serverPlayer);
                if (soul == null || soul.isEmpty()) return;

                CompoundTag persistentData = serverPlayer.getPersistentData();
                if (persistentData.contains(SBSDValues.SPRINT_SUCCESSED_PATH) &&
                        persistentData.getBoolean(SBSDValues.SPRINT_SUCCESSED_PATH)) return;

                int gain = SBSDValues.SPRINT_SUCCESS_AP;
                AttributeInstance attributeInstance = serverPlayer.getAttribute(SBSDAttributes.AP_GAIN_PERSENTAGE.get());
                if (attributeInstance != null) gain = (int) (gain * attributeInstance.getValue());
                UmaSoulUtils.addActionPoint(soul, gain);
                persistentData.putBoolean(SBSDValues.SPRINT_SUCCESSED_PATH, true);
                double scale = SBSDAttributes.getAttributeValue(serverPlayer, SBSDAttributes.SPRINT_CD_RETURN.get());
                persistentData.putInt(SBSDValues.SPRINT_CD_PATH, (int) (persistentData.getInt(SBSDValues.SPRINT_CD_PATH) * scale));

                SBSDValues.doSprintSuccessIndicators(serverPlayer, 4);
                SenDims.serverScheduler.schedule(1, () -> SBSDValues.doSprintSuccessIndicators(serverPlayer, 4));
                SenDims.serverScheduler.schedule(2, () -> SBSDValues.doSprintSuccessIndicators(serverPlayer, 2));
                SenDims.serverScheduler.schedule(4, () -> SBSDValues.doSprintSuccessIndicators(serverPlayer, 1));

            }
        }
    }


}
