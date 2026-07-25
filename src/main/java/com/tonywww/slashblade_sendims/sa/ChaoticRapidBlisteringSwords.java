package com.tonywww.slashblade_sendims.sa;

import com.tonywww.slashblade_sendims.entities.EntityChaoticBlisteringSwords;
import com.tonywww.slashblade_sendims.registeries.SBSDEntities;
import com.tonywww.slashblade_sendims.utils.ChaoticSlashArtEffects;
import mods.flammpfeil.slashblade.capability.concentrationrank.CapabilityConcentrationRank;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class ChaoticRapidBlisteringSwords {
    private ChaoticRapidBlisteringSwords() {
    }

    public static void doSlash(LivingEntity attacker, boolean critical, double damage, float speed) {
        doSlash(attacker, ChaoticSlashArtEffects.PRIMARY_COLOR, critical, damage, speed);
    }

    public static void doSlash(
            LivingEntity attacker,
            int color,
            boolean critical,
            double damage,
            float speed
    ) {
        if (attacker.level().isClientSide()) {
            return;
        }

        attacker.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> {
            Level level = attacker.level();
            int rank = attacker.getCapability(CapabilityConcentrationRank.RANK_POINT)
                    .map(value -> value.getRank(level.getGameTime()).level)
                    .orElse(0);
            int count = 3 + rank;

            for (int index = 0; index < count; index++) {
                EntityChaoticBlisteringSwords sword = new EntityChaoticBlisteringSwords(
                    SBSDEntities.CHAOTIC_BLISTERING_SWORDS.get(),
                        level
                );
                level.addFreshEntity(sword);
                sword.setSpeed(speed);
                sword.setIsCritical(critical);
                sword.setOwner(attacker);
                sword.setColor(color);
                sword.setRoll(0.0F);
                sword.setDamage(damage);
                sword.startRiding(attacker, true);
                sword.setDelay(20 + index);

                boolean rightSide = sword.getDelay() % 2 == 0;
                RandomSource random = level.getRandom();
                double xOffset = random.nextDouble() * 2.5D * (rightSide ? 1.0D : -1.0D);
                double yOffset = random.nextFloat() * 2.0F;
                double zOffset = random.nextFloat() * 0.5D;
                Vec3 offset = new Vec3(xOffset, yOffset, zOffset);
                sword.setPos(attacker.position().add(offset));
                sword.setOffset(offset);
                attacker.playSound(SoundEvents.CHORUS_FRUIT_TELEPORT, 0.2F, 1.45F);
            }
        });
    }
}