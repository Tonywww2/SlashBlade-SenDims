package com.tonywww.slashblade_sendims.sa;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.events.SuperSlashArtsReleaseEvent;
import com.tonywww.slashblade_sendims.network.SukunaVisualPacket;
import com.tonywww.slashblade_sendims.se.NamelessSeries;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.item.SwordType;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid=SenDims.MOD_ID)
public final class SukunaArts {
    public static final ResourceLocation ID=SenDims.prefix("malevolent_shrine");
    public static final int PULSES=15, INTERVAL=10, DURATION=150;
    private static final Map<UUID,Domain> DOMAINS=new HashMap<>();
    private static long nextId;
    private static final class Domain {
        final LivingEntity user;final ServerLevel level;final ItemStack blade;
        final long started,id;final Vec3 origin,forward;
        final Map<UUID,Double> baseDamage=new HashMap<>();
        int completed;
        Domain(LivingEntity user,ServerLevel level) {
            this.user=user;this.level=level;blade=user.getMainHandItem();started=level.getGameTime();id=++nextId;
            origin=user.position();forward=SukunaCombat.horizontal(user.getLookAngle(),user.getYRot());
        }
        boolean valid() {
            var state=NamelessSeries.getBladeState(user);
            return user.isAlive() && !user.isRemoved() && user.level()==level && user.getMainHandItem()==blade
                    && state!=null && !state.isBroken() && !state.isSealed() && ID.equals(state.getSlashArtsKey());
        }
        void visual(SukunaVisualPacket.Stage stage,int age) {
            SukunaVisualPacket.send(level,user,stage,id,origin,forward,age);
        }
    }
    private SukunaArts() {}
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void release(SlashBladeEvent.PerformSlashArtEvent event) {
        if(!ID.equals(event.getComboState()) || event.getEntityLiving().level().isClientSide()
                || event.getType()==SlashArts.ArtsType.Fail) return;
        var state=event.getSlashBladeState();
        var current=state==null ? null : ComboStateRegistry.REGISTRY.get().getValue(
                state.resolvCurrentComboStateTicks(event.getEntityLiving()).getValue());
        if(state==null || state.isBroken() || state.isSealed() || current==null || current.getPriority()<=50)
            event.setCanceled(true);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void superRelease(SuperSlashArtsReleaseEvent event) {
        ServerPlayer player=event.getPlayer();var state=NamelessSeries.getBladeState(player);
        if(state==null || !ID.equals(state.getSlashArtsKey())) return;
        var current=ComboStateRegistry.REGISTRY.get().getValue(state.resolvCurrentComboStateTicks(player).getValue());
        var types=SwordType.from(player.getMainHandItem());
        if(!player.onGround() || state.isBroken() || state.isSealed() || state.getDamage()>0
                || !types.contains(SwordType.BEWITCHED) || !types.contains(SwordType.FIERCEREDGE)
                || current==null || current.getPriority()<=50) event.setCanceled(true);
    }
    public static void begin(LivingEntity user) {
        if(!(user.level() instanceof ServerLevel level)) return;
        var state=NamelessSeries.getBladeState(user);
        if(state==null || !ID.equals(state.getComboSeq())) return;
        Domain old=DOMAINS.remove(user.getUUID());if(old!=null) old.visual(SukunaVisualPacket.Stage.END,0);
        Domain domain=new Domain(user,level);DOMAINS.put(user.getUUID(),domain);
        domain.visual(SukunaVisualPacket.Stage.DOMAIN,0);
        level.playSound(null,user.blockPosition(),SoundEvents.WITHER_SPAWN,SoundSource.PLAYERS,.6F,.65F);
        tick(user);
    }
    public static void tick(LivingEntity user) {
        Domain domain=DOMAINS.get(user.getUUID());
        if(domain==null || !domain.valid()) return;
        long elapsed=domain.level.getGameTime()-domain.started;
        while(domain.completed<PULSES && elapsed>=domain.completed*INTERVAL) {
            domain.completed++;
            for(var target:domain.level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(domain.origin,domain.origin).inflate(16,6,16),
                    e->SukunaCombat.enemy(user,e))) {
                AABB box=target.getBoundingBox();
                double dx=Math.max(0,Math.max(box.minX-domain.origin.x,domain.origin.x-box.maxX));
                double dz=Math.max(0,Math.max(box.minZ-domain.origin.z,domain.origin.z-box.maxZ));
                if(dx*dx+dz*dz<=16*16 && box.maxY>=domain.origin.y-6 && box.minY<=domain.origin.y+6)
                    SukunaCombat.damage(user,target,domain.baseDamage.computeIfAbsent(target.getUUID(),
                            ignored->SukunaCombat.base(user,target)),.5F);
            }
            domain.level.playSound(null,domain.origin.x,domain.origin.y,domain.origin.z,
                    SoundEvents.PLAYER_ATTACK_SWEEP,SoundSource.PLAYERS,.6F,1.3F+domain.completed*.025F);
        }
        if(elapsed>0 && elapsed<DURATION && elapsed%20==0)
            domain.visual(SukunaVisualPacket.Stage.DOMAIN,(int)elapsed);
    }
    public static boolean active(LivingEntity user) {return DOMAINS.containsKey(user.getUUID());}
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END) return;
        Iterator<Domain> it=DOMAINS.values().iterator();
        while(it.hasNext()) {
            Domain d=it.next();
            if(!d.valid() || d.level.getGameTime()-d.started>=DURATION) {
                d.visual(SukunaVisualPacket.Stage.END,0);it.remove();
            } else tick(d.user);
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        Domain d=DOMAINS.remove(event.getEntity().getUUID());if(d!=null)d.visual(SukunaVisualPacket.Stage.END,0);
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) {DOMAINS.clear();}
}
