package com.tonywww.slashblade_sendims.se;

import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import mods.flammpfeil.slashblade.util.AttackManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.server.level.ServerLevel;
import org.joml.Vector3f;

public class Aftershock extends SpecialEffect {
    public static final String AFTERSHOCK_FLAG = "sbsd.se.as_flag";
    
    public static final DustColorTransitionOptions DARK_RED_SMOKE = new DustColorTransitionOptions(
            new Vector3f(0.6f, 0.0f, 0.0f),
            new Vector3f(0.2f, 0.0f, 0.0f),
            1.2f
    );

    public Aftershock() {
        super(40);
    }

    public static void onHit(ServerPlayer player, LivingEntity target) {
        CompoundTag data = player.getPersistentData();
        if (data.getBoolean(AFTERSHOCK_FLAG)) return;

        data.putBoolean(AFTERSHOCK_FLAG, true);

        AttackManager.doMeleeAttack(player, target, true, false, 0.15f);

        if (target.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(DARK_RED_SMOKE, target.getX(), target.getY() + target.getBbHeight() / 2.0, target.getZ(), 8, 0.5, 0.5, 0.5, 0.05);
        }

        data.remove(AFTERSHOCK_FLAG);

    }
}
