package com.tonywww.slashblade_sendims.events;

import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.event.handler.InputCommandEvent;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import mods.flammpfeil.slashblade.util.InputCommand;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tracen.umapyoi.api.UmapyoiAPI;
import net.tracen.umapyoi.utils.UmaSoulUtils;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber
public class ComboListener {

    public static final Map<ResourceLocation, Integer> COMBO_COST_MAP = new HashMap<>();

    static {
        COMBO_COST_MAP.put(ComboStateRegistry.RAPID_SLASH.getId(), 800);
        COMBO_COST_MAP.put(ComboStateRegistry.UPPERSLASH.getId(), 400);
    }

    @SubscribeEvent
    public static void DoSlashEventListener(SlashBladeEvent.DoSlashEvent event) {
        if (event.getUser() instanceof Player player) {
            ResourceLocation combo = event.getSlashBladeState().getComboSeq();
            if (COMBO_COST_MAP.containsKey(combo)) {
                ItemStack soul = UmapyoiAPI.getUmaSoul(player);
                if (soul == null || soul.isEmpty()) {
                    event.setCanceled(true);
                    return;
                }
                int ap = UmaSoulUtils.getActionPoint(soul);
                int cost = COMBO_COST_MAP.get(combo);
                if (ap < cost) {
                    event.setCanceled(true);
                } else {
                    UmaSoulUtils.addActionPoint(soul, -cost);
                }

            }
        }
    }

}
