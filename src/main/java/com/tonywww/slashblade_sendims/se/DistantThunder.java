package com.tonywww.slashblade_sendims.se;

import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;

public class DistantThunder extends SpecialEffect {
    public DistantThunder() {
        super(75);
    }

    public static void onLivingHurt(ServerPlayer player, LivingEntity target, float originalDamage, boolean isMagic) {
        if (isMagic) {
            double mDist = manhattanDistance(target.position(), player.position());
            float extraDamage = (float) (originalDamage * Math.min(0.3f, 0.0000175 * mDist * mDist));
            DamageSource extraDamageSource = player.damageSources().sonicBoom(player);
            target.invulnerableTime = 0;
            target.hurt(extraDamageSource, extraDamage);
        }

    }

    public static double manhattanDistance(Vec3 self, Vec3 other) {
        return Math.abs(self.x - other.x) +
                Math.abs(self.y - other.y) +
                Math.abs(self.z - other.z);
    }

}
