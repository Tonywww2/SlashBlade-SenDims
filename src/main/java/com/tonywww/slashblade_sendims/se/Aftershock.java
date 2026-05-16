package com.tonywww.slashblade_sendims.se;

import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import mods.flammpfeil.slashblade.util.AttackManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public class Aftershock extends SpecialEffect {
    public static final String AFTERSHOCK_FLAG = "sbsd.se.as_flag";

    public Aftershock() {
        super(40);
    }

    public static void onHit(ServerPlayer player, LivingEntity target) {
        CompoundTag data = player.getPersistentData();
        if (data.getBoolean(AFTERSHOCK_FLAG)) return;

        data.putBoolean(AFTERSHOCK_FLAG, true);

        AttackManager.doMeleeAttack(player, target, true, false, 0.15f);

        data.remove(AFTERSHOCK_FLAG);

    }
}
