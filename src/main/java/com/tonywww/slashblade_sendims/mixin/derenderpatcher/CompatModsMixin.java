package com.tonywww.slashblade_sendims.mixin.derenderpatcher;

import com.tonywww.slashblade_sendims.performance.LoadedModSnapshot;
import com.tonywww.slashblade_sendims.performance.ModLookupCache;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.derenderpatcher.compat.CompatMods", remap = false)
public abstract class CompatModsMixin {
    @Inject(method = "isModLoaded", at = @At("HEAD"), cancellable = true)
    private static void sdbf$loaded(String id, CallbackInfoReturnable<Boolean> callback) {
        LoadedModSnapshot snapshot = ModLookupCache.snapshot();
        if (snapshot != null) {
            callback.setReturnValue(snapshot.isLoaded(id));
        }
    }

    @Inject(method = "getModVersion", at = @At("HEAD"), cancellable = true)
    private static void sdbf$version(String id, CallbackInfoReturnable<String> callback) {
        LoadedModSnapshot snapshot = ModLookupCache.snapshot();
        if (snapshot != null) {
            callback.setReturnValue(snapshot.version(id));
        }
    }

    @Inject(method = "getForgeModList", at = @At("HEAD"), cancellable = true)
    private static void sdbf$modList(CallbackInfoReturnable<Object> callback) {
        if (ModLookupCache.snapshot() != null) {
            callback.setReturnValue(ModList.get());
        }
    }
}
