package com.tonywww.slashblade_sendims.client.startup;

import com.tonywww.slashblade_sendims.startup.ScopedIdentityCache;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public final class ModelLocationMemo {
    public static final ScopedIdentityCache<BlockState, ResourceLocation, ModelResourceLocation> CACHE = new ScopedIdentityCache<>();
    private ModelLocationMemo() {}
}
