package com.tonywww.slashblade_sendims.mixin.minecraft;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/** 资源重载时整张表被替换，每次读取都拿当前这张。 */
@Mixin(BlockEntityRenderDispatcher.class)
public interface BlockEntityRenderDispatcherAccessor {
    @Accessor("renderers")
    Map<BlockEntityType<?>, BlockEntityRenderer<?>> sdbf$renderers();
}
