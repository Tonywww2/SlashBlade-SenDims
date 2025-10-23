package com.tonywww.slashblade_sendims.curios;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.tonywww.slashblade_sendims.registeries.SBSDItems;
import com.tonywww.slashblade_sendims.utils.CuriosUtils;
import com.tonywww.slashblade_sendims.utils.NBTUtils;
import mods.flammpfeil.slashblade.registry.ModAttributes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeMod;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

import java.util.UUID;

import static com.tonywww.slashblade_sendims.items.DeepRealmCertificate.*;

public class DeepRealmCertificateWrapper implements ICurio {

    private final ItemStack stack;

    public DeepRealmCertificateWrapper(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public ItemStack getStack() {
        return stack;
    }

    @Override
    public @NotNull SoundInfo getEquipSound(SlotContext slotContext) {
        return new SoundInfo(SoundEvents.ARMOR_EQUIP_NETHERITE, 1.0f, 1.0f);
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext) {
        return false;
    }

    @Override
    public boolean canEquip(SlotContext slotContext) {
        return CuriosUtils.noSameCurio(slotContext.entity(),
                itemStack -> itemStack.is(SBSDItems.DEEPREALM_CERTIFICATE.get()));
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid) {
        Multimap<Attribute, AttributeModifier> atts = LinkedHashMultimap.create();
        if (!slotContext.identifier().equalsIgnoreCase(SLOT)) return atts;

        CompoundTag drTag = getDRTag(this.getStack());
        int rank = NBTUtils.getSpecificIntField(drTag, RANK);
        int healthProgress = NBTUtils.getSpecificIntField(drTag, HEALTH_PROGRESS);
        int damageProgress = NBTUtils.getSpecificIntField(drTag, DAMAGE_RATE_PROGRESS);

        atts.put(ForgeMod.ENTITY_REACH.get(),
                new AttributeModifier(uuid, "drc_reach",
                        0.5d + rank * 0.15d,
                        AttributeModifier.Operation.ADDITION));
        atts.put(Attributes.MAX_HEALTH,
                new AttributeModifier(uuid, "drc_max_health",
                        1d + calcFinalValue(healthProgress, MATERIAL_COUNT_PER_PROGRESS, 80d),
                        AttributeModifier.Operation.ADDITION));

        atts.put(ModAttributes.SLASHBLADE_DAMAGE.get(),
                new AttributeModifier(uuid, "drc_sb_damage",
                        0.01d + calcFinalValue(damageProgress, MATERIAL_COUNT_PER_PROGRESS, 0.03d),
                        AttributeModifier.Operation.ADDITION));

        return atts;

    }
}
