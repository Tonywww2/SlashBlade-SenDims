package com.tonywww.slashblade_sendims.client.leader;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.api.leader.LeaderApi;
import com.tonywww.slashblade_sendims.api.leader.LeaderPhase;
import com.tonywww.slashblade_sendims.api.leader.LeaderProfile;
import com.tonywww.slashblade_sendims.api.leader.LeaderSnapshot;
import com.tonywww.slashblade_sendims.leader.LeaderIndicatorVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = SenDims.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LeaderStatusCharacterRenderer {
    private static final Component DANGER_CHARACTER = Component.literal("危");
    private static final Component STUN_CHARACTER = Component.literal("晕");
    private static final int TEXT_BACKGROUND = 0x78000000;
    private static final int FULL_BRIGHT = 0x00F000F0;
    private static final double MAX_RENDER_DISTANCE_SQUARED = 96.0 * 96.0;

    private LeaderStatusCharacterRenderer() {
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
        if (danger && state.profile() != LeaderProfile.MANAGED) {
            return;
        }
        int remainingTicks = state.remainingTicks().orElse(
                LeaderIndicatorVisuals.MANAGED_WARNING_DURATION_TICKS);
        Component character = danger ? DANGER_CHARACTER : STUN_CHARACTER;
        int color = danger
                ? LeaderIndicatorVisuals.dangerArgb(remainingTicks)
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