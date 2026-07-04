package com.tonywww.slashblade_sendims.items;

import cofh.core.common.item.IAugmentItem;
import cofh.core.util.helpers.AugmentDataHelper;
import cofh.lib.util.constants.NBTTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 热力系列 Integral Component。
 * 作为 Thermal / CoFH 的「升级增幅部件」（Upgrade Augment）使用，
 * 通过 {@code BaseMod} 提供整体倍率加成。
 */
public class IntegralComponent extends Item implements IAugmentItem {

    private final float baseMod;
    private final CompoundTag augmentData;

    public IntegralComponent(Properties pProperties, float baseMod) {
        super(pProperties);
        this.baseMod = baseMod;
        this.augmentData = AugmentDataHelper.builder()
                .type(NBTTags.TAG_AUGMENT_TYPE_UPGRADE)
                .mod(NBTTags.TAG_AUGMENT_BASE_MOD, baseMod)
                .build();
    }

    @Override
    public CompoundTag getAugmentData(ItemStack augment) {
        return augmentData.copy();
    }

    public float getBaseMod() {
        return baseMod;
    }
}
