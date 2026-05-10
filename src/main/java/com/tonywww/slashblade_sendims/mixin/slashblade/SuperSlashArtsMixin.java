package com.tonywww.slashblade_sendims.mixin.slashblade;

import com.tonywww.slashblade_sendims.events.SuperSlashArtsReleaseEvent;
import mods.flammpfeil.slashblade.ability.SuperSlashArts;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SuperSlashArts.class, remap = false)
public class SuperSlashArtsMixin {
    @Inject(method = "releaseSSA(Lnet/minecraft/server/level/ServerPlayer;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onReleaseSSA(ServerPlayer entity, CallbackInfo ci) {
        if (MinecraftForge.EVENT_BUS.post(new SuperSlashArtsReleaseEvent(entity))) {
            ci.cancel();
        }
    }
}

