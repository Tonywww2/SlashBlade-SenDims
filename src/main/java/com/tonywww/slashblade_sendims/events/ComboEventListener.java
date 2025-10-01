package com.tonywww.slashblade_sendims.events;

import com.tonywww.slashblade_sendims.SBSDValues;
import com.tonywww.slashblade_sendims.registeries.SBSDAttributes;
import mods.flammpfeil.slashblade.event.BladeMotionEvent;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.tracen.umapyoi.api.UmapyoiAPI;
import net.tracen.umapyoi.utils.UmaSoulUtils;

@Mod.EventBusSubscriber
public class ComboEventListener {

    @SubscribeEvent
    public static void BladeMotionEventListener(BladeMotionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ResourceLocation combo = event.getCombo();
            if (SBSDValues.COMBO_COST_MAP.containsKey(combo)) {
                ItemStack soul = UmapyoiAPI.getUmaSoul(player);
                if (soul == null || soul.isEmpty()) {
                    event.setCombo(ComboStateRegistry.NONE.getId());
                    return;
                }
                int ap = UmaSoulUtils.getActionPoint(soul);
                int cost = SBSDValues.COMBO_COST_MAP.get(combo);
                if (cost < 0) {
                    AttributeInstance attributeInstance = player.getAttribute(SBSDAttributes.AP_REDUCE_AMOUNT.get());
                    if (attributeInstance != null) cost = (int) Math.min(0, cost + attributeInstance.getValue());
                }
                if (ap + cost < 0) {
                    event.setCombo(ComboStateRegistry.NONE.getId());
                    SBSDValues.notifyPlayer(player, Component.translatable("text.slashblade_sendims.no_ap"));
                    player.getCooldowns().addCooldown(player.getMainHandItem().getItem(), SBSDValues.CANCELED_CD);

                } else {
                    UmaSoulUtils.addActionPoint(soul, cost);
                }

            }
        }
    }

    @SubscribeEvent
    public static void ChargeActionEventListener(SlashBladeEvent.ChargeActionEvent event) {
        if (event.getEntityLiving() instanceof ServerPlayer player) {
            ResourceLocation sa = event.getComboState();

            ItemStack soul = UmapyoiAPI.getUmaSoul(player);
            if (soul == null || soul.isEmpty()) {
                event.setComboState(ComboStateRegistry.NONE.getId());
                event.setCanceled(true);
                return;
            }
            int ap = UmaSoulUtils.getActionPoint(soul);
            int cost = 0;
            if (SBSDValues.SA_COST_MAP.containsKey(sa)) {
                cost = SBSDValues.SA_COST_MAP.get(sa);
            }
            if (cost < 0) {
                AttributeInstance attributeInstance = player.getAttribute(SBSDAttributes.AP_REDUCE_AMOUNT.get());
                if (attributeInstance != null) cost = (int) Math.min(0, cost + attributeInstance.getValue());
            }
            if (ap + cost < 0) {
                event.setComboState(ComboStateRegistry.NONE.getId());
                SBSDValues.notifyPlayer(player, Component.translatable("text.slashblade_sendims.no_ap"));
                player.getCooldowns().addCooldown(player.getMainHandItem().getItem(), SBSDValues.CANCELED_CD);
                event.setCanceled(true);

            } else {
                UmaSoulUtils.addActionPoint(soul, cost);
            }

        }
    }

    @SubscribeEvent
    public static void PlayerTickEventListener(TickEvent.PlayerTickEvent event) {
        if (event.side != LogicalSide.SERVER && event.phase != TickEvent.Phase.END) return;
        CompoundTag data = event.player.getPersistentData();

        if (!data.contains(SBSDValues.SPRINT_CD_PATH)) {
            data.putInt(SBSDValues.SPRINT_CD_PATH, 0);
        }
        int cd = data.getInt(SBSDValues.SPRINT_CD_PATH);
        if (cd > 0) {
            cd--;
            data.putInt(SBSDValues.SPRINT_CD_PATH, cd);
        }

    }

}
