package com.tonywww.slashblade_sendims.mixin.ftbchunks;

import dev.architectury.event.EventResult;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 地图改由 Xaero 负责，FTB Chunks 客户端只保留领地数据（ftbxaerocompat 从这里读取）。
 * 不再扫描区块、不生成和保存地图图像，也不画小地图、大地图、世界内路径点，不响应地图相关按键。
 */
@Pseudo
@Mixin(targets = "dev.ftb.mods.ftbchunks.client.FTBChunksClient", remap = false)
public abstract class FTBChunksClientMixin {
    @Inject(method = {
            "handlePacket(Lnet/minecraft/network/protocol/game/ClientboundLevelChunkWithLightPacket;)V",
            "handlePacket(Lnet/minecraft/network/protocol/game/ClientboundSectionBlocksUpdatePacket;)V",
            "handlePacket(Lnet/minecraft/network/protocol/game/ClientboundBlockUpdatePacket;)V",
            "rerender",
            "runRerenderTasks",
            "syncRegionFromServer",
            "renderWorldLast",
            "openGui",
            "handlePlayerDeath"
    }, at = @At("HEAD"), cancellable = true, remap = false, require = 9)
    private void sdbf$noMapping(CallbackInfo ci) {
        ci.cancel();
    }

    /**
     * renderHud 开头的维度切换检查要保留：MapDimension.getCurrent() 只在这里随维度更新，ftbxaerocompat 靠它显示当前维度的领地。
     * 之后的小地图贴图生成和绘制全部跳过。
     */
    @Inject(method = "renderHud", at = @At(value = "FIELD",
            target = "Ldev/ftb/mods/ftbchunks/client/FTBChunksClient;minimapTextureId:I",
            opcode = Opcodes.GETFIELD, ordinal = 0), cancellable = true, remap = false, require = 1)
    private void sdbf$noMinimap(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = {
            "keyPressed(Lnet/minecraft/client/Minecraft;IIII)Ldev/architectury/event/EventResult;",
            "keyPressed(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/screens/Screen;III)Ldev/architectury/event/EventResult;"
    }, at = @At("HEAD"), cancellable = true, remap = false, require = 2)
    private void sdbf$noMapKeys(CallbackInfoReturnable<EventResult> cir) {
        cir.setReturnValue(EventResult.pass());
    }
}
