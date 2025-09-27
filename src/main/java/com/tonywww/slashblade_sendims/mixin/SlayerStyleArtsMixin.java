package com.tonywww.slashblade_sendims.mixin;

import mods.flammpfeil.slashblade.ability.SlayerStyleArts;
import mods.flammpfeil.slashblade.capability.mobeffect.CapabilityMobEffect;
import mods.flammpfeil.slashblade.capability.mobeffect.IMobEffectState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.InputCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.tracen.umapyoi.api.UmapyoiAPI;
import net.tracen.umapyoi.utils.UmaSoulUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SlayerStyleArts.class)
public class SlayerStyleArtsMixin {

    @Unique
    private static final int SPRINT_COST = 200;

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
            if (ap < SPRINT_COST) {
                return LazyOptional.empty();
            } else {
                UmaSoulUtils.addActionPoint(soul, -SPRINT_COST);
            }
        }

        return serverPlayer.getCapability(CapabilityMobEffect.MOB_EFFECT);
    }

}