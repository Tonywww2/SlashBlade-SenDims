package com.tonywww.slashblade_sendims.client;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.sa.NamelessThreefold;
import com.tonywww.slashblade_sendims.se.NamelessSeries;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.compat.playerAnim.PlayerAnimationOverrider;
import mods.flammpfeil.slashblade.compat.playerAnim.VmdAnimation;
import mods.flammpfeil.slashblade.event.BladeMotionEvent;
import mods.flammpfeil.slashblade.event.client.UserPoseOverrider;
import mods.flammpfeil.slashblade.registry.combo.ComboState;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Body motion and the Last Smith style full turn, restricted to the client. */
@Mod.EventBusSubscriber(modid = SenDims.MOD_ID, value = Dist.CLIENT)
public final class NamelessArtClient {
    private static final String POSE_ACTIVE = "sbsd.nameless.pursuit_pose";

    @SubscribeEvent
    public static void renderTurn(RenderLivingEvent.Pre<?, ?> event) {
        // SlashBlade registers the body pose renderer only without Player Animator.
        // Render this skill's turn explicitly when that library supplies the limb motion.
        if (!UserPoseOverrider.UsePoseOverrider
                && event.getEntity().getPersistentData().getBoolean(POSE_ACTIVE)) {
            UserPoseOverrider.getInstance().onRenderPlayerEventPre(event);
        }
    }

    @SubscribeEvent
    public static void motion(BladeMotionEvent event) {
        if (!event.getEntity().level().isClientSide()) return;
        var data = event.getEntity().getPersistentData();
        if (NamelessThreefold.PURSUIT_ID.equals(event.getCombo())) {
            UserPoseOverrider.resetRot(event.getEntity());
            data.putBoolean(POSE_ACTIVE, true);
        } else if (data.getBoolean(POSE_ACTIVE)) {
            UserPoseOverrider.resetRot(event.getEntity());
            data.remove(POSE_ACTIVE);
        }
    }

    @SubscribeEvent
    public static void pose(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !event.player.level().isClientSide()) return;
        var data = event.player.getPersistentData();
        if (!data.getBoolean(POSE_ACTIVE)) return;
        var state = NamelessSeries.getBladeState(event.player);
        if (state == null || !NamelessThreefold.PURSUIT_ID.equals(state.getComboSeq())) {
            UserPoseOverrider.resetRot(event.player);
            data.remove(POSE_ACTIVE);
            return;
        }
        long elapsed = ComboState.getElapsed(event.player);
        if (elapsed >= 1 && elapsed <= 5) {
            UserPoseOverrider.setRot(event.player, elapsed * 72.0F, false);
        } else {
            UserPoseOverrider.resetRot(event.player);
        }
    }

    @Mod.EventBusSubscriber(modid = SenDims.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Setup {
        @SubscribeEvent
        public static void animations(FMLClientSetupEvent event) {
            if (!ModList.get().isLoaded("playeranimator")) return;
            event.enqueueWork(() -> {
                ResourceLocation motion = new ResourceLocation(SlashBlade.MODID, "model/pa/player_motion.vmd");
                var animations = PlayerAnimationOverrider.getInstance().getAnimation();
                animations.put(NamelessThreefold.ID, new VmdAnimation(motion, 400, 451, false));
                animations.put(NamelessThreefold.PURSUIT_ID, new VmdAnimation(motion, 918, 957, false));
            });
        }
    }
}
