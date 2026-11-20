package com.tonywww.slashblade_sendims.mixin.twilightforest;

import com.tonywww.slashblade_sendims.twilight.TwilightProgression;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.init.custom.Enforcement;

/** The acid rain biome penalty also gives Slowness and Mining Fatigue; it keeps its own ID so the rain still renders. */
@Mixin(value = Enforcement.class, remap = false)
public abstract class EnforcementMixin {
    @Inject(method = "enforceBiomeProgression", at = @At("HEAD"))
    private static void sendims$acidRainExtras(Player player, ServerLevel level, CallbackInfo ci) {
        TwilightProgression.applyAcidRainExtras(player, level);
    }
}
