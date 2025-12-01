package com.tonywww.slashblade_sendims.registeries;

import com.tonywww.slashblade_sendims.SenDims;
import dev.shadowsoffire.attributeslib.impl.PercentBasedAttribute;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class SBSDAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, SenDims.MOD_ID);
    public static final RegistryObject<Attribute> SPRINT_CD = ATTRIBUTES.register("sprint_cd",
            () -> new PercentBasedAttribute("attribute.name.sbsd.sprint_cd", 0.0d, -1.0d, 0.6d).setSyncable(true));

    public static final RegistryObject<Attribute> SPRINT_CD_RETURN = ATTRIBUTES.register("sprint_cd_return",
            () -> new PercentBasedAttribute("attribute.name.sbsd.sprint_cd_return", 0.0d, 0.0d, 0.8d).setSyncable(true));

    public static final RegistryObject<Attribute> PARRY_HEAL_AMOUNT = ATTRIBUTES.register("parry_heal_amount",
            () -> new RangedAttribute("attribute.name.sbsd.parry_heal_amount", 3.0d, 0.0d, 512.0d).setSyncable(true));

    public static final RegistryObject<Attribute> AP_REDUCE_AMOUNT = ATTRIBUTES.register("ap_reduce_amount",
            () -> new RangedAttribute("attribute.name.sbsd.ap_reduce_amount", 0.0d, -512.0d, 512.0d).setSyncable(true));

    public static final RegistryObject<Attribute> AP_GAIN_PERCENTAGE = ATTRIBUTES.register("ap_gain_percentage",
            () -> new PercentBasedAttribute("attribute.name.sbsd.ap_gain_percentage", 1.0d, 0.0d, 512.0d).setSyncable(true));

    public static final RegistryObject<Attribute> MADNESS_REDUCE = ATTRIBUTES.register("madness_reduce",
            () -> new RangedAttribute("attribute.name.sbsd.madness_reduce", 0.0d, 0.0d, 512.0d).setSyncable(true));

    public static final RegistryObject<Attribute> FRENZY_RESISTANCE = ATTRIBUTES.register("frenzy_resistance",
            () -> new PercentBasedAttribute("attribute.name.sbsd.frenzy_resistance", 0.0d, 0.0d, 1.0d).setSyncable(true));

    public static final RegistryObject<Attribute> FRENZY_DAMAGE = ATTRIBUTES.register("frenzy_damage",
            () -> new PercentBasedAttribute("attribute.name.sbsd.frenzy_damage", 0.0d, 0.0d, 512.0d).setSyncable(true));

    @SubscribeEvent
    public static void onEntityAttributeModificationEvent(final EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, SPRINT_CD.get());
        event.add(EntityType.PLAYER, SPRINT_CD_RETURN.get());
        event.add(EntityType.PLAYER, PARRY_HEAL_AMOUNT.get());
        event.add(EntityType.PLAYER, AP_REDUCE_AMOUNT.get());
        event.add(EntityType.PLAYER, AP_GAIN_PERCENTAGE.get());

        event.add(EntityType.PLAYER, MADNESS_REDUCE.get());
        event.add(EntityType.PLAYER, FRENZY_RESISTANCE.get());
        event.add(EntityType.PLAYER, FRENZY_DAMAGE.get());

        event.add(EntityType.SHULKER, Attributes.ATTACK_DAMAGE);
    }

    public static double getAttributeValue(LivingEntity serverPlayer, Attribute attribute) {
        AttributeInstance attributeInstance = serverPlayer.getAttribute(attribute);
        double scale = 1d;
        if (attributeInstance != null) scale = attributeInstance.getValue();
        return scale;
    }

    public static void register(IEventBus eventBus) {
        ATTRIBUTES.register(eventBus);
    }

}
