package com.tonywww.slashblade_sendims.mixin.minecraft;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = NoiseBasedChunkGenerator.class)
public class NoiseBasedChunkGeneratorMixin {
    @Inject(method = "createFluidPicker", at = @At("HEAD"), cancellable = true)
    private static void modifyCreateFluidPicker(NoiseGeneratorSettings settings, CallbackInfoReturnable<Aquifer.FluidPicker> cir) {
        Aquifer.FluidStatus lavaFluid = new Aquifer.FluidStatus(-54, Blocks.LAVA.defaultBlockState());
        int i = settings.seaLevel();
        Aquifer.FluidStatus defaultFluid = new Aquifer.FluidStatus(i, settings.defaultFluid());
        Aquifer.FluidStatus airFluid = new Aquifer.FluidStatus(DimensionType.MIN_Y * 2, Blocks.AIR.defaultBlockState());

        if (settings.defaultBlock() == Blocks.DEEPSLATE.defaultBlockState()) {
            Aquifer.FluidPicker modify = (x, y, z) -> defaultFluid;
            cir.setReturnValue(modify);
        } else {
            cir.setReturnValue((x, y, z) -> {
                return y < Math.min(-54, i) ? lavaFluid : defaultFluid;
            });
        }
    }

}