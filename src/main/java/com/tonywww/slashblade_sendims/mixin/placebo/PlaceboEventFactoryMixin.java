package com.tonywww.slashblade_sendims.mixin.placebo;

import com.tonywww.slashblade_sendims.compat.placebo.StoredEnchantmentScope;
import dev.shadowsoffire.placebo.events.PlaceboEventFactory;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.common.extensions.IForgeItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Placebo 用 coremod 让 IForgeItemStack#getAllEnchantments / getEnchantmentLevel 经过这里发布
 * GetEnchantmentLevelEvent。处于 {@link StoredEnchantmentScope} 时直接返回事件前的等级，不加临时加成。
 */
@Mixin(value = PlaceboEventFactory.class, remap = false)
public class PlaceboEventFactoryMixin {

    @Inject(method = "getEnchantmentLevel", at = @At("HEAD"), cancellable = true)
    private static void sendims$storedEnchantmentsOnly(Map<Enchantment, Integer> enchantments, IForgeItemStack stack,
                                                       CallbackInfoReturnable<Map<Enchantment, Integer>> cir) {
        if (StoredEnchantmentScope.active()) {
            cir.setReturnValue(new LinkedHashMap<>(enchantments));
        }
    }

    @Inject(method = "getEnchantmentLevelSpecific", at = @At("HEAD"), cancellable = true)
    private static void sendims$storedEnchantmentLevelOnly(int level, IForgeItemStack stack, Enchantment enchantment,
                                                           CallbackInfoReturnable<Integer> cir) {
        if (StoredEnchantmentScope.active()) {
            cir.setReturnValue(level);
        }
    }
}
