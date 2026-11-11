package com.tonywww.slashblade_sendims.se;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.entities.EntityMapleLeaf;
import com.tonywww.slashblade_sendims.registeries.SBSDEntities;
import com.tonywww.slashblade_sendims.registeries.SBSDSpecialEffects;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 天云系列 SE「鸦」与无念 SE「风」。
 * <p>
 * 斩击命中时，从命中处放出 2 片红枫叶，锁定最近的敌人，造成 5 + x × 攻击力的魔法伤害。
 * x = 最大提升 ×（1 − 0.75^((移速 / 0.1 − 1) × 移速倍率)），移速不高于 0.1 时为 0。
 * 「鸦」：最大提升 20%，移速倍率 1.5；带「风」时为 40% 与 2.5。
 * 同一 tick 内（同一次挥刀或同一段 SA）最多放出一次。
 */
@Mod.EventBusSubscriber(modid = SenDims.MOD_ID)
public final class AmagumoCrow {
    public static final double BASE_SPEED = 0.1D;
    public static final double CROW_MAX_BONUS = 0.20D;
    public static final double CROW_SPEED_RATE = 1.5D;
    public static final double WIND_MAX_BONUS = 0.40D;
    public static final double WIND_SPEED_RATE = 2.5D;
    public static final float FLAT_DAMAGE = 5.0F;
    public static final int LEAVES_PER_SWING = 2;

    private static final Map<UUID, Long> LAST_RELEASE = new HashMap<>();

    private AmagumoCrow() {}

    public static double bonus(double speed, double maxBonus, double speedRate) {
        double exponent = Math.max(0.0D, (speed / BASE_SPEED - 1.0D) * speedRate);
        return maxBonus * (1.0D - Math.pow(0.75D, exponent));
    }

    public static float leafDamage(LivingEntity user, boolean wind) {
        double x = bonus(user.getAttributeValue(Attributes.MOVEMENT_SPEED),
                wind ? WIND_MAX_BONUS : CROW_MAX_BONUS, wind ? WIND_SPEED_RATE : CROW_SPEED_RATE);
        return (float) (FLAT_DAMAGE + x * user.getAttributeValue(Attributes.ATTACK_DAMAGE));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHit(SlashBladeEvent.HitEvent event) {
        if (!(event.getUser() instanceof ServerPlayer player)) return;
        ISlashBladeState state = event.getSlashBladeState();
        LivingEntity target = event.getTarget();
        if (state == null || target == null
                || !SEEventHandlers.isSEActive(state, player.experienceLevel, SBSDSpecialEffects.AMAGUMO_CROW)) return;
        long now = player.level().getGameTime();
        Long last = LAST_RELEASE.put(player.getUUID(), now);
        if (last != null && last == now) return;
        boolean wind = SEEventHandlers.isSEActive(state, player.experienceLevel, SBSDSpecialEffects.AMAGUMO_WIND);
        release(player, target.getBoundingBox().getCenter(), leafDamage(player, wind));
    }

    public static void release(LivingEntity owner, Vec3 origin, float damage) {
        RandomSource random = owner.getRandom();
        for (int i = 0; i < LEAVES_PER_SWING; i++) {
            EntityMapleLeaf leaf = SBSDEntities.MAPLE_LEAF.get().create(owner.level());
            if (leaf == null) return;
            double side = i == 0 ? -1.0D : 1.0D;
            Vec3 spread = new Vec3((random.nextDouble() - 0.5D) * 0.5D, 0.25D + random.nextDouble() * 0.3D,
                    (random.nextDouble() - 0.5D) * 0.5D);
            Vec3 lateral = owner.getLookAngle().cross(new Vec3(0, 1, 0)).normalize().scale(0.28D * side);
            leaf.configure(owner, origin.add(spread.scale(0.6D)), lateral.add(0.0D, 0.22D + random.nextDouble() * 0.08D, 0.0D), damage);
            owner.level().addFreshEntity(leaf);
        }
    }

    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { LAST_RELEASE.remove(event.getEntity().getUUID()); }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { LAST_RELEASE.clear(); }
}
