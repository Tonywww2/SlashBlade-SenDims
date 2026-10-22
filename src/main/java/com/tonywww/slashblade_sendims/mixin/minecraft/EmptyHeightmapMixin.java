package com.tonywww.slashblade_sendims.mixin.minecraft;

import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Set;

/** Chunk reloads commonly have every heightmap already saved. Vanilla still scans an empty set. */
@Mixin(Heightmap.class)
public abstract class EmptyHeightmapMixin {
    @Inject(method="primeHeightmaps",at=@At("HEAD"),cancellable=true)
    private static void sdbf$skipEmptyHeightmaps(ChunkAccess chunk,Set<Heightmap.Types> types,CallbackInfo ci){
        if(types.isEmpty())ci.cancel();
    }
}
