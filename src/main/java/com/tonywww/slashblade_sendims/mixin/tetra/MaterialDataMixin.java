package com.tonywww.slashblade_sendims.mixin.tetra;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tonywww.slashblade_sendims.utils.IMaterialData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import se.mickelus.tetra.module.data.MaterialData;

import java.lang.reflect.Type;

@Mixin(MaterialData.class)
public class MaterialDataMixin implements IMaterialData {
    @Unique
    private float slashBlade_SenDims$countFactor = 1;

    @Override
    public float slashBlade_SenDims$getCountFactor() {
        return slashBlade_SenDims$countFactor;
    }

    @Override
    public void slashBlade_SenDims$setCountFactor(float factor) {
        slashBlade_SenDims$countFactor = factor;
    }

    @Mixin(MaterialData.Deserializer.class)
    public static class DeserializerMixin {
        @Inject(method = "deserialize(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;Lcom/google/gson/JsonDeserializationContext;)Lse/mickelus/tetra/module/data/MaterialData;", at = @At("RETURN"), remap = false)
        private void injectCountFactor(JsonElement json, Type typeOfT, JsonDeserializationContext context, CallbackInfoReturnable<MaterialData> cir) {
            JsonObject jsonObject = json.getAsJsonObject();
            if (jsonObject.has("countFactor")) {
                IMaterialData.cast(cir.getReturnValue())
                        .slashBlade_SenDims$setCountFactor(jsonObject.get("countFactor").getAsFloat());
            }
        }
    }
}
