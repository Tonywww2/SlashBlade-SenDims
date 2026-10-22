package com.tonywww.slashblade_sendims.yakuman;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.api.leader.LeaderApi;
import com.tonywww.slashblade_sendims.entities.EntityYakumanDrive;
import com.tonywww.slashblade_sendims.registeries.SBSDEntities;
import com.tonywww.slashblade_sendims.events.SuperSlashArtsReleaseEvent;
import com.tonywww.slashblade_sendims.sa.AmmoSA;
import com.tonywww.slashblade_sendims.se.NamelessSeries;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.entity.EntitySlashEffect;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.world.entity.Entity;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.tracen.umapyoi.api.UmapyoiAPI;
import net.tracen.umapyoi.utils.UmaSoulUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
import java.util.function.*;
import static com.tonywww.slashblade_sendims.yakuman.YakumanCombat.Kind.*;
import static com.tonywww.slashblade_sendims.yakuman.YakumanCombat.*;
import static com.tonywww.slashblade_sendims.yakuman.YakumanVisualPacket.Shape.*;

/** One server damage scheduler. Native SlashBlade entities provide slash rendering and lifetime. */
@Mod.EventBusSubscriber(modid=SenDims.MOD_ID)
public final class YakumanArts {
    private static final List<Cast> ACTIVE=new ArrayList<>();
    private static final ArrayDeque<Cast> PENDING=new ArrayDeque<>();
    private static long sequence;
    public static final String NATIVE_SLASH="sbsd.yakuman.native_slash";
    // Native rendering masks the high color byte. Reuse it as a synchronized series marker.
    public static final int NATIVE_COLOR_MARKER=0x59000000;
    private record NativeCut(Cast cast,Vec3 point,double radius,double ratio,YakumanCombat.Kind kind) {}
    private static final Map<EntitySlashEffect,NativeCut> CUTS=new IdentityHashMap<>();
    /** Native slashes must use the same physical/magic damage and recursion rules as other arts. */
    public static List<Entity> nativeSlashDamage(EntitySlashEffect slash) {
        var cut=CUTS.remove(slash);
        if(cut==null || !cut.cast.valid())return List.of();
        List<Entity> hit=new ArrayList<>();
        for(var target:nearby(cut.cast.user,cut.point,cut.radius)) {
            cut.cast.hit(target,cut.ratio,cut.kind);hit.add(target);
        }
        return hit;
    }
    private static EntitySlashEffect nativeSlash(Cast c,Vec3 point,Vec3 facing,double radius,int rgb,int life) {
        var slash=new EntitySlashEffect(SlashBlade.RegistryEvents.SlashEffect,c.level);
        slash.setPos(point);slash.setOwner(c.user);slash.setIndirect(true);slash.setCycleHit(false);
        slash.setDamage(0);slash.setBaseSize((float)(radius/4));
        slash.setYRot((float)Math.toDegrees(Math.atan2(-facing.x,facing.z)));
        slash.setRotationRoll(12);slash.setColor(NATIVE_COLOR_MARKER|(rgb&0xffffff));slash.setMute(true);
        slash.setLifetime(life);
        slash.getPersistentData().putBoolean(NATIVE_SLASH,true);
        c.level.addFreshEntity(slash);return slash;
    }
    /** Mahjong cards retain their separate throwing speed; all sword/light projectiles double. */
    public static double projectileSpeed(YakumanVisualPacket.Shape shape,double speed) {
        return speed*switch(shape) {
            case SLASH,NEEDLE,PHANTOM,DRAGON,SHADOW,GOLD -> 2;
            default -> 1;
        };
    }
    private static double collisionWidth(YakumanVisualPacket.Shape shape,double width) {
        return width*switch(shape) {
            case SLASH,NEEDLE,PHANTOM,DRAGON,SHADOW,GOLD -> 2;
            default -> 1;
        };
    }
    private static final String RELEASE_ACTION="sbsd.yakuman.release_action", RELEASE_ART="sbsd.yakuman.release_art";
    private static final String AMMO_ART="sbsd.yakuman.ammo_art", SPEED="sbsd.yakuman.speed",
            LAST_SHOT="sbsd.yakuman.last_shot";
    public static long nextId(){return ++sequence;}
    private static long reserveVisuals(){long base=sequence+1;sequence+=8;return base;}
    private record Action(int tick,long order,Runnable run) {}
    public static final class Cast {
        final LivingEntity user; final ServerLevel level; final ItemStack blade;
        final long started; final Vec3 origin,forward; final YakumanSpec spec;
        final Map<UUID,Double> bases=new HashMap<>();
        final Map<Vec3,List<LivingEntity>> neighborhoods=new HashMap<>();
        final PriorityQueue<Action> actions=new PriorityQueue<>(Comparator.comparingInt(Action::tick).thenComparingLong(Action::order));
        final List<Shot> shots=new ArrayList<>();
        final List<Shot> added=new ArrayList<>();
        final Set<UUID> waveHits=new HashSet<>(), initialHits=new HashSet<>();
        final double speed; final int marks;
        final long visualBase=reserveVisuals();
        int age;
        Cast(LivingEntity u,YakumanSpec spec,int marks,double speed) {
            user=u;level=(ServerLevel)u.level();blade=u.getMainHandItem();started=level.getGameTime();
            origin=u.position();forward=u.getLookAngle().normalize();this.spec=spec;this.marks=marks;this.speed=speed;
        }
        void at(int tick,Runnable run){actions.add(new Action(tick,nextId(),run));}
        void later(int ticks,Runnable run){at(age+ticks,run);}
        boolean valid(){
            var state=NamelessSeries.getBladeState(user);
            return user.isAlive()&&!user.isRemoved()&&user.level()==level&&user.getMainHandItem()==blade
                    && state!=null&&!state.isBroken()&&!state.isSealed()&&!YakumanStun.active(user);
        }
        boolean hit(LivingEntity t,double ratio,YakumanCombat.Kind kind) {
            return YakumanCombat.hit(user,t,bases.computeIfAbsent(t.getUUID(),id->base(user,t)),ratio,kind);
        }
        void area(Vec3 pos,double radius,double ratio,YakumanCombat.Kind kind) {
            for(var t:nearby(user,pos,radius))hit(t,ratio,kind);
        }
        LivingEntity target(boolean nearest) {
            var t=locked(user);return t!=null?t:nearest?YakumanCombat.nearest(user,user.position()):null;
        }
        void visual(long id,YakumanVisualPacket.Shape shape,Vec3 pos,Vec3 direction,int life,float scale,int color,int tile) {
            // Tile meshes do not use tint. This shape carries the caster entity id in that field.
            YakumanVisualPacket.emit(level,id,shape,pos,direction,life,scale,shape==FACING_TILE?user.getId():color,tile);
        }
        void flash(YakumanVisualPacket.Shape shape,Vec3 pos,Vec3 dir,int life,float scale,int rgb,int tile) {
            visual(nextId(),shape,pos,dir,life,scale,rgb,tile);
        }
        void cut(Vec3 point,double radius,double ratio,YakumanCombat.Kind kind,int rgb) {
            var slash=nativeSlash(this,point,forward,radius,rgb,spec==YakumanSpec.SUUSHI_JIKOKU?20:10);
            CUTS.put(slash,new NativeCut(this,point,radius,ratio,kind));
        }
        Shot shot(Vec3 pos,Vec3 dir,LivingEntity target,double velocity,double damage,YakumanCombat.Kind kind,
                  YakumanVisualPacket.Shape shape,int color,int tile,boolean blocks,boolean pierce) {
            var s=new Shot(this,pos,dir,target,velocity,damage,kind,shape,color,tile,blocks,pierce);
            added.add(s);return s;
        }
        void advance() {
            age=(int)(level.getGameTime()-started);
            neighborhoods.clear();
            while(!actions.isEmpty()&&actions.peek().tick<=age)actions.remove().run.run();
            shots.addAll(added);added.clear();
            for(var it=shots.iterator();it.hasNext();)if(!it.next().tick())it.remove();
        }
        void end() {for(var s:shots)s.end();for(var s:added)s.end();}
    }
    /** Swept horizontal blade volume. SAT keeps a diagonal wave's width/height exact. */
    private record HorizontalSweep(AABB bounds,Vec3 middle,Vec3 forward,Vec3 right,double along,double across) {
        static HorizontalSweep of(Vec3 start,Vec3 end,Vec3 direction,double halfWidth,double halfHeight,double halfLength) {
            Vec3 middle=start.add(end).scale(.5),right=new Vec3(direction.z,0,-direction.x);
            double along=start.distanceTo(end)*.5+halfLength;
            double x=Math.abs(direction.x)*along+Math.abs(right.x)*halfWidth;
            double z=Math.abs(direction.z)*along+Math.abs(right.z)*halfWidth;
            return new HorizontalSweep(new AABB(middle.x-x,middle.y-halfHeight,middle.z-z,
                    middle.x+x,middle.y+halfHeight,middle.z+z),middle,direction,right,along,halfWidth);
        }
        boolean touches(AABB box) {
            if(!bounds.intersects(box))return false;
            Vec3 delta=box.getCenter().subtract(middle);
            double x=(box.maxX-box.minX)*.5,z=(box.maxZ-box.minZ)*.5;
            return Math.abs(delta.dot(forward))<=along+x*Math.abs(forward.x)+z*Math.abs(forward.z)
                    &&Math.abs(delta.dot(right))<=across+x*Math.abs(right.x)+z*Math.abs(right.z);
        }
    }
    private static final class Shot {
        final Cast c; final long id=nextId(); final YakumanCombat.Kind kind;
        final YakumanVisualPacket.Shape shape; final int rgb,tile; final double velocity,ratio;
        final boolean blocks,pierce; final Set<UUID> seen=new HashSet<>();
        Vec3 pos,dir;LivingEntity target;int age,maxAge=100,hits=1,pause;
        double travelled,range=32,width=.4,area;
        double horizontalHalfWidth=-1,horizontalHalfHeight,horizontalHalfLength;
        float visualScale=1;
        Set<UUID> shared;
        List<LivingEntity> pool;
        BiConsumer<LivingEntity,Vec3> impact;
        Runnable expired;
        boolean ground,impacted;
        EntityYakumanDrive drive;
        boolean driveAdded;
        Shot(Cast c,Vec3 p,Vec3 dir,LivingEntity target,double velocity,double damage,YakumanCombat.Kind kind,
             YakumanVisualPacket.Shape shape,int rgb,int tile,boolean blocks,boolean pierce) {
            this.c=c;pos=p;this.dir=dir.normalize();this.target=target;this.velocity=projectileSpeed(shape,velocity);ratio=damage;
            this.kind=kind;this.shape=shape;this.rgb=rgb;this.tile=tile;this.blocks=blocks;this.pierce=pierce;
            if(shape==SLASH||shape==NEEDLE||shape==PHANTOM) {
                drive=new EntityYakumanDrive(SBSDEntities.YAKUMAN_DRIVE.get(),c.level);
                drive.setPos(pos);drive.setOwner(c.user);drive.setDamage(0);
                drive.setSpeed((float)this.velocity);drive.setBaseSize(shape==SLASH?1F:shape==NEEDLE?.25F:.4F);
                drive.setRotationRoll(shape==SLASH?12:90);drive.setColor(NATIVE_COLOR_MARKER|(rgb&0xffffff));
                drive.setKnockBack(KnockBacks.cancel);drive.setLifetime((float)Math.ceil(range/this.velocity)+2);
            }
        }
        void end(){
            if(drive!=null)drive.discard();
            else c.visual(id,END,pos,Vec3.ZERO,0,1,rgb,tile);
        }
        void impactOnce(LivingEntity victim,Vec3 point) {
            if(impacted)return;
            impacted=true;impact.accept(victim,point);
        }
        boolean tick() {
            if(age++>=maxAge || travelled>=range){if(expired!=null)expired.run();end();return false;}
            if(pause>0){pause--;return true;}
            if(target!=null && !enemy(c.user,target))target=null;
            if(target==null && pool!=null) {
                pool.removeIf(t->!enemy(c.user,t));
                target=random(c.user,pool);
                if(target==null)return true;
            }
            if(target!=null)dir=center(target).subtract(pos).normalize();
            if(drive!=null&&!driveAdded) {
                drive.shoot(dir.x,dir.y,dir.z,(float)velocity,0);
                c.level.addFreshEntity(drive);driveAdded=true;
            }
            // Publish the launch position before movement/contact can remove a close-range shot.
            // Later four-tick snapshots let the client animate the same swept server path.
            if(age==1 && drive==null) {
                c.visual(id,shape,pos,dir.scale(velocity),6,visualScale,rgb,tile);
                return true;
            }
            double reach=collisionWidth(shape,width);
            double step=Math.min(velocity,range-travelled);
            Vec3 next=pos.add(dir.scale(step));travelled+=step;
            HitResult block=blocks?c.level.clip(new ClipContext(pos,next,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,c.user)):null;
            if(block!=null && block.getType()!=HitResult.Type.MISS)next=block.getLocation();
            final Vec3 segmentEnd=next;
            final HorizontalSweep sweep=horizontalHalfWidth>=0?
                    HorizontalSweep.of(pos,segmentEnd,dir,horizontalHalfWidth,horizontalHalfHeight,horizontalHalfLength):null;
            var candidates=c.level.getEntitiesOfClass(LivingEntity.class,sweep==null?new AABB(pos,segmentEnd).inflate(reach):sweep.bounds,
                    t->enemy(c.user,t)&&!seen.contains(t.getUUID())
                            &&(shared==null||!shared.contains(t.getUUID()))
                            &&(sweep!=null?sweep.touches(t.getBoundingBox()):
                            t.getBoundingBox().inflate(reach).contains(pos)||t.getBoundingBox().inflate(reach).clip(pos,segmentEnd).isPresent()));
            candidates.sort(Comparator.comparingDouble(t->center(t).distanceToSqr(pos)));
            for(var t:candidates) {
                if(sweep==null&&center(t).distanceToSqr(pos)>next.distanceToSqr(pos)+Math.pow(t.getBbWidth()+reach,2))continue;
                seen.add(t.getUUID());if(shared!=null)shared.add(t.getUUID());
                Vec3 hit=center(t);
                if(impact!=null) {if(pierce)impact.accept(t,hit);else impactOnce(t,hit);}
                else if(area>0)c.area(hit,area,ratio,kind);
                else c.hit(t,ratio,kind);
                c.flash(NEEDLE,hit,dir,5,.8F,rgb,tile);
                if(!pierce) {
                    if(--hits<=0){end();return false;}
                    pos=hit;seen.clear();pause=2;impacted=false;
                    target=pool!=null?random(c.user,pool):random(c.user,
                            c.neighborhoods.computeIfAbsent(hit,point->nearby(c.user,point,SEARCH_RADIUS)));
                    if(target==null && pool==null){end();return false;}
                    return true;
                }
                // A piercing homing wave keeps moving through its target instead of circling it.
                if(target==t)target=null;
            }
            pos=next;
            if(block!=null && block.getType()!=HitResult.Type.MISS) {
                if(impact!=null)impactOnce(null,pos);else if(area>0)c.area(pos,area,ratio,kind);
                end();return false;
            }
            if(ground && target==null && dir.y<0 && pos.y<=c.origin.y) {
                if(impact!=null)impactOnce(null,pos);end();return false;
            }
            if(drive!=null) {
                drive.setPos(pos);drive.setDeltaMovement(dir.scale(velocity));
            } else if(age%4==1)c.visual(id,shape,pos,dir.scale(velocity),6,visualScale,rgb,tile);
            return true;
        }
    }
    private static Cast cast(LivingEntity user,YakumanSpec spec,int marks,double speed) {
        var c=new Cast(user,spec,marks,speed);PENDING.add(c);return c;
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void release(SlashBladeEvent.PerformSlashArtEvent e) {
        if(YakumanSpec.find(e.getComboState())==null || e.getEntityLiving().level().isClientSide())return;
        var state=e.getSlashBladeState();
        var current=state==null?null:ComboStateRegistry.REGISTRY.get().getValue(state.resolvCurrentComboStateTicks(e.getEntityLiving()).getValue());
        if(state==null||state.isBroken()||state.isSealed()||e.getType()==SlashArts.ArtsType.Fail
                ||current==null||current.getPriority()<=50||YakumanStun.active(e.getEntityLiving()))e.setCanceled(true);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void superRelease(SuperSlashArtsReleaseEvent e) {
        var state=NamelessSeries.getBladeState(e.getPlayer());
        if(state!=null&&YakumanSpec.find(state.getSlashArtsKey())!=null
                &&(state.isBroken()||state.isSealed()||YakumanStun.active(e.getPlayer())))e.setCanceled(true);
    }
    public static void begin(LivingEntity user,YakumanSpec spec) {
        if(!(user instanceof ServerPlayer p)||secondary())return;
        var state=NamelessSeries.getBladeState(p);
        if(state==null||!spec.id.equals(state.getComboSeq())||state.isBroken()||state.isSealed())return;
        // Re-entering a non-ammo combo's clickAction is the same release, not another SA.
        if(spec.ammo==0) {
            var data=p.getPersistentData();
            if(data.contains(RELEASE_ACTION)&&data.getLong(RELEASE_ACTION)==state.getLastActionTime()
                    &&data.getString(RELEASE_ART).equals(spec.id.toString()))return;
            data.putLong(RELEASE_ACTION,state.getLastActionTime());data.putString(RELEASE_ART,spec.id.toString());
        }
        var release=YakumanEffects.accepted(p,spec);
        if(spec.ammo>0) {
            AmmoSA.onInit(p,p.getMainHandItem(),state,spec.ammo);
            var tag=p.getMainHandItem().getOrCreateTag();tag.putString(AMMO_ART,spec.id.toString());
            tag.putDouble(SPEED,release.speed());return;
        }
        var c=cast(p,spec,release.marks(),release.speed());
        p.serverLevel().playSound(null,p.blockPosition(),SoundEvents.PLAYER_ATTACK_SWEEP,SoundSource.PLAYERS,.8F,.7F);
        switch(spec) {
            case SUUANKOU,CHINROU_SUUKOU -> {
                for(int i=0;i<4;i++) {int index=i;c.at(i*4,()->shadowCut(c,index,spec==YakumanSpec.SUUANKOU?2.5:2+.125*c.marks));}
                if(spec==YakumanSpec.CHINROU_SUUKOU) {
                    c.at(16,()->areaTarget(c,.5+.125*c.marks,0xdbcfab));c.at(20,()->areaTarget(c,2+.5*c.marks,0xf3dc9c));
                }
            }
            case SHOUSUUSHII -> {
                for(int i=0;i<4;i++){int n=i;c.at(i*4,()->forwardBlade(c,n==3?1:2.5,n==3?PHANTOM:SLASH,n!=3));}
            }
            case TSUUIISOU -> honors(c);
            case RYUUIISOU -> bamboo(c);
            case CHINROUTOU -> {c.at(0,()->areaTarget(c,1,0xe0d6b9));c.at(8,()->areaTarget(c,9,0xffe8b2));}
            case TENHOU -> heaven(c);
            case CHIIHOU -> earth(c);
            case DAISUUSHII,SUUSHI_JIKOKU -> cardinal(c);
            default -> {}
        }
    }
    public static void fireAmmo(ServerPlayer p,YakumanSpec spec) {
        if(spec.ammo==0 || secondary())return;
        var tag=p.getMainHandItem().getOrCreateTag();int ammo=tag.getInt(AmmoSA.AMMO_PATH);
        long now=p.server.overworld().getGameTime();
        if(ammo<=0||!tag.getString(AMMO_ART).equals(spec.id.toString())
                ||tag.contains(LAST_SHOT)&&now-tag.getLong(LAST_SHOT)<spec.interval)return;
        if(spec==YakumanSpec.SHISHOU_KIITSU) {
            var state=NamelessSeries.getBladeState(p);
            if(state==null||p.getCooldowns().isOnCooldown(p.getMainHandItem().getItem())
                    ||!AmmoSA.onSlashEffects(p,p.getMainHandItem(),state,spec.ammo,spec.interval))return;
        } else {
            tag.putInt(AmmoSA.AMMO_PATH,ammo-1);
            if(ammo==1){tag.remove(AmmoSA.AMMO_PATH);tag.remove(AmmoSA.MAX_AMMO_PATH);}
        }
        tag.putLong(LAST_SHOT,now);
        var c=cast(p,spec,0,tag.getDouble(SPEED)>1?1.5:1);
        int index=spec.ammo-ammo;
        c.at(0,()->{
            switch(spec) {
                case KOKUSHI,TENJU_KOKUSHI -> {
                    var t=c.target(true);if(t==null)return;
                    int count=spec==YakumanSpec.KOKUSHI?1:3;
                    for(int i=0;i<count;i++) {
                        Vec3 start=p.getEyePosition().add(0,(i-(count-1)*.5)*.12,0);
                        var shot=c.shot(start,center(t).subtract(start),t,.8,
                                spec==YakumanSpec.KOKUSHI?1.25:.6,PHYSICAL,NEEDLE,
                                spec==YakumanSpec.KOKUSHI?0xdce7f5:0xffe8a4,0,false,false);
                        shot.visualScale=2F;
                    }
                }
                case DAISANGEN,SANGEN_JIREI -> {
                    int dragon=index%3;var t=c.target(false);
                    var dragonShot=c.shot(p.getEyePosition(),c.forward,t,1.2,5,dragon==1?MAGIC:PHYSICAL,DRAGON,
                            new int[]{0xf5f4d9,0x77deac,0xe86f72}[dragon],31+dragon,false,true);
                    dragonShot.width=.8;dragonShot.visualScale=2;
                }
                case CHUUREN,CHISEI_CHUUREN -> gates(c);
                case SUUKANTSU,HEKIGYOKU_SUUKANTSU -> kong(c,index);
                case SHISHOU_KIITSU -> fourSymbols(c,index);
                default -> {}
            }
        });
    }
    private static final List<TagKey<EntityType<?>>> GATHER_BOSS_TAGS=List.of(
            TagKey.create(Registries.ENTITY_TYPE,ResourceLocation.fromNamespaceAndPath("sdbf","bosses")),
            TagKey.create(Registries.ENTITY_TYPE,ResourceLocation.fromNamespaceAndPath("forge","bosses")),
            TagKey.create(Registries.ENTITY_TYPE,ResourceLocation.fromNamespaceAndPath("c","bosses")));
    private static boolean boss(LivingEntity entity) {
        if(entity.getType()==EntityType.ENDER_DRAGON||entity.getType()==EntityType.WITHER||LeaderApi.isLeader(entity))return true;
        for(var tag:GATHER_BOSS_TAGS)if(entity.getType().is(tag))return true;
        return false;
    }
    private static void fourSymbols(Cast c,int round) {
        switch(round) {
            case 0 -> {
                c.user.heal(Math.max(20F,Math.max(0,c.user.getMaxHealth()-c.user.getHealth())*.14F));
                fourSymbolsLight(c);
            }
            case 1 -> {
                var soul=UmapyoiAPI.getUmaSoul(c.user);
                if(soul!=null&&!soul.isEmpty()) {
                    int missing=Math.max(0,UmaSoulUtils.getMaxActionPoint(soul)-UmaSoulUtils.getActionPoint(soul));
                    int gain=Math.min(missing,Math.max(500,(int)Math.round(missing*.14)));
                    if(gain>0)UmaSoulUtils.addActionPoint(soul,gain);
                }
                fourSymbolsLight(c);
            }
            case 2 -> gatherFourSymbols(c);
            case 3 -> {
                var target=c.target(false);
                Vec3 direction=(target==null?c.user.getLookAngle():center(target).subtract(c.user.getEyePosition())).multiply(1,0,1);
                if(direction.lengthSqr()<1E-8) {
                    double yaw=Math.toRadians(c.user.getYRot());direction=new Vec3(-Math.sin(yaw),0,Math.cos(yaw));
                }
                var shot=c.shot(c.user.getEyePosition(),direction.normalize(),null,.8,24,MAGIC,SLASH,0xffe3ac,0,false,true);
                shot.horizontalHalfWidth=4;shot.horizontalHalfHeight=1;
                shot.drive.setRotationRoll(0);shot.drive.setVisualDimensions(8,2);
                shot.horizontalHalfLength=EntityYakumanDrive.MODEL_DEPTH*shot.drive.getBaseSize()*.5;
                // The leading edge, including the blade's depth, stops at the requested range.
                shot.range=32-shot.horizontalHalfLength;
                shot.drive.setLifetime((float)Math.ceil(shot.range/shot.velocity)+2);
            }
            default -> {}
        }
    }
    private static void fourSymbolsLight(Cast c) {
        var target=c.target(true);Vec3 start=c.user.getEyePosition();
        Vec3 direction=target==null?c.user.getLookAngle():center(target).subtract(start);
        // Reuse Heaven's light sword appearance, with one confirmed true-damage hit only.
        c.shot(start,direction,target,1.2,.5,TRUE,GOLD,0xffedb8,0,false,false).maxAge=140;
    }
    private static void gatherFourSymbols(Cast c) {
        var locked=c.target(false);
        Vec3 anchor=locked==null?c.user.position().add(c.user.getLookAngle().scale(5)):locked.position();
        // One neighborhood query per round. Entity overlap is intentional; only solid blocks are rejected.
        var victims=nearby(c.user,anchor,12);
        for(var target:victims) {
            if(target==locked||boss(target)||center(target).distanceToSqr(anchor)>144)continue;
            for(int attempt=0;attempt<12;attempt++) {
                double angle=c.user.getRandom().nextDouble()*Math.PI*2;
                double radius=Math.sqrt(c.user.getRandom().nextDouble())*.5;
                Vec3 destination=anchor.add(Math.cos(angle)*radius,0,Math.sin(angle)*radius);
                AABB bounds=target.getBoundingBox().move(destination.subtract(target.position()));
                if(bounds.minY<c.level.getMinBuildHeight()||bounds.maxY>c.level.getMaxBuildHeight()
                        ||!c.level.getWorldBorder().isWithinBounds(bounds)
                        ||c.level.getBlockCollisions(target,bounds).iterator().hasNext())continue;
                target.stopRiding();target.teleportTo(destination.x,destination.y,destination.z);
                target.setDeltaMovement(Vec3.ZERO);target.hurtMarked=true;target.fallDistance=0;
                break;
            }
        }
        c.flash(GATE,anchor.add(0,1,0),c.forward,12,1.5F,0xdac8fa,0);
        c.level.playSound(null,anchor.x,anchor.y,anchor.z,SoundEvents.ENDERMAN_TELEPORT,SoundSource.PLAYERS,.8F,.8F);
    }
    private static void areaTarget(Cast c,double ratio,int rgb) {
        var t=c.target(false);c.cut(t==null?c.user.position():center(t),2,ratio,PHYSICAL,rgb);
    }
    private static void shadowCut(Cast c,int n,double ratio) {
        var t=c.target(false);Vec3 point=t==null?c.user.position():center(t);
        Vec3 side=new Vec3(Math.cos(n*Math.PI/2),.15,Math.sin(n*Math.PI/2));
        c.flash(SHADOW,point.add(side.scale(2)),side.scale(-1),9,1,0x171321,0);
        c.cut(point,2,ratio,PHYSICAL,0xbeb2d1);
    }
    private static void forwardBlade(Cast c,double ratio,YakumanVisualPacket.Shape shape,boolean pierce) {
        var t=c.target(false);Vec3 dir=t==null?c.user.getLookAngle():center(t).subtract(c.user.getEyePosition()).normalize();
        double velocity=c.spec==YakumanSpec.SHOUSUUSHII?.4:.8;
        c.shot(c.user.getEyePosition(),dir,null,velocity,ratio,PHYSICAL,shape,0xc1e8dc,0,false,pierce).width=pierce?1.5:.4;
    }
    public static void wind(ServerPlayer p,double ratio) {
        var c=cast(p,null,0,1);c.at(0,()->forwardBlade(c,ratio,SLASH,true));
    }
    public static void seals(ServerPlayer p) {
        var c=cast(p,null,0,1);c.at(0,()->{
            var choices=nearby(p,p.position(),8);
            for(int i=0;i<4;i++) {
                var t=random(p,choices);if(t==null)continue;
                c.shot(p.getEyePosition(),p.getLookAngle(),t,.8,.75,PHYSICAL,TILE,0xf2e8c8,27+i,true,false);
            }
        });
    }
    private static void gates(Cast c) {
        Vec3 f=c.user.getLookAngle(),right=f.cross(new Vec3(0,1,0)).normalize();
        var choices=nearby(c.user,c.user.position(),SEARCH_RADIUS);
        for(int i=0;i<3;i++) {
            Vec3 pos=c.user.getEyePosition().subtract(f.scale(1.5+c.user.getRandom().nextDouble()))
                    .add(right.scale(c.user.getRandom().nextDouble()*2.4-1.2)).add(0,c.user.getRandom().nextDouble()*1.2-.6,0);
            c.flash(GATE,pos,f,12,1,c.spec==YakumanSpec.CHISEI_CHUUREN?0xf4cc8f:0xe7ba79,0);
            var t=random(c.user,choices);
            c.shot(pos,f,t,.6*c.speed,.4,MAGIC,PHANTOM,0xffdb99,0,false,false);
        }
    }
    private static void kong(Cast c,int index) {
        var t=c.target(false);boolean jade=c.spec==YakumanSpec.HEKIGYOKU_SUUKANTSU;
        int tile=jade?new int[]{19,20,21,25}[index%4]:new int[]{0,4,10,26}[index%4];
        int rgb=jade?0x8ae7bf:0xe8e1c9;
        Vec3 start=t!=null?center(t).add(0,6,0):c.user.getEyePosition();
        var shot=c.shot(start,t!=null?new Vec3(0,-1,0):c.forward,t,.8,0,PHYSICAL,STACK,rgb,tile,t==null,false);
        shot.impact=(victim,point)-> {
            var victims=nearby(c.user,point,t!=null?2:4);
            for(int j=0;j<4;j++)for(var enemy:victims)c.hit(enemy,.75,PHYSICAL);
            c.flash(RING,point,new Vec3(0,1,0),10,t!=null?2:4,rgb,0);
        };
        shot.ground=t!=null;
    }
    private static void honors(Cast c) {
        LivingEntity initial=c.target(true);if(initial==null)return;
        Vec3[] anchor={center(initial)};
        for(int tick=0;tick<=130;tick+=10) {
            int step=tick;
            c.at(tick,()->{
                if(enemy(c.user,initial))anchor[0]=center(initial);
                int active=Math.min(7,step/10+1);
                var touching=nearby(c.user,anchor[0],3);
                for(int j=0;j<active;j++) {
                    double angle=step*.11+j*Math.PI*2/7;
                    Vec3 pos=anchor[0].add(Math.cos(angle)*1.8,.3,Math.sin(angle)*1.8);
                    c.visual(c.visualBase+j,ORBIT,anchor[0],new Vec3((step*.11)%(Math.PI*2),0,0),11,.25F,0xf5e9c9,27+j);
                    if(step>j*10) {
                        if(enemy(c.user,initial))c.hit(initial,.2,PHYSICAL);
                        for(var t:touching)if(t!=initial&&t.getBoundingBox().inflate(.65).contains(pos))c.hit(t,.2,PHYSICAL);
                    }
                }
                if(step==130) {
                    var target=enemy(c.user,initial)?initial:nearest(c.user,anchor[0]);
                    for(int j=0;j<7;j++) {
                        double a=step*.11+j*Math.PI*2/7;Vec3 pos=anchor[0].add(Math.cos(a)*1.8,.3,Math.sin(a)*1.8);
                        c.visual(c.visualBase+j,END,pos,Vec3.ZERO,0,1,0xffffff,0);
                        var s=c.shot(pos,c.forward,target,.8,1,PHYSICAL,TILE,0xf5e9c9,27+j,true,false);s.area=2;s.visualScale=.25F;
                    }
                }
            });
        }
    }
    private static void bamboo(Cast c) {
        int[] counts={2,3,4,6,8};
        for(int i=0;i<5;i++) {
            int n=counts[i];c.at(i*5,()->{
                Vec3 dir=c.user.getLookAngle();Vec3 right=dir.cross(new Vec3(0,1,0)).normalize(),up=right.cross(dir).normalize();
                for(int j=0;j<n;j++) {
                    int rows=n==3?2:n/2;
                    double x=n==8?(j%4-1.5)*.44:n==2?0:n==3&&j==0?0:(j%2==0?-.45:.45);
                    double y=n==8?(j<4?.45:-.45):n==2?(j==0?.5:-.5):n==3?(j==0?.5:-.5):((rows-1)*.5-(j/2))*.55;
                    Vec3 pos=c.user.getEyePosition().add(right.scale(x*.6)).add(up.scale(y*.6));
                    c.shot(pos,dir,null,.6,.35,MAGIC,PHANTOM,0x87ddb1,0,false,false);
                }
            });
        }
        c.at(25,()->c.shot(c.user.getEyePosition().add(0,.5,0),c.user.getLookAngle(),c.target(false),1.2,2,MAGIC,TILE,0xa7ebbb,32,true,false));
    }
    private static void heaven(Cast c) {
        c.at(0,()->{
            var locked=c.target(false);var choices=nearby(c.user,locked==null?c.origin:center(locked),locked==null?SEARCH_RADIUS:8);
            for(int i=0;i<14;i++) {
                var t=random(c.user,choices);if(t==null)continue;
                double a=i*Math.PI*2/14;Vec3 pos=c.origin.add(Math.cos(a)*3,7+i%3*.4,Math.sin(a)*3);
                var s=c.shot(pos,center(t).subtract(pos),t,1.2,.4,LIGHTNING,GOLD,0xffedb8,0,false,false);
                s.hits=3;s.maxAge=140;s.range=Double.POSITIVE_INFINITY;
            }
        });
    }
    private static void earth(Cast c) {
        c.at(0,()->{
            for(int i=0;i<13;i++) {
                double a=i*Math.PI*2/13;Vec3 point=c.origin.add(Math.cos(a)*2,.17,Math.sin(a)*2);
                // +Z is the tile's printed face; keep it looking inward toward the caster.
                c.flash(FACING_TILE,point,c.user.getEyePosition().subtract(point).normalize(),20,.25F,0xe7cea1,i);
            }
            wave(c,c.origin,4,TRUE,0xeac997);
        });
        c.at(20,()-> {
            var t=c.target(false);if(t==null)t=random(c.user,nearby(c.user,c.user.position(),SEARCH_RADIUS));
            c.shot(c.user.getEyePosition().add(0,2,0),c.user.getLookAngle(),t,1.2,8,MAGIC,FACING_TILE,0xffddaa,32,true,false).visualScale=.25F;
        });
    }
    private static void wave(Cast c,Vec3 origin,double ratio,YakumanCombat.Kind kind,int rgb) {
        c.flash(RING,origin,new Vec3(0,1,0),10,8,rgb,0);
        for(int i=1;i<=10;i++) {double r=i*.8;c.later(i,()->{
            for(var t:nearby(c.user,origin,r))if(c.waveHits.add(t.getUUID())) {
                c.hit(t,ratio,kind);
            }
        });}
    }
    private static void cardinal(Cast c) {
        for(int i=0;i<4;i++) {
            int side=i;c.at(i*4,()->{
                Vec3 dir=new Vec3(Math.cos(side*Math.PI/2),0,Math.sin(side*Math.PI/2));
                var shot=c.shot(c.user.getEyePosition(),dir,null,.4,c.spec==YakumanSpec.DAISUUSHII?2.5:6,
                        PHYSICAL,SLASH,0xc3e6d9,0,false,true);
                shot.width=1.5;
                if(c.spec==YakumanSpec.SUUSHI_JIKOKU) {
                    shot.shared=c.initialHits;
                    shot.impact=(target,point)->{c.hit(target,6,PHYSICAL);for(int j=0;j<4;j++)c.later(j*2,
                            ()->c.cut(enemy(c.user,target)?center(target):point,2,2,PHYSICAL,0xe7defb));};
                }
            });
        }
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e) {
        if(e.phase!=TickEvent.Phase.END)return;
        while(!PENDING.isEmpty())ACTIVE.add(PENDING.remove());
        for(var it=ACTIVE.iterator();it.hasNext();) {
            var c=it.next();
            if(!c.valid() || c.age>260){c.end();it.remove();continue;}
            c.advance();
            if(c.actions.isEmpty()&&c.shots.isEmpty()&&c.added.isEmpty())it.remove();
        }
        CUTS.keySet().removeIf(Entity::isRemoved);
        YakumanStun.tick();
        YakumanVisualPacket.flush();
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent e){ACTIVE.clear();PENDING.clear();CUTS.clear();YakumanVisualPacket.clear();YakumanStun.clear();}
}
