package com.tonywww.slashblade_sendims.mixin.minecraft;

import com.tonywww.slashblade_sendims.yakuman.YakumanStun;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class YakumanStunMixin {
    @Inject(method="travel",at=@At("HEAD"),cancellable=true)
    private void stopTravel(Vec3 motion,CallbackInfo ci) {
        if(YakumanStun.active((LivingEntity)(Object)this))ci.cancel();
    }
    @Mixin(Mob.class)
    public static abstract class Ai {
        @Inject(method="serverAiStep",at=@At("HEAD"),cancellable=true)
        private void stopAi(CallbackInfo ci) {
            if(YakumanStun.active((Mob)(Object)this))ci.cancel();
        }
    }
    @Mixin(ServerGamePacketListenerImpl.class)
    public static abstract class Movement {
        @Shadow public ServerPlayer player;
        @Inject(method="handleMovePlayer",at=@At("HEAD"),cancellable=true)
        private void stopMove(ServerboundMovePlayerPacket packet,CallbackInfo ci) {
            // The first invocation can be on Netty's thread. Vanilla schedules it again on the server thread.
            if(player.getServer()!=null && player.getServer().isSameThread() && YakumanStun.active(player))ci.cancel();
        }
    }
}
