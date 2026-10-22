package com.tonywww.slashblade_sendims.sa;

import com.tonywww.slashblade_sendims.se.NamelessSeries;
import com.tonywww.slashblade_sendims.utils.TrueDamage;
import mods.flammpfeil.slashblade.util.AttackHelper;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;

/** Shared damage accounting for space cuts and the domain. */
public final class SukunaCombat {
    private static final ThreadLocal<Integer> SECONDARY = ThreadLocal.withInitial(() -> 0);
    private SukunaCombat() {}
    public static boolean secondary() { return SECONDARY.get() > 0; }
    public static boolean enemy(LivingEntity user, LivingEntity target) {
        return target != user && target.isAlive() && !target.isSpectator() && !user.isAlliedTo(target)
                && target.isAttackable() && !target.skipAttackInteraction(user)
                && (!(target instanceof Player p) || (!p.isCreative() && !p.isSpectator()));
    }
    public static Vec3 center(Entity entity) { return entity.position().add(0,entity.getBbHeight()*.5,0); }
    public static Vec3 horizontal(Vec3 direction, float yaw) {
        Vec3 flat = direction.multiply(1,0,1);
        return flat.lengthSqr() < 1e-8 ? Vec3.directionFromRotation(0,yaw) : flat.normalize();
    }
    public static double base(LivingEntity user, LivingEntity target) {
        var state=NamelessSeries.getBladeState(user);
        if(state==null) return 0;
        boolean clicked=state.onClick();state.setOnClick(true);
        try { return Math.max(0,AttackHelper.calculateTotalDamage(user,target,1,false)); }
        finally {state.setOnClick(clicked);}
    }
    public static boolean damage(LivingEntity user, LivingEntity target, double base, float ratio) {
        var state=NamelessSeries.getBladeState(user);
        if(state==null || !enemy(user,target) || !Double.isFinite(base) || base<=0) return false;
        boolean clicked=state.onClick();state.setOnClick(true);
        SECONDARY.set(SECONDARY.get()+1);
        try {
            // Native blades route a left click into combos unless onClick is already set.
            if(user instanceof Player player && !ForgeHooks.onPlayerAttackTarget(player,target)) return false;
            float before=target.getHealth(), amount=(float)(base*ratio);
            target.invulnerableTime=0;
            var physical=user instanceof Player p ? user.damageSources().playerAttack(p) : user.damageSources().mobAttack(user);
            boolean hit=target.hurt(physical,amount);
            if(target.isAlive()) hit |= TrueDamage.deal(user,target,amount);
            if(hit) {
                AttackHelper.handleEnchantmentsAndDurability(user,target);
                if(user instanceof Player player) {
                    player.awardStat(Stats.DAMAGE_DEALT,Math.round(Math.max(0,before-target.getHealth())*10));
                    player.causeFoodExhaustion(.1F);
                }
            }
            return hit;
        } finally {SECONDARY.set(SECONDARY.get()-1);state.setOnClick(clicked);}
    }
}
