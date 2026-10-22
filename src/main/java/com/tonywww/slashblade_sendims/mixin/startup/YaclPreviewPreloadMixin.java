package com.tonywww.slashblade_sendims.mixin.startup;

import com.tonywww.slashblade_sendims.startup.ConfigImagePolicy;
import com.tonywww.slashblade_sendims.startup.StartupSettings;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Pseudo
@Mixin(targets = "dev.isxander.yacl3.gui.image.YACLImageReloadListener", remap = false)
public abstract class YaclPreviewPreloadMixin {
    @Unique private final AtomicBoolean sendims$initialReload = new AtomicBoolean(true);

    @ModifyArg(method = "prepare", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/resources/ResourceManager;listResources(Ljava/lang/String;Ljava/util/function/Predicate;)Ljava/util/Map;", remap = true), index = 1, require = 0, remap = false)
    private Predicate<ResourceLocation> sendims$deferConfigPreviews(Predicate<ResourceLocation> original) {
        if (!StartupSettings.INSTANCE.lazyConfigImages || !sendims$initialReload.getAndSet(false)) return original;
        // YACL's registerOrGetImage already prepares non-preloaded images asynchronously on demand.
        // Subsequent reloads use upstream preload behavior, preserving resource-pack changes.
        return location -> original.test(location) && !ConfigImagePolicy.defer(location.getNamespace(), location.getPath());
    }
}
