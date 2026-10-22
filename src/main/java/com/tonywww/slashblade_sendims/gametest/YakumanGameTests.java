package com.tonywww.slashblade_sendims.gametest;

import com.mojang.authlib.GameProfile;
import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.entities.EntityYakumanDrive;
import com.tonywww.slashblade_sendims.yakuman.*;
import com.tonywww.slashblade_sendims.se.NamelessSeries;
import mods.flammpfeil.slashblade.registry.*;
import mods.flammpfeil.slashblade.util.AttackManager;
import mods.flammpfeil.slashblade.util.KnockBacks;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.entity.EntitySlashEffect;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.*;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.gametest.*;
import dev.shadowsoffire.attributeslib.api.ALObjects;
import java.util.*;
import static com.tonywww.slashblade_sendims.yakuman.YakumanRegistry.Effect.*;

@GameTestHolder(SenDims.MOD_ID)
@PrefixGameTestTemplate(false)
public final class YakumanGameTests {
    private static ServerPlayer player(GameTestHelper h,YakumanSpec spec) {
        var p=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"YakumanTest"));
        p.setPos(h.absoluteVec(new Vec3(8,2,4)));p.setOnGround(true);p.setYRot(0);p.setXRot(0);
        var blade=new ItemStack(SlashBladeItems.SLASHBLADE.get());blade.enchant(Enchantments.UNBREAKING,1);
        p.setItemSlot(EquipmentSlot.MAINHAND,blade);
        p.getAttribute(ALObjects.Attributes.CRIT_CHANCE.get()).setBaseValue(0);
        p.getAttribute(ALObjects.Attributes.CRIT_DAMAGE.get()).setBaseValue(1);
        var state=NamelessSeries.getBladeState(p);state.setNonEmpty();state.setDefaultBewitched(true);
        state.setBaseAttackModifier(4);state.setMaxDamage(10000);state.setSlashArtsKey(spec.id);
        state.setComboSeq(ComboStateRegistry.NONE.getId());state.setLastActionTime(h.getLevel().getGameTime());
        return p;
    }
    private static LivingEntity target(GameTestHelper h,ServerPlayer p,double distance) {
        var t=h.spawnWithNoFreeWill(EntityType.HUSK,new Vec3(8,2,4+distance));
        t.setNoGravity(true);t.setSilent(true);t.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10000);
        t.getAttribute(Attributes.ARMOR).setBaseValue(0);t.setHealth(10000);
        t.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        return t;
    }
    private static void normal(ServerPlayer p,LivingEntity target) {
        var state=NamelessSeries.getBladeState(p);
        state.setComboSeq(ComboStateRegistry.COMBO_A1.getId());state.setLastActionTime(p.level().getGameTime());
        YakumanEffects.combo(p,ComboStateRegistry.COMBO_A1.getId(),100);
        target.invulnerableTime=0;AttackManager.doMeleeAttack(p,target,true,false,1);
    }
    private static void release(ServerPlayer p) {
        var state=NamelessSeries.getBladeState(p);
        state.setComboSeq(ComboStateRegistry.NONE.getId());state.setLastActionTime(p.level().getGameTime());
        state.doChargeAction(p,state.getFullChargeTicks(p));
    }
    private static void near(GameTestHelper h,double value,double expected,String label) {
        h.assertTrue(Math.abs(value-expected)<.02,label+" expected "+expected+" got "+value);
    }
    /** Only used by single-test batches: wide projectiles can reach fixtures left by other batches. */
    private static void isolate(GameTestHelper h,ServerPlayer p) {
        List<Entity> old=new ArrayList<>();
        for(var entity:h.getLevel().getAllEntities())if(entity instanceof LivingEntity&&entity!=p)old.add(entity);
        old.forEach(Entity::discard);
    }
    public static final class Probe {
        final LivingEntity target;double total;final List<String> hits=new ArrayList<>();
        Probe(LivingEntity t){target=t;}
        @SubscribeEvent(priority=EventPriority.LOWEST) public void hurt(LivingHurtEvent e){if(e.getEntity()==target){total+=e.getAmount();hits.add(target.level().getGameTime()+":"+e.getAmount()+":"+(e.getSource().getEntity()==null?"none":e.getSource().getEntity().getUUID()));}}
    }
    @GameTest(template="holy_sword_test",batch="yakuman")
    public static void registrationsAndAmmoReload(GameTestHelper h) {
        var p=player(h,YakumanSpec.KOKUSHI);
        for(var spec:YakumanSpec.values()) {
            h.assertTrue(ComboStateRegistry.REGISTRY.get().containsKey(spec.id),"Missing combo "+spec);
            h.assertTrue(SlashArtsRegistry.REGISTRY.get().containsKey(spec.id),"Missing art "+spec);
        }
        release(p);
        near(h,p.getMainHandItem().getOrCreateTag().getInt("sdbf.ammo"),13,"Initial reload");
        YakumanArts.fireAmmo(p,YakumanSpec.KOKUSHI);
        near(h,p.getMainHandItem().getOrCreateTag().getInt("sdbf.ammo"),12,"First shot");
        YakumanArts.fireAmmo(p,YakumanSpec.KOKUSHI);
        near(h,p.getMainHandItem().getOrCreateTag().getInt("sdbf.ammo"),12,"Same-tick interval");
        release(p);
        near(h,p.getMainHandItem().getOrCreateTag().getInt("sdbf.ammo"),13,"Reload overwrites");
        p.discard();h.succeed();
    }
    @GameTest(template="holy_sword_test",batch="yakuman",timeoutTicks=180)
    public static void sevenHonorsDamageBudget(GameTestHelper h) {
        var p=player(h,YakumanSpec.TSUUIISOU);var t=target(h,p,3);
        NamelessSeries.getBladeState(p).setTargetEntityId(t.getId());
        double base=YakumanCombat.base(p,t);Probe probe=new Probe(t);MinecraftForge.EVENT_BUS.register(probe);
        release(p);
        h.runAtTickTime(166,()->{
            try {near(h,probe.total,base*21,"Seven honors full pulses and final seven cards");h.succeed();}
            finally {MinecraftForge.EVENT_BUS.unregister(probe);p.discard();}
        });
    }
    @GameTest(template="holy_sword_test",batch="yakuman")
    public static void hiddenMarksAndApCommit(GameTestHelper h) {
        var p=player(h,YakumanSpec.CHINROU_SUUKOU);var t=target(h,p,2);
        var state=NamelessSeries.getBladeState(p);state.addSpecialEffect(HIDDEN_TRIPLETS.id);
        for(int i=0;i<36;i++)normal(p,t);
        var d=p.getPersistentData().getCompound("sbsd.yakuman");
        near(h,d.getInt("marks"),4,"Four marks after thirty-six accepted normal hits");
        release(p);near(h,d.getInt("marks"),0,"Accepted art consumes marks");
        state.addSpecialEffect(HEAVEN.id);
        near(h,YakumanEffects.apMultiplier(p),.8,"Ready quote");
        near(h,YakumanEffects.apMultiplier(p),.8,"Quote cannot commit cooldown");
        release(p);near(h,YakumanEffects.apMultiplier(p),1,"Successful release commits cooldown");
        p.discard();h.succeed();
    }
    @GameTest(template="holy_sword_test",batch="yakuman",timeoutTicks=160)
    public static void forcedStunExpiresOnGlobalTime(GameTestHelper h) {
        var p=player(h,YakumanSpec.CHIIHOU);var t=target(h,p,3);
        YakumanStun.apply(t,140);h.assertTrue(YakumanStun.active(t),"Stun was not active");
        var anchor=t.position();t.setDeltaMovement(new Vec3(1,0,0));
        h.runAtTickTime(40,()->near(h,t.position().distanceTo(anchor),0,"Stun freezes movement"));
        h.runAtTickTime(141,()->{h.assertFalse(YakumanStun.active(t),"Stun did not expire");p.discard();h.succeed();});
    }

    @GameTest(template="holy_sword_test",batch="yakuman")
    public static void normalSwingDedupAndKongBonus(GameTestHelper h) {
        var p=player(h,YakumanSpec.HEKIGYOKU_SUUKANTSU);var t=target(h,p,2);
        var state=NamelessSeries.getBladeState(p);state.addSpecialEffect(KONG.id);state.addSpecialEffect(HIDDEN_TRIPLETS.id);
        normal(p,t);
        target(h,p,3);t.invulnerableTime=0;AttackManager.doMeleeAttack(p,t,true,false,1);
        var d=p.getPersistentData().getCompound("sbsd.yakuman");
        near(h,d.getInt("hidden"),1,"A second victim/hit in one swing cannot increment progress");
        for(int i=0;i<3;i++)normal(p,t);
        h.assertTrue(d.getBoolean("kong_ready"),"Kong must be ready after four effective swings");
        Probe probe=new Probe(t);MinecraftForge.EVENT_BUS.register(probe);
        double base=YakumanCombat.base(p,t);
        try {normal(p,t);near(h,probe.total,base*1.5,"Next normal hit has the fifty-percent bonus");}
        finally {MinecraftForge.EVENT_BUS.unregister(probe);}
        near(h,d.getInt("kong"),1,"Boosted normal counts toward next cycle");
        h.assertFalse(d.getBoolean("kong_ready"),"Boost consumed once");p.discard();h.succeed();
    }

    @GameTest(template="holy_sword_test",batch="yakuman",timeoutTicks=90)
    public static void kongFourHitsAndNoCollisionLockedFall(GameTestHelper h) {
        var p=player(h,YakumanSpec.SUUKANTSU);var t=target(h,p,3);
        var state=NamelessSeries.getBladeState(p);state.setTargetEntityId(t.getId());
        h.getLevel().setBlock(t.blockPosition().above(3),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);
        Probe probe=new Probe(t);MinecraftForge.EVENT_BUS.register(probe);double base=YakumanCombat.base(p,t);
        release(p);YakumanArts.fireAmmo(p,YakumanSpec.SUUKANTSU);
        h.runAtTickTime(50,()->{
            try {near(h,probe.total,base*3,"One ammo produces four 0.75 hits through overhead blocks");near(h,probe.hits.size(),4,"One stack must not produce sixteen damage pulses");h.succeed();}
            finally {MinecraftForge.EVENT_BUS.unregister(probe);p.discard();}
        });
    }

    @GameTest(template="holy_sword_test",batch="yakuman_cardinal",timeoutTicks=100)
    public static void cardinalInitialHitDedupAndFollowups(GameTestHelper h) {
        var p=player(h,YakumanSpec.SUUSHI_JIKOKU);
        // Other batches leave combat fixtures alive. Isolate this single-target damage budget.
        isolate(h,p);
        var t=target(h,p,1);
        // Close enough to intersect more than one cardinal slash's swept width.
        Probe probe=new Probe(t);MinecraftForge.EVENT_BUS.register(probe);double base=YakumanCombat.base(p,t);
        h.runAtTickTime(2,()->{
            SenDims.LOGGER.info("YAKUMAN_CARDINAL_ISOLATED {}",YakumanCombat.nearby(p,p.position(),40).stream().map(m->m.getId()+":"+m.position()).toList());
            release(p);
        });
        h.runAtTickTime(65,()->{
            SenDims.LOGGER.info("YAKUMAN_WIND_TEST caster {} hits {} nearby {}",p.getUUID(),probe.hits,
                    YakumanCombat.nearby(p,t.position(),3).stream().map(m->m.getId()+":"+m.position()).toList());
            try {near(h,probe.total,base*14,"Shared initial dedup plus four two-times area followups");h.succeed();}
            finally {MinecraftForge.EVENT_BUS.unregister(probe);p.discard();}
        });
    }

    /** Simulates integrations attempting to re-trigger ammunition from a secondary hit. */
    public static final class ReentryProbe {
        final ServerPlayer player;int attacks,hits;boolean nestedDamage;
        ReentryProbe(ServerPlayer player){this.player=player;}
        @SubscribeEvent public void attack(AttackEntityEvent e){if(e.getEntity()==player)attacks++;}
        @SubscribeEvent public void hit(SlashBladeEvent.HitEvent e) {
            if(e.getUser()!=player)return;
            hits++;
            YakumanArts.fireAmmo(player,YakumanSpec.KOKUSHI);
            nestedDamage|=YakumanCombat.hit(player,e.getTarget(),10,1,YakumanCombat.Kind.PHYSICAL);
        }
    }
    private static void slash(ServerPlayer p) {
        var state=NamelessSeries.getBladeState(p);
        MinecraftForge.EVENT_BUS.post(new SlashBladeEvent.DoSlashEvent(p.getMainHandItem(),state,p,0,false,1,KnockBacks.cancel));
    }
    @GameTest(template="holy_sword_test",batch="yakuman_reentry",timeoutTicks=60)
    public static void kokushiUsesSlashAndSecondaryHitCannotReenter(GameTestHelper h) {
        var p=player(h,YakumanSpec.KOKUSHI);var t=target(h,p,3);
        var state=NamelessSeries.getBladeState(p);state.setTargetEntityId(t.getId());
        state.addSpecialEffect(HIDDEN_TRIPLETS.id);
        release(p);var tag=p.getMainHandItem().getOrCreateTag();
        state.setComboSeq(ComboStateRegistry.COMBO_A1.getId());state.setLastActionTime(h.getLevel().getGameTime());
        YakumanEffects.combo(p,ComboStateRegistry.COMBO_A1.getId(),100);
        near(h,tag.getInt("sdbf.ammo"),13,"Combo boundary must not fire ammunition");
        var damage=new Probe(t);var reentry=new ReentryProbe(p);
        MinecraftForge.EVENT_BUS.register(damage);MinecraftForge.EVENT_BUS.register(reentry);
        double base=YakumanCombat.base(p,t);
        slash(p);slash(p);
        near(h,tag.getInt("sdbf.ammo"),12,"Two slash callbacks in one swing consume only one round");
        h.runAtTickTime(15,()->{
            try {
                near(h,tag.getInt("sdbf.ammo"),12,"Secondary hit cannot discharge more ammunition");
                near(h,damage.total,base*1.25,"Exactly one thirteen-orphans cut");
                near(h,reentry.attacks,0,"Secondary damage never posts AttackEntityEvent");
                near(h,reentry.hits,1,"Secondary damage posts one confirmed HitEvent");
                h.assertFalse(reentry.nestedDamage,"Recursive secondary damage was accepted");
                near(h,p.getPersistentData().getCompound("sbsd.yakuman").getInt("hidden"),0,"SA damage is not an ordinary hit counter");
                h.succeed();
            } finally {MinecraftForge.EVENT_BUS.unregister(damage);MinecraftForge.EVENT_BUS.unregister(reentry);p.discard();}
        });
    }
    @GameTest(template="holy_sword_test",batch="yakuman_reentry",timeoutTicks=60)
    public static void zeroIntervalAmmoStillDeduplicatesOneSwing(GameTestHelper h) {
        var p=player(h,YakumanSpec.CHUUREN);target(h,p,3);release(p);
        var state=NamelessSeries.getBladeState(p);
        state.setComboSeq(ComboStateRegistry.COMBO_A1.getId());state.setLastActionTime(h.getLevel().getGameTime());
        YakumanEffects.combo(p,ComboStateRegistry.COMBO_A1.getId(),100);slash(p);slash(p);slash(p);
        near(h,p.getMainHandItem().getOrCreateTag().getInt("sdbf.ammo"),8,"Zero interval means one round per slash swing, not per nested callback");
        p.discard();h.succeed();
    }
    @GameTest(template="holy_sword_test",batch="yakuman_native_slash",timeoutTicks=70)
    public static void nativeSlashesDamageLockedTargetOnceEach(GameTestHelper h) {
        var p=player(h,YakumanSpec.SUUANKOU);var t=target(h,p,3);
        NamelessSeries.getBladeState(p).setTargetEntityId(t.getId());
        var probe=new Probe(t);MinecraftForge.EVENT_BUS.register(probe);double base=YakumanCombat.base(p,t);
        release(p);
        h.runAtTickTime(6,()->h.assertTrue(!h.getLevel().getEntitiesOfClass(EntitySlashEffect.class,t.getBoundingBox().inflate(3),
                s->s.getPersistentData().getBoolean(YakumanArts.NATIVE_SLASH)).isEmpty(),"No native SlashBlade slash entity rendered"));
        h.runAtTickTime(40,()->{
            try {near(h,probe.total,base*10,"Four native cuts preserve the ten-times physical budget");near(h,probe.hits.size(),4,"Native lifetime must not repeat area damage");h.succeed();}
            finally {MinecraftForge.EVENT_BUS.unregister(probe);p.discard();}
        });
    }

    @GameTest(template="holy_sword_test",batch="yakuman_projectile_speed",timeoutTicks=60)
    public static void mediumWindDoublesTravelLifetime(GameTestHelper h) {
        var p=player(h,YakumanSpec.DAISUUSHII);var t=target(h,p,13);
        var probe=new Probe(t);MinecraftForge.EVENT_BUS.register(probe);double base=YakumanCombat.base(p,t);
        release(p);
        h.runAtTickTime(12,()->near(h,probe.total,0,"Medium-speed wind must not arrive at the old doubled speed"));
        h.runAtTickTime(30,()->{
            try {
                near(h,probe.total,base*2.5,"Medium-speed wind still reaches its target once");
                var slashes=h.getLevel().getEntitiesOfClass(EntityYakumanDrive.class,p.getBoundingBox().inflate(40),
                        s->s.getOwner()==p);
                h.assertTrue(!slashes.isEmpty(),"Wind must remain visible beyond the former twenty-tick flight");
                h.assertTrue(slashes.stream().allMatch(s->s.getLifetime()>=40),"Native wind lifetime must double");
                h.succeed();
            }
            finally {MinecraftForge.EVENT_BUS.unregister(probe);p.discard();}
        });
    }

    @GameTest(template="holy_sword_test",batch="yakuman_search_revision",timeoutTicks=50)
    public static void unlockedOrphansSearchTwelveBlocks(GameTestHelper h) {
        var p=player(h,YakumanSpec.KOKUSHI);
        isolate(h,p);
        var t=target(h,p,11);var probe=new Probe(t);MinecraftForge.EVENT_BUS.register(probe);
        double base=YakumanCombat.base(p,t);
        h.runAtTickTime(2,()->{
            SenDims.LOGGER.info("YAKUMAN_SEARCH caster {} target {} choices {}",p.position(),t.position(),
                    YakumanCombat.nearby(p,p.position(),12).stream().map(m->m.getId()+":"+m.position()).toList());
            h.assertTrue(YakumanCombat.nearest(p,p.position())==t,"Unlocked target at eleven blocks was not acquired");
        });
        release(p);YakumanArts.fireAmmo(p,YakumanSpec.KOKUSHI);
        h.runAtTickTime(15,()->{
            try {near(h,probe.total,base*1.25,"Unlocked ammo targets beyond the former eight-block radius");h.succeed();}
            finally {MinecraftForge.EVENT_BUS.unregister(probe);p.discard();}
        });
    }

    @GameTest(template="holy_sword_test",batch="yakuman_dragon_revision",timeoutTicks=60)
    public static void dragonPiercesTargetsAtIncreasedSpeed(GameTestHelper h) {
        var p=player(h,YakumanSpec.DAISANGEN);
        isolate(h,p);
        var first=target(h,p,5);var second=target(h,p,11);
        NamelessSeries.getBladeState(p).setTargetEntityId(first.getId());
        var a=new Probe(first);var b=new Probe(second);
        MinecraftForge.EVENT_BUS.register(a);MinecraftForge.EVENT_BUS.register(b);
        double base=YakumanCombat.base(p,first);
        release(p);YakumanArts.fireAmmo(p,YakumanSpec.DAISANGEN);
        h.runAtTickTime(9,()->{
            try {
                near(h,a.total,base*5,"Piercing dragon hits locked target once");
                near(h,b.total,base*5,"Faster dragon continues through the first target");
                near(h,a.hits.size(),1,"Homing dragon must not circle and re-hit its initial target");
                near(h,b.hits.size(),1,"Piercing preserves per-target damage");
                h.succeed();
            } finally {MinecraftForge.EVENT_BUS.unregister(a);MinecraftForge.EVENT_BUS.unregister(b);p.discard();}
        });
    }

    @GameTest(template="holy_sword_test",batch="yakuman_collision_revision",timeoutTicks=60)
    public static void swordWaveDoubledCollisionWidth(GameTestHelper h) {
        var p=player(h,YakumanSpec.SHOUSUUSHII);
        isolate(h,p);
        var t=target(h,p,8);t.setPos(t.position().add(2.5,0,0));
        var probe=new Probe(t);MinecraftForge.EVENT_BUS.register(probe);double base=YakumanCombat.base(p,t);
        release(p);
        h.runAtTickTime(40,()->{
            SenDims.LOGGER.info("YAKUMAN_COLLISION caster {} target {} hits {}",p.position(),t.position(),probe.hits);
            try {near(h,probe.total,base*7.5,"Three piercing winds hit a target outside their former width");h.succeed();}
            finally {MinecraftForge.EVENT_BUS.unregister(probe);p.discard();}
        });
    }

    @GameTest(template="holy_sword_test",batch="yakuman_bamboo_revision",timeoutTicks=80)
    public static void bambooFasterSequencePreservesDamage(GameTestHelper h) {
        var p=player(h,YakumanSpec.RYUUIISOU);
        isolate(h,p);
        var t=target(h,p,6);NamelessSeries.getBladeState(p).setTargetEntityId(t.getId());
        var probe=new Probe(t);MinecraftForge.EVENT_BUS.register(probe);double base=YakumanCombat.base(p,t);
        release(p);
        h.runAtTickTime(38,()->{
            SenDims.LOGGER.info("YAKUMAN_BAMBOO caster {} target {} hits {}",p.position(),t.position(),probe.hits);
            try {
                near(h,probe.total,base*10.05,"Five compressed waves retain twenty-three 0.35 hits and one two-times finisher");
                near(h,probe.hits.size(),24,"Visual density must not create additional damage projectiles");
                h.succeed();
            } finally {MinecraftForge.EVENT_BUS.unregister(probe);p.discard();}
        });
    }

    @GameTest(template="holy_sword_test",batch="yakuman_orphans_flight",timeoutTicks=60)
    public static void orphansTravelAndHitAnInterveningEnemy(GameTestHelper h) {
        var p=player(h,YakumanSpec.KOKUSHI);
        isolate(h,p);
        var locked=target(h,p,22);var intercept=target(h,p,8);
        NamelessSeries.getBladeState(p).setTargetEntityId(locked.getId());
        var far=new Probe(locked);var near=new Probe(intercept);
        MinecraftForge.EVENT_BUS.register(far);MinecraftForge.EVENT_BUS.register(near);
        double base=YakumanCombat.base(p,intercept);
        release(p);YakumanArts.fireAmmo(p,YakumanSpec.KOKUSHI);
        h.runAtTickTime(3,()->{
            near(h,far.total,0,"A locked enemy must not take damage before the sword wave arrives");
            near(h,near.total,0,"The projectile must traverse the gap to an intervening enemy");
        });
        h.runAtTickTime(24,()->{
            try {
                near(h,near.total,base*1.25,"The first enemy on the travelled path receives the original damage");
                near(h,near.hits.size(),1,"One round remains one confirmed hit");
                near(h,far.total,0,"A stopped projectile must not also damage the original lock");
                h.succeed();
            } finally {MinecraftForge.EVENT_BUS.unregister(far);MinecraftForge.EVENT_BUS.unregister(near);p.discard();}
        });
    }

    @GameTest(template="holy_sword_test",batch="yakuman_golden_orphans_flight",timeoutTicks=60)
    public static void goldenOrphansTravelBeforeThreeHits(GameTestHelper h) {
        var p=player(h,YakumanSpec.TENJU_KOKUSHI);
        isolate(h,p);
        var t=target(h,p,16);NamelessSeries.getBladeState(p).setTargetEntityId(t.getId());
        var probe=new Probe(t);MinecraftForge.EVENT_BUS.register(probe);
        double base=YakumanCombat.base(p,t);
        release(p);YakumanArts.fireAmmo(p,YakumanSpec.TENJU_KOKUSHI);
        h.runAtTickTime(5,()->near(h,probe.total,0,"Golden cuts must fly from the caster before damaging a distant target"));
        h.runAtTickTime(24,()->{
            try {
                near(h,probe.total,base*1.8,"Three travelling cuts retain the three 0.6 damage ratios");
                near(h,probe.hits.size(),3,"One golden round still produces exactly three confirmed hits");
                near(h,p.getMainHandItem().getOrCreateTag().getInt("sdbf.ammo"),YakumanSpec.TENJU_KOKUSHI.ammo-1,
                        "Three projectiles consume only one round");
                h.succeed();
            } finally {MinecraftForge.EVENT_BUS.unregister(probe);p.discard();}
        });
    }


}
