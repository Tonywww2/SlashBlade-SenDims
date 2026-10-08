package com.tonywww.slashblade_sendims.mixin.adastra;

import earth.terrarium.adastra.common.systems.EnvironmentEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The belt's primordial ice preserves its plants and water without an oxygen scan or repair ticker. */
@Mixin(value=EnvironmentEffects.class,remap=false)
public abstract class AsteroidEnvironmentMixin {
    @Unique private static final ResourceLocation SEN_DIMS$BELT=new ResourceLocation("sdbf:asteroid_belt");
    @Inject(method="tickChunk",at=@At("HEAD"),cancellable=true,remap=false)
    private static void senDims$preserveEcology(ServerLevel level,LevelChunk chunk,CallbackInfo ci){
        if(level.dimension().location().equals(SEN_DIMS$BELT))ci.cancel();
    }
}
