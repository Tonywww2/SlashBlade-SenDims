package com.tonywww.slashblade_sendims.se;

import dev.shadowsoffire.attributeslib.api.ALObjects;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.entity.EntitySlashEffect;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import org.joml.Vector3f;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;

public class BlessingAndBane extends SpecialEffect {
    public static final DustColorTransitionOptions GOLDEN_SMOKE = new DustColorTransitionOptions(
            new Vector3f(1.0f, 0.84f, 0.0f),
            new Vector3f(0.8f, 0.6f, 0.0f),
            1.2f
    );
    public static final DustColorTransitionOptions WHITE_SMOKE = new DustColorTransitionOptions(
            new Vector3f(1.0f, 1.0f, 1.0f),
            new Vector3f(0.8f, 0.8f, 0.8f),
            1.2f
    );

    public BlessingAndBane() {
        super(40);
    }

    public static void onDoSlash(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (player.getRandom().nextFloat() < 0.15f) {
            // 如果激活SE，等级必定大于40
            int expToConsume = 10 + Math.min(20, player.experienceLevel / 4);
            player.giveExperiencePoints(-expToConsume);
            player.addEffect(new MobEffectInstance(ALObjects.MobEffects.KNOWLEDGE.get(), 100, 1));
        }

        EntitySlashEffect slash = new EntitySlashEffect(SlashBlade.RegistryEvents.SlashEffect, level);

        slash.setPos(
                player.getX(),
                player.getY() + player.getEyeHeight() / 2.0,
                player.getZ()
        );

        slash.setDamage(0.25f);
        slash.setRank(7);
        slash.setBaseSize(1.25f);

        slash.setYRot(player.getYRot());
        slash.setXRot(player.getXRot());

        slash.setRotationRoll(player.getRandom().nextFloat() * 360f);
        slash.setColor(0xFFFF55);

        slash.setOwner(player);

        level.addFreshEntity(slash);

        level.sendParticles(GOLDEN_SMOKE, player.getX(), player.getY() + player.getBbHeight() / 2.0, player.getZ(), 6, 0.5, 0.5, 0.5, 0.05);
        level.sendParticles(WHITE_SMOKE, player.getX(), player.getY() + player.getBbHeight() / 2.0, player.getZ(), 2, 0.5, 0.5, 0.5, 0.05);
    }
}

