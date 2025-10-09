package com.tonywww.slashblade_sendims.utils;

import com.tonywww.slashblade_sendims.SBSDValues;
import com.tonywww.slashblade_sendims.registeries.SBSDAttributes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.ItemStack;
import net.tracen.umapyoi.api.UmapyoiAPI;
import net.tracen.umapyoi.utils.UmaSoulUtils;

public class UmaUtils {
    public static boolean checkSprint(ServerPlayer serverPlayer) {
        ItemStack soul = UmapyoiAPI.getUmaSoul(serverPlayer);
        if (soul == null || soul.isEmpty()) {
            return false;
        }
        int ap = UmaSoulUtils.getActionPoint(soul);
        CompoundTag data = serverPlayer.getPersistentData();
        if (ap < SBSDValues.SPRINT_COST) {
            SBSDValues.notifyPlayer(serverPlayer, Component.translatable("text.slashblade_sendims.no_ap"));
            return false;
        } else if (data.contains(SBSDValues.SPRINT_CD_PATH) &&
                data.getInt(SBSDValues.SPRINT_CD_PATH) > 0) {
            return false;
        } else {
            // Success
            double scale = 1 - SBSDAttributes.getAttributeValue(serverPlayer, SBSDAttributes.SPRINT_CD.get());
            data.putInt(SBSDValues.SPRINT_CD_PATH, (int) (SBSDValues.SPRINT_CD * scale + 0.5d));
            data.putBoolean(SBSDValues.SPRINT_SUCCESSED_PATH, false);
            int cost = SBSDValues.SPRINT_COST;
            AttributeInstance attributeInstance = serverPlayer.getAttribute(SBSDAttributes.AP_REDUCE_AMOUNT.get());
            if (attributeInstance != null) cost = (int) Math.min(0, cost + attributeInstance.getValue());
            UmaSoulUtils.addActionPoint(soul, cost);
            return true;
        }
    }
}
