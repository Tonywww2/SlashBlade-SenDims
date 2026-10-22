package com.tonywww.slashblade_sendims.mixin.startup;

import com.tonywww.slashblade_sendims.client.startup.ModelLocationMemo;
import com.tonywww.slashblade_sendims.startup.StartupSettings;
import java.util.ArrayDeque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ModelManager.class)
public abstract class ModelReloadScopeMixin {
    @Unique private static final ThreadLocal<ArrayDeque<Long>> sendims$tokens = ThreadLocal.withInitial(ArrayDeque::new);

    @Inject(method = "reload", at = @At("HEAD"))
    private void sendims$begin(PreparableReloadListener.PreparationBarrier barrier, ResourceManager resources,
                               ProfilerFiller prepareProfiler, ProfilerFiller applyProfiler, Executor prepare, Executor apply,
                               CallbackInfoReturnable<CompletableFuture<Void>> callback) {
        if (StartupSettings.INSTANCE.modelLocationCache) {
            sendims$tokens.get().push(ModelLocationMemo.CACHE.begin(StartupSettings.INSTANCE.modelLocationEntries));
        }
    }

    @Inject(method = "reload", at = @At("RETURN"), cancellable = true)
    private void sendims$end(PreparableReloadListener.PreparationBarrier barrier, ResourceManager resources,
                             ProfilerFiller prepareProfiler, ProfilerFiller applyProfiler, Executor prepare, Executor apply,
                             CallbackInfoReturnable<CompletableFuture<Void>> callback) {
        if (!StartupSettings.INSTANCE.modelLocationCache) return;
        ArrayDeque<Long> stack = sendims$tokens.get();
        long token = stack.pop();
        if (stack.isEmpty()) sendims$tokens.remove();
        callback.setReturnValue(callback.getReturnValue().whenComplete((ignored, error) -> ModelLocationMemo.CACHE.finish(token)));
    }
}
