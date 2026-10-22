package com.tonywww.slashblade_sendims.gametest;

import com.mojang.authlib.GameProfile;
import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.registeries.SBSDMobEffects;
import com.tonywww.slashblade_sendims.registeries.SBSDSpecialEffects;
import com.tonywww.slashblade_sendims.sa.HolySwordArts;
import com.tonywww.slashblade_sendims.se.HolySwordEffects;
import com.tonywww.slashblade_sendims.se.NamelessSeries;
import dev.shadowsoffire.attributeslib.api.ALObjects;
import mods.flammpfeil.slashblade.ability.SuperSlashArts;
import mods.flammpfeil.slashblade.capability.concentrationrank.ConcentrationRankCapabilityProvider;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import mods.flammpfeil.slashblade.registry.SlashBladeItems;
import mods.flammpfeil.slashblade.util.AttackHelper;
import mods.flammpfeil.slashblade.util.AttackManager;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** Run with runGameTestServer: exercises the actual native state, capabilities and event bus. */
@GameTestHolder(SenDims.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HolySwordGameTests {
    private static ServerPlayer player(GameTestHelper helper, boolean ex) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "HolySwordTest"));
        Vec3 pos = helper.absoluteVec(new Vec3(8, 2, 4));
        player.setPos(pos); player.setYRot(0); player.setXRot(0); player.setOnGround(true);
        // The pack rolls Apothic Attributes crits for every damage source. Fix the
        // test actor's crit damage at 1x so core SA ratios are deterministic.
        player.getAttribute(ALObjects.Attributes.CRIT_CHANCE.get()).setBaseValue(0);
        player.getAttribute(ALObjects.Attributes.CRIT_DAMAGE.get()).setBaseValue(1);
        ItemStack blade = new ItemStack(SlashBladeItems.SLASHBLADE.get());
        blade.enchant(Enchantments.UNBREAKING, 1);
        player.setItemSlot(EquipmentSlot.MAINHAND, blade);
        ISlashBladeState state = NamelessSeries.getBladeState(player);
        helper.assertTrue(state != null, "Native blade capability missing");
        state.setNonEmpty(); state.setDefaultBewitched(true);
        state.setSlashArtsKey(ex ? HolySwordArts.EXCALIBUR : HolySwordArts.CALIBURN);
        state.setBaseAttackModifier(4); state.setMaxDamage(1000); state.setDamage(0);
        state.setComboSeq(ComboStateRegistry.NONE.getId()); state.setLastActionTime(helper.getLevel().getGameTime());
        state.addSpecialEffect(SBSDSpecialEffects.MANA_BURST.getId());
        if (ex) state.addSpecialEffect(SBSDSpecialEffects.INVISIBLE_AIR.getId());
        return player;
    }

    private static LivingEntity target(GameTestHelper helper, double forward) {
        var mob = helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(8, 2, 4 + forward));
        mob.setNoGravity(true); mob.setSilent(true);
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        mob.getAttribute(Attributes.ARMOR).setBaseValue(0);
        mob.getAttribute(ALObjects.Attributes.CRIT_CHANCE.get()).setBaseValue(0);
        mob.getAttribute(ALObjects.Attributes.CRIT_DAMAGE.get()).setBaseValue(1);
        mob.setHealth(1000);
        return mob;
    }

    private static void near(GameTestHelper helper, double actual, double expected, String reason) {
        helper.assertTrue(Math.abs(actual - expected) < 0.01,
                reason + ": expected " + expected + ", got " + actual);
    }

    public static final class DamageProbe {
        private final LivingEntity target;
        public double physical, magic;
        public DamageProbe(LivingEntity target) { this.target = target; }
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void damage(LivingHurtEvent event) {
            if (event.getEntity() != target) return;
            if (event.getSource().is(DamageTypes.INDIRECT_MAGIC)) magic += event.getAmount();
            else physical += event.getAmount();
        }
    }

    public static final class CancelRelease {
        @SubscribeEvent public void release(SlashBladeEvent.PerformSlashArtEvent event) { event.setCanceled(true); }
    }

    @GameTest(template = "holy_sword_test")
    public static void caliburnPhysicalBudgetAndAutomaticFollowup(GameTestHelper helper) {
        ServerPlayer player = player(helper, false);
        LivingEntity target = target(helper, 3);
        ISlashBladeState state = NamelessSeries.getBladeState(player);
        near(helper, state.getFullChargeTicks(player), 9, "Caliburn charge ticks");
        double base = AttackHelper.calculateTotalDamage(player, target, 1, false);
        helper.assertTrue(base > 0, "Native 1x damage must be positive");
        DamageProbe probe = new DamageProbe(target);
        MinecraftForge.EVENT_BUS.register(probe);
        try {
            state.doChargeAction(player, 9);
            near(helper, probe.physical, base * 3, "Opening cut");
            // Native inventory timeout resolution must enter the second animation itself.
            state.setLastActionTime(helper.getLevel().getGameTime() - 7);
            state.resolvCurrentComboState(player);
            near(helper, probe.physical, base * 13, "Complete Caliburn damage");
            near(helper, probe.magic, 0, "Caliburn cannot add magic damage");
            near(helper, player.getMainHandItem().getOrCreateTag().getInt(HolySwordEffects.HIT_COUNT), 0, "SA cannot build charge");
            state.resolvCurrentComboState(player);
            near(helper, probe.physical, base * 13, "Follow-up must not repeat");
        } finally { MinecraftForge.EVENT_BUS.unregister(probe); player.discard(); }
        helper.succeed();
    }

    @GameTest(template = "holy_sword_test")
    public static void excaliburThreeMixedPulses(GameTestHelper helper) {
        ServerPlayer player = player(helper, true);
        LivingEntity target = target(helper, 15);
        ISlashBladeState state = NamelessSeries.getBladeState(player);
        near(helper, state.getFullChargeTicks(player), 18, "Excalibur charge ticks");
        double base = AttackHelper.calculateTotalDamage(player, target, 1, false);
        DamageProbe probe = new DamageProbe(target);
        MinecraftForge.EVENT_BUS.register(probe);
        try {
            state.doChargeAction(player, 18);
            near(helper, probe.physical, base * 2, "First physical pulse");
            near(helper, probe.magic, base * 2, "First magic pulse");
            state.setLastActionTime(helper.getLevel().getGameTime() - 4);
            HolySwordArts.tick(player);
            state.setLastActionTime(helper.getLevel().getGameTime() - 10);
            HolySwordArts.tick(player);
            near(helper, probe.physical, base * 7, "Physical total");
            near(helper, probe.magic, base * 7, "Magic total");
            HolySwordArts.tick(player);
            near(helper, probe.physical + probe.magic, base * 14, "Pulses must not repeat");
        } finally { MinecraftForge.EVENT_BUS.unregister(probe); player.discard(); }
        helper.succeed();
    }

    @GameTest(template = "holy_sword_test")
    public static void chargeCancellationRoyalUpgradeAndPotion(GameTestHelper helper) {
        ServerPlayer player = player(helper, false);
        LivingEntity target = target(helper, 3);
        ISlashBladeState state = NamelessSeries.getBladeState(player);
        for (int i = 0; i < 20; i++) {
            state.setComboSeq(ComboStateRegistry.COMBO_A1.getId());
            state.setLastActionTime(helper.getLevel().getGameTime());
            target.invulnerableTime = 0;
            AttackManager.doMeleeAttack(player, target, true, false, 1);
        }
        near(helper, player.getMainHandItem().getOrCreateTag().getInt(HolySwordEffects.HIT_COUNT), 20, "Native melee hit counter");
        state.setComboSeq(ComboStateRegistry.NONE.getId());
        CancelRelease cancel = new CancelRelease();
        MinecraftForge.EVENT_BUS.register(cancel);
        try { state.doChargeAction(player, 9); }
        finally { MinecraftForge.EVENT_BUS.unregister(cancel); }
        helper.assertFalse(player.hasEffect(SBSDMobEffects.MANA_BURST.get()), "Canceled release granted a potion");
        near(helper, player.getMainHandItem().getOrCreateTag().getInt(HolySwordEffects.HIT_COUNT), 20, "Canceled release consumed charge");
        state.doChargeAction(player, 9);
        MobEffectInstance effect = player.getEffect(SBSDMobEffects.MANA_BURST.get());
        helper.assertTrue(effect != null && effect.getAmplifier() == 1 && effect.getDuration() == 100, "Expected Mana Burst II for 100 ticks");
        near(helper, player.getMainHandItem().getOrCreateTag().getInt(HolySwordEffects.HIT_COUNT), 0, "Accepted release must consume charge");
        state.setComboSeq(ComboStateRegistry.NONE.getId());
        state.addSpecialEffect(SBSDSpecialEffects.KINGS_SELECTION.getId());
        player.getCapability(ConcentrationRankCapabilityProvider.RANK_POINT).ifPresent(rank -> {
            rank.setRawRankPoint(rank.getUnitCapacity() * 4);
            rank.setLastUpdte(helper.getLevel().getGameTime());
        });
        helper.assertTrue(HolySwordEffects.royal(player, state), "Native rank A must enable King's Selection");
        player.getMainHandItem().getOrCreateTag().putInt(HolySwordEffects.HIT_COUNT, 14);
        state.doChargeAction(player, 9);
        effect = player.getEffect(SBSDMobEffects.MANA_BURST.get());
        helper.assertTrue(effect != null && effect.getAmplifier() == 2 && effect.getDuration() == 140, "Expected Mana Burst III for 140 ticks");
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STICK));
        LivingHurtEvent otherDamage = new LivingHurtEvent(target, player.damageSources().indirectMagic(player, player), 100);
        MinecraftForge.EVENT_BUS.post(otherDamage);
        near(helper, otherDamage.getAmount(), 115, "Independent potion must enhance other weapon/magic damage");
        player.discard(); helper.succeed();
    }

    @GameTest(template = "holy_sword_test")
    public static void nativeSuperArtAndChangedArt(GameTestHelper helper) {
        ServerPlayer player = player(helper, true);
        ISlashBladeState state = NamelessSeries.getBladeState(player);
        state.setKillCount(1000);
        player.getMainHandItem().getOrCreateTag().putInt(HolySwordEffects.HIT_COUNT, 20);
        SuperSlashArts.releaseSSA(player);
        helper.assertTrue(HolySwordArts.EXCALIBUR.equals(state.getComboSeq()), "Native SSA did not enter Excalibur");
        helper.assertTrue(player.hasEffect(SBSDMobEffects.MANA_BURST.get()), "Native SSA did not grant Mana Burst");
        near(helper, player.getMainHandItem().getOrCreateTag().getInt(HolySwordEffects.HIT_COUNT), 0, "SSA charge consumption");
        state.setComboSeq(ComboStateRegistry.NONE.getId());
        player.removeEffect(SBSDMobEffects.MANA_BURST.get());
        player.getMainHandItem().getOrCreateTag().putInt(HolySwordEffects.HIT_COUNT, 20);
        state.setSlashArtsKey(new net.minecraft.resources.ResourceLocation("slashblade:judgement_cut"));
        state.doChargeAction(player, 9);
        helper.assertTrue(player.hasEffect(SBSDMobEffects.MANA_BURST.get()), "Changing SA must retain the Mana Burst SE");
        near(helper, player.getMainHandItem().getOrCreateTag().getInt(HolySwordEffects.HIT_COUNT), 0, "Changed SA consumes charge once");
        player.discard(); helper.succeed();
    }

    @GameTest(template = "holy_sword_test")
    public static void windDistanceBoundaryAndCharging(GameTestHelper helper) {
        ServerPlayer player = player(helper, true);
        LivingEntity attacker = target(helper, 6);
        player.startUsingItem(InteractionHand.MAIN_HAND);
        helper.assertTrue(HolySwordEffects.charging(player, NamelessSeries.getBladeState(player)), "Native charge state missing");
        LivingHurtEvent boundary = new LivingHurtEvent(player, player.damageSources().mobAttack(attacker), 100);
        MinecraftForge.EVENT_BUS.post(boundary);
        near(helper, boundary.getAmount(), 100, "Exactly 6 blocks is not beyond 6 blocks");
        attacker.setPos(attacker.position().add(0, 0, 0.1));
        LivingHurtEvent distant = new LivingHurtEvent(player, player.damageSources().indirectMagic(attacker, attacker), 100);
        MinecraftForge.EVENT_BUS.post(distant);
        near(helper, distant.getAmount(), 90, "Distant magic damage reduction");
        player.stopUsingItem();
        LivingHurtEvent idle = new LivingHurtEvent(player, player.damageSources().mobAttack(attacker), 100);
        MinecraftForge.EVENT_BUS.post(idle);
        near(helper, idle.getAmount(), 100, "No protection when not charging");
        player.discard(); helper.succeed();
    }
}
