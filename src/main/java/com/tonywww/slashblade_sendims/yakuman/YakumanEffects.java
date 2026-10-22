package com.tonywww.slashblade_sendims.yakuman;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.se.NamelessSeries;
import com.tonywww.slashblade_sendims.events.SuperSlashArtsReleaseEvent;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
import static com.tonywww.slashblade_sendims.yakuman.YakumanRegistry.Effect.*;
import static com.tonywww.slashblade_sendims.yakuman.YakumanRegistry.has;

@Mod.EventBusSubscriber(modid=SenDims.MOD_ID)
public final class YakumanEffects {
    private static final String KEY="sbsd.yakuman";
    private static final Map<UUID,Swing> SWINGS=new HashMap<>();
    private record PendingNativeArt(ItemStack blade,long tick) {}
    private static final Map<UUID,PendingNativeArt> PENDING_NATIVE_ARTS=new HashMap<>();
    private static final Map<UUID,WeakHashMap<LivingEntity,Integer>> EDICTS=new HashMap<>();
    private static final Map<LivingEntity,Long> WEAKENED=new WeakHashMap<>();
    private record NearbyCount(Object level,long tick,int count) {}
    private static final Map<UUID,NearbyCount> NEARBY_COUNTS=new HashMap<>();
    private static int nearbyCount(ServerPlayer p) {
        long tick=p.level().getGameTime();var cached=NEARBY_COUNTS.get(p.getUUID());
        if(cached!=null && cached.level==p.level() && cached.tick==tick)return cached.count;
        int count=YakumanCombat.nearby(p,p.position(),8).size();
        NEARBY_COUNTS.put(p.getUUID(),new NearbyCount(p.level(),tick,count));return count;
    }
    private static final class Swing {
        final ItemStack blade; final ResourceLocation combo; final int mask;
        final boolean kong,wind; boolean counted,slashed;
        Swing(ServerPlayer p,ResourceLocation combo,int mask) {
            blade=p.getMainHandItem();this.combo=combo;this.mask=mask;
            var data=data(p);
            kong=has(mask,KONG)&&data.getBoolean("kong_ready");
            wind=has(mask,WIND_CYCLE)&&data.getBoolean("wind_ready");
        }
    }
    public record Release(int marks,double speed) {}
    private static CompoundTag data(ServerPlayer p) {
        var tag=p.getPersistentData();
        if(!tag.contains(KEY))tag.put(KEY,new CompoundTag());
        return tag.getCompound(KEY);
    }
    private static long now(LivingEntity p) {return p.getServer().overworld().getGameTime();}
    /** Called at the accepted native combo boundary, not from raw mouse input. */
    public static void combo(LivingEntity user,ResourceLocation combo,int priority) {
        if(!(user instanceof ServerPlayer p) || YakumanCombat.secondary())return;
        SWINGS.remove(p.getUUID());
        var state=NamelessSeries.getBladeState(p);
        if(state==null || priority<=50 || ComboStateRegistry.NONE.getId().equals(combo)
                || ComboStateRegistry.STANDBY.getId().equals(combo))return;
        int mask=YakumanRegistry.effects(state);
        var spec=YakumanSpec.find(state.getSlashArtsKey());
        if(mask!=0 || spec!=null) {
            SWINGS.put(p.getUUID(),new Swing(p,combo,mask));
        }
    }
    private static Swing swing(ServerPlayer p,ISlashBladeState state) {
        var s=SWINGS.get(p.getUUID());
        return !YakumanCombat.secondary() && s!=null && state!=null && !state.isBroken() && !state.isSealed()
                && s.blade==p.getMainHandItem() && s.combo.equals(state.getComboSeq())?s:null;
    }
    /** A slash effect enters here exactly once per accepted ordinary swing. */
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void onSlash(SlashBladeEvent.DoSlashEvent event) {
        if(!(event.getUser() instanceof ServerPlayer p))return;
        var state=event.getSlashBladeState();var s=swing(p,state);
        if(s==null || s.slashed)return;
        s.slashed=true;
        if(s.wind) {
            data(p).putBoolean("wind_ready",false);YakumanArts.wind(p,.4);
        }
        var spec=YakumanSpec.find(state.getSlashArtsKey());
        if(spec!=null)YakumanArts.fireAmmo(p,spec);
    }
    /** Read-only quote: canceled releases do not consume ready effects or start cooldowns. */
    public static double apMultiplier(LivingEntity user) {
        if(!(user instanceof ServerPlayer p))return 1;
        var state=NamelessSeries.getBladeState(p);
        if(state==null)return 1;
        int mask=YakumanRegistry.effects(state);
        if(!has(mask,HEAVEN) && !has(mask,NINE_LAMPS))return 1;
        var spec=YakumanSpec.find(state.getSlashArtsKey());
        var d=data(p);double multiplier=1;
        if(has(mask,HEAVEN) && now(p)>=d.getLong("heaven_until"))multiplier*=.8;
        if(spec!=null && spec.ammo>0 && has(mask,NINE_LAMPS) && d.getBoolean("lamps_ready"))multiplier*=.9;
        return multiplier;
    }
    /** Prepare only; canceled casts never reach the native clickAction commit. */
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void prepareNativeArt(SlashBladeEvent.PerformSlashArtEvent event) {
        if(event.getType()!=SlashArts.ArtsType.Fail)prepareNativeArt(event.getEntityLiving());
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void prepareNativeSuperArt(SuperSlashArtsReleaseEvent event) {
        prepareNativeArt(event.getPlayer());
    }
    private static void prepareNativeArt(LivingEntity user) {
        if(!(user instanceof ServerPlayer p))return;
        PENDING_NATIVE_ARTS.remove(p.getUUID());
        var state=NamelessSeries.getBladeState(p);
        if(state==null || YakumanSpec.find(state.getSlashArtsKey())!=null)return;
        int mask=YakumanRegistry.effects(state);
        if(!has(mask,HEAVEN) && !has(mask,THREE_SEALS) && !has(mask,FOUR_SEALS))return;
        PENDING_NATIVE_ARTS.put(p.getUUID(),new PendingNativeArt(p.getMainHandItem(),p.level().getGameTime()));
    }
    /** Reuse SE release rules for inherited native SAs, once after AP/cancellation. */
    public static void acceptedNativeArt(LivingEntity user,ISlashBladeState state) {
        if(!(user instanceof ServerPlayer p) || state==null)return;
        var pending=PENDING_NATIVE_ARTS.remove(p.getUUID());
        if(pending==null || pending.blade()!=p.getMainHandItem()
                || pending.tick()!=p.level().getGameTime() || state.isBroken() || state.isSealed())return;
        // Hidden triplets and Nine Lamps retain their corresponding-art requirements.
        accepted(p,null);
    }
    /** Commit exactly once, from the accepted art's native clickAction. */
    public static Release accepted(ServerPlayer p,YakumanSpec spec) {
        var state=NamelessSeries.getBladeState(p);int mask=YakumanRegistry.effects(state);var d=data(p);
        int marks=0;double speed=1;
        if(has(mask,HIDDEN_TRIPLETS) && spec==YakumanSpec.CHINROU_SUUKOU) {
            marks=Math.min(4,d.getInt("marks"));d.putInt("marks",0);
        }
        if(has(mask,HEAVEN) && now(p)>=d.getLong("heaven_until"))d.putLong("heaven_until",now(p)+2400);
        if(spec!=null && has(mask,NINE_LAMPS) && d.getBoolean("lamps_ready") && spec.ammo>0) {
            speed=1.5;d.putBoolean("lamps_ready",false);
        }
        if(has(mask,THREE_SEALS) && d.getInt("three")>=9) {
            d.putInt("three",0);p.heal(p.getMaxHealth()*.02F);
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,200,0));
        }
        if(has(mask,FOUR_SEALS) && d.getInt("four")>=12) {
            d.putInt("four",0);YakumanArts.seals(p);
            if(has(mask,WIND_VEIL))p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,80,0));
        }
        return new Release(marks,speed);
    }
    private static int increment(CompoundTag d,String key,int cap) {
        int n=Math.min(cap,Math.max(0,d.getInt(key))+1);d.putInt(key,n);return n;
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void onHit(SlashBladeEvent.HitEvent event) {
        if(!(event.getUser() instanceof ServerPlayer p))return;
        var s=swing(p,event.getSlashBladeState());
        if(s==null || s.counted || !YakumanCombat.enemy(p,event.getTarget()))return;
        s.counted=true;var d=data(p);
        if(s.kong)d.putBoolean("kong_ready",false);
        if(has(s.mask,KONG) && increment(d,"kong",4)>=4) {
            d.putInt("kong",0);d.putBoolean("kong_ready",true);
        }
        if(has(s.mask,WIND_CYCLE) && increment(d,"wind",4)>=4) {
            d.putInt("wind",0);d.putBoolean("wind_ready",true);
        }
        if(has(s.mask,HIDDEN_TRIPLETS) && d.getInt("marks")<4 && increment(d,"hidden",9)>=9) {
            d.putInt("hidden",0);d.putInt("marks",Math.min(4,d.getInt("marks")+1));
        }
        if(has(s.mask,THREE_SEALS))increment(d,"three",9);
        if(has(s.mask,FOUR_SEALS))increment(d,"four",12);
        if(has(s.mask,NINE_LAMPS) && !d.getBoolean("lamps_ready") && increment(d,"lamps",27)>=27) {
            d.putInt("lamps",0);d.putBoolean("lamps_ready",true);
        }
        if(has(s.mask,JADE_LIFE) && increment(d,"jade",6)>=6) {
            d.putInt("jade",0);p.heal(p.getMaxHealth()*.02F);
        }
        if(has(s.mask,SEVEN_EDICT)) {
            var counts=EDICTS.computeIfAbsent(p.getUUID(),id->new WeakHashMap<>());
            var t=event.getTarget();int count=counts.getOrDefault(t,0)+1;
            if(count>=7){counts.remove(t);WEAKENED.put(t,now(p)+80);}
            else counts.put(t,count);
        }
    }
    @SubscribeEvent(priority=EventPriority.LOW)
    public static void damage(LivingHurtEvent event) {
        if(event.getEntity().level().isClientSide() || event.getAmount()<=0)return;
        if(event.getSource().getEntity() instanceof LivingEntity attacker) {
            var until=WEAKENED.get(attacker);
            if(until!=null) {
                if(now(attacker)<until)event.setAmount(event.getAmount()*.9F);
                else WEAKENED.remove(attacker);
            }
        }
        if(!(event.getSource().getEntity() instanceof ServerPlayer p))return;
        var state=NamelessSeries.getBladeState(p);int mask=YakumanRegistry.effects(state);
        if(mask==0)return;
        double mult=1;
        if(has(mask,TERMINALS)) {
            double hp=event.getEntity().getHealth()/event.getEntity().getMaxHealth();
            if(hp>.9 || hp<.1)mult*=1.15;
        }
        if(has(mask,SOLITARY) && nearbyCount(p)==1)mult*=1.12;
        var s=swing(p,state);
        if(s!=null && event.getSource().getDirectEntity()==p && state.onClick()) {
            if(s.kong)mult*=1.5;
            if(has(mask,BORROW_WIND)) {
                var t=event.getEntity();
                Vec3 toward=p.position().subtract(t.position()).multiply(1,0,1).normalize();
                if(t.getLookAngle().multiply(1,0,1).normalize().dot(toward)<=.5)mult*=1.1;
            }
        }
        event.setAmount((float)(event.getAmount()*mult));
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void earth(LivingDamageEvent e) {
        if(!(e.getEntity() instanceof ServerPlayer p) || e.getAmount()<=0
                || !(e.getSource().getEntity() instanceof LivingEntity attacker)
                || !YakumanCombat.enemy(p,attacker))return;
        if(!has(YakumanRegistry.effects(NamelessSeries.getBladeState(p)),EARTH))return;
        var d=data(p);if(now(p)<d.getLong("earth_until"))return;
        d.putLong("earth_until",now(p)+1200);YakumanStun.apply(attacker,140);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        SWINGS.remove(e.getEntity().getUUID());EDICTS.remove(e.getEntity().getUUID());NEARBY_COUNTS.remove(e.getEntity().getUUID());PENDING_NATIVE_ARTS.remove(e.getEntity().getUUID());
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent e) {SWINGS.clear();EDICTS.clear();WEAKENED.clear();NEARBY_COUNTS.clear();PENDING_NATIVE_ARTS.clear();}
}
