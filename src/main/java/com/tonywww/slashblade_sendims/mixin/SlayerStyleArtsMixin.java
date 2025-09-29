package com.tonywww.slashblade_sendims.mixin;

import com.tonywww.slashblade_sendims.SBSDValues;
import com.tonywww.slashblade_sendims.registeries.SBSDAttributes;
import mods.flammpfeil.slashblade.ability.SlayerStyleArts;
import mods.flammpfeil.slashblade.ability.Untouchable;
import mods.flammpfeil.slashblade.capability.mobeffect.CapabilityMobEffect;
import mods.flammpfeil.slashblade.capability.mobeffect.IMobEffectState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.tracen.umapyoi.api.UmapyoiAPI;
import net.tracen.umapyoi.utils.UmaSoulUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SlayerStyleArts.class)
public class SlayerStyleArtsMixin {

    @Redirect(
            method = "onInputChange(Lmods/flammpfeil/slashblade/event/handler/InputCommandEvent;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;getCapability(Lnet/minecraftforge/common/capabilities/Capability;)Lnet/minecraftforge/common/util/LazyOptional;"
            ),
            remap = false
    )
    private LazyOptional<IMobEffectState> redirectCount(ServerPlayer serverPlayer, Capability<CapabilityMobEffect> capability) {
        if (
                serverPlayer.getMainHandItem().getItem() instanceof ItemSlashBlade
        ) {
            ItemStack soul = UmapyoiAPI.getUmaSoul(serverPlayer);
            if (soul == null || soul.isEmpty()) {
                return LazyOptional.empty();
            }
            int ap = UmaSoulUtils.getActionPoint(soul);
            CompoundTag data = serverPlayer.getPersistentData();
            if (ap < SBSDValues.SPRINT_COST ||
                    (
                            data.contains(SBSDValues.SPRINT_CD_PATH) &&
                                    data.getInt(SBSDValues.SPRINT_CD_PATH) > 0
                    )) {
                return LazyOptional.empty();
            } else {
                // Success
                double scale = SBSDAttributes.getAttributeValue(serverPlayer, SBSDAttributes.SPRINT_CD.get());
                data.putInt(SBSDValues.SPRINT_CD_PATH, (int) (SBSDValues.SPRINT_CD * scale + 0.5d));
                data.putBoolean(SBSDValues.SPRINT_SUCCESSED_PATH, false);
                UmaSoulUtils.addActionPoint(soul, -SBSDValues.SPRINT_COST);
            }
        }

        return serverPlayer.getCapability(CapabilityMobEffect.MOB_EFFECT);
    }

    @Redirect(
            method = "onInputChange(Lmods/flammpfeil/slashblade/event/handler/InputCommandEvent;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lmods/flammpfeil/slashblade/ability/Untouchable;setUntouchable(Lnet/minecraft/world/entity/LivingEntity;I)V"
            ),
            remap = false
    )
    private void redirectUntouchable(LivingEntity entity, int ticks) {
        Untouchable.setUntouchable(entity, SBSDValues.UNTOUCHABLE_TICK);
    }


}