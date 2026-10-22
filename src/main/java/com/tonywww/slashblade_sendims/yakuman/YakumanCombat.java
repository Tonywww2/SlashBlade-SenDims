package com.tonywww.slashblade_sendims.yakuman;

import com.tonywww.slashblade_sendims.sa.SukunaCombat;
import com.tonywww.slashblade_sendims.se.NamelessSeries;
import com.tonywww.slashblade_sendims.utils.TrueDamage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.common.MinecraftForge;
import java.util.List;

public final class YakumanCombat {
    public static final double SEARCH_RADIUS=12;
    public enum Kind { PHYSICAL, MAGIC, LIGHTNING, TRUE }
    private static final ThreadLocal<Integer> DEPTH=ThreadLocal.withInitial(()->0);
    public static boolean secondary() {return DEPTH.get()>0;}
    public static boolean enemy(LivingEntity user,LivingEntity target) {
        return SukunaCombat.enemy(user,target) && target.level()==user.level()
                && (!(user instanceof Player p && target instanceof Player t) || p.canHarmPlayer(t));
    }
    public static Vec3 center(LivingEntity e) {return SukunaCombat.center(e);}
    public static List<LivingEntity> nearby(LivingEntity user,Vec3 pos,double radius) {
        return user.level().getEntitiesOfClass(LivingEntity.class,new AABB(pos,pos).inflate(radius),
                t->{double reach=radius+t.getBbWidth()*.5;
                    return enemy(user,t) && center(t).distanceToSqr(pos)<=reach*reach;});
    }
    public static LivingEntity locked(LivingEntity user) {
        var state=NamelessSeries.getBladeState(user);
        if(state!=null && state.getTargetEntity(user.level()) instanceof LivingEntity t
                && enemy(user,t) && user.distanceToSqr(t)<=32*32)return t;
        return null;
    }
    public static LivingEntity nearest(LivingEntity user,Vec3 point) {
        LivingEntity best=null;double distance=Double.MAX_VALUE;
        for(var t:nearby(user,point,SEARCH_RADIUS)) {
            double d=center(t).distanceToSqr(point);
            if(d<distance){distance=d;best=t;}
        }
        return best;
    }
    public static LivingEntity random(LivingEntity user,List<LivingEntity> candidates) {
        // Query results may be shared by several attacks in one tick. Revalidate deaths without another world scan.
        LivingEntity selected=null;int valid=0;
        for(var candidate:candidates)if(enemy(user,candidate)&&user.getRandom().nextInt(++valid)==0)selected=candidate;
        return selected;
    }
    public static double base(LivingEntity user,LivingEntity target) {return SukunaCombat.base(user,target);}
    public static boolean hit(LivingEntity user,LivingEntity target,double base,double ratio,Kind kind) {
        var state=NamelessSeries.getBladeState(user);
        if(secondary() || state==null || !enemy(user,target) || !Double.isFinite(base) || base<=0 || !Double.isFinite(ratio) || ratio<=0)return false;
        boolean click=state.onClick();state.setOnClick(true);DEPTH.set(DEPTH.get()+1);
        try {
            float amount=(float)(base*ratio);
            target.invulnerableTime=0;
            DamageSource source=switch(kind) {
                case MAGIC -> user.damageSources().indirectMagic(user,user);
                case LIGHTNING -> new DamageSource(user.level().registryAccess()
                        .registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(DamageTypes.LIGHTNING_BOLT),user,user);
                default -> user instanceof Player p?user.damageSources().playerAttack(p):user.damageSources().mobAttack(user);
            };
            boolean hit=kind==Kind.TRUE?TrueDamage.deal(user,target,amount):target.hurt(source,amount);
            if(hit) {
                // This is a confirmed secondary hit, never another left click or combo.hitEffect.
                // Calling ItemSlashBlade.hurtEnemy would re-enter the current combo's hit action.
                user.setLastHurtMob(target);
                EnchantmentHelper.doPostHurtEffects(target,user);
                EnchantmentHelper.doPostDamageEffects(user,target);
                var blade=user.getMainHandItem();
                if(!MinecraftForge.EVENT_BUS.post(new SlashBladeEvent.HitEvent(blade,state,target,user)))
                    blade.hurtAndBreak(1,user,ItemSlashBlade.getOnBroken(blade));
            }
            return hit;
        } finally {DEPTH.set(DEPTH.get()-1);state.setOnClick(click);}
    }
}
