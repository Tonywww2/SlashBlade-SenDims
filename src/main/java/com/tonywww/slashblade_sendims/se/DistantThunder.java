package com.tonywww.slashblade_sendims.se;

import com.tonywww.slashblade_sendims.registeries.SBSDSpecialEffects;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class DistantThunder extends SpecialEffect {
    public DistantThunder() {
        super(75);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        DamageSource damageSource = event.getSource();
        if (!(damageSource.is(DamageTypes.MAGIC) || damageSource.is(DamageTypes.INDIRECT_MAGIC))) return;

        LivingEntity self = event.getEntity();
        Entity attacker = damageSource.getEntity();

        if (!(attacker instanceof LivingEntity attackerLiving)) return;
        ItemStack bladeStack = attackerLiving.getMainHandItem();

        if (!(bladeStack.getItem() instanceof ItemSlashBlade)) return;
        ISlashBladeState state = bladeStack.getCapability(ItemSlashBlade.BLADESTATE).orElseThrow(NullPointerException::new);

        int expLevel = attacker instanceof Player player ?
                player.experienceLevel :
                30;

        if (!(SpecialEffect.isEffective(SBSDSpecialEffects.DISTANT_THUNDER.get(), expLevel) &
                state.hasSpecialEffect(SBSDSpecialEffects.DISTANT_THUNDER.getId()))) return;

        double mDist = manhattanDistance(self.position(), attacker.position());
        float extraDamage = event.getAmount() * (float) Math.max(1.15f, (1f + (0.01f * Math.sqrt(mDist))));

        DamageSource extraDamageSource = attacker.damageSources().sting(attackerLiving);
        self.hurt(extraDamageSource, extraDamage);


    }

    public static double manhattanDistance(Vec3 self, Vec3 other) {
        return Math.abs(self.x - other.x) +
                Math.abs(self.y - other.y) +
                Math.abs(self.z - other.z);
    }

}
