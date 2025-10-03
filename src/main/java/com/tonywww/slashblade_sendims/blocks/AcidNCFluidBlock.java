package com.tonywww.slashblade_sendims.blocks;

import com.tonywww.slashblade_sendims.registeries.SBSDParticles;
import igentuman.nc.block.NCFluidBlock;
import igentuman.nc.setup.registration.NCFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class AcidNCFluidBlock extends NCFluidBlock {
    public AcidNCFluidBlock(NCFluids.FluidEntry entry, Properties props) {
        super(entry, props);
    }

    @Override
    public void animateTick(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, RandomSource randomSource) {
        if (randomSource.nextInt(400) == 0) {
            level.playLocalSound((double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D,
                    SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT,
                    SoundSource.BLOCKS,
                    0.5F, randomSource.nextFloat() * 0.4F + 0.8F, false);
        }
        boolean top = level.getFluidState(pos.above()).isEmpty();
        if (randomSource.nextInt(top ? 30 : 100) == 0) {
            float height = top ? state.getFluidState().getHeight(level, pos) : randomSource.nextFloat();
            level.addParticle(SBSDParticles.ACID_BUBBLE.get(),
                    pos.getX() + randomSource.nextFloat(), pos.getY() + height, pos.getZ() + randomSource.nextFloat(),
                    (randomSource.nextFloat() - 0.5F) * 0.1F, 0.05F + randomSource.nextFloat() * 0.1F,
                    (randomSource.nextFloat() - 0.5F) * 0.1F);
        }
    }
}
