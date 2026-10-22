package com.tonywww.slashblade_sendims.client.startup;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.tonywww.slashblade_sendims.startup.BoundedPreparation;
import com.tonywww.slashblade_sendims.startup.StartupSettings;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Keeps vanilla parsing/pack order, but allocates O(workers) futures instead of one per file. */
public final class ModelInputBatches {
    private static final Logger LOGGER = LogManager.getLogger("SenDims Startup");
    private ModelInputBatches() {}

    public static CompletableFuture<Map<ResourceLocation, BlockModel>> models(ResourceManager resources, Executor executor) {
        return CompletableFuture.supplyAsync(() -> new ArrayList<>(ModelBakery.MODEL_LISTER.listMatchingResources(resources).entrySet()), executor)
                .thenCompose(inputs -> BoundedPreparation.map(inputs.size(), StartupSettings.INSTANCE.modelWorkers, executor, index -> {
                    var input = inputs.get(index);
                    try (Reader reader = input.getValue().openAsReader()) {
                        return Pair.of(input.getKey(), BlockModel.fromStream(reader));
                    } catch (Exception error) {
                        LOGGER.error("Failed to load model {}", input.getKey(), error); return null;
                    }
                })).thenApply(ModelInputBatches::map);
    }

    public static CompletableFuture<Map<ResourceLocation, List<ModelBakery.LoadedJson>>> states(ResourceManager resources, Executor executor) {
        return CompletableFuture.supplyAsync(() -> new ArrayList<>(ModelBakery.BLOCKSTATE_LISTER.listMatchingResourceStacks(resources).entrySet()), executor)
                .thenCompose(inputs -> BoundedPreparation.map(inputs.size(), StartupSettings.INSTANCE.modelWorkers, executor, index -> {
                    var input = inputs.get(index);
                    List<ModelBakery.LoadedJson> decoded = new ArrayList<>(input.getValue().size());
                    for (Resource resource : input.getValue()) {
                        try (Reader reader = resource.openAsReader()) {
                            JsonObject json = GsonHelper.parse(reader);
                            decoded.add(new ModelBakery.LoadedJson(resource.sourcePackId(), json));
                        } catch (Exception error) {
                            LOGGER.error("Failed to load blockstate {} from pack {}", input.getKey(), resource.sourcePackId(), error);
                        }
                    }
                    return Pair.of(input.getKey(), decoded);
                })).thenApply(ModelInputBatches::map);
    }

    private static <T> Map<ResourceLocation, T> map(List<Pair<ResourceLocation, T>> values) {
        Map<ResourceLocation, T> result = new HashMap<>(values.size());
        for (Pair<ResourceLocation, T> pair : values) if (pair != null) result.put(pair.getFirst(), pair.getSecond());
        return Map.copyOf(result);
    }
}
