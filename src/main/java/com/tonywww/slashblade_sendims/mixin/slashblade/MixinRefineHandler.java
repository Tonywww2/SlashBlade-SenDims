package com.tonywww.slashblade_sendims.mixin.slashblade;

import mods.flammpfeil.slashblade.SlashBladeConfig;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.capability.slashblade.SlashBladeState;
import mods.flammpfeil.slashblade.event.RefineProgressEvent;
import mods.flammpfeil.slashblade.event.RefineSettlementEvent;
import mods.flammpfeil.slashblade.event.handler.RefineHandler;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AnvilUpdateEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicInteger;

@Mixin(value = RefineHandler.class, remap = false)
public abstract class MixinRefineHandler {

    @ModifyConstant(method = "refineLimitCheck", constant = @Constant(intValue = 10, ordinal = 0))
    private int modifyRefineLimitCheck(int constant) {
        return 5;
    }

    @Unique
    private int slashBlade_SenDims$getRefineProudsoulCount(int level, ISlashBladeState state, RefineSettlementEvent e2) {
        int refineDiff = e2.getRefineResult() - state.getRefine();
        // x ^ 2
        int refineSoul = refineDiff * level * 5;
        // x
        int baseSoul = e2.getMaterialCost() * 75;

        return baseSoul + refineSoul;

    }

    @Inject(
            method = "onAnvilUpdateEvent(Lnet/minecraftforge/event/AnvilUpdateEvent;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void modifyOnAnvilUpdateEvent(AnvilUpdateEvent event, CallbackInfo ci) {
        if (event.getOutput().isEmpty()) {
            ItemStack base = event.getLeft();
            ItemStack material = event.getRight();
            if (!base.isEmpty()) {
                if (base.getCapability(ItemSlashBlade.BLADESTATE).isPresent()) {
                    if (!material.isEmpty()) {
                        boolean isRepairable = base.getItem().isValidRepairItem(base, material);
                        if (isRepairable) {
                            int level = material.getEnchantmentValue();
                            if (level >= 0) {
                                ItemStack result = base.copy();
                                int refineLimit = Math.max(5, level);
                                int materialCost = 0;
                                int levelCostBase = SlashBladeConfig.REFINE_LEVEL_COST.get();
                                int costResult = 0;
                                AtomicInteger refineResult = new AtomicInteger(0);
                                result.getCapability(ItemSlashBlade.BLADESTATE).ifPresent((s) -> refineResult.set(s.getRefine()));

                                while (materialCost < material.getCount()) {
                                    RefineProgressEvent e = new RefineProgressEvent(result, result.getCapability(ItemSlashBlade.BLADESTATE).orElse(new SlashBladeState(result)), materialCost + 1, levelCostBase, costResult, refineResult.get(), event);
                                    MinecraftForge.EVENT_BUS.post(e);
                                    if (e.isCanceled()) {
                                        break;
                                    }

                                    refineResult.set(e.getRefineResult());
                                    materialCost = e.getMaterialCost();
                                    costResult = e.getCostResult() + e.getLevelCost();
                                    if (!event.getPlayer().getAbilities().instabuild && event.getPlayer().experienceLevel < costResult) {
                                        break;
                                    }
                                }

                                if (result.getCapability(ItemSlashBlade.BLADESTATE).isPresent()) {
                                    ISlashBladeState state = result.getCapability(ItemSlashBlade.BLADESTATE).resolve().orElse(new SlashBladeState(result));
                                    RefineSettlementEvent e2 = new RefineSettlementEvent(result, state, materialCost, costResult, refineResult.get(), event);
                                    MinecraftForge.EVENT_BUS.post(e2);
                                    if (e2.isCanceled()) {
                                        return;
                                    }

                                    materialCost = e2.getMaterialCost();
                                    costResult = e2.getCostResult();
                                    if (state.getRefine() <= refineLimit) {
                                        if (state.getRefine() + e2.getRefineResult() < 200) {
                                            state.setMaxDamage(state.getMaxDamage() + e2.getRefineResult());
                                        } else if (state.getRefine() < 200) {
                                            state.setMaxDamage(state.getMaxDamage() + Math.min(state.getRefine() + e2.getRefineResult(), 200) - state.getRefine());
                                        }

                                        state.setRefine(e2.getRefineResult());
                                    }
                                    state.setProudSoulCount(state.getProudSoulCount() + slashBlade_SenDims$getRefineProudsoulCount(level, state, e2));

                                    result.setDamageValue(result.getDamageValue() - Math.max(result.getDamageValue(), materialCost * Math.max(1, level / 2)));
                                    result.getOrCreateTag().put("bladeState", state.serializeNBT());
                                }

                                event.setMaterialCost(materialCost);
                                event.setCost(costResult);
                                event.setOutput(result);
                            }
                        }
                    }
                }
            }
        }
        ci.cancel();
    }
}

