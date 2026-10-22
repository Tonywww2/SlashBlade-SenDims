package com.tonywww.slashblade_sendims.yakuman;

import com.tonywww.slashblade_sendims.SenDims;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Global server time is independent of AI, player movement packets and status immunities. */
@Mod.EventBusSubscriber(modid=SenDims.MOD_ID)
public final class YakumanStun {
    private record Lease(LivingEntity entity,Object level,Vec3 anchor,long until) {}
    private static final Map<UUID,Lease> LEASES=new HashMap<>();
    public static boolean active(LivingEntity entity) {
        if(entity.level().isClientSide()||entity.getServer()==null)return false;
        var lease=LEASES.get(entity.getUUID());
        return lease!=null&&lease.level==entity.level()&&entity.getServer().overworld().getGameTime()<lease.until;
    }
    public static void apply(LivingEntity entity,int ticks) {
        if(entity.level().isClientSide()||entity.getServer()==null)return;
        long end=entity.getServer().overworld().getGameTime()+ticks;
        var previous=LEASES.get(entity.getUUID());
        LEASES.put(entity.getUUID(),new Lease(entity,entity.level(),
                previous!=null&&active(entity)?previous.anchor:entity.position(),
                previous==null?end:Math.max(end,previous.until)));
        entity.stopUsingItem();entity.setDeltaMovement(Vec3.ZERO);
        if(entity instanceof ServerPlayer p)p.connection.teleport(p.getX(),p.getY(),p.getZ(),p.getYRot(),p.getXRot());
    }
    public static void tick() {
        for(var it=LEASES.values().iterator();it.hasNext();) {
            var lease=it.next();var entity=lease.entity;
            if(!entity.isAlive()||entity.isRemoved()||!active(entity)){it.remove();continue;}
            entity.setDeltaMovement(Vec3.ZERO);entity.setPos(lease.anchor);
            entity.fallDistance=0;entity.stopUsingItem();
        }
    }
    public static void clear(){LEASES.clear();}
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void damage(LivingAttackEvent e) {
        if(e.getSource().getEntity() instanceof LivingEntity attacker&&active(attacker))e.setCanceled(true);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void attack(AttackEntityEvent e){if(active(e.getEntity()))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void interact(PlayerInteractEvent e){if(e.isCancelable()&&active(e.getEntity()))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void item(LivingEntityUseItemEvent.Start e){if(active(e.getEntity()))e.setCanceled(true);}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){LEASES.remove(e.getEntity().getUUID());}
}
