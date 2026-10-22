package com.tonywww.slashblade_sendims.mixin.startup;

import com.tonywww.slashblade_sendims.client.startup.ModelInputBatches;
import com.tonywww.slashblade_sendims.startup.StartupSettings;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ModelManager.class, priority = 1100)
public abstract class ModelInputBatchesMixin {
    @Inject(method = "loadBlockModels", at = @At("HEAD"), cancellable = true)
    private static void sendims$models(ResourceManager resources, Executor executor,
                                       CallbackInfoReturnable<CompletableFuture<Map<ResourceLocation, BlockModel>>> callback) {
        if (StartupSettings.INSTANCE.modelInputBatches && !Boolean.getBoolean("lightspeed.modelInputSnapshot")) {
            callback.setReturnValue(ModelInputBatches.models(resources, executor));
        }
    }

    @Inject(method = "loadBlockStates", at = @At("HEAD"), cancellable = true)
    private static void sendims$states(ResourceManager resources, Executor executor,
                                       CallbackInfoReturnable<CompletableFuture<Map<ResourceLocation, List<ModelBakery.LoadedJson>>>> callback) {
        if (StartupSettings.INSTANCE.modelInputBatches && !Boolean.getBoolean("lightspeed.modelInputSnapshot")) {
            callback.setReturnValue(ModelInputBatches.states(resources, executor));
        }
    }
}
