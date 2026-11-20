package com.tonywww.slashblade_sendims.mixin.twilightforest;

import com.tonywww.slashblade_sendims.twilight.FinalCastleTeleports;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.PortalInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import twilightforest.world.TFTeleporter;

import java.util.function.Function;

/** Entering the Twilight Forest lands in the nearest Final Castle; its portal leads each player back where they came from. */
@Mixin(value = TFTeleporter.class, remap = false)
public abstract class TFTeleporterCastleMixin {
    @Inject(method = "getPortalInfo", at = @At("HEAD"), cancellable = true)
    private void sendims$finalCastlePortal(Entity entity, ServerLevel dest, Function<ServerLevel, PortalInfo> defaultPortalInfo,
                                           CallbackInfoReturnable<PortalInfo> cir) {
        PortalInfo into = FinalCastleTeleports.intoCastle(entity, dest);
        if (into != null) {
            cir.setReturnValue(into);
            return;
        }
        FinalCastleTeleports.Exit exit = FinalCastleTeleports.outOfCastle(entity, dest);
        if (exit != null) {
            cir.setReturnValue(exit.info());
        }
    }
}
