package com.tonywww.slashblade_sendims.mixin.minecraft;

import com.tonywww.slashblade_sendims.worldgen.DeepRealmPillarBlocks;
import net.minecraft.world.level.block.Block;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** 深境1层的大型滴水石直接放置深板岩（见 DeepRealmPillarBlocks）。 */
@Mixin(targets = "net.minecraft.world.level.levelgen.feature.LargeDripstoneFeature$LargeDripstone")
public abstract class LargeDripstonePillarBlockMixin {
    @Redirect(method = "placeBlocks", at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/level/block/Blocks;DRIPSTONE_BLOCK:Lnet/minecraft/world/level/block/Block;",
            opcode = Opcodes.GETSTATIC), require = 1)
    private Block sdbf$pillarBlock() {
        return DeepRealmPillarBlocks.largeDripstoneBlock();
    }
}
