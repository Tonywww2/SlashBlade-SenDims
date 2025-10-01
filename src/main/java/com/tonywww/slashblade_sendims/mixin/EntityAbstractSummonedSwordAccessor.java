package com.tonywww.slashblade_sendims.mixin;

import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityAbstractSummonedSword.class)
public interface EntityAbstractSummonedSwordAccessor {
    @Accessor(value = "inGround", remap = false)
    boolean getInGround();

    @Accessor(value = "inGround", remap = false)
    void setInGround(boolean value);

    @Accessor(value = "inBlockState", remap = false)
    BlockState getInBlockState();

    @Accessor(value = "ticksInAir", remap = false)
    int getTicksInAir();

    @Accessor(value = "ticksInAir", remap = false)
    void setTicksInAir(int value);

    @Accessor(value = "ticksInGround", remap = false)
    int getTicksInGround();
}