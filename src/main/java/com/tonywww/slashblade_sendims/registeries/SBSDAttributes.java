package com.tonywww.slashblade_sendims.registeries;

import com.tonywww.slashblade_sendims.SenDims;
import dev.shadowsoffire.attributeslib.impl.PercentBasedAttribute;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class SBSDAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, SenDims.MOD_ID);

    public static final RegistryObject<Attribute> SPRINT_CD = ATTRIBUTES.register("sprint_cd",
            () -> new PercentBasedAttribute("attribute.name.sbsd.sprint_cd", 1.0d, 0.5d, 10.0d) {
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

    public static final RegistryObject<Attribute> SPRINT_CD_RETURN = ATTRIBUTES.register("sprint_cd_return",
            () -> new PercentBasedAttribute("attribute.name.sbsd.sprint_cd_return", 0.0d, 0.0d, 0.8d).setSyncable(true));

    public static final RegistryObject<Attribute> PARRY_HEAL_AMOUNT = ATTRIBUTES.register("parry_heal_amount",
            () -> new RangedAttribute("attribute.name.sbsd.parry_heal_amount", 3.0d, 0.0d, 512.0d).setSyncable(true));

    @SubscribeEvent
    public static void onEntityAttributeModificationEvent(final EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, SPRINT_CD.get());
        event.add(EntityType.PLAYER, SPRINT_CD_RETURN.get());
        event.add(EntityType.PLAYER, PARRY_HEAL_AMOUNT.get());
    }

    public static double getAttributeValue(LivingEntity serverPlayer, Attribute attribute) {
        AttributeInstance attributeInstance = serverPlayer.getAttribute(attribute);
        double scale = 1d;
        if (attributeInstance != null) scale = attributeInstance.getValue();
        return scale;
    }

}
