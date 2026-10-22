package com.tonywww.slashblade_sendims.mixin.startup;

import com.tonywww.slashblade_sendims.client.startup.ParallelObjReload;
import com.tonywww.slashblade_sendims.startup.StartupSettings;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import mods.flammpfeil.slashblade.client.renderer.event.ModelResourceLoader;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ModelResourceLoader.class)
public abstract class SlashBladeReloadMixin {
    @Inject(method = "reload", at = @At("HEAD"), cancellable = true)
    private void sendims$parallelReload(PreparableReloadListener.PreparationBarrier barrier, ResourceManager resources,
                                        ProfilerFiller preparationProfiler, ProfilerFiller applicationProfiler,
                                        Executor preparation, Executor application, CallbackInfoReturnable<CompletableFuture<Void>> callback) {
        if (StartupSettings.INSTANCE.parallelObjPreparation) {
            callback.setReturnValue(ParallelObjReload.reload(barrier, resources, preparation, application));
        }
    }
}
