package com.tonywww.slashblade_sendims.client.startup;

import com.tonywww.slashblade_sendims.startup.BoundedPreparation;
import com.tonywww.slashblade_sendims.startup.StartupSettings;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import mods.flammpfeil.slashblade.client.renderer.model.BladeModelManager;
import mods.flammpfeil.slashblade.client.renderer.model.obj.WavefrontObject;
import mods.flammpfeil.slashblade.init.DefaultResources;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ParallelObjReload {
    private static final Logger LOGGER = LogManager.getLogger("SenDims Startup");
    private record Input(ResourceLocation location, Resource resource) {}
    private record Prepared(ResourceLocation location, WavefrontObject model) {}
    private record Batch(List<Prepared> models, long preparationMillis) {}

    private ParallelObjReload() {}

    public static CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier barrier,
                                                 ResourceManager resources, Executor preparation, Executor application) {
        long start = System.nanoTime(), parsed = ObjLoadStats.PARSED.sum(), hits = ObjLoadStats.DISK_HITS.sum();
        return CompletableFuture.supplyAsync(() -> {
            Map<ResourceLocation, Resource> found = resources.listResources("model", id -> id.getPath().endsWith(".obj"));
            List<Input> inputs = new ArrayList<>(found.size() + 1);
            try { inputs.add(new Input(DefaultResources.resourceDefaultModel, resources.getResourceOrThrow(DefaultResources.resourceDefaultModel))); }
            catch (IOException error) { throw new CompletionException(error); }
            found.forEach((id, resource) -> { if (!id.equals(DefaultResources.resourceDefaultModel)) inputs.add(new Input(id, resource)); });
            return inputs;
        }, preparation).thenCompose(inputs -> BoundedPreparation.map(inputs.size(), StartupSettings.INSTANCE.objWorkers,
                preparation, index -> {
                    Input input = inputs.get(index);
                    try (var stream = input.resource().open()) {
                        return new Prepared(input.location(), new WavefrontObject(input.location().toString(), stream));
                    } catch (Exception error) {
                        if (index == 0) throw new CompletionException(error);
                        LOGGER.debug("Using default OBJ for {}", input.location(), error);
                        return new Prepared(input.location(), null);
                    }
                })).thenApply(models -> new Batch(models, (System.nanoTime() - start) / 1_000_000))
                .thenCompose(barrier::wait).thenAcceptAsync(batch -> {
            List<Prepared> models = batch.models();
            BladeModelManager target = BladeModelManager.getInstance();
            WavefrontObject fallback = models.get(0).model();
            target.cache.invalidateAll();
            target.defaultModel = fallback;
            for (Prepared model : models) target.cache.put(model.location(), model.model() == null ? fallback : model.model());
            LOGGER.info("OBJ reload: models={} workers={} parsed={} diskHits={} fallbackParses={} preparationMs={} totalWithBarrierMs={}", models.size(),
                    StartupSettings.INSTANCE.objWorkers, ObjLoadStats.PARSED.sum() - parsed, ObjLoadStats.DISK_HITS.sum() - hits,
                    ObjLoadStats.FALLBACKS.sum(), batch.preparationMillis(), (System.nanoTime() - start) / 1_000_000);
            ObjParserBridge.maintain();
        }, application);
    }
}
