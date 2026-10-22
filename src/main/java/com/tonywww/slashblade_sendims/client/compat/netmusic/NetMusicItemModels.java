package com.tonywww.slashblade_sendims.client.compat.netmusic;

import com.tonywww.slashblade_sendims.client.performance.RenderResourceEpoch;
import java.lang.reflect.InvocationTargetException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;

/** Two render-thread-owned models, independent of lazy block renderer singletons. */
enum NetMusicItemModels {
    BIG_MEGAPHONE("ModelBigMegaphone"),
    MUSIC_PLAYER("ModelMusicPlayer");

    private final String modelClass;
    private Model model;
    private long modelEpoch = Long.MIN_VALUE;

    NetMusicItemModels(String name) {
        modelClass = "com.github.tartaricacid.netmusic.client.model." + name;
    }

    Model model() {
        long epoch = RenderResourceEpoch.current();
        if (model == null || modelEpoch != epoch) {
            try {
                // Optional-mod reflection runs only once per model/resource generation.
                Class<?> type = Class.forName(modelClass);
                ModelLayerLocation layer = (ModelLayerLocation) type.getField("LAYER").get(null);
                ModelPart root = Minecraft.getInstance().getEntityModels().bakeLayer(layer);
                model = (Model) type.getConstructor(ModelPart.class).newInstance(root);
                modelEpoch = epoch;
            } catch (InvocationTargetException failure) {
                throw new IllegalStateException("Cannot bake Net Music item model " + modelClass, failure.getCause());
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException("Unsupported Net Music item model API " + modelClass, failure);
            }
        }
        return model;
    }
}
