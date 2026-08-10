package com.tonywww.slashblade_sendims.client.leader;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.api.leader.LeaderApi;
import com.tonywww.slashblade_sendims.api.leader.LeaderPhase;
import com.tonywww.slashblade_sendims.api.leader.LeaderProfile;
import com.tonywww.slashblade_sendims.api.leader.LeaderSnapshot;
import com.tonywww.slashblade_sendims.api.leader.client.ClientLeaderIndicatorApi;
import com.tonywww.slashblade_sendims.leader.LeaderIndicatorVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = SenDims.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LeaderStatusCharacterRenderer {
    private static final Component DANGER_CHARACTER = Component.literal("危");
    private static final Component STUN_CHARACTER = Component.literal("晕");
    private static final int TEXT_BACKGROUND = 0x78000000;
    private static final int FULL_BRIGHT = 0x00F000F0;
    private static final double MAX_RENDER_DISTANCE_SQUARED = 96.0 * 96.0;
        private static final Map<Integer, List<Particle>> ACTIVE_EXTERNAL_RINGS = new HashMap<>();
        private static final Set<UUID> ACTIVE_WARNINGS = new HashSet<>();

    private LeaderStatusCharacterRenderer() {
    }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
                if (event.phase != TickEvent.Phase.END) {
                        return;
                }
                clearExternalIndicatorRings();
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.level == null || minecraft.player == null) {
                        ACTIVE_WARNINGS.clear();
                        return;
                }
                Set<UUID> visibleWarnings = new HashSet<>();
                for (net.minecraft.world.entity.Entity candidate
                                : minecraft.level.entitiesForRendering()) {
                        if (!(candidate instanceof LivingEntity entity)
                                        || minecraft.player.distanceToSqr(entity) > MAX_RENDER_DISTANCE_SQUARED) {
                                continue;
                        }
                        Optional<LeaderSnapshot> snapshot = LeaderApi.getSnapshot(entity);
                        if (snapshot.isEmpty() || snapshot.get().phase() != LeaderPhase.PARRYABLE) {
                                continue;
                        }
                        OptionalDouble progress = dangerProgress(entity, snapshot.get());
                        if (progress.isEmpty()) {
                                continue;
                        }
                        visibleWarnings.add(entity.getUUID());
                        if (!ACTIVE_WARNINGS.contains(entity.getUUID())) {
                                playWarningSound(minecraft, entity);
                        }
                        if (snapshot.get().profile() == LeaderProfile.EXTERNAL) {
                                createExternalIndicatorRing(minecraft, entity, (float) progress.getAsDouble());
                        }
                }
                ACTIVE_WARNINGS.retainAll(visibleWarnings);
                ACTIVE_WARNINGS.addAll(visibleWarnings);
        }

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || minecraft.player.distanceToSqr(entity) > MAX_RENDER_DISTANCE_SQUARED) {
            return;
        }

        Optional<LeaderSnapshot> snapshot = LeaderApi.getSnapshot(entity);
        if (snapshot.isEmpty() || snapshot.get().phase() == LeaderPhase.NORMAL) {
            return;
        }

        LeaderSnapshot state = snapshot.get();
        boolean danger = state.phase() == LeaderPhase.PARRYABLE;
        OptionalDouble dangerProgress = danger
                ? dangerProgress(entity, state)
                : OptionalDouble.empty();
        if (danger && dangerProgress.isEmpty()) {
            return;
        }
        Component character = danger ? DANGER_CHARACTER : STUN_CHARACTER;
        int color = danger
                ? LeaderIndicatorVisuals.dangerArgb((float) dangerProgress.getAsDouble())
                : LeaderIndicatorVisuals.STUN_COLOR;
        float pulse = danger
                ? 1.0F + 0.08F * Mth.sin((entity.tickCount + event.getPartialTick()) * 0.45F)
                : 1.0F;
        double bob = danger
                ? 0.0
                : 0.08 * Mth.sin((entity.tickCount + event.getPartialTick()) * 0.2F);

        renderCharacter(
                event.getPoseStack(),
                event.getMultiBufferSource(),
                entity,
                character,
                color,
                pulse,
                bob);
    }

    private static OptionalDouble dangerProgress(LivingEntity entity, LeaderSnapshot snapshot) {
        if (snapshot.profile() == LeaderProfile.EXTERNAL) {
            return ClientLeaderIndicatorApi.getExternalWarningProgress(entity);
        }
        int remainingTicks = snapshot.remainingTicks().orElse(
                LeaderIndicatorVisuals.MANAGED_WARNING_DURATION_TICKS);
        return OptionalDouble.of(LeaderIndicatorVisuals.dangerProgress(remainingTicks));
    }

    private static void playWarningSound(Minecraft minecraft, LivingEntity entity) {
        minecraft.level.playLocalSound(
                entity.getX(),
                entity.getY() + entity.getBbHeight() * 0.5,
                entity.getZ(),
                SoundEvents.NOTE_BLOCK_BELL.get(),
                SoundSource.HOSTILE,
                1.0F,
                1.0F,
                false);
    }

    private static void createExternalIndicatorRing(Minecraft minecraft, LivingEntity entity,
                                                     float progress) {
        Vector3f color = LeaderIndicatorVisuals.dangerColor(progress);
        DustParticleOptions options = new DustParticleOptions(color, 1.0F);
        double centerY = entity.getY() + entity.getBoundingBox().getYsize() * 0.5;
        List<Particle> particles = new ArrayList<>(32);
        for (int index = 0; index < 16; index++) {
            double angle = Math.PI * 2.0 * index / 16.0;
            double x = entity.getX() + Math.cos(angle) * 2.0;
            double z = entity.getZ() + Math.sin(angle) * 2.0;
            for (int copy = 0; copy < 2; copy++) {
                Particle particle = minecraft.particleEngine.createParticle(
                        options, x, centerY, z, 0.0, 0.0, 0.0);
                if (particle != null) {
                    particle.setColor(color.x(), color.y(), color.z());
                    particle.setLifetime(2);
                    particles.add(particle);
                }
            }
        }
        if (!particles.isEmpty()) {
            ACTIVE_EXTERNAL_RINGS.put(entity.getId(), particles);
        }
    }

    private static void clearExternalIndicatorRings() {
        for (List<Particle> particles : ACTIVE_EXTERNAL_RINGS.values()) {
            for (Particle particle : particles) {
                particle.remove();
            }
        }
        ACTIVE_EXTERNAL_RINGS.clear();
    }

    private static void renderCharacter(PoseStack poseStack, MultiBufferSource buffers,
                                        LivingEntity entity,
                                        Component character, int color,
                                        float pulse, double bob) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();

        poseStack.pushPose();
        poseStack.translate(0.0, entity.getBbHeight() + 1.05 + bob, 0.0);
        poseStack.mulPose(dispatcher.cameraOrientation());
        float scale = 0.055F * pulse;
        poseStack.scale(-scale, -scale, scale);
        float x = -font.width(character) / 2.0F;
        font.drawInBatch(
                character,
                x,
                -font.lineHeight / 2.0F,
                color,
                false,
                poseStack.last().pose(),
                buffers,
                Font.DisplayMode.NORMAL,
                TEXT_BACKGROUND,
                FULL_BRIGHT);
        poseStack.popPose();
    }
}