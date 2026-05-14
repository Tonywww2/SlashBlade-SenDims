package com.tonywww.slashblade_sendims.curios;

import com.tonywww.slashblade_sendims.registeries.SBSDItems;
import com.tonywww.slashblade_sendims.utils.CuriosUtils;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.tonywww.slashblade_sendims.registeries.SBSDTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

import static com.tonywww.slashblade_sendims.items.TheNectarQuest.*;

public class TheNectarQuestWrapper implements ICurio {

    private final ItemStack stack;

    public TheNectarQuestWrapper(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public ItemStack getStack() {
        return stack;
    }

    @Override
    public @NotNull SoundInfo getEquipSound(SlotContext slotContext) {
        return new SoundInfo(SoundEvents.ARMOR_EQUIP_GOLD, 1.0f, 1.0f);
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext) {
        return false;
    }

    @Override
    public boolean canEquip(SlotContext slotContext) {
        return CuriosUtils.noSameCurio(slotContext.entity(),
                itemStack -> itemStack.is(SBSDItems.THE_NECTAR_QUEST.get()));
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid) {
        Multimap<Attribute, AttributeModifier> atts = LinkedHashMultimap.create();
        if (!slotContext.identifier().equalsIgnoreCase(SLOT)) return atts;
        CompoundTag tag = getTNQTag(this.getStack());
        int counts = tag.getInt(ITEM_COUNTS);
        double atk = counts * 0.04d + 0.02d;
        atts.put(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(uuid, "the_nectar_quest_attack_damage",
                        atk,
                        AttributeModifier.Operation.MULTIPLY_TOTAL));
        return atts;
    }
}
