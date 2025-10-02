package com.tonywww.slashblade_sendims.utils;

import com.tonywww.slashblade_sendims.SenDims;
import dev.shadowsoffire.attributeslib.impl.PercentBasedAttribute;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

public final class CuriosUtils {
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, SenDims.MOD_ID);
    public static final RegistryObject<Attribute> SPRINT_CD = ATTRIBUTES.register("sprint_cd",
            () -> new PercentBasedAttribute("attribute.name.sbsd.sprint_cd", 1.0d, 0.6d, 10.0d) {
                @Override
                public MutableComponent toComponent(AttributeModifier modif, TooltipFlag flag) {
                    Attribute attr = this.ths();
                    double value = modif.getAmount();
                    MutableComponent comp;
                    if (value > 0.0) {
                        comp = Component.translatable("attributeslib.modifier.plus",
                                this.toValueComponent(modif.getOperation(), value, flag), Component.translatable(attr.getDescriptionId())).withStyle(ChatFormatting.RED);
                    } else {
                        value *= -1.0;
                        comp = Component.translatable("attributeslib.modifier.take",
                                this.toValueComponent(modif.getOperation(), value, flag), Component.translatable(attr.getDescriptionId())).withStyle(ChatFormatting.BLUE);
                    }

                    return comp.append(this.getDebugInfo(modif, flag));
                }
            }.setSyncable(true));

    /**
     * From MagicHarp/confluence
     */
    public static boolean noSameCurio(LivingEntity living, Predicate<ItemStack> predicate) {
        AtomicBoolean isEmpty = new AtomicBoolean(true);
        CuriosApi.getCuriosInventory(living).ifPresent(handler -> {
            for (ICurioStacksHandler curioStacksHandler : handler.getCurios().values()) {
                IDynamicStackHandler stackHandler = curioStacksHandler.getStacks();
                for (int i = 0; i < stackHandler.getSlots(); i++) {
                    ItemStack stack = stackHandler.getStackInSlot(i);
                    if (!stack.isEmpty() && predicate.test(stack)) {
                        isEmpty.set(false);
                        return;
                    }
                }
            }
        });
        return isEmpty.get();
    }

}