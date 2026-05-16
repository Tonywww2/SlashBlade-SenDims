package com.tonywww.slashblade_sendims.se;

import com.tonywww.slashblade_sendims.registeries.SBSDSpecialEffects;
import mods.flammpfeil.slashblade.ability.StunManager;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

public class Mahakala extends SpecialEffect {
    public static final String MAHAKALA_FLAG = "sbsd.se.mahakala_flag";

    public Mahakala() {
        super(60);
    }

    public static void onSlashBladeUpdate(ServerPlayer player, SlashBladeEvent.UpdateEvent event) {
        if (player.level() instanceof ServerLevel serverLevel) {
            if (serverLevel.getGameTime() % 4 == 0) {
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                        player.getX() + (player.getRandom().nextDouble() - 0.5),
                        player.getY() + player.getRandom().nextDouble() * 2.0,
                        player.getZ() + (player.getRandom().nextDouble() - 0.5),
                        1, 0, 0, 0, 0.1);
            }
        }
    }

    public static void onHit(ServerPlayer player, LivingEntity target) {
        if (player.getRandom().nextFloat() < 0.15f) {
            StunManager.setStun(target, 40);
            player.heal(player.getMaxHealth() * 0.01f);
        }
    }

    public static void onLivingHurt(LivingHurtEvent event, Entity attacker, LivingEntity target, float originalDamage) {
        if (target == attacker) return;
        if (!(attacker instanceof ServerPlayer serverPlayer)) return;

        ItemStack bladeStack = serverPlayer.getMainHandItem();
        if (!(bladeStack.getItem() instanceof ItemSlashBlade)) return;
        ISlashBladeState state = bladeStack.getCapability(ItemSlashBlade.BLADESTATE).orElse(null);
        if (state == null) return;

        int experienceLevel = serverPlayer.experienceLevel;

        if (!SEEventHandlers.isSEActive(state, experienceLevel, SBSDSpecialEffects.MAHAKALA)) return;

        DamageSource source = event.getSource();

        if (source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK)) return;

        if (serverPlayer.getPersistentData().getBoolean(MAHAKALA_FLAG)) return;

        serverPlayer.getPersistentData().putBoolean(MAHAKALA_FLAG, true);

        target.invulnerableTime = 0;
        target.hurt(serverPlayer.damageSources().playerAttack(serverPlayer), originalDamage * 1.25f);

        serverPlayer.getPersistentData().putBoolean(MAHAKALA_FLAG, false);

        event.setCanceled(true);
    }
}

