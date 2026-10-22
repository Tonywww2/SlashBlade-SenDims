package com.tonywww.slashblade_sendims.mixin.startup;

import com.tonywww.slashblade_sendims.client.startup.ModelLocationMemo;
import com.tonywww.slashblade_sendims.startup.StartupSettings;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockModelShaper.class)
public abstract class ModelLocationMixin {
    @Inject(method = "stateToModelLocation(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/client/resources/model/ModelResourceLocation;", at = @At("HEAD"), cancellable = true)
    private static void sendims$reuseLocation(ResourceLocation id, BlockState state, CallbackInfoReturnable<ModelResourceLocation> callback) {
        if (!StartupSettings.INSTANCE.modelLocationCache) return;
        ModelResourceLocation found = ModelLocationMemo.CACHE.get(state, id);
        if (found != null) callback.setReturnValue(found);
    }

    @Inject(method = "stateToModelLocation(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/client/resources/model/ModelResourceLocation;", at = @At("RETURN"))
    private static void sendims$rememberLocation(ResourceLocation id, BlockState state, CallbackInfoReturnable<ModelResourceLocation> callback) {
        if (StartupSettings.INSTANCE.modelLocationCache) ModelLocationMemo.CACHE.put(state, id, callback.getReturnValue());
    }
}
