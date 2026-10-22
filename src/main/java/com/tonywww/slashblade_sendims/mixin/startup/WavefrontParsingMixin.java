package com.tonywww.slashblade_sendims.mixin.startup;

import com.tonywww.slashblade_sendims.client.startup.ObjLoadStats;
import com.tonywww.slashblade_sendims.client.startup.ObjModelAdapter;
import com.tonywww.slashblade_sendims.client.startup.ObjParserBridge;
import com.tonywww.slashblade_sendims.startup.ObjMeshData;
import com.tonywww.slashblade_sendims.startup.StartupSettings;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import mods.flammpfeil.slashblade.client.renderer.model.obj.ModelFormatException;
import mods.flammpfeil.slashblade.client.renderer.model.obj.WavefrontObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WavefrontObject.class, remap = false)
public abstract class WavefrontParsingMixin {
    @Invoker("loadObjModel")
    protected abstract void sendims$originalParse(InputStream input);

    @Inject(method = "loadObjModel", at = @At("HEAD"), cancellable = true)
    private void sendims$parse(InputStream input, CallbackInfo callback) {
        StartupSettings settings = StartupSettings.INSTANCE;
        if (!settings.objParser || ObjParserBridge.BYPASS.get()) return;
        try (input) {
            byte[] source = input.readNBytes(settings.objMaxSourceBytes + 1);
            if (source.length <= settings.objMaxSourceBytes) {
                try {
                    ObjModelAdapter.apply(ObjParserBridge.parse(source), (WavefrontObject) (Object) this);
                    callback.cancel(); return;
                } catch (ObjMeshData.UnsupportedSyntax unsupported) {
                    // Original input and original parser provide exact compatibility/error behavior.
                }
            }
            ObjLoadStats.FALLBACKS.increment();
            synchronized (ObjParserBridge.UPSTREAM_LOCK) {
                ObjParserBridge.BYPASS.set(true);
                try { sendims$originalParse(new SequenceInputStream(new ByteArrayInputStream(source), input)); }
                finally { ObjParserBridge.BYPASS.remove(); }
            }
            callback.cancel();
        } catch (IOException error) {
            throw new ModelFormatException("IO Exception reading model format", error);
        }
    }
}
