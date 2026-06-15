package com.tonywww.slashblade_sendims.mixin.integrateddynamics;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import org.cyclops.integrateddynamics.core.item.ItemBlockEnergyContainerAutoSupply;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ItemBlockEnergyContainerAutoSupply.class, remap = false)
public class MixinItemBlockEnergyContainerAutoSupply {

    @Inject(method = "autofill", at = @At("HEAD"), remap = false)
    private static void injectAutofillArmor(IEnergyStorage source, Level world, Entity entity, CallbackInfo ci) {
        // 确保是服务端且实体是玩家
        // Ensure we are on the server side and the entity is a player
        if (entity instanceof Player player && !world.isClientSide()) {

            // 遍历玩家身上的所有护甲槽位
            // Iterate through all armor slots on the player
            for (ItemStack armorItem : player.getArmorSlots()) {
                if (armorItem.isEmpty()) continue;

                // 尝试获取护甲的能量 Capability
                // Try to get the Forge Energy capability of the armor
                armorItem.getCapability(ForgeCapabilities.ENERGY).ifPresent(target -> {

                    // 模拟提取，看看能量源目前还能提供多少能量
                    // Simulate extraction to see how much energy the source can still provide
                    int maxExtractable = source.extractEnergy(Integer.MAX_VALUE, true);

                    if (maxExtractable > 0) {
                        // 模拟护甲接收能量，获取实际能接收的最大值
                        // Simulate the armor receiving energy to get the actual maximum it can receive
                        int maxReceive = target.receiveEnergy(maxExtractable, true);

                        if (maxReceive > 0) {
                            // 实际执行能量转移
                            // Perform the actual energy transfer
                            int extracted = source.extractEnergy(maxReceive, false);
                            target.receiveEnergy(extracted, false);
                        }
                    }
                });
            }
        }
    }
}