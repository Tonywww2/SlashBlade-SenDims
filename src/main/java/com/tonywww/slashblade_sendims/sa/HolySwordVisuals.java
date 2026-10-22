package com.tonywww.slashblade_sendims.sa;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.network.HolySwordVisualPacket;
import com.tonywww.slashblade_sendims.network.HolySwordVisualPacket.Stage;
import com.tonywww.slashblade_sendims.se.HolySwordEffects;
import com.tonywww.slashblade_sendims.se.NamelessSeries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative phase markers; all decorative geometry is client-only. */
@Mod.EventBusSubscriber(modid = SenDims.MOD_ID)
public final class HolySwordVisuals {
    private record Charge(ItemStack blade, boolean ex, long id) {}
    private static final Map<UUID, Charge> CHARGES = new HashMap<>();
    private HolySwordVisuals() {}

    public static void phase(LivingEntity user, long id, boolean ex, Stage stage, Vec3 origin, Vec3 end) {
        if (!(user.level() instanceof ServerLevel level)) return;
        float height = stage == Stage.FINISH || stage == Stage.PULSE_0 || stage == Stage.PULSE_1 || stage == Stage.PULSE_2
                ? skyHeight(level, user, origin, ex ? 96 : 64) : 0;
        send(level, new HolySwordVisualPacket(level.dimension().location(), user.getUUID(), user.getId(),
                id, stage, ex, origin, end, height, 0));
    }

    private static float skyHeight(ServerLevel level, LivingEntity user, Vec3 origin, float max) {
        // Five probes keep the pillar footprint below low ceilings, including the Nether roof.
        double height = Math.min(max, level.getMaxBuildHeight() - origin.y);
        for (int i = 0; i < 5; i++) {
            Vec3 start = origin.add(i == 1 ? 1.5 : i == 2 ? -1.5 : 0, 0,
                    i == 3 ? 1.5 : i == 4 ? -1.5 : 0);
            Vec3 hit = level.clip(new ClipContext(start, start.add(0, height, 0),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, user)).getLocation();
            height = Math.min(height, Math.max(0, hit.y - start.y - 0.2));
        }
        return (float) Math.max(0, height);
    }

    private static void send(ServerLevel level, HolySwordVisualPacket p) {
        double range = p.excalibur() ? 192 : 128;
        for (ServerPlayer observer : level.players()) {
            if (observer.position().distanceToSqr(p.origin()) <= range * range)
                SenDims.NETWORK.send(PacketDistributor.PLAYER.with(() -> observer), p);
        }
    }

    @SubscribeEvent
    public static void charge(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var state = NamelessSeries.getBladeState(player);
        Charge old = CHARGES.get(player.getUUID());
        boolean charging = HolySwordEffects.charging(player, state)
                && HolySwordArts.isArt(state.getSlashArtsKey());
        boolean ex = charging && HolySwordArts.EXCALIBUR.equals(state.getSlashArtsKey());
        if (old != null && (!charging || old.blade != player.getMainHandItem() || old.ex != ex)) {
            phase(player, old.id, old.ex, Stage.STOP_CHARGE, player.getEyePosition(), player.getEyePosition());
            CHARGES.remove(player.getUUID()); old = null;
        }
        if (!charging) return;
        boolean fresh = old == null;
        if (fresh) {
            old = new Charge(player.getMainHandItem(), ex, player.level().getGameTime());
            CHARGES.put(player.getUUID(), old);
        }
        // Sparse pose/lease refresh also supports observers beyond normal player tracking distance.
        if (fresh || player.getTicksUsingItem() % 10 == 0) {
            Vec3 origin = player.getEyePosition().add(0, -0.25, 0);
            var level = player.serverLevel();
            send(level, new HolySwordVisualPacket(level.dimension().location(), player.getUUID(), player.getId(),
                    old.id, Stage.CHARGE, ex, origin, origin.add(player.getLookAngle().scale(1.2)),
                    0, player.getTicksUsingItem()));
        }
    }

    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { CHARGES.remove(event.getEntity().getUUID()); }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { CHARGES.clear(); }
}
