package com.tonywww.slashblade_sendims.mixin;

import com.tonywww.slashblade_sendims.SBSDValues;
import com.tonywww.slashblade_sendims.utils.UmaUtils;
import mods.flammpfeil.slashblade.ability.SlayerStyleArts;
import mods.flammpfeil.slashblade.ability.Untouchable;
import mods.flammpfeil.slashblade.util.InputCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.EnumSet;

@Mixin(SlayerStyleArts.class)
public class SlayerStyleArtsMixin {

    @Inject(method = "handleForwardSprintSneak(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/Level;)Z", at = @At("HEAD"), cancellable = true, remap = false)
    private void injectHandleForwardSprintSneak(ServerPlayer sender, Level worldIn, CallbackInfoReturnable<Boolean> cir) {
        if (!UmaUtils.checkSprint(sender)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "handleBackSprintSneak(Lnet/minecraft/server/level/ServerPlayer;)Z", at = @At("HEAD"), cancellable = true, remap = false)
    private void injectHandleBackSprintSneak(ServerPlayer sender, CallbackInfoReturnable<Boolean> cir) {
        if (!UmaUtils.checkSprint(sender)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "handleSprintMove(Lnet/minecraft/server/level/ServerPlayer;Ljava/util/EnumSet;)Z", at = @At("HEAD"), cancellable = true, remap = false)
    private void injectHandleSprintMove(ServerPlayer sender, EnumSet<InputCommand> current, CallbackInfoReturnable<Boolean> cir) {
        if (!UmaUtils.checkSprint(sender)) {
            cir.setReturnValue(true);
        }
    }

    @Redirect(
            method = "applyBasicTrickEffects(Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lmods/flammpfeil/slashblade/ability/Untouchable;setUntouchable(Lnet/minecraft/world/entity/LivingEntity;I)V"
            ),
            remap = false
    )
    private void redirectBasicTrickUntouchable(LivingEntity entity, int ticks) {
        Untouchable.setUntouchable(entity, SBSDValues.UNTOUCHABLE_TICK);
    }

    @Redirect(
            method = "prepareTeleportEffects(Lnet/minecraft/world/entity/Entity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lmods/flammpfeil/slashblade/ability/Untouchable;setUntouchable(Lnet/minecraft/world/entity/LivingEntity;I)V"
            ),
            remap = false
    )
    private static void redirectTeleportUntouchable(LivingEntity entity, int ticks) {
        Untouchable.setUntouchable(entity, SBSDValues.UNTOUCHABLE_TICK);
    }


}