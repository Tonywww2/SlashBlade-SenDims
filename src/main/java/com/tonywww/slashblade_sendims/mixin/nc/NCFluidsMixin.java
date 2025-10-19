package com.tonywww.slashblade_sendims.mixin.nc;

import com.tonywww.slashblade_sendims.blocks.AcidNCFluidBlock;
import igentuman.nc.NuclearCraft;
import igentuman.nc.block.NCFluidBlock;
import igentuman.nc.fluid.AcidDefinition;
import igentuman.nc.setup.registration.NCFluids;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = NCFluids.class)
public class NCFluidsMixin {

    @Redirect(method = "acids()V",
            at = @At(
                    value = "NEW",
                    target = "(Ljava/lang/String;I)Ligentuman/nc/fluid/AcidDefinition;"
            ),
            remap = false)
    private static AcidDefinition redirectSulfuricAcid(String name, int color) {
        if ("sulfuric_acid".equals(name)) {
            return new AcidDefinition(name, 0xCCA7A340);
        }
        return new AcidDefinition(name, color);
    }

    @Mixin(value = NCFluids.FluidEntry.class)
    private static class FluidEntryMixin {

        @Unique
        private static ResourceLocation slashBlade_SenDims$ACID = ResourceLocation.fromNamespaceAndPath(NuclearCraft.MODID, "sulfuric_acid");

        @Redirect(method = "lambda$make$5",
                at = @At(
                        value = "NEW",
                        target = "(Ligentuman/nc/setup/registration/NCFluids$FluidEntry;Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;)Ligentuman/nc/block/NCFluidBlock;"
                ),
                remap = false)
        private static NCFluidBlock redirectNCFluidBlock(NCFluids.FluidEntry fluidEntry, BlockBehaviour.Properties properties) {
            if (fluidEntry.still().getId().equals(slashBlade_SenDims$ACID)) {
                return new AcidNCFluidBlock(fluidEntry, properties);
            }

            return new NCFluidBlock(fluidEntry, properties);
        }
    }

}
