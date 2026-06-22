package com.tonywww.slashblade_sendims.mixin.sbtetra;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import mods.flammpfeil.slashblade.SlashBladeConfig;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.SlashBladeItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;
import net.yiran.sbtetra.Config;
import net.yiran.sbtetra.module.schematic.EnchantedSoulExtractionSchematic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Objects;

@Mixin(value = EnchantedSoulExtractionSchematic.class, remap = false)
public abstract class MixinEnchantedSoulExtractionSchematic {

    @Inject(
            method = "isApplicableForSlot(Ljava/lang/String;Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At("RETURN"),
            cancellable = true,
            remap = false
    )
    private void slashblade_sendims$requireProudSoul(String slot, ItemStack targetStack, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            return;
        }

        ISlashBladeState state = targetStack.getCapability(ItemSlashBlade.BLADESTATE).orElse(null);
        if (state == null) {
            cir.setReturnValue(false);
            return;
        }

        cir.setReturnValue(state.getProudSoulCount() >= Config.Server.SoulDropNeeded.get());
    }

    @Inject(
            method = "applyUpgrade(Lnet/minecraft/world/item/ItemStack;[Lnet/minecraft/world/item/ItemStack;ZLjava/lang/String;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void slashblade_sendims$consumeProudSoulOnApply(ItemStack itemStack, ItemStack[] itemStacks, boolean isApply, String soul, Player player, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack newStack = itemStack.copy();
        newStack.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(bladeState -> {
            int enchantedSoulNeed = Config.Server.EnchantedSoulDropNeeded.get();
            int proudSoulNeed = Config.Server.SoulDropNeeded.get();
            if (enchantedSoulNeed <= 0 || proudSoulNeed <= 0) {
                return;
            }

            int killCount = Math.max(0, bladeState.getKillCount() - 1000);
            int count = Math.min(killCount / enchantedSoulNeed, bladeState.getProudSoulCount() / proudSoulNeed);
            if (itemStacks[0].is(EnchantedSoulExtractionSchematic.TAG)) {
                itemStacks[0].shrink(1);
            } else {
                count = Math.min(count, Config.Server.MaxEnchantedSoulDrop.get());
            }

            if (isApply) {
                List<Enchantment> enchantments = ForgeRegistries.ENCHANTMENTS.getValues().stream()
                        .filter(newStack::canApplyAtEnchantingTable)
                        .filter(enchantment -> !SlashBladeConfig.NON_DROPPABLE_ENCHANTMENT.get().contains(Objects.requireNonNull(ForgeRegistries.ENCHANTMENTS.getKey(enchantment)).toString()))
                        .toList();
                Object2IntOpenHashMap<Enchantment> enchantmentMap = new Object2IntOpenHashMap<>();
                for (int i = 0; i < count; i++) {
                    Enchantment enchantment = enchantments.get(player.getRandom().nextInt(0, enchantments.size()));
                    if (enchantment != null) {
                        enchantmentMap.addTo(enchantment, 1);
                    }
                }

                enchantmentMap.forEach((enchantment, enchantmentCount) -> {
                    ItemStack enchantedSoul = new ItemStack(SlashBladeItems.PROUDSOUL_TINY.get());
                    enchantedSoul.enchant(enchantment, 1);
                    enchantedSoul.setCount(enchantmentCount);
                    while (player.getInventory().add(enchantedSoul)) {
                    }
                    player.drop(enchantedSoul, false);
                });
            }

            bladeState.setKillCount(bladeState.getKillCount() - enchantedSoulNeed * count);
            bladeState.setProudSoulCount(Math.max(0, bladeState.getProudSoulCount() - proudSoulNeed * count));
        });

        cir.setReturnValue(newStack);
    }
}
