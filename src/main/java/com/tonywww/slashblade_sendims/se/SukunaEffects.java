package com.tonywww.slashblade_sendims.se;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.network.SukunaVisualPacket;
import com.tonywww.slashblade_sendims.registeries.SBSDSpecialEffects;
import com.tonywww.slashblade_sendims.sa.SukunaCombat;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid=SenDims.MOD_ID)
public final class SukunaEffects {
    public static final String HISTORY="sbsd.sukuna.hurt_history";
    public static final String HITS="sbsd.sukuna.reverse_hits";
    private static final String LAST_SPACE="sbsd.sukuna.last_space_tick";
    public static final int SPACE_CD=20, REVERSE_HITS=12;
    private static long sequence;
    private static final Map<UUID,Swing> SWINGS=new HashMap<>();
    private static final class Swing {
        final ItemStack blade; final ResourceLocation combo; final long id;
        boolean counted;
        Swing(ItemStack blade,ResourceLocation combo) {this.blade=blade;this.combo=combo;id=++sequence;}
    }
    private SukunaEffects() {}
    private static boolean active(ServerPlayer player,ISlashBladeState state,
            net.minecraftforge.registries.RegistryObject<mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect> se) {
        return state!=null && !state.isBroken() && !state.isSealed()
                && SEEventHandlers.isSEActive(state,player.experienceLevel,se);
    }
    /** Called only after native combo cancellation and priority checks. */
    public static void combo(LivingEntity user,ResourceLocation key,int priority) {
        if(!(user instanceof ServerPlayer player)) return;
        SWINGS.remove(player.getUUID());
        var state=NamelessSeries.getBladeState(player);
        if(priority<=50 || ComboStateRegistry.NONE.getId().equals(key)
                || ComboStateRegistry.STANDBY.getId().equals(key) || state==null) return;
        if(active(player,state,SBSDSpecialEffects.SPACE_CUT)
                || active(player,state,SBSDSpecialEffects.REVERSE_CURSED_TECHNIQUE))
            SWINGS.put(player.getUUID(),new Swing(player.getMainHandItem(),key));
    }
    private static Swing swing(ServerPlayer player,ISlashBladeState state) {
        Swing swing=SWINGS.get(player.getUUID());
        return !SukunaCombat.secondary() && state!=null && swing!=null
                && swing.blade==player.getMainHandItem() && swing.combo.equals(state.getComboSeq()) ? swing : null;
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void slash(SlashBladeEvent.DoSlashEvent event) {
        if(event.getUser() instanceof ServerPlayer player && swing(player,event.getSlashBladeState())!=null)
            space(player,event.getSlashBladeState());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void hit(SlashBladeEvent.HitEvent event) {
        if(!(event.getUser() instanceof ServerPlayer player)) return;
        var state=event.getSlashBladeState();var swing=swing(player,state);
        if(swing==null) return;
        space(player,state); // Native thrusts without a decorative DoSlash still trigger on their melee hit.
        if(swing.counted || !active(player,state,SBSDSpecialEffects.REVERSE_CURSED_TECHNIQUE)) return;
        swing.counted=true;
        int count=Math.max(0,player.getPersistentData().getInt(HITS))+1;
        if(count>=REVERSE_HITS) {
            count=0;player.heal(player.getMaxHealth()*.02F);
            player.serverLevel().playSound(null,player.blockPosition(),SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS,.45F,.75F);
        }
        player.getPersistentData().putInt(HITS,count);
    }
    public static boolean space(ServerPlayer player,ISlashBladeState state) {
        if(SukunaCombat.secondary() || !active(player,state,SBSDSpecialEffects.SPACE_CUT)) return false;
        var target=state.getTargetEntity(player.level());
        if(!(target instanceof LivingEntity locked) || !SukunaCombat.enemy(player,locked)
                || player.distanceToSqr(locked)>24*24) return false;
        long now=player.server.overworld().getGameTime();
        var data=player.getPersistentData();
        if(data.contains(LAST_SPACE) && now-data.getLong(LAST_SPACE)<SPACE_CD) return false;
        data.putLong(LAST_SPACE,now);
        Vec3 origin=SukunaCombat.center(locked);
        Vec3 forward=SukunaCombat.horizontal(origin.subtract(player.position()),player.getYRot());
        Vec3 right=forward.cross(new Vec3(0,1,0));
        for(var victim:player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                new AABB(origin,origin).inflate(6,4,6),e->SukunaCombat.enemy(player,e))) {
            if(inPlane(victim,origin,right,forward))
                SukunaCombat.damage(player,victim,SukunaCombat.base(player,victim),.3F);
        }
        SukunaVisualPacket.send(player,SukunaVisualPacket.Stage.SPACE,++sequence,origin,forward,0);
        player.serverLevel().playSound(null,locked.blockPosition(),SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS,.9F,.5F);
        return true;
    }
    public static boolean inPlane(LivingEntity target,Vec3 origin,Vec3 right,Vec3 forward) {
        Vec3 offset=SukunaCombat.center(target).subtract(origin);
        double radius=target.getBbWidth()*.5;
        double projected=radius*(Math.abs(right.x)+Math.abs(right.z));
        double depth=radius*(Math.abs(forward.x)+Math.abs(forward.z));
        return Math.abs(offset.dot(right))<=5+projected
                && Math.abs(offset.y)<=2+target.getBbHeight()*.5 && Math.abs(offset.dot(forward))<=1+depth;
    }
    private static String type(DamageSource source) {
        return source.typeHolder().unwrapKey().map(key->key.location().toString()).orElse("");
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void adapt(LivingHurtEvent event) {
        if(!(event.getEntity() instanceof ServerPlayer player) || event.getAmount()<=0) return;
        if(!active(player,NamelessSeries.getBladeState(player),SBSDSpecialEffects.MAHORAGA_FALSE)) return;
        String key=type(event.getSource());
        ListTag history=player.getPersistentData().getList(HISTORY,Tag.TAG_STRING);
        if(!key.isEmpty() && history.contains(StringTag.valueOf(key))) event.setAmount(event.getAmount()*.9F);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void remember(LivingDamageEvent event) {
        if(!(event.getEntity() instanceof ServerPlayer player) || event.getAmount()<=0) return;
        if(!active(player,NamelessSeries.getBladeState(player),SBSDSpecialEffects.MAHORAGA_FALSE)) return;
        String key=type(event.getSource());if(key.isEmpty()) return;
        var data=player.getPersistentData();ListTag history=data.getList(HISTORY,Tag.TAG_STRING);
        boolean repeated=history.contains(StringTag.valueOf(key));
        history.add(StringTag.valueOf(key));while(history.size()>3) history.remove(0);data.put(HISTORY,history);
        if(repeated) {
            SukunaVisualPacket.send(player,SukunaVisualPacket.Stage.WHEEL,++sequence,
                    player.position().add(0,2.4,0),new Vec3(0,0,1),0);
            player.serverLevel().playSound(null,player.blockPosition(),SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS,.35F,.55F);
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {SWINGS.remove(e.getEntity().getUUID());}
    @SubscribeEvent public static void stopped(ServerStoppedEvent e) {SWINGS.clear();}
}
